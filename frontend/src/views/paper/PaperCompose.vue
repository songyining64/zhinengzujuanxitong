<template>
  <div class="compose-page">
    <div class="hero page-card">
      <div class="hero-text">
        <h1>智能组卷工作台</h1>
        <p>多约束选题 · 知识点覆盖 · 分值阶梯 · 错题过滤 · 支持异步组卷</p>
      </div>
      <div class="hero-course">
        <span class="label">课程</span>
        <el-select
          v-model="courseId"
          placeholder="选择课程"
          filterable
          class="course-select"
          @change="onCourseChange"
        >
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
    </div>

    <el-tabs v-model="tab" type="border-card" class="main-tabs page-card">
      <el-tab-pane label="试卷列表" name="list">
        <el-table v-loading="loadingPapers" :data="papers" stripe class="data-table">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="mode" label="模式" width="100" />
          <el-table-column prop="totalScore" label="总分" width="100" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button v-if="canManage" link type="danger" @click="removePaper(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="canManage" label="智能组卷" name="auto">
        <div class="auto-grid">
          <el-form label-position="top" class="auto-form">
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="试卷标题" required>
                  <el-input v-model="autoForm.title" placeholder="例如：高一数学单元检测" clearable />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="组卷方式">
                  <el-space wrap>
                    <el-switch v-model="autoForm.randomPool" active-text="全课程随机池" />
                    <el-switch v-model="useAsyncCompose" active-text="异步组卷（大题库推荐）" />
                  </el-space>
                </el-form-item>
              </el-col>
            </el-row>

            <el-collapse v-model="collapseActive">
              <el-collapse-item title="知识点与覆盖约束" name="kp">
                <template v-if="!autoForm.randomPool">
                  <el-form-item label="抽题知识点（不选则需在下方开启全库随机）">
                    <el-select
                      v-model="autoForm.knowledgePointIds"
                      multiple
                      filterable
                      collapse-tags
                      collapse-tags-tooltip
                      placeholder="选择知识点"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="k in knowledgePoints"
                        :key="k.id"
                        :label="k.name + ' (#' + k.id + ')'"
                        :value="k.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="包含子知识点">
                    <el-switch v-model="autoForm.includeDescendants" />
                  </el-form-item>
                  <el-form-item label="最低知识点覆盖率">
                    <el-slider v-model="coveragePercent" :min="50" :max="100" :step="1" show-input />
                    <div class="hint">100 表示不强制；下调可放宽约束。未达标时可勾选「允许次优」。</div>
                  </el-form-item>
                </template>
                <el-alert v-else type="info" :closable="false" show-icon title="全库随机时不校验知识点覆盖率" />
              </el-collapse-item>

              <el-collapse-item title="题型、难度与分值" name="score">
                <div class="type-grid">
                  <div v-for="row in typeRows" :key="row.key" class="type-row">
                    <span class="type-name">{{ row.label }}</span>
                    <el-input-number v-model="autoForm.counts[row.key]" :min="0" :max="999" controls-position="right" />
                    <span class="score-label">分值</span>
                    <el-input-number
                      v-model="autoForm.scores[row.key]"
                      :min="0"
                      :max="100"
                      :step="0.5"
                      controls-position="right"
                    />
                  </div>
                </div>
                <el-form-item label="难度权重（易/中/难，按题型内分配）" class="mt">
                  <div class="dw-row">
                    <span>易</span>
                    <el-input-number v-model="autoForm.dwEasy" :min="0" :max="99" />
                    <span>中</span>
                    <el-input-number v-model="autoForm.dwMid" :min="0" :max="99" />
                    <span>难</span>
                    <el-input-number v-model="autoForm.dwHard" :min="0" :max="99" />
                  </div>
                </el-form-item>
                <el-form-item label="目标总分（可选，与上表不一致时需开启次优）">
                  <el-input-number v-model="autoForm.targetTotalScore" :min="0" :max="1000" :step="1" :precision="2" />
                </el-form-item>
              </el-collapse-item>

              <el-collapse-item title="排除 / 必考 / 禁选" name="exclude">
                <el-form-item label="必考题 ID（逗号分隔，置前）">
                  <el-input v-model="autoForm.fixedIdsText" type="textarea" :rows="2" placeholder="例：101,102" />
                </el-form-item>
                <el-form-item label="禁选题 ID（永不入卷）">
                  <el-input v-model="autoForm.forbiddenIdsText" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="排除题 ID（已做卷等）">
                  <el-input v-model="autoForm.excludeIdsText" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="排除某学生错题本（巩固卷）">
                  <el-input-number v-model="autoForm.excludeWrongBookStudentId" :min="0" :step="1" controls-position="right" />
                  <span class="hint inline">填 0 表示不启用；需与当前课程一致</span>
                </el-form-item>
                <el-form-item label="策略">
                  <el-space wrap>
                    <el-switch v-model="autoForm.dedup" active-text="跨题型去重" />
                    <el-switch v-model="autoForm.allowPartial" active-text="允许次优（覆盖/总分未达标仍生成）" />
                  </el-space>
                </el-form-item>
              </el-collapse-item>
            </el-collapse>

            <div class="actions">
              <el-button type="primary" size="large" :loading="busy" @click="runAuto">
                {{ useAsyncCompose ? '提交异步组卷' : '立即组卷' }}
              </el-button>
            </div>
          </el-form>

          <div v-if="lastResult" class="result-panel page-card">
            <h3>上次结果</h3>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="试卷 ID">{{ lastResult.paper?.id }}</el-descriptions-item>
              <el-descriptions-item label="总分">{{ lastResult.paper?.totalScore }}</el-descriptions-item>
              <el-descriptions-item label="知识点覆盖率">
                {{ (Number(lastResult.knowledgeCoverage) * 100).toFixed(1) }}%
              </el-descriptions-item>
              <el-descriptions-item label="算法">{{ lastResult.algorithmMode }}</el-descriptions-item>
              <el-descriptions-item label="耗时 ms">{{ lastResult.durationMs }}</el-descriptions-item>
              <el-descriptions-item label="次优">{{ lastResult.partialConstraint ? '是' : '否' }}</el-descriptions-item>
            </el-descriptions>
            <el-alert
              v-for="(w, i) in lastResult.warnings"
              :key="'w' + i"
              type="warning"
              :closable="false"
              class="mt"
              :title="w"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canManage" label="手动组卷" name="manual">
        <el-form label-width="100px" class="narrow-form">
          <el-form-item label="标题" required>
            <el-input v-model="manualTitle" />
          </el-form-item>
          <el-form-item label="题目">
            <el-table :data="manualItems" size="small" class="data-table">
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
        <el-table :data="templates" size="small" class="data-table">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="name" label="名称" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="genFromTpl(row)">生成试卷</el-button>
              <el-button link type="danger" @click="delTpl(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" title="试卷题目" width="720px" destroy-on-close>
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
import * as knowledgeApi from '@/api/modules/knowledge';
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
const knowledgePoints = ref<knowledgeApi.KnowledgePoint[]>([]);
const collapseActive = ref(['kp', 'score', 'exclude']);
const useAsyncCompose = ref(false);
const lastResult = ref<paperApi.PaperAutoGenResult | null>(null);

