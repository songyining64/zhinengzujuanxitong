-- 已有库补「主观题阅卷」菜单（若种子未执行或需手工升级）
-- 执行前请确认不存在同名同 path 的菜单

INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `icon`, `sort_order`, `perms`, `create_time`)
VALUES (NULL, '主观题阅卷', 'exam/grading', NULL, 55, 'exam:manage', NOW());

SET @mid = LAST_INSERT_ID();

INSERT IGNORE INTO `sys_role_menu` (`role`, `menu_id`) VALUES
  ('ADMIN', @mid),
  ('TEACHER', @mid);
