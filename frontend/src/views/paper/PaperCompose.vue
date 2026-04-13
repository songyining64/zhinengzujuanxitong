<template>
  <div class="page">
    <el-card shadow="never" class="mb">
      <div class="course-row">
        <span>课程</span>
        <el-select v-model="courseId" placeholder="选择课程" filterable style="width: 320px" @change="onCourseChange">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
    </el-card>

    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="试卷列表" name="list">
        <el-table v-loading="loadingPapers" :data="papers" stripe>
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="mode" label="模式" width="100" />
          <el-table-column prop="totalScore" label="总分" width="100" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button v-if="canManage" link type="danger" @click="removePaper(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="canManage" label="智能组卷" name="auto">
        <el-form :model="autoForm" label-width="120px" style="max-width: 640px">
          <el-form-item label="试卷标题" required>
            <el-input v-model="autoForm.title" />
          </el-form-item>
          <el-form-item label="全库随机">
            <el-switch v-model="autoForm.randomPool" />
          </el-form-item>
          <el-form-item label="题型数量">
            <div class="grid">
              <span>单选</span><el-input-number v-model="autoForm.nSingle" :min="0" />
              <span>多选</span><el-input-number v-model="autoForm.nMulti" :min="0" />
              <span>判断</span><el-input-number v-model="autoForm.nTf" :min="0" />
              <span>填空</span><el-input-number v-model="autoForm.nFill" :min="0" />
              <span>简答</span><el-input-number v-model="autoForm.nShort" :min="0" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="busy" @click="runAuto">生成试卷</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane v-if="canManage" label="手动组卷" name="manual">
        <el-form label-width="100px" style="max-width: 720px">
          <el-form-item label="标题" required>
            <el-input v-model="manualTitle" />
          </el-form-item>
          <el-form-item label="题目">
            <el-table :data="manualItems" size="small">
              <el-table-column label="试题 ID">
                <template #default="{ row }">
                  <el-input-number v-model="row.questionId" :min="1" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="分值">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="0" :step="0.5" />
                </template>
              </el-table-column>
              <el-table-column width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="manualItems.splice($index, 1)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button class="mt" @click="manualItems.push({ questionId: 0, score: 5 })">添加一行</el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="busy" @click="runManual">保存试卷</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane v-if="canManage" label="组卷模板" name="tpl">
        <div class="tpl-head">
          <el-button type="primary" @click="saveTemplate">保存当前规则为模板</el-button>
        </div>
        <el-table :data="templates" size="small">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="name" label="名称" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button link type="primary" @click="genFromTpl(row)">生成试卷</el-button>
              <el-button link type="danger" @click="delTpl(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" title="试卷题目" width="720px">
      <el-table v-if="detail" :data="detail.questions" size="small">
        <el-table-column prop="questionOrder" label="#" width="50" />
        <el-table-column prop="questionId" label="题目 ID" width="100" />
        <el-table-column prop="type" label="题型" width="90" />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="stem" label="题干" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="tplNameVisible" title="模板名称" width="400px">
      <el-input v-model="tplName" placeholder="模板名称" />
      <template #footer>
        <el-button @click="tplNameVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSaveTpl">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="genTitleVisible" title="生成试卷标题" width="400px">
      <el-input v-model="genTitle" placeholder="试卷标题" />
      <template #footer>
        <el-button @click="genTitleVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmGenFromTpl">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as courseApi from '@/api/modules/course';
import * as paperApi from '@/api/modules/paper';
import http from '@/api/http';
import { hasPerm } from '@/composables/usePermission';

const courses = ref<courseApi.Course[]>([]);
const courseId = ref<number | undefined>();
const tab = ref('list');
const loadingPapers = ref(false);
const papers = ref<paperApi.Paper[]>([]);
const templates = ref<paperApi.PaperTemplate[]>([]);
const busy = ref(false);
const canManage = computed(() => hasPerm('paper:manage'));

const autoForm = reactive({
  title: '智能试卷',
  randomPool: false,
  nSingle: 5,
  nMulti: 0,
  nTf: 0,
  nFill: 0,
  nShort: 0
});

const manualTitle = ref('手动试卷');
const manualItems = ref<{ questionId: number; score: number }[]>([{ questionId: 0, score: 5 }]);

const detailVisible = ref(false);
const detail = ref<paperApi.PaperDetail | null>(null);

const tplNameVisible = ref(false);
const tplName = ref('');
const genTitleVisible = ref(false);
const genTitle = ref('');
const tplPicking = ref<paperApi.PaperTemplate | null>(null);

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function onCourseChange() {
  await refreshPapers();
  await refreshTemplates();
}

