import type { Course } from '@/types/models';

/** 内置学科示例（仅前端展示，ID 段 900001–900006，不与后端真实 ID 冲突即可） */
export type SampleCourse = Course & { isSample: true };

export const SAMPLE_SUBJECT_COURSES: SampleCourse[] = [
  {
    id: 900001,
    name: '语文',
    code: 'SUBJ-CHN',
    description:
      '培养语言文字运用能力，涵盖现代文与古诗文阅读、写作与口语交际，夯实人文素养与书面、口头表达能力。',
    isSample: true
  },
  {
    id: 900002,
    name: '数学',
    code: 'SUBJ-MATH',
    description:
      '系统学习数与代数、图形与几何、统计与概率及应用问题，发展抽象思维、逻辑推理与数学建模素养。',
    isSample: true
  },
  {
    id: 900003,
    name: '英语',
    code: 'SUBJ-ENG',
    description:
      '侧重听、说、读、写综合训练与语篇理解，提升用英语获取信息、表达观点及跨文化交际的初步能力。',
    isSample: true
  },
  {
    id: 900004,
    name: '物理',
    code: 'SUBJ-PHY',
    description:
      '从观察与实验出发认识运动与力、声光热、电与磁及能量转化，建立科学探究习惯与物理观念。',
    isSample: true
  },
  {
    id: 900005,
    name: '美术',
    code: 'SUBJ-ART',
    description:
      '融合绘画、设计与作品欣赏，培养审美感知、创意表现与文化理解，发展形象思维与艺术表达能力。',
    isSample: true
  },
  {
    id: 900006,
    name: '化学',
    code: 'SUBJ-CHEM',
    description:
      '认识物质的组成、结构与变化规律，重视实验安全与定量分析，联系生活、环境与科技中的化学应用。',
    isSample: true
  }
];

export const SAMPLE_COURSE_IDS = new Set(SAMPLE_SUBJECT_COURSES.map((c) => c.id));

/** 与课程管理页一致：内置示例在前，接口课程在后（按 id 去重） */
export function mergeCoursesWithSamples(apiRows: Course[]): Course[] {
  const sampleIds = new Set(SAMPLE_SUBJECT_COURSES.map((c) => c.id));
  const rest = apiRows.filter((c) => !sampleIds.has(c.id));
  return [...SAMPLE_SUBJECT_COURSES, ...rest];
}

export function isSampleCourseId(id: number): boolean {
  return SAMPLE_COURSE_IDS.has(id);
}
