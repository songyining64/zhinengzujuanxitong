import type { KnowledgePoint } from '@/types/models';
import { isSampleCourseId } from '@/data/sampleCourses';

/**
 * 与六门示例课程（900001–900006）对应的知识点及说明，便于无后端或空库时演示。
 * 正式课程仍以接口返回为准。
 */
const SAMPLE_KNOWLEDGE_BY_COURSE = new Map<number, KnowledgePoint[]>([
  [
    900001,
    [
      {
        id: 911001,
        courseId: 900001,
        parentId: null,
        name: '识字与写字',
        sortOrder: 1,
        content: '掌握常用汉字的音、形、义，规范书写，积累词汇并在语境中正确运用。'
      },
      {
        id: 911002,
        courseId: 900001,
        parentId: null,
        name: '阅读',
        sortOrder: 2,
        content: '整体感知文本，理解词句含义，把握结构与作者意图，发展语感与思维品质。'
      },
      {
        id: 911003,
        courseId: 900001,
        parentId: 911002,
        name: '现代文阅读',
        sortOrder: 1,
        content: '记叙、说明、议论等文体阅读策略；信息筛选、概括要点与简单鉴赏。'
      },
      {
        id: 911004,
        courseId: 900001,
        parentId: 911002,
        name: '古诗文阅读',
        sortOrder: 2,
        content: '诵读与积累常见文言词语；借助注释理解浅易古诗文大意与情感。'
      },
      {
        id: 911005,
        courseId: 900001,
        parentId: null,
        name: '写作',
        sortOrder: 3,
        content: '围绕中心选材组篇，做到文从字顺；学习修改，表达真情实感或清楚说明事理。'
      },
      {
        id: 911006,
        courseId: 900001,
        parentId: null,
        name: '口语交际',
        sortOrder: 4,
        content: '注意对象与场合，清楚连贯表达，学会倾听与简要即席发言。'
      }
    ]
  ],
  [
    900002,
    [
      {
        id: 912001,
        courseId: 900002,
        parentId: null,
        name: '数与代数',
        sortOrder: 1,
        content: '数系扩充、代数式运算与变形，用符号表示数量关系并建立简单模型。'
      },
      {
        id: 912002,
        courseId: 900002,
        parentId: 912001,
        name: '数与式',
        sortOrder: 1,
        content: '有理数、实数、整式、分式与根式的概念与运算规则。'
      },
      {
        id: 912003,
        courseId: 900002,
        parentId: 912001,
        name: '方程与不等式',
        sortOrder: 2,
        content: '一元一次与简单二次方程（组）、不等式（组）的建模与求解。'
      },
      {
        id: 912004,
        courseId: 900002,
        parentId: 912001,
        name: '函数初步',
        sortOrder: 3,
        content: '变量与函数概念，一次函数图像与性质，联系实际问题。'
      },
      {
        id: 912005,
        courseId: 900002,
        parentId: null,
        name: '图形与几何',
        sortOrder: 2,
        content: '基本图形（点线面体）的认识、度量与证明入门，发展空间观念。'
      },
      {
        id: 912006,
        courseId: 900002,
        parentId: 912005,
        name: '图形的性质',
        sortOrder: 1,
        content: '相交线平行线、三角形与四边形、圆的基本性质与简单推理。'
      },
      {
        id: 912007,
        courseId: 900002,
        parentId: null,
        name: '统计与概率',
        sortOrder: 3,
        content: '数据收集整理、统计图表阅读；随机现象与简单概率估计。'
      }
    ]
  ],
  [
    900003,
    [
      {
        id: 913001,
        courseId: 900003,
        parentId: null,
        name: '语言知识',
        sortOrder: 1,
        content: '语音、词汇、语法等基础知识的系统梳理与在语境中的运用。'
      },
      {
        id: 913002,
        courseId: 900003,
        parentId: 913001,
        name: '词汇',
        sortOrder: 1,
        content: '主题词汇扩展、词块搭配与词义辨析，重视复现与运用。'
      },
      {
        id: 913003,
        courseId: 900003,
        parentId: 913001,
        name: '语法',
        sortOrder: 2,
        content: '时态、语态、从句等核心结构的表意功能与常见错误纠正。'
      },
      {
        id: 913004,
        courseId: 900003,
        parentId: null,
        name: '听说能力',
        sortOrder: 2,
        content: '听懂大意与关键信息；模仿朗读、简短问答与日常交际用语。'
      },
      {
        id: 913005,
        courseId: 900003,
        parentId: null,
        name: '读写能力',
        sortOrder: 3,
        content: '读懂简易读物与应用文；能写段落与常见体裁的短文，注意衔接与规范。'
      }
    ]
  ],
  [
    900004,
    [
      {
        id: 914001,
        courseId: 900004,
        parentId: null,
        name: '物质的性质与结构',
        sortOrder: 1,
        content: '物质形态、密度与简单物态变化；分子动理论与内能的定性认识。'
      },
      {
        id: 914002,
        courseId: 900004,
        parentId: null,
        name: '运动与力',
        sortOrder: 2,
        content: '机械运动描述、速度与路程；力的概念、重力与摩擦力、牛顿第一定律。'
      },
      {
        id: 914003,
        courseId: 900004,
        parentId: null,
        name: '声与光',
        sortOrder: 3,
        content: '声的产生与传播、乐音三要素；光的直线传播、反射与折射现象。'
      },
      {
        id: 914004,
        courseId: 900004,
        parentId: null,
        name: '电与磁',
        sortOrder: 4,
        content: '简单电路、电流电压与欧姆定律入门；磁体、磁场与电磁联系初识。'
      },
      {
        id: 914005,
        courseId: 900004,
        parentId: null,
        name: '能量',
        sortOrder: 5,
        content: '机械能、内能与能量转化；能源分类与节约能源、安全用电意识。'
      }
    ]
  ],
  [
    900005,
    [
      {
        id: 915001,
        courseId: 900005,
        parentId: null,
        name: '造型·表现',
        sortOrder: 1,
        content: '运用线条、色彩、明暗与多种媒材进行绘画与立体造型，表达感受与想象。'
      },
      {
        id: 915002,
        courseId: 900005,
        parentId: null,
        name: '设计·应用',
        sortOrder: 2,
        content: '从生活需求出发进行图形、版式与手工设计，体会实用与美观的结合。'
      },
      {
        id: 915003,
        courseId: 900005,
        parentId: null,
        name: '欣赏·评述',
        sortOrder: 3,
        content: '了解中外美术门类与代表作品，能用简短语言描述形式特点与个人感受。'
      },
      {
        id: 915004,
        courseId: 900005,
        parentId: null,
        name: '综合·探索',
        sortOrder: 4,
        content: '跨学科主题创作与展示，体验策划、合作与反思的完整过程。'
      }
    ]
  ],
  [
    900006,
    [
      {
        id: 916001,
        courseId: 900006,
        parentId: null,
        name: '物质的组成与结构',
        sortOrder: 1,
        content: '元素、原子、分子与化学式；物质分类与微粒观，重视实验观察。'
      },
      {
        id: 916002,
        courseId: 900006,
        parentId: null,
        name: '物质的化学变化',
        sortOrder: 2,
        content: '化学反应类型、质量守恒、化学方程式书写与简单计算；酸、碱、盐初识。'
      },
      {
        id: 916003,
        courseId: 900006,
        parentId: null,
        name: '化学与社会发展',
        sortOrder: 3,
        content: '化学与资源、环境、健康及材料；实验安全规范与绿色化学意识。'
      }
    ]
  ]
]);

export function resolveKnowledgePointsForCourse(courseId: number, apiList: KnowledgePoint[]): KnowledgePoint[] {
  if (isSampleCourseId(courseId)) {
    const built = SAMPLE_KNOWLEDGE_BY_COURSE.get(courseId);
    return built ? built.map((p) => ({ ...p })) : [];
  }
  return apiList;
}
