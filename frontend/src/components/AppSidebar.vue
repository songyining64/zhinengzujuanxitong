<template>
  <aside class="sidebar">
    <div class="sidebar-brand" @click="router.push('/')">
      <div class="brand-icon" aria-hidden="true">
        <el-icon :size="22"><Collection /></el-icon>
      </div>
      <div class="brand-text">
        <div class="brand-title">课程智能组卷</div>
        <div class="brand-sub">试题 · 组卷 · 考试</div>
      </div>
    </div>

    <nav class="sidebar-nav" aria-label="主导航">
      <button
        v-for="item in primaryItems"
        :key="item.key"
        type="button"
        class="nav-item"
        :class="{ active: isPrimaryActive(item) }"
        @click="goPrimary(item)"
      >
        <el-icon class="nav-ico"><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>

      <div v-if="extraItems.length" class="nav-divider">
        <span>更多功能</span>
      </div>

      <button
        v-for="m in extraItems"
        :key="'ex-' + m.id"
        type="button"
        class="nav-item nav-item-extra"
        :class="{ active: isMenuActive(m) }"
        @click="goMenu(m)"
      >
        <el-icon class="nav-ico"><Menu /></el-icon>
        <span>{{ m.name }}</span>
      </button>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  CirclePlus,
  Collection,
  DataAnalysis,
  DataLine,
  Document,
  EditPen,
  FolderOpened,
  MagicStick,
  Management,
  Menu,
  Notebook,
  Reading,
  Share,
  Tickets,
  Upload
} from '@element-plus/icons-vue';
import { isRole } from '@/composables/usePermission';
import type { MenuTreeVO } from '@/types/menu';
import type { Component } from 'vue';

const props = defineProps<{
  menuTree: MenuTreeVO[];
}>();

const route = useRoute();
const router = useRouter();

type PrimaryNav = {
  key: string;
  label: string;
  path: string;
  icon: Component;
  /** 仅当 URL 含该 query 时高亮（如新增题目） */
  queryActive?: string;
  /** 同 path 下排除某 query 时才高亮「列表」项 */
  excludeQuery?: string;
};

const isStudent = computed(() => isRole('STUDENT'));
const isTeacherLike = computed(() => isRole('TEACHER', 'ADMIN'));

function pathOf(m: MenuTreeVO): string {
  const p = m.path;
  if (!p) return `/menu-${m.id}`;
  return p.startsWith('/') ? p : `/${p}`;
}

function flattenMenus(nodes: MenuTreeVO[]): MenuTreeVO[] {
  const out: MenuTreeVO[] = [];
  function walk(list: MenuTreeVO[]) {
    for (const n of list) {
      if (n.path) out.push(n);
      if (n.children?.length) walk(n.children);
    }
  }
  walk(nodes);
  return out.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id);
}

const flatMenus = computed(() => flattenMenus(props.menuTree));

const primaryPaths = computed(() => {
  const set = new Set<string>();
  for (const item of primaryItems.value) {
    const p = item.path.replace(/\/$/, '') || '/';
    set.add(p);
  }
  return set;
});

const primaryItems = computed<PrimaryNav[]>(() => {
  if (isStudent.value) {
    return [
      { key: 'dash', label: '仪表盘', path: '/', icon: DataLine },
      { key: 'exam', label: '我的考试', path: '/exam/take', icon: Notebook },
      { key: 'wrong', label: '错题本', path: '/wrongbook', icon: Reading }
    ];
  }
  if (!isTeacherLike.value) {
    return [{ key: 'dash', label: '仪表盘', path: '/', icon: DataLine }];
  }
  return [
    { key: 'dash', label: '仪表盘', path: '/', icon: DataLine },
    { key: 'course', label: '课程', path: '/course/manage', icon: Management },
    { key: 'kp', label: '知识点', path: '/knowledge/manage', icon: Share },
    {
      key: 'qm',
      label: '题目管理',
      path: '/question/manage',
      icon: Collection,
      excludeQuery: 'new'
    },
    {
      key: 'qn',
      label: '新增题目',
      path: '/question/manage',
      icon: CirclePlus,
      queryActive: 'new'
    },
    { key: 'auto', label: '智能组卷', path: '/paper/manage', icon: MagicStick },
    { key: 'manual', label: '人工组卷', path: '/paper', icon: Document },
    { key: 'papers', label: '试卷浏览', path: '/paper/browse', icon: FolderOpened },
    { key: 'exam', label: '考试', path: '/exam/manage', icon: Tickets },
    { key: 'analytics', label: '成绩分析', path: '/exam/analytics', icon: DataAnalysis },
    { key: 'grade', label: '主观题阅卷', path: '/exam/grading', icon: EditPen },
    { key: 'files', label: '文件工具', path: '/tools/file', icon: Upload }
  ];
});

