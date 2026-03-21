-- P1 功能：试题审核字段、组卷模板、组卷审计日志（在已有库上执行一次）
-- MySQL 8+

ALTER TABLE `question`
  ADD COLUMN `review_status` VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PENDING/PUBLISHED/REJECTED' AFTER `status`;

CREATE INDEX `idx_q_pool` ON `question` (`course_id`, `type`, `knowledge_point_id`, `difficulty`, `status`, `review_status`);

CREATE TABLE IF NOT EXISTS `paper_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `rules_json` TEXT NOT NULL COMMENT 'PaperAutoGenRequest 不含 title',
  `creator_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pt_course` (`course_id`),
  CONSTRAINT `fk_pt_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pt_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组卷模板';

CREATE TABLE IF NOT EXISTS `paper_generation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `paper_id` BIGINT DEFAULT NULL,
  `course_id` BIGINT NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `mode` VARCHAR(32) NOT NULL COMMENT 'AUTO / TEMPLATE',
  `rules_json` TEXT DEFAULT NULL,
  `duration_ms` INT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pgl_course_time` (`course_id`, `create_time`),
  KEY `idx_pgl_paper` (`paper_id`),
  CONSTRAINT `fk_pgl_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pgl_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_pgl_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组卷操作审计';
