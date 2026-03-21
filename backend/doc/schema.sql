CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '姓名',
  `role` VARCHAR(32) NOT NULL COMMENT '角色：ADMIN/TEACHER/STUDENT',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0 禁用，1 启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `teacher_id` BIGINT NOT NULL COMMENT '授课教师',
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `code` VARCHAR(64) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_teacher` (`teacher_id`),
  CONSTRAINT `fk_course_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程';

CREATE TABLE IF NOT EXISTS `course_student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`, `student_id`),
  KEY `idx_cs_student` (`student_id`),
  CONSTRAINT `fk_cs_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cs_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程学生';

CREATE TABLE IF NOT EXISTS `knowledge_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `parent_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(128) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_kp_course` (`course_id`),
  KEY `idx_kp_parent` (`parent_id`),
  CONSTRAINT `fk_kp_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_kp_parent` FOREIGN KEY (`parent_id`) REFERENCES `knowledge_point` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点';

CREATE TABLE IF NOT EXISTS `question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `knowledge_point_id` BIGINT NOT NULL,
  `type` VARCHAR(32) NOT NULL,
  `stem` TEXT NOT NULL,
  `options_json` TEXT DEFAULT NULL,
  `answer` TEXT NOT NULL,
  `analysis` TEXT DEFAULT NULL,
  `score_default` DECIMAL(10,2) NOT NULL DEFAULT 10.00,
  `difficulty` TINYINT DEFAULT 1,
  `creator_id` BIGINT DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `review_status` VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PENDING/PUBLISHED/REJECTED',
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_q_course` (`course_id`),
  KEY `idx_q_kp` (`knowledge_point_id`),
  KEY `idx_q_pool` (`course_id`, `type`, `knowledge_point_id`, `difficulty`, `status`, `review_status`),
  CONSTRAINT `fk_q_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_q_kp` FOREIGN KEY (`knowledge_point_id`) REFERENCES `knowledge_point` (`id`),
  CONSTRAINT `fk_q_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题';

CREATE TABLE IF NOT EXISTS `question_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT NOT NULL,
  `version_no` INT NOT NULL COMMENT '该快照对应的版本号',
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
  `editor_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qv_q_v` (`question_id`, `version_no`),
  KEY `idx_qv_question` (`question_id`),
  CONSTRAINT `fk_qv_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_qv_editor` FOREIGN KEY (`editor_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试题历史版本快照';

CREATE TABLE IF NOT EXISTS `paper` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `title` VARCHAR(256) NOT NULL,
  `mode` VARCHAR(16) NOT NULL COMMENT 'MANUAL / AUTO',
  `total_score` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `random_seed` BIGINT DEFAULT NULL,
  `rules_json` TEXT DEFAULT NULL,
  `creator_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_paper_course` (`course_id`),
  CONSTRAINT `fk_paper_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_paper_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷';

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

CREATE TABLE IF NOT EXISTS `paper_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `paper_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  `question_order` INT NOT NULL DEFAULT 0,
  `score` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pq_paper` (`paper_id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
  CONSTRAINT `fk_pq_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pq_q` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目';

CREATE TABLE IF NOT EXISTS `exam` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `paper_id` BIGINT NOT NULL,
  `title` VARCHAR(256) NOT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `duration_minutes` INT NOT NULL DEFAULT 60,
  `pass_score` DECIMAL(10,2) DEFAULT NULL COMMENT '及格线',
  `score_published` TINYINT NOT NULL DEFAULT 0 COMMENT '成绩是否已发布',
  `shuffle_questions` TINYINT NOT NULL DEFAULT 1 COMMENT '题目乱序',
  `shuffle_options` TINYINT NOT NULL DEFAULT 1 COMMENT '客观题选项乱序',
  `switch_blur_limit` INT DEFAULT NULL COMMENT '切屏次数上限',
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  `creator_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_exam_course` (`course_id`),
  CONSTRAINT `fk_exam_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_exam_paper` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`),
  CONSTRAINT `fk_exam_creator` FOREIGN KEY (`creator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试';

CREATE TABLE IF NOT EXISTS `exam_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  `started_at` DATETIME DEFAULT NULL,
  `submitted_at` DATETIME DEFAULT NULL,
  `total_score` DECIMAL(10,2) DEFAULT NULL,
  `shuffle_seed` BIGINT DEFAULT NULL,
  `question_order_json` TEXT DEFAULT NULL,
  `switch_blur_count` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_student` (`exam_id`, `student_id`),
  KEY `idx_er_student` (`student_id`),
  CONSTRAINT `fk_er_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_er_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录';

CREATE TABLE IF NOT EXISTS `exam_answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_record_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  `user_answer` TEXT DEFAULT NULL,
  `score` DECIMAL(10,2) DEFAULT NULL,
  `is_correct` TINYINT DEFAULT NULL,
  `graded_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_question` (`exam_record_id`, `question_id`),
  KEY `idx_ea_question` (`question_id`),
  CONSTRAINT `fk_ea_record` FOREIGN KEY (`exam_record_id`) REFERENCES `exam_record` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ea_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作答明细';

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT NULL,
  `name` VARCHAR(64) NOT NULL,
  `path` VARCHAR(256) DEFAULT NULL,
  `icon` VARCHAR(64) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `perms` VARCHAR(256) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_menu_parent` (`parent_id`),
  CONSTRAINT `fk_menu_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_menu` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role` VARCHAR(32) NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role`, `menu_id`),
  KEY `idx_rm_menu` (`menu_id`),
  CONSTRAINT `fk_rm_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单';

CREATE TABLE IF NOT EXISTS `file_resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `original_name` VARCHAR(512) NOT NULL,
  `stored_name` VARCHAR(128) NOT NULL,
  `relative_path` VARCHAR(512) NOT NULL,
  `content_type` VARCHAR(128) DEFAULT NULL,
  `size_bytes` BIGINT NOT NULL,
  `uploader_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_fr_uploader` (`uploader_id`),
  CONSTRAINT `fk_fr_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源';

CREATE TABLE IF NOT EXISTS `wrong_book` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `course_id` BIGINT NOT NULL,
  `question_id` BIGINT NOT NULL,
  `wrong_count` INT NOT NULL DEFAULT 1,
  `last_wrong_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wb_student_q` (`student_id`, `question_id`),
  KEY `idx_wb_course` (`course_id`),
  CONSTRAINT `fk_wb_student` FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_wb_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wb_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';
