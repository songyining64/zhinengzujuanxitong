<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>试卷管理</span>
        <span class="acts">
          <el-button type="primary" :disabled="!courseId" @click="openManual">手动组卷</el-button>
          <el-button type="success" :disabled="!courseId" @click="openAuto">自动组卷</el-button>
        </span>
      </div>
    </template>
    <div class="bar">
      <el-select v-model="courseId" placeholder="课程" filterable style="width: 260px" @change="onCourseChange">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button @click="load">刷新列表</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="mode" label="模式" width="88" />
      <el-table-column prop="totalScore" label="总分" width="88" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />

    <el-collapse class="collapse">
      <el-collapse-item title="组卷模板（快捷复用规则）" name="tpl">
        <div class="tpl-bar">
          <el-input v-model="tplName" placeholder="模板名称" style="width: 200px" />
          <el-button :disabled="!courseId" @click="saveTemplate">保存当前自动规则为模板</el-button>
        </div>
        <el-table :data="templates" size="small">
          <el-table-column prop="name" label="模板名" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button type="primary" link @click="genFromTpl(row)">生成试卷</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
      <el-collapse-item title="组卷日志" name="log">
        <el-table :data="logs" size="small">
          <el-table-column prop="mode" label="模式" width="100" />
          <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
          <el-table-column prop="createTime" label="时间" min-width="160" />
        </el-table>
      </el-collapse-item>
    </el-collapse>

    <el-dialog v-model="manualDlg" title="手动组卷" width="560px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="试卷标题" required>
          <el-input v-model="manualTitle" />
        </el-form-item>
        <el-form-item label="题目">
          <div v-for="(line, idx) in manualLines" :key="idx" class="line">
            <el-input-number v-model="line.qid" :min="1" placeholder="试题ID" />
            <el-input-number v-model="line.score" :min="0" :step="1" placeholder="分值" />
            <el-button link type="danger" @click="manualLines.splice(idx, 1)">删</el-button>
          </div>
          <el-button @click="manualLines.push({ qid: undefined, score: 10 })">添加一行</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitManual">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="autoDlg" title="自动组卷" width="520px" destroy-on-close>
      <el-form label-width="120px">
        <el-form-item label="试卷标题" required>
          <el-input v-model="autoTitle" />
        </el-form-item>
        <el-form-item label="单选数量">
          <el-input-number v-model="cntSingle" :min="0" />
        </el-form-item>
        <el-form-item label="多选数量">
          <el-input-number v-model="cntMulti" :min="0" />
        </el-form-item>
        <el-form-item label="判断数量">
          <el-input-number v-model="cntTf" :min="0" />
        </el-form-item>
        <el-form-item label="填空数量">
          <el-input-number v-model="cntFill" :min="0" />
        </el-form-item>
        <el-form-item label="每题分值">
          <el-input-number v-model="autoScore" :min="1" />
        </el-form-item>
        <el-form-item label="随机全库">
          <el-switch v-model="randomPool" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAuto">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tplGenDlg" title="从模板生成" width="400px">
      <el-form label-width="88px">
        <el-form-item label="试卷标题" required>
          <el-input v-model="tplGenTitle" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tplGenDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTplGen">生成</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { fetchCoursePage, type CourseRow } from '@/api/course';
import {
  fetchPaperPage,
  createPaperManual,
  generatePaperAuto,
  deletePaper,
  fetchPaperTemplates,
  savePaperTemplate,
  generatePaperFromTemplate,
  fetchPaperGenerationLogs
} from '@/api/modules/paper';
import { getLastCourseId, setLastCourseId } from '@/composables/useLastCourseId';

const courses = ref<CourseRow[]>([]);
const courseId = ref<number | null>(getLastCourseId());
const loading = ref(false);
const rows = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);

const templates = ref<{ id: number; name: string }[]>([]);
const logs = ref<Record<string, unknown>[]>([]);
const tplName = ref('');

const manualDlg = ref(false);
const manualTitle = ref('');
const manualLines = ref<{ qid?: number; score?: number }[]>([{ qid: undefined, score: 10 }]);

const autoDlg = ref(false);
const autoTitle = ref('测验');
const cntSingle = ref(2);
const cntMulti = ref(1);
const cntTf = ref(1);
const cntFill = ref(0);
const autoScore = ref(10);
const randomPool = ref(false);

const tplGenDlg = ref(false);
const tplGenTitle = ref('');
const activeTplId = ref<number | null>(null);

const saving = ref(false);

