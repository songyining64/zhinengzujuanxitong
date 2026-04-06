-- 浏览/管理菜单曾共用同一 path，侧栏会同时高亮。按 perms 改为唯一路径（适用于已有数据的数据库）。
UPDATE sys_menu SET path = 'course/browse' WHERE perms = 'course:read';
UPDATE sys_menu SET path = 'course/manage' WHERE perms = 'course:manage';
UPDATE sys_menu SET path = 'knowledge/browse' WHERE perms = 'knowledge:read';
UPDATE sys_menu SET path = 'knowledge/manage' WHERE perms = 'knowledge:manage';
UPDATE sys_menu SET path = 'question/browse' WHERE perms = 'question:read';
UPDATE sys_menu SET path = 'question/manage' WHERE perms = 'question:manage';
UPDATE sys_menu SET path = 'paper/browse' WHERE perms = 'paper:read';
UPDATE sys_menu SET path = 'paper/manage' WHERE perms = 'paper:manage';
