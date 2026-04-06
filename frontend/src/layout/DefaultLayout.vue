<template>
  <div class="app-frame layout-zujuan">
    <AppSidebar :menu-tree="menuTree" />
    <div class="layout-main">
      <AppHeaderCompact />
      <main class="main-shell">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import AppSidebar from '@/components/AppSidebar.vue';
import AppHeaderCompact from '@/components/AppHeaderCompact.vue';
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
.app-frame.layout-zujuan {
  min-height: 100vh;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  background: #e8edf3;
}

.layout-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
</style>