async function refreshPapers() {
  if (!courseId.value) {
    papers.value = [];
    return;
  }
  loadingPapers.value = true;
  try {
    const data = await paperApi.fetchPaperPage(courseId.value, 1, 50);
    papers.value = data?.records ?? [];
  } finally {
    loadingPapers.value = false;
  }
}

async function refreshTemplates() {
  if (!courseId.value) {
    templates.value = [];
    return;
  }
  templates.value = (await paperApi.fetchPaperTemplates(courseId.value)) ?? [];
}

async function showDetail(row: paperApi.Paper) {
  detail.value = await paperApi.fetchPaperDetail(row.id);
  detailVisible.value = true;
}

async function removePaper(row: paperApi.Paper) {
  await ElMessageBox.confirm('确定删除该试卷？', '确认', { type: 'warning' });
  await paperApi.deletePaper(row.id);
  ElMessage.success('已删除');
  await refreshPapers();
}

function buildCountByType() {
  const m: Record<string, number> = {};
  if (autoForm.nSingle) m.SINGLE = autoForm.nSingle;
  if (autoForm.nMulti) m.MULTIPLE = autoForm.nMulti;
  if (autoForm.nTf) m.TRUE_FALSE = autoForm.nTf;
  if (autoForm.nFill) m.FILL = autoForm.nFill;
  if (autoForm.nShort) m.SHORT = autoForm.nShort;
  return m;
}

async function runAuto() {
  if (!courseId.value || !autoForm.title.trim()) {
    ElMessage.warning('请选择课程并填写标题');
    return;
  }
  const countByType = buildCountByType();
  if (!Object.keys(countByType).length) {
    ElMessage.warning('请至少设置一种题型的数量');
    return;
  }
  busy.value = true;
  try {
    await paperApi.autoGeneratePaper({
      courseId: courseId.value,
      title: autoForm.title,
      randomPool: autoForm.randomPool,
      countByType,
      dedup: true,
      includeKnowledgeDescendants: true
    });
    ElMessage.success('组卷成功');
    tab.value = 'list';
    await refreshPapers();
  } catch {
    /* */
  } finally {
    busy.value = false;
  }
}

async function runManual() {
  if (!courseId.value || !manualTitle.value.trim()) return;
  const items = manualItems.value.filter((x) => x.questionId > 0);
  if (!items.length) {
    ElMessage.warning('请填写有效的试题 ID');
    return;
  }
  busy.value = true;
  try {
    await paperApi.createManualPaper({
      courseId: courseId.value,
      title: manualTitle.value,
      items
    });
    ElMessage.success('已保存');
    tab.value = 'list';
    await refreshPapers();
  } catch {
    /* */
  } finally {
    busy.value = false;
  }
}

function saveTemplate() {
  tplName.value = '';
  tplNameVisible.value = true;
}

async function confirmSaveTpl() {
  if (!courseId.value || !tplName.value.trim()) {
    ElMessage.warning('请填写模板名称');
    return;
  }
  const countByType = buildCountByType();
  if (!Object.keys(countByType).length) {
    ElMessage.warning('请先在「智能组卷」中设置题型数量');
    return;
  }
  await paperApi.savePaperTemplate({
    courseId: courseId.value,
    name: tplName.value,
    rules: {
      courseId: courseId.value,
      randomPool: autoForm.randomPool,
      countByType,
      dedup: true,
      includeKnowledgeDescendants: true
    }
  });
  ElMessage.success('模板已保存');
  tplNameVisible.value = false;
  await refreshTemplates();
}

function genFromTpl(row: paperApi.PaperTemplate) {
  tplPicking.value = row;
  genTitle.value = autoForm.title || '试卷';
  genTitleVisible.value = true;
}

async function confirmGenFromTpl() {
  if (!tplPicking.value || !genTitle.value.trim()) return;
  busy.value = true;
  try {
    await paperApi.generateFromTemplate(tplPicking.value.id, { title: genTitle.value });
    ElMessage.success('已生成');
    genTitleVisible.value = false;
    tab.value = 'list';
    await refreshPapers();
  } catch {
    /* */
  } finally {
    busy.value = false;
  }
}

async function delTpl(row: paperApi.PaperTemplate) {
  await ElMessageBox.confirm('删除该模板？', '确认', { type: 'warning' });
  await http.delete(`/api/paper-template/${row.id}`);
  ElMessage.success('已删除');
  await refreshTemplates();
}

onMounted(async () => {
  await boot();
});
</script>

<style scoped>
.page {
  max-width: 1100px;
}
.mb {
  margin-bottom: 12px;
}
.course-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.grid {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 8px 12px;
  align-items: center;
}
.mt {
  margin-top: 8px;
}
.tpl-head {
  margin-bottom: 12px;
}
</style>
