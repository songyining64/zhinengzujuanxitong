-- 若数据库在旧版种子下已写入菜单，可将 path 更新为独立路由（与前端 router 一致）。
-- 仅在需要时执行；若 sys_menu 为空则无需执行。

UPDATE sys_menu SET path = 'course/browse' WHERE name = '课程浏览' AND path = 'course';
UPDATE sys_menu SET path = 'course/manage' WHERE name = '课程管理' AND path = 'course';
UPDATE sys_menu SET path = 'knowledge/manage' WHERE name = '知识点管理' AND path = 'knowledge';
UPDATE sys_menu SET path = 'knowledge/browse' WHERE name = '知识点浏览' AND path = 'knowledge';
UPDATE sys_menu SET path = 'question/manage' WHERE name = '题库管理' AND path = 'question';
UPDATE sys_menu SET path = 'question/browse' WHERE name = '题库浏览' AND path = 'question';
UPDATE sys_menu SET path = 'paper/manage' WHERE name = '试卷管理' AND path = 'paper';
UPDATE sys_menu SET path = 'paper/browse' WHERE name = '试卷浏览' AND path = 'paper';
UPDATE sys_menu SET path = 'exam/manage' WHERE name = '考试管理' AND path = 'exam';
