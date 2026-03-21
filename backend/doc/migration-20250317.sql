-- 成绩发布、及格线、防作弊（乱序/切屏）、组卷难度比例等字段
-- 执行前请备份数据库

ALTER TABLE `exam`
  ADD COLUMN `pass_score` DECIMAL(10,2) DEFAULT NULL COMMENT '及格线，NULL 表示不设' AFTER `duration_minutes`,
  ADD COLUMN `score_published` TINYINT NOT NULL DEFAULT 0 COMMENT '成绩是否已发布（学生可见排名/统计）' AFTER `pass_score`,
  ADD COLUMN `shuffle_questions` TINYINT NOT NULL DEFAULT 1 COMMENT '题目乱序' AFTER `score_published`,
  ADD COLUMN `shuffle_options` TINYINT NOT NULL DEFAULT 1 COMMENT '客观题选项乱序' AFTER `shuffle_questions`,
  ADD COLUMN `switch_blur_limit` INT DEFAULT NULL COMMENT '切屏次数上限，NULL/0 表示不限制' AFTER `shuffle_options`;

ALTER TABLE `exam_record`
  ADD COLUMN `shuffle_seed` BIGINT DEFAULT NULL COMMENT '本场次乱序种子' AFTER `total_score`,
  ADD COLUMN `question_order_json` TEXT DEFAULT NULL COMMENT '题目顺序(JSON 数组 questionId)' AFTER `shuffle_seed`,
  ADD COLUMN `switch_blur_count` INT NOT NULL DEFAULT 0 COMMENT '切屏/失焦次数' AFTER `question_order_json`;
