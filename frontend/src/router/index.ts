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
        path: 'course',
        name: 'CourseList',
        component: () => import('@/views/course/CourseList.vue'),
        meta: { title: '课程' }
      },
      {
        path: 'knowledge',
        name: 'KnowledgeList',
        component: () => import('@/views/knowledge/KnowledgeList.vue'),
        meta: { title: '知识点' }
      },
      {
        path: 'question',
        name: 'QuestionList',
        component: () => import('@/views/question/QuestionList.vue'),
        meta: { title: '题库' }
      },
      {
        path: 'paper',
        name: 'PaperList',
        component: () => import('@/views/paper/PaperList.vue'),
        meta: { title: '试卷' }
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
        path: 'exam/stats',
        name: 'ExamStats',
        component: () => import('@/views/exam/ExamStats.vue'),
        meta: { title: '成绩统计' }
      },
      {
        path: 'exam',
        name: 'ExamManage',
        component: () => import('@/views/exam/ExamManage.vue'),
        meta: { title: '考试管理' }
      },
      {
        path: 'wrongbook',
        name: 'WrongBook',
        component: () => import('@/views/wrongbook/WrongBookList.vue'),
        meta: { title: '错题本' }
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
  next();
});

export default router;

