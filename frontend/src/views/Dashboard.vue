<template>
  <div class="dash">
    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="welcome">
          <h2>你好，{{ greeting }}</h2>
          <p class="muted">当前角色：<el-tag size="small" type="info">{{ roleText }}</el-tag></p>
          <p class="lead">
            顶部导航菜单由服务端按权限下发，与接口鉴权一致。可从右侧快捷入口进入常用功能。
          </p>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>快捷入口</template>
          <div class="quick">
            <el-button v-if="isStudent" type="primary" class="w-full" @click="$router.push('/exam/take')">
              我的考试
            </el-button>
            <el-button v-if="isTeacherLike" type="primary" plain class="w-full" @click="$router.push('/course')">
              课程中心
            </el-button>
            <el-button v-if="isTeacherLike" type="primary" plain class="w-full" @click="$router.push('/paper')">
              试卷与组卷
            </el-button>
            <el-button v-if="isTeacherLike" type="success" plain class="w-full" @click="$router.push('/exam/grading')">
              主观题阅卷
            </el-button>
            <el-button v-if="isAdmin" type="warning" plain class="w-full" @click="$router.push('/system/user')">
              用户管理
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt">
      <el-col v-for="card in featureCards" :key="card.title" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="feature" @click="card.path && $router.push(card.path)">
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/store/user';
import { isRole } from '@/composables/usePermission';

const { role, realName, username } = storeToRefs(useUserStore());
const greeting = computed(() => realName.value || username.value || '用户');

const roleText = computed(() => {
  const m: Record<string, string> = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' };
  return m[role.value] || role.value || '—';
});

const isStudent = computed(() => isRole('STUDENT'));
const isTeacherLike = computed(() => isRole('TEACHER', 'ADMIN'));
const isAdmin = computed(() => isRole('ADMIN'));

const featureCards = computed(() => {
  const all = [
    { title: '课程', desc: '创建课程、维护学员名单', path: '/course' },
    { title: '题库与知识点', desc: '录入试题、划分知识树', path: '/question' },
    { title: '智能组卷', desc: '手动/自动组卷与模板', path: '/paper' },
    { title: '考试与成绩', desc: '发布考试、统计分析', path: '/exam' },
    { title: '在线答题', desc: '学生端限时作答与防作弊', path: '/exam/take' },
    { title: '错题本', desc: '错题巩固', path: '/wrongbook' }
  ];
  if (isStudent.value) {
    return all.filter((c) => ['在线答题', '错题本'].includes(c.title));
  }
  if (isAdmin.value) {
    return all;
  }
  return all.filter((c) => c.title !== '在线答题');
});
</script>

<style scoped>
.dash {
  max-width: 1200px;
}
.welcome h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #0f172a;
}
.muted {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 14px;
}
.lead {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  font-size: 14px;
}
.quick {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.quick .el-button {
  margin: 0;
}
.w-full {
  width: 100%;
}
.mt {
  margin-top: 16px;
}
.feature {
  cursor: pointer;
  border-radius: 10px;
  transition: transform 0.15s ease;
  margin-bottom: 16px;
}
.feature:hover {
  transform: translateY(-2px);
}
.feature h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #0f172a;
}
.feature p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
</style>
