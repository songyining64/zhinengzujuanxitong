<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>组卷模板</span>
        <div class="row">
          <CoursePicker />
          <el-button type="primary" @click="openEdit(null)">新建模板</el-button>
        </div>
      </div>
    </template>

    <el-alert v-if="!courseId" type="info" show-icon :closable="false" title="请先选择课程" />

    <template v-else>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="updateTime" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openGen(row)">生成试卷</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-dialog v-model="editDlg.visible" :title="editDlg.id ? '编辑模板' : '新建模板'" width="560px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="editDlg.name" />
        </el-form-item>
      </el-form>
      <p class="hint">规则与「自动组卷」一致（保存时不含试卷标题）</p>
      <PaperRuleMini v-model="editDlg.rules" />
      <template #footer>
        <el-button @click="editDlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="editDlg.saving" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="genDlg.visible" title="按模板生成试卷" width="440px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="试卷标题" required>
          <el-input v-model="genDlg.title" />
        </el-form-item>
        <el-form-item label="随机种子">
          <el-input-number v-model="genDlg.randomSeed" :step="1" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genDlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="genDlg.loading" @click="runGen">生成</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage, ElMessageBox } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import PaperRuleMini from '@/views/paper/PaperRuleMini.vue';
import { useCourseContextStore } from '@/store/courseContext';
import {
  fetchPaperTemplates,
  savePaperTemplate,
  updatePaperTemplate,
  deletePaperTemplate,
  generatePaperFromTemplate
} from '@/api/modules/paper';
import type { PaperAutoGenRequest, PaperTemplate } from '@/types/models';

const router = useRouter();
const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const loading = ref(false);
const list = ref<PaperTemplate[]>([]);

function emptyRules(): PaperAutoGenRequest {
  return {
    courseId: courseId.value!,
    title: '',
    randomPool: false,
    knowledgePointIds: [],
    includeKnowledgeDescendants: true,
    scorePerQuestion: 10,
    dedup: true,
    countByType: { SINGLE: 5, MULTIPLE: 2 },
    difficultyWeights: undefined
  };
}

async function load() {
  if (!courseId.value) return;
  loading.value = true;
  try {
    const { data } = await fetchPaperTemplates(courseId.value);
    list.value = data ?? [];
  } finally {
    loading.value = false;
  }
}

watch(courseId, () => {
  list.value = [];
  if (courseId.value) void load();
}, { immediate: true });

const editDlg = reactive({
  visible: false,
  id: null as number | null,
  name: '',
  rules: emptyRules(),
  saving: false
});

function openEdit(row: PaperTemplate | null) {
  if (!courseId.value) return;
  if (row) {
    editDlg.id = row.id;
    editDlg.name = row.name;
    try {
      const parsed = JSON.parse(row.rulesJson || '{}') as Partial<PaperAutoGenRequest>;
      editDlg.rules = { ...emptyRules(), ...parsed, courseId: courseId.value, title: '' };
    } catch {
      editDlg.rules = emptyRules();
    }
  } else {
    editDlg.id = null;
    editDlg.name = '';
    editDlg.rules = emptyRules();
  }
  editDlg.visible = true;
}

async function saveTemplate() {
  if (!courseId.value || !editDlg.name.trim()) {
    ElMessage.warning('请填写模板名称');
    return;
  }
  const rules = { ...editDlg.rules, courseId: courseId.value, title: '' };
  editDlg.saving = true;
  try {
    if (editDlg.id) {
      await updatePaperTemplate(editDlg.id, { courseId: courseId.value, name: editDlg.name.trim(), rules });
    } else {
      await savePaperTemplate({ courseId: courseId.value, name: editDlg.name.trim(), rules });
    }
    ElMessage.success('已保存');
    editDlg.visible = false;
    await load();
  } finally {
    editDlg.saving = false;
  }
}

const genDlg = reactive({
  visible: false,
  templateId: null as number | null,
  title: '',
  randomSeed: undefined as number | undefined,
  loading: false
});

function openGen(row: PaperTemplate) {
  genDlg.templateId = row.id;
  genDlg.title = '';
  genDlg.randomSeed = undefined;
  genDlg.visible = true;
}

async function runGen() {
  if (!genDlg.templateId || !genDlg.title.trim()) {
    ElMessage.warning('请填写试卷标题');
    return;
  }
  genDlg.loading = true;
  try {
    const { data } = await generatePaperFromTemplate(genDlg.templateId, {
      title: genDlg.title.trim(),
      randomSeed: genDlg.randomSeed
    });
    genDlg.visible = false;
    ElMessage.success('已生成');
    await router.push(`/paper/${data.id}`);
  } finally {
    genDlg.loading = false;
  }
}

async function onDelete(row: PaperTemplate) {
  try {
    await ElMessageBox.confirm(`删除模板「${row.name}」？`, '确认');
  } catch {
    return;
  }
  await deletePaperTemplate(row.id);
  ElMessage.success('已删除');
  await load();
}
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hint {
  font-size: 12px;
  color: #909399;
  margin: 0 0 12px;
}
</style>
