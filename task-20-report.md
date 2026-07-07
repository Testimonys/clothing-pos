# Task 20 报告: 构建配置 + 启动脚本 + 部署文档

## 已完成工作

### 1. 启动脚本
- **start.sh** (macOS/Linux): `java -Xms256m -Xmx512m -jar clothing-pos-1.0.0.jar`，含文件存在性检查及提示信息
- **start.bat** (Windows): 同上 + `pause`，含文件存在性检查及提示信息

### 2. 部署文档 DEPLOY.md
涵盖以下内容：
- 环境要求（JDK 17+、MySQL 8.0+、Node.js 18+）
- 数据库创建（SQL 脚本）
- application.yml 配置说明（含所有关键配置项）
- 完整构建步骤（前端 + 后端）
- 启动方式（macOS/Linux/Windows/后台运行/外部配置）
- 访问方式与默认账号（admin/admin123）
- 扫码枪配置（USB 键盘模拟模式）
- 打印机配置（串口/USB/网络）
- 数据备份（自动 + 手动）
- 安全提醒（修改默认密码、JWT 密钥、HTTPS 等）
- 常见问题 FAQ

### 3. 构建验证
```bash
cd frontend && npm run build   # 成功，1680 modules，6.29s
cd .. && mvn clean package -DskipTests  # 成功，7.526s
ls -la target/clothing-pos-1.0.0.jar   # 存在，56MB
```

### 4. Git 提交
提交 `2f6e622`，包含：
- `start.sh` (mode 755)
- `start.bat`
- `DEPLOY.md`
