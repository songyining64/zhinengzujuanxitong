<template>
  <el-card>
    <el-empty description="该功能页面尚未实现">
      <template #description>
        <p>路径：<code>{{ route.path }}</code></p>
        <p class="hint">菜单已由后端权限动态加载，可在此接入业务页面。</p>
      </template>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>
  </el-card>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';

const route = useRoute();
</script>

<style scoped>
.hint {
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}
code {
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
