-- 补「文件与文本」菜单（已有库且未跑过新种子时执行）
INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `icon`, `sort_order`, `perms`, `create_time`)
VALUES (NULL, '文件与文本', 'tools/file', NULL, 52, 'course:read', NOW());

SET @mid = LAST_INSERT_ID();

INSERT IGNORE INTO `sys_role_menu` (`role`, `menu_id`) VALUES
  ('ADMIN', @mid),
  ('TEACHER', @mid),
  ('STUDENT', @mid);