async function loadCourses() {
  const { data } = await fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
  if (!courseId.value && courses.value.length) {
    courseId.value = courses.value[0].id;
  }
}

function onCourseChange() {
  if (courseId.value) setLastCourseId(courseId.value);
  void load();
  void loadTplAndLogs();
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchPaperPage({ courseId: courseId.value, page: page.value, size: size.value });
    rows.value = (data as { records?: Record<string, unknown>[] })?.records ?? [];
    total.value = (data as { total?: number })?.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function loadTplAndLogs() {
  if (!courseId.value) return;
  const [tRes, lRes] = await Promise.all([
    fetchPaperTemplates(courseId.value),
    fetchPaperGenerationLogs({ courseId: courseId.value, page: 1, size: 20 })
  ]);
  const td = tRes.data as { id: number; name: string }[] | undefined;
  templates.value = Array.isArray(td) ? td : [];
  const ld = lRes.data as { records?: Record<string, unknown>[] } | undefined;
  logs.value = ld?.records ?? [];
}

function openManual() {
  manualTitle.value = '手动试卷';
  manualLines.value = [{ qid: undefined, score: 10 }];
  manualDlg.value = true;
}

async function submitManual() {
  if (!courseId.value || !manualTitle.value.trim()) return;
  const items = manualLines.value
    .filter((x) => x.qid && x.score != null)
    .map((x) => ({ questionId: x.qid!, score: x.score! }));
  if (!items.length) return;
  saving.value = true;
  try {
    await createPaperManual({ courseId: courseId.value, title: manualTitle.value, items });
    manualDlg.value = false;
    await load();
    await loadTplAndLogs();
  } finally {
    saving.value = false;
  }
}

function openAuto() {
  autoTitle.value = '自动试卷';
  autoDlg.value = true;
}

async function submitAuto() {
  if (!courseId.value || !autoTitle.value.trim()) return;
  const countByType: Record<string, number> = {};
  if (cntSingle.value > 0) countByType.SINGLE = cntSingle.value;
  if (cntMulti.value > 0) countByType.MULTIPLE = cntMulti.value;
  if (cntTf.value > 0) countByType.TRUE_FALSE = cntTf.value;
  if (cntFill.value > 0) countByType.FILL = cntFill.value;
  if (!Object.keys(countByType).length) return;
  saving.value = true;
  try {
    await generatePaperAuto({
      courseId: courseId.value,
      title: autoTitle.value,
      countByType,
      scorePerQuestion: autoScore.value,
      randomPool: randomPool.value,
      includeKnowledgeDescendants: true
    });
    autoDlg.value = false;
    await load();
    await loadTplAndLogs();
  } finally {
    saving.value = false;
  }
}

async function saveTemplate() {
  if (!courseId.value || !tplName.value.trim()) return;
  const countByType: Record<string, number> = {};
  if (cntSingle.value > 0) countByType.SINGLE = cntSingle.value;
  if (cntMulti.value > 0) countByType.MULTIPLE = cntMulti.value;
  if (cntTf.value > 0) countByType.TRUE_FALSE = cntTf.value;
  if (cntFill.value > 0) countByType.FILL = cntFill.value;
  if (!Object.keys(countByType).length) return;
  await savePaperTemplate({
    courseId: courseId.value,
    name: tplName.value,
    rules: {
      countByType,
      scorePerQuestion: autoScore.value,
      randomPool: true,
      includeKnowledgeDescendants: true
    }
  });
  await loadTplAndLogs();
}

function genFromTpl(row: { id: number }) {
  activeTplId.value = row.id;
  tplGenTitle.value = '从模板生成';
  tplGenDlg.value = true;
}

async function submitTplGen() {
  if (!activeTplId.value || !tplGenTitle.value.trim()) return;
  saving.value = true;
  try {
    await generatePaperFromTemplate(activeTplId.value, { title: tplGenTitle.value });
    tplGenDlg.value = false;
    await load();
    await loadTplAndLogs();
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: Record<string, unknown>) {
  try {
    await ElMessageBox.confirm('删除试卷可能影响关联考试，确定？', '删除', { type: 'warning' });
  } catch {
    return;
  }
  await deletePaper(row.id as number);
  await load();
}

onMounted(async () => {
  await loadCourses();
  await load();
  await loadTplAndLogs();
});
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.acts {
  display: flex;
  gap: 8px;
}
.bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.collapse {
  margin-top: 16px;
}
.tpl-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.line {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  align-items: center;
}
</style>
