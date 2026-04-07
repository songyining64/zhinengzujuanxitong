<template>
  <el-result icon="warning" title="页面不存在" sub-title="请从左侧菜单进入功能模块">
    <template #extra>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </template>
  </el-result>
</template>
