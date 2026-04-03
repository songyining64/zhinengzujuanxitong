<template>
  <div class="app-frame">
    <AppNavBar :menu-tree="menuTree" />
    <main class="main-shell">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AppNavBar from '@/components/AppNavBar.vue';
import http from '@/api/http';
import type { MenuTreeVO } from '@/types/menu';

const menuTree = ref<MenuTreeVO[]>([]);

onMounted(async () => {
  try {
    const { data } = await http.get<MenuTreeVO[]>('/api/system/menu/tree');
    menuTree.value = data || [];
  } catch {
    menuTree.value = [];
  }
});
</script>

<style scoped>
.app-frame {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #eef2f7;
}
</style>
