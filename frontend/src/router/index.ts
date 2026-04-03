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
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
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
        name: 'Course',
        component: () => import('@/views/course/CourseList.vue'),
        meta: { title: '课程中心' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgeManage.vue'),
        meta: { title: '知识点' }
      },
      {
        path: 'question',
        name: 'Question',
        component: () => import('@/views/question/QuestionBank.vue'),
        meta: { title: '题库' }
      },
      {
        path: 'paper',
        name: 'Paper',
        component: () => import('@/views/paper/PaperCompose.vue'),
        meta: { title: '试卷与组卷' }
      },
      {
        path: 'tools/file',
        name: 'FileTools',
        component: () => import('@/views/tools/FileTools.vue'),
        meta: { title: '文件与文本' }
      },
      {
        path: 'exam',
        name: 'Exam',
        component: () => import('@/views/exam/ExamManage.vue'),
        meta: { title: '考试管理' }
      },
      {
        path: 'exam/grading',
        name: 'ExamGrading',
        component: () => import('@/views/exam/ExamGrading.vue'),
        meta: { title: '主观题阅卷' }
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
        meta: { title: '成绩分析' }
      },
      {
        path: 'wrongbook',
        name: 'WrongBook',
        component: () => import('@/views/wrongbook/WrongBook.vue'),
        meta: { title: '错题本' }
      },
      {
        path: ':pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/NotFound.vue'),
        meta: { title: '未找到' }
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
  if (to.path.startsWith('/exam/take') && role && role !== 'STUDENT') {
    ElMessage.warning('「我的考试」仅学生账号可用，请使用演示账号 student / student123');
    next('/');
    return;
  }
  next();
});

export default router;