const typeRows = [
  { key: 'SINGLE', label: '单选' },
  { key: 'MULTIPLE', label: '多选' },
  { key: 'TRUE_FALSE', label: '判断' },
  { key: 'FILL', label: '填空' },
  { key: 'SHORT', label: '简答' }
] as const;

const autoForm = reactive({
  title: '智能试卷',
  randomPool: false,
  includeDescendants: true,
  knowledgePointIds: [] as number[],
  counts: {
    SINGLE: 5,
    MULTIPLE: 0,
    TRUE_FALSE: 0,
    FILL: 0,
    SHORT: 0
  } as Record<string, number>,
  scores: {
    SINGLE: 10,
    MULTIPLE: 10,
    TRUE_FALSE: 10,
    FILL: 10,
    SHORT: 10
  } as Record<string, number>,
  dwEasy: 3,
  dwMid: 5,
  dwHard: 2,
  targetTotalScore: undefined as number | undefined,
  fixedIdsText: '',
  forbiddenIdsText: '',
  excludeIdsText: '',
  excludeWrongBookStudentId: 0,
  dedup: true,
  allowPartial: false
});

const coveragePercent = ref(100);

const manualTitle = ref('手动试卷');
const manualItems = ref<{ questionId: number; score: number }[]>([{ questionId: 0, score: 5 }]);

const detailVisible = ref(false);
const detail = ref<paperApi.PaperDetail | null>(null);

const tplNameVisible = ref(false);
const tplName = ref('');
const genTitleVisible = ref(false);
const genTitle = ref('');
const tplPicking = ref<paperApi.PaperTemplate | null>(null);

