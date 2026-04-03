<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <span>知识点</span>
      </template>
      <div class="row">
        <span class="label">课程</span>
        <el-select v-model="courseId" placeholder="先选择课程" filterable style="width: 320px" @change="load">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button v-if="courseId && canManage" type="primary" style="margin-left: 12px" @click="openCreate">
          新建知识点
        </el-button>
      </div>

      <el-table v-loading="loading" :data="flatRows" stripe style="margin-top: 16px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="parentId" label="父节点" width="100" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column v-if="canManage" label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="title" width="480px" destroy-on-close>
      <el-form :model="form" label-width="88px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="父节点">
          <el-select v-model="form.parentId" clearable placeholder="无（顶级）" style="width: 100%">
            <el-option
              v-for="p in parentOptions"
              :key="p.id"
              :label="`${p.name} (#${p.id})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as kpApi from '@/api/modules/knowledge';
import type { KnowledgePoint } from '@/api/modules/knowledge';
import * as courseApi from '@/api/modules/course';
import { hasPerm } from '@/composables/usePermission';

const courses = ref<courseApi.Course[]>([]);
const courseId = ref<number | undefined>();
const loading = ref(false);
const list = ref<KnowledgePoint[]>([]);
const canManage = computed(() => hasPerm('knowledge:manage'));

const flatRows = computed(() => list.value);

const visible = ref(false);
const title = ref('新建');
const editingId = ref<number | null>(null);
const saving = ref(false);
const form = reactive({
  name: '',
  parentId: undefined as number | undefined | null,
  sortOrder: 0
});

const parentOptions = computed(() => list.value.filter((x) => x.id !== editingId.value));

async function boot() {
  const data = await courseApi.fetchCoursePage({ page: 1, size: 200 });
  courses.value = data?.records ?? [];
}

async function load() {
  if (!courseId.value) {
    list.value = [];
    return;
  }
  loading.value = true;
  try {
    list.value = (await kpApi.listKnowledge(courseId.value)) ?? [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = null;
  title.value = '新建知识点';
  form.name = '';
  form.parentId = null;
  form.sortOrder = 0;
  visible.value = true;
}

function openEdit(row: KnowledgePoint) {
  editingId.value = row.id;
  title.value = '编辑知识点';
  form.name = row.name;
  form.parentId = row.parentId ?? null;
  form.sortOrder = row.sortOrder ?? 0;
  visible.value = true;
}

async function submit() {
  if (!courseId.value || !form.name.trim()) {
    ElMessage.warning('请填写名称');
    return;
  }
  saving.value = true;
  try {
    if (editingId.value) {
      await kpApi.updateKnowledge(editingId.value, {
        name: form.name,
        parentId: form.parentId ?? undefined,
        sortOrder: form.sortOrder
      });
    } else {
      await kpApi.createKnowledge({
        courseId: courseId.value,
        parentId: form.parentId ?? undefined,
        name: form.name,
        sortOrder: form.sortOrder
      });
    }
    ElMessage.success('已保存');
    visible.value = false;
    await load();
  } catch {
    /* http */
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: KnowledgePoint) {
  await ElMessageBox.confirm(`确定删除「${row.name}」及其子节点？`, '确认', { type: 'warning' });
  await kpApi.deleteKnowledge(row.id);
  ElMessage.success('已删除');
  await load();
}

onMounted(async () => {
  await boot();
});
</script>

<style scoped>
.page {
  max-width: 1000px;
}
.row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.label {
  color: #606266;
  font-size: 14px;
}
</style>
