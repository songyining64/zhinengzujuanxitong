<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <span>知识点</span>
        <CoursePicker />
      </div>
    </template>

    <el-alert
      v-if="!courseId"
      type="info"
      show-icon
      :closable="false"
      title="可在右上角选择当前课程（与其它页面联动）；下方课程表点击「查看」浏览该课知识点结构树。"
      style="margin-bottom: 12px"
    />

    <div class="course-section" v-loading="coursesLoading">
      <div class="subhead">与课程管理页课程一致；点击课程行的「查看」在弹窗中打开整课知识点树（含要点说明）。</div>
      <el-table :data="allCourses" stripe size="small" max-height="280">
        <el-table-column prop="id" label="ID" width="88" />
        <el-table-column label="课程名称" min-width="140">
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-tag v-if="isSampleCourseId(row.id)" size="small" type="info" class="name-tag">示例</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编号" width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="88" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openViewCourse(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="below-blank" aria-hidden="true" />

    <el-dialog
      v-model="viewDlg.visible"
      :title="viewDlg.title"
      width="640px"
      destroy-on-close
      align-center
      class="view-kp-dialog"
      @closed="onViewDlgClosed"
    >
      <div v-loading="viewDlg.loading" class="view-kp-body">
        <el-empty
          v-if="!viewDlg.loading && viewDlg.tree.length === 0"
          description="该课程暂无知识点"
          :image-size="80"
        />
        <el-tree
          v-else-if="!viewDlg.loading && viewDlg.tree.length"
          :data="viewDlg.tree"
          :props="{ label: 'label', children: 'children' }"
          default-expand-all
          highlight-current
          class="view-kp-tree"
        >
          <template #default="{ data }">
            <div class="view-tree-node">
              <span class="view-tree-name">{{ data.label }}</span>
              <p v-if="data.content" class="view-tree-content">{{ data.content }}</p>
            </div>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button type="primary" @click="viewDlg.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import CoursePicker from '@/components/CoursePicker.vue';
import { useCourseContextStore } from '@/store/courseContext';
import { fetchCoursePage } from '@/api/modules/course';
import { fetchKnowledgeList } from '@/api/modules/knowledge';
import { mergeCoursesWithSamples, isSampleCourseId } from '@/data/sampleCourses';
import { resolveKnowledgePointsForCourse } from '@/data/sampleKnowledgeByCourse';
import { buildKnowledgeTree, type TreeNode } from '@/utils/knowledgeTree';
import type { Course, KnowledgePoint } from '@/types/models';

const store = useCourseContextStore();
const { courseId } = storeToRefs(store);

const allCourses = ref<Course[]>([]);
const coursesLoading = ref(false);

async function loadAllCourses() {
  coursesLoading.value = true;
  try {
    try {
      const { data } = await fetchCoursePage(1, 500);
      allCourses.value = mergeCoursesWithSamples(data?.records ?? []);
    } catch {
      allCourses.value = mergeCoursesWithSamples([]);
    }
  } finally {
    coursesLoading.value = false;
  }
}

onMounted(() => {
  void loadAllCourses();
});

const viewDlg = reactive({
  visible: false,
  title: '',
  loading: false,
  tree: [] as TreeNode[]
});

function onViewDlgClosed() {
  viewDlg.tree = [];
  viewDlg.title = '';
}

async function openViewCourse(row: Course) {
  viewDlg.visible = true;
  viewDlg.title = `知识点 — ${row.name}`;
  viewDlg.loading = true;
  viewDlg.tree = [];
  try {
    let list: KnowledgePoint[];
    try {
      const { data } = await fetchKnowledgeList(row.id);
      list = resolveKnowledgePointsForCourse(row.id, data ?? []);
    } catch {
      list = resolveKnowledgePointsForCourse(row.id, []);
    }
    viewDlg.tree = buildKnowledgeTree(list);
    if (list.length === 0 && !isSampleCourseId(row.id)) {
      ElMessage.warning('暂无知识点数据，请检查网络或后端服务');
    }
  } finally {
    viewDlg.loading = false;
  }
}
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.course-section {
  margin-bottom: 16px;
}

.subhead {
  margin-bottom: 8px;
  font-size: 14px;
  color: #303133;
}

.name-tag {
  margin-left: 6px;
  vertical-align: middle;
}

.below-blank {
  min-height: min(50vh, 420px);
  margin-top: 8px;
}

.view-kp-body {
  min-height: 120px;
}

.view-kp-tree {
  max-height: 50vh;
  overflow: auto;
}

.view-tree-node {
  max-width: 560px;
  white-space: normal;
  padding: 2px 0;
}

.view-tree-name {
  font-weight: 500;
}

.view-tree-content {
  margin: 6px 0 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
</style>