function parseIdList(text: string): number[] {
  if (!text.trim()) return [];
  return text
    .split(/[,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => Number(s))
    .filter((n) => !Number.isNaN(n) && n > 0);
}

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function loadKnowledge() {
  if (!courseId.value) {
    knowledgePoints.value = [];
    return;
  }
  knowledgePoints.value = (await knowledgeApi.listKnowledge(courseId.value)) ?? [];
}

async function onCourseChange() {
  await loadKnowledge();
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

function buildCountByType(): Record<string, number> {
  const m: Record<string, number> = {};
  for (const row of typeRows) {
    const n = autoForm.counts[row.key] ?? 0;
    if (n > 0) m[row.key] = n;
  }
  return m;
}

function buildScoreByType(countByType: Record<string, number>): Record<string, number> | undefined {
  const m: Record<string, number> = {};
  for (const k of Object.keys(countByType)) {
    m[k] = autoForm.scores[k] ?? 10;
  }
  return Object.keys(m).length ? m : undefined;
}

function buildAutoPayload(): Record<string, unknown> {
  const countByType = buildCountByType();
  const scoreByType = buildScoreByType(countByType);
  const payload: Record<string, unknown> = {
    courseId: courseId.value,
    title: autoForm.title,
    randomPool: autoForm.randomPool,
    countByType,
    dedup: autoForm.dedup,
    includeKnowledgeDescendants: autoForm.includeDescendants,
    allowPartialConstraints: autoForm.allowPartial
  };
  if (!autoForm.randomPool) {
    payload.knowledgePointIds =
      autoForm.knowledgePointIds.length > 0 ? [...autoForm.knowledgePointIds] : undefined;
  }
  if (scoreByType) payload.scoreByType = scoreByType;
  const fixed = parseIdList(autoForm.fixedIdsText);
  if (fixed.length) payload.fixedQuestionIds = fixed;
  const forbid = parseIdList(autoForm.forbiddenIdsText);
  if (forbid.length) payload.forbiddenQuestionIds = forbid;
  const excl = parseIdList(autoForm.excludeIdsText);
  if (excl.length) payload.excludeQuestionIds = excl;
  if (autoForm.excludeWrongBookStudentId > 0) {
    payload.excludeWrongBookForStudentId = autoForm.excludeWrongBookStudentId;
  }
  if (autoForm.targetTotalScore != null && autoForm.targetTotalScore > 0) {
    payload.targetTotalScore = autoForm.targetTotalScore;
  }
  if (!autoForm.randomPool && coveragePercent.value < 100) {
    payload.minKnowledgeCoverage = coveragePercent.value / 100;
  }
  if (autoForm.dwEasy + autoForm.dwMid + autoForm.dwHard > 0) {
    payload.difficultyWeights = {
      EASY: autoForm.dwEasy,
      MEDIUM: autoForm.dwMid,
      HARD: autoForm.dwHard
    };
  }
  return payload;
}

async function pollUntilDone(taskId: string): Promise<paperApi.PaperAutoGenResult | null> {
  for (let i = 0; i < 120; i++) {
    const entry = await paperApi.pollComposeTask(taskId);
    if (entry.status === 'DONE' && entry.result) return entry.result;
    if (entry.status === 'FAILED') {
      ElMessage.error(entry.errorMessage || '组卷失败');
      return null;
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  ElMessage.error('组卷任务超时，请稍后重试');
  return null;
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
  if (!autoForm.randomPool && (!autoForm.knowledgePointIds || autoForm.knowledgePointIds.length === 0)) {
    ElMessage.warning('请选择题库知识点，或开启「全课程随机池」');
    return;
  }
  busy.value = true;
  try {
    const body = buildAutoPayload();
    if (useAsyncCompose.value) {
      const taskId = await paperApi.autoGeneratePaperAsync(body);
      ElMessage.info('已提交组卷任务，请稍候…');
      const res = await pollUntilDone(taskId);
      if (res) {
        lastResult.value = res;
        ElMessage.success('组卷成功');
        tab.value = 'list';
        await refreshPapers();
      }
    } else {
      const res = await paperApi.autoGeneratePaper(body);
      lastResult.value = res;
      if (res.partialConstraint) {
        ElMessage.warning('已生成试卷（部分约束为次优），请查看提示');
      } else {
        ElMessage.success('组卷成功');
      }
      tab.value = 'list';
      await refreshPapers();
    }
  } catch {
    /* http 拦截器已提示 */
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

function buildTemplateRules(): Record<string, unknown> {
  return buildAutoPayload();
}

async function confirmSaveTpl() {
  if (!courseId.value || !tplName.value.trim()) {
    ElMessage.warning('请填写模板名称');
    return;
  }
  const countByType = buildCountByType();
  if (!Object.keys(countByType).length) {
    ElMessage.warning('请先在题型中设置数量');
    return;
  }
  await paperApi.savePaperTemplate({
    courseId: courseId.value,
    name: tplName.value,
    rules: buildTemplateRules()
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
    const res = await paperApi.generateFromTemplate(tplPicking.value.id, { title: genTitle.value });
    lastResult.value = res;
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
.compose-page {
  max-width: 1180px;
  margin: 0 auto;
}

.hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  margin-bottom: 16px;
}

.hero h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--text-dark, #2c3e50);
  font-weight: 650;
}

.hero p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.hero-course {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-course .label {
  color: #606266;
  font-size: 14px;
}

.course-select {
  width: min(360px, 72vw);
}

.main-tabs {
  border-radius: 12px;
  overflow: hidden;
}

.auto-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

@media (min-width: 1100px) {
  .auto-grid {
    grid-template-columns: 1fr 320px;
    align-items: start;
  }
}

.auto-form {
  padding: 4px 0 12px;
}

.type-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-row {
  display: grid;
  grid-template-columns: 72px 1fr 48px 1fr;
  gap: 8px;
  align-items: center;
}

.type-name {
  font-size: 13px;
  color: #606266;
}

.score-label {
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.dw-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}

.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.hint.inline {
  margin-left: 8px;
  margin-top: 0;
}

.actions {
  margin-top: 20px;
}

.result-panel {
  padding: 16px;
}

.result-panel h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.mt {
  margin-top: 10px;
}

.data-table {
  width: 100%;
}

.narrow-form {
  max-width: 760px;
}

.tpl-head {
  margin-bottom: 12px;
}
</style>
