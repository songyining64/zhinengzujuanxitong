import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import DefaultLayout from '@/layout/DefaultLayout.vue';
import Login from '@/views/Login.vue';
import Dashboard from '@/views/Dashboard.vue';

const T = ['ADMIN', 'TEACHER'];
const ALL = ['ADMIN', 'TEACHER', 'STUDENT'];

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '首页' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'course/browse',
        name: 'CourseBrowse',
        component: () => import('@/views/course/CourseBrowse.vue'),
        meta: { title: '课程浏览', roles: ALL }
      },
      {
        path: 'course/manage',
        name: 'CourseManage',
        component: () => import('@/views/course/CourseManage.vue'),
        meta: { title: '课程管理', roles: T }
      },
      {
        path: 'course/preview/:id',
        name: 'CoursePreview',
        component: () => import('@/views/course/CoursePreview.vue'),
        meta: { title: '课程预览', roles: ALL }
      },
      {
        path: 'knowledge/manage',
        name: 'KnowledgeManage',
        component: () => import('@/views/knowledge/KnowledgeManage.vue'),
        meta: { title: '知识点管理', roles: T }
      },
      {
        path: 'knowledge/browse',
        name: 'KnowledgeBrowse',
        component: () => import('@/views/knowledge/KnowledgeBrowse.vue'),
        meta: { title: '知识点浏览', roles: ALL }
      },
      {
        path: 'question/manage',
        name: 'QuestionManage',
        component: () => import('@/views/question/QuestionManage.vue'),
        meta: { title: '题库管理', roles: T }
      },
      {
        path: 'question/browse',
        name: 'QuestionBrowse',
        component: () => import('@/views/question/QuestionBrowse.vue'),
        meta: { title: '题库浏览', roles: ALL }
      },
      {
        path: 'paper/manage',
        name: 'PaperManage',
        component: () => import('@/views/paper/PaperManage.vue'),
        meta: { title: '试卷管理', roles: T }
      },
      {
        path: 'paper/browse',
        name: 'PaperBrowse',
        component: () => import('@/views/paper/PaperBrowse.vue'),
        meta: { title: '试卷浏览', roles: ALL }
      },
      {
        path: 'exam/manage',
        name: 'ExamManage',
        component: () => import('@/views/exam/ExamManage.vue'),
        meta: { title: '考试管理', roles: T }
      },
      {
        path: 'exam/stats',
        name: 'ExamStats',
        component: () => import('@/views/exam/ExamStats.vue'),
        meta: { title: '成绩统计', roles: T }
      },
      {
        path: 'exam/take',
        name: 'ExamStudentList',
        component: () => import('@/views/exam/ExamStudentList.vue'),
        meta: { title: '我的考试' }
      },
      {
        path: 'exam/take/:recordId',
        name: 'ExamTake',
        component: () => import('@/views/exam/ExamTake.vue'),
        meta: { title: '答题' }
      },
      {
        path: 'wrongbook',
        name: 'WrongBookList',
        component: () => import('@/views/wrongbook/WrongBookList.vue'),
        meta: { title: '错题本', roles: ['ADMIN', 'STUDENT'] }
      },
      {
        path: ':pathMatch(.*)*',
        name: 'Placeholder',
        component: () => import('@/views/Placeholder.vue'),
        meta: { title: '功能建设中' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

function roleAllowed(roles: string[] | undefined, role: string | null): boolean {
  if (!roles || roles.length === 0) {
    return true;
  }
  if (!role) {
    return false;
  }
  if (role === 'ADMIN') {
    return true;
  }
  return roles.includes(role);
}

router.beforeEach((to, from, next) => {
  const isPublic = to.meta.public;
  const token = localStorage.getItem('access_token');
  if (!isPublic && !token) {
    next('/login');
    return;
  }
  const role = localStorage.getItem('role');
  const metaRoles = to.matched
    .map((r) => r.meta.roles as string[] | undefined)
    .find((x) => x && x.length);
  if (!roleAllowed(metaRoles, role)) {
    ElMessage.warning('当前账号无权访问该页面');
    next('/');
    return;
  }
  if (to.path.startsWith('/exam/take') && role && role !== 'STUDENT' && role !== 'ADMIN') {
    ElMessage.warning('仅学生账号可进入答题');
    next('/');
    return;
  }
  next();
});

export default router;
