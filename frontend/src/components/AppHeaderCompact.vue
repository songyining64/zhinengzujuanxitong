<template>
  <el-header class="app-header-compact">
    <div class="left">
      <slot name="left">
        <span>{{ title }}</span>
      </slot>
    </div>
    <div class="right">
      <slot name="right" />
    </div>
  </el-header>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string;
  }>(),
  { title: '' }
);
</script>

<style scoped>
.app-header-compact {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  box-sizing: border-box;
  border-bottom: 1px solid #f0f0f0;
}
</style>
