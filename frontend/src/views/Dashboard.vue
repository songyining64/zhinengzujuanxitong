<template>
  <el-card>
    <h2>欢迎使用课程智能组卷系统</h2>
    <p v-if="role">
      当前角色：<el-tag size="small">{{ role }}</el-tag>
    </p>
    <el-divider />
    <p class="actions">
      <el-button type="primary" @click="$router.push('/exam/take')">我的考试</el-button>
      <el-button @click="$router.push('/course/browse')">课程浏览</el-button>
      <el-button @click="$router.push('/question/browse')">题库浏览</el-button>
      <el-button @click="$router.push('/paper/browse')">试卷浏览</el-button>
    </p>
    <el-divider />
    <ul class="tips">
      <li>左侧菜单由后端按角色返回，与接口权限一致。</li>
      <li>学生端答题页可使用 <code>hooks/useExamAntiCheat</code> 上报切屏。</li>
      <li>交卷后可调用接口 <code v-pre>/api/exam/record/{recordId}/summary</code> 一次获取分数、名次与及格情况。</li>
      <li>运维可探测 <code>GET /actuator/health</code> 检查服务健康状态。</li>
    </ul>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const role = computed(() => localStorage.getItem('role') || '');
</script>

<style scoped>
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tips {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 1.8;
}
code {
  font-size: 12px;
  background: #f4f4f5;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>

