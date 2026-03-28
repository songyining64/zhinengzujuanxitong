# 推送到 GitHub

远程仓库：<https://github.com/songyining64/zhinengzujuanxitong.git>

在**包含完整 `backend/`、`frontend/` 源码**的项目根目录下执行：

```bash
cd "/你的路径/课程智能组卷系统"

# 若尚未初始化
git init
git branch -M main

# 首次提交
git add .
git commit -m "feat: 课程智能组卷系统（Spring Boot + Vue）"

git remote add origin https://github.com/songyining64/zhinengzujuanxitong.git
# 若已添加过 remote，可改为：git remote set-url origin https://github.com/songyining64/zhinengzujuanxitong.git

# 若 GitHub 上已有提交（例如已有 README），先拉再推：
# git pull origin main --allow-unrelated-histories
# 解决冲突后：
git push -u origin main
```

**认证**：HTTPS 推送时按提示使用 Personal Access Token（GitHub Settings → Developer settings → Tokens），或改用 SSH：

```bash
git remote set-url origin git@github.com:songyining64/zhinengzujuanxitong.git
```

**注意**：根目录已提供 `.gitignore`（忽略 `backend/target/`、`frontend/node_modules/` 等），提交前请确认未把数据库密码等敏感信息写进仓库。
