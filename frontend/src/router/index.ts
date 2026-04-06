import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import DefaultLayout from '@/layout/DefaultLayout.vue';
import Login from '@/views/Login.vue';
import Dashboard from '@/views/Dashboard.vue';

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
        meta: { title: '用户管理' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/MenuManage.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'course/browse',
        name: 'CourseBrowse',
        component: () => import('@/views/course/CourseBrowse.vue'),
        meta: { title: '课程浏览' }
      },
      {
        path: 'course/manage',
        name: 'CourseManage',
        component: () => import('@/views/course/CourseManage.vue'),
        meta: { title: '课程管理' }
      },
      {
        path: 'knowledge/manage',
        name: 'KnowledgeManage',
        component: () => import('@/views/knowledge/KnowledgeManage.vue'),
        meta: { title: '知识点管理' }
      },
      {
        path: 'knowledge/browse',
        name: 'KnowledgeBrowse',
        component: () => import('@/views/knowledge/KnowledgeBrowse.vue'),
        meta: { title: '知识点浏览' }
      },
      {
        path: 'question/manage',
        name: 'QuestionManage',
        component: () => import('@/views/question/QuestionManage.vue'),
        meta: { title: '题库管理' }
      },
      {
        path: 'question/browse',
        name: 'QuestionBrowse',
        component: () => import('@/views/question/QuestionBrowse.vue'),
        meta: { title: '题库浏览' }
      },
      {
        path: 'paper/manage',
        name: 'PaperManage',
        component: () => import('@/views/paper/PaperManage.vue'),
        meta: { title: '试卷管理' }
      },
      {
        path: 'paper/browse',
        name: 'PaperBrowse',
        component: () => import('@/views/paper/PaperBrowse.vue'),
        meta: { title: '试卷浏览' }
      },
      {
        path: 'exam',
        name: 'ExamManage',
        component: () => import('@/views/exam/ExamManage.vue'),
        meta: { title: '考试管理' }
      },
      {
        path: 'exam/analytics',
        name: 'ExamAnalytics',
        component: () => import('@/views/exam/ExamAnalytics.vue'),
        meta: { title: '成绩统计' }
      },
      {
        path: 'exam/stats',
        name: 'ExamStats',
        component: () => import('@/views/exam/ExamAnalytics.vue'),
        meta: { title: '成绩统计' }
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

router.beforeEach((to, from, next) => {
  const isPublic = to.meta.public;
  const token = localStorage.getItem('access_token');
  if (!isPublic && !token) {
    next('/login');
    return;
  }
  const role = localStorage.getItem('role');
  if (to.path.startsWith('/exam/take') && role && role !== 'STUDENT' && role !== 'ADMIN') {
    ElMessage.warning('仅学生账号可进入答题');
    next('/');
    return;
  }
  if (to.path.startsWith('/system/menu') && role !== 'ADMIN') {
    ElMessage.warning('仅管理员可访问菜单管理');
    next('/');
    return;
  }
  next();
});

export default router;