const extraItems = computed(() => {
  const primary = primaryPaths.value;
  return flatMenus.value.filter((m) => {
    const p = pathOf(m).replace(/\/$/, '') || '/';
    if (primary.has(p)) return false;
    if (p === '/' || !m.path) return false;
    return true;
  });
});

const activeMenuId = computed(() => {
  const pathsExtras = extraItems.value
    .map((m) => ({ id: m.id, p: pathOf(m) }))
    .filter((x) => x.p && x.p !== '/')
    .sort((a, b) => b.p.length - a.p.length);
  const rp = route.path;
  for (const { id, p } of pathsExtras) {
    if (rp === p || rp.startsWith(p + '/')) return id;
  }
  return null;
});

function isMenuActive(m: MenuTreeVO): boolean {
  return activeMenuId.value === m.id;
}

function queryActionVal(): string | undefined {
  const q = route.query.action;
  if (q == null) return undefined;
  if (Array.isArray(q)) return q[0] ?? undefined;
  return q;
}

/** 仅精确匹配路径，避免 /paper 在访问 /paper/manage 时也被高亮 */
const PRIMARY_EXACT_MATCH_PATHS = new Set(['/paper']);

function isPrimaryActive(item: PrimaryNav): boolean {
  const p = item.path.replace(/\/$/, '') || '/';
  const rp = route.path.replace(/\/$/, '') || '/';
  const qa = queryActionVal();
  if (item.queryActive) {
    return rp === p && qa === item.queryActive;
  }
  if (item.excludeQuery) {
    return rp === p && qa !== item.excludeQuery;
  }
  if (p === '/') return rp === '/';
  if (PRIMARY_EXACT_MATCH_PATHS.has(p)) {
    return rp === p;
  }
  return rp === p || rp.startsWith(p + '/');
}

function goPrimary(item: PrimaryNav) {
  if (item.queryActive) {
    router.push({ path: item.path, query: { action: item.queryActive } });
    return;
  }
  if (item.path === '/question/manage') {
    router.push({ path: item.path, query: {} });
    return;
  }
  router.push(item.path);
}

function goMenu(m: MenuTreeVO) {
  router.push(pathOf(m));
}
</script>

<style scoped>
.sidebar {
  width: 232px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid rgba(52, 152, 219, 0.15);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  box-shadow: 4px 0 24px rgba(44, 62, 240, 0.06);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 16px;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: #fff;
  cursor: pointer;
  user-select: none;
}

.brand-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.25;
}

.brand-sub {
  font-size: 11px;
  opacity: 0.88;
  margin-top: 2px;
}

.sidebar-nav {
  padding: 12px 10px 24px;
  flex: 1;
  overflow-y: auto;
}

.nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  margin-bottom: 4px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #2c3e50;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.nav-item:hover {
  background: rgba(52, 152, 219, 0.08);
  color: var(--primary);
}

.nav-item.active {
  background: rgba(52, 152, 219, 0.14);
  color: var(--primary);
  font-weight: 600;
}

.nav-ico {
  font-size: 18px;
  flex-shrink: 0;
}

.nav-item-extra {
  font-weight: 500;
  color: #5a6c7d;
}

.nav-divider {
  margin: 16px 6px 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #94a3b8;
  text-transform: uppercase;
}
</style>
