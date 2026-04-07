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
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/MenuManage.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'course',
        name: 'Course',
        component: () => import('@/views/course/CourseList.vue'),
        meta: { title: '课程中心' }
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
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/KnowledgeManage.vue'),
        meta: { title: '知识点' }
      },
      {
        path: 'knowledge/browse',
        name: 'KnowledgeBrowse',
        component: () => import('@/views/knowledge/KnowledgeBrowse.vue'),
        meta: { title: '知识点浏览' }
      },
      {
        path: 'question',
        name: 'Question',
        component: () => import('@/views/question/QuestionBank.vue'),
        meta: { title: '题库' }
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
        path: 'paper',
        name: 'Paper',
        component: () => import('@/views/paper/PaperCompose.vue'),
        meta: { title: '试卷与组卷' }
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
        path: 'exam/analytics',
        name: 'ExamAnalytics',
        component: () => import('@/views/exam/ExamAnalytics.vue'),
        meta: { title: '成绩统计' }
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
        path: 'wrongbook',
        name: 'WrongBookList',
        component: () => import('@/views/wrongbook/WrongBookList.vue'),
        meta: { title: '错题本', roles: ['ADMIN', 'STUDENT'] }
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
    ElMessage.warning('「我的考试」仅学生账号可用，请使用演示账号 student / student123');
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
