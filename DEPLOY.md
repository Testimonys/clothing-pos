# 华兴服装店 POS 系统部署文档

## 方式一：Docker 部署（推荐 ⭐）

### 环境要求
- **Docker Desktop**（macOS/Windows）或 **Docker Engine + Docker Compose**（Linux）
- 无需安装 JDK、Maven、Node.js —— 全部在 Docker 内完成

### 快速开始（一键构建 + 启动）

```bash
# luohuai codeX generate: create local environment configuration and fill in COS placeholders before startup.
cp .env.example .env

# 一行命令：构建前端 + 构建后端 + 启动全部服务
docker compose up -d --build

# 首次启动需等待 MySQL 初始化（约 30 秒），之后自动拉起后端和前端
```

### 验证

```bash
# 前端页面
curl http://localhost

# 后端 API
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 访问地址
| 服务 | 地址 | 说明 |
|------|------|------|
| 收银系统 | http://localhost | nginx :80 |
| 后端 API | http://localhost:8080 | Spring Boot |

### 默认账号
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 老板 | admin | admin123 |
| 店员 | user | user123 |

### Docker 服务管理

```bash
docker compose ps                    # 查看运行状态
docker compose logs -f               # 实时日志
docker compose logs backend          # 只看后端日志
docker compose down                  # 停止所有服务（保留数据）
docker compose down -v               # 停止并删除数据库卷（⚠️ 数据丢失）
docker compose up -d --build         # 重新构建镜像并启动
docker compose restart backend       # 重启后端
```

### 数据说明
| 数据 | 存储位置 | 持久化方式 |
|------|---------|-----------|
| MySQL 数据库 | `mysql-data` volume | Docker volume |
| 新上传图片/文件 | 腾讯云 COS | COS 存储桶 |
| 历史本地图片 | `uploads` volume | Docker volume（仅兼容旧 URL） |
| 备份文件 | `backup` volume | Docker volume |

### 构建说明
- 后端 Dockerfile 采用**多阶段构建**：Stage 1 用 Maven 编译打包，Stage 2 用精简 JRE 运行
- 前端 Dockerfile 采用**多阶段构建**：Stage 1 用 Node.js 构建，Stage 2 用 nginx 托管
- `docker compose up --build` 自动完成所有构建步骤，无需手动 `npm run build` 或 `mvn package`

---

## 方式二：本地直接部署（开发/无 Docker 环境）

### 环境要求
- JDK 17+
- MySQL 8.0
- Node.js 18+（构建前端）
- 腾讯云 COS 存储桶，以及仅具备所需权限的服务端密钥

### 腾讯云 COS 配置

本地启动前可在 `application.yml` 中替换占位符，也可以设置以下环境变量：

```bash
# luohuai codeX generate: inject COS configuration into the backend process.
export COS_SECRET_ID=YOUR_SECRET_ID
export COS_SECRET_KEY=YOUR_SECRET_KEY
export COS_BUCKET_NAME=YOUR_BUCKET_NAME-APPID
export COS_REGION=ap-shanghai
export COS_PUBLIC_URL=
export COS_KEY_PREFIX=clothing-pos
```

`COS_PUBLIC_URL` 留空时使用存储桶默认 HTTPS 域名；若使用 CDN 或自定义域名，填写其完整 HTTPS 地址。商品图片需要浏览器能够读取返回的 URL，因此存储桶须配置相应的公开读取或 CDN 访问策略。服务端密钥至少需要目标前缀的 `cos:PutObject` 权限，不要把 SecretKey 放在前端代码或数据库中。

### 步骤

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS clothing_pos DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 构建
cd frontend && npm install && npm run build && cd ..
mvn clean package -DskipTests

# 3. 配置 nginx 指向 frontend/dist/（或直接用 Spring Boot 托管静态文件）

# 4. 启动后端
java -jar target/clothing-pos-1.0.0.jar
```

### 访问
- 有 nginx：http://localhost（nginx 代理 `/api/` → :8080，`/uploads/` → :8080）
- 无 nginx（开发模式）：后端 `http://localhost:8080`，前端 `cd frontend && npm run dev` → `http://localhost:5173`

---

## 2026-09-16 商品管理版本升级

已有数据库需要在启动新版后端前，执行 `scripts/migrations/2026-09-16-product-catalog.sql`，增量加入货号、单位与货号唯一索引。先按备份流程保存现有数据，并确认选中的业务库；仅替换 `schema.sql` 不会升级已有表。

```bash
# luohuai codeX generate: apply the additive catalog migration to the selected existing database.
mysql -u YOUR_DB_USER -p clothing_pos < scripts/migrations/2026-09-16-product-catalog.sql
```

新库仍按原初始化流程创建。商品导入先预览、后由管理员确认写入商品档案，始终不会从 Excel 写入库存；详见 [实现与验收说明](docs/2026-09-16-product-catalog-implementation.md)。

## 2026-09-18 经销商、颜色与条码升级

在完成上一版商品目录迁移后，继续执行 `scripts/migrations/2026-09-18-dealer-color-barcode.sql`。该脚本增加经销商归属、颜色/尺码业务编码、条码流水和经销商变更记录；不会修改库存，只会重建资料完整的 `HUAXING...` 测试条码，Excel 与人工条码保持原值。完整规则见 [经销商、颜色字典与16位条码实现说明](docs/2026-09-18-dealer-color-barcode.md)。

## 扫码枪

USB 键盘模拟模式，即插即用。收银台页面输入框始终自动聚焦，扫码后自动回车查询。确保输入法为英文模式。

## 小票打印机

- macOS: `/dev/cu.usbprinter`
- Linux: `/dev/usb/lp0`
- Windows: 在 `application.yml` 中配置 COM 端口

打印机未连接不影响系统运行（打印失败记录日志，不阻塞）。结账时弹窗可勾选是否打印小票。

## 备份

### 系统内置备份（推荐）
系统设置 → 备份管理 → 一键备份。

### 手动备份
```bash
# Docker 环境
docker exec clothing-mysql mysqldump -u root -padmin123 clothing_pos > backup_$(date +%Y%m%d_%H%M%S).sql

# 本地环境
mysqldump -u root -p clothing_pos > backup_$(date +%Y%m%d_%H%M%S).sql
```

## 安全提醒
1. 首次登录后立即修改默认密码
2. 生产环境修改 `app.jwt.secret`（`application.yml`）
3. 不要对外暴露 8080 端口（nginx 只暴露 80）
4. 定期执行数据库备份
