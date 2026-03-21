-- 试题版本历史（question_version）+ question.version_no
-- 在已执行 migration_v2_p1 的基础上执行

ALTER TABLE `question`
  ADD COLUMN `version_no` INT NOT NULL DEFAULT 1 COMMENT '当前版本号，每次有内容归档后递增' AFTER `review_status`;

CREATE TABLE IF NOT EXISTS `question_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT NOT NULL,
  `version_no` INT NOT NULL COMMENT '该快照对应的版本号（归档时的版本）',
  `knowledge_point_id` BIGINT NOT NULL,
  `type` VARCHAR(32) NOT NULL,
  `stem` TEXT NOT NULL,
  `options_json` TEXT DEFAULT NULL,
  `answer` TEXT NOT NULL,
  `analysis` TEXT DEFAULT NULL,
  `score_default` DECIMAL(10,2) NOT NULL DEFAULT 10.00,
  `difficulty` TINYINT DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `review_status` VARCHAR(32) DEFAULT NULL,
  `editor_id` BIGINT DEFAULT NULL COMMENT '产生该次归档的操作人（修改前快照）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qv_q_v` (`question_id`, `version_no`),
  KEY `idx_qv_question` (`question_id`),
  CONSTRAINT `fk_qv_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_qv_editor` FOREIGN KEY (`editor_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题历史版本快照';
