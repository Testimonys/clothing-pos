# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

华兴服装店库存管理与收银系统 — Spring Boot 3.2 + Vue 3 前后端分离应用，Docker 部署，服务于自有服装商店（2-3 人使用，老板 + 店员角色）。

## 构建与运行

### Docker 部署（推荐）

```bash
# 一键构建 + 启动（前端 + 后端 + MySQL 全部在 Docker 内完成）
docker compose up -d --build

# 前端: http://localhost
# 后端 API: http://localhost:8080
# 停止: docker compose down
```

### 开发模式（前后端分离）

```bash
# 终端 1：启动后端
mvn spring-boot:run                        # → http://localhost:8080

# 终端 2：启动前端 Vite dev server
cd frontend && npm run dev                  # → http://localhost:5173
```

> 注意：开发模式需要本地 MySQL 运行中。

### 无 Docker 本地运行

```bash
# 1. 创建 MySQL 数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS clothing_pos DEFAULT CHARSET utf8mb4;"

# 2. 构建
cd frontend && npm run build && cd .. && mvn clean package -DskipTests

# 3. 启动
java -jar target/clothing-pos-1.0.0.jar
```

**默认账号：** `admin` / `admin123`（老板），`user` / `user123`（店员）

## 技术架构

```
浏览器 → nginx (Vue 静态文件, :80)
              ↓ /api/* (proxy_pass)
         Spring Boot (:8080)
              ↓ MyBatis-Plus
         MySQL 8.0 (:3306)
```

- **前后端完全分离** — 前端 nginx 容器服务 Vue SPA；后端容器服务 REST API；nginx 反向代理 `/api/` 到后端
- **Vue Router history 模式** — nginx `try_files $uri /index.html` 处理 SPA 路由回退
- **无状态 JWT 认证** — jjwt 0.12.x，Bearer token，24h 过期
- **BOSS/CLERK 角色控制** — SecurityConfig `/api/setting/**` 需 ROLE_BOSS；前端路由守卫 `meta.bossOnly`

## 关键设计决策

### 数据库
- **7 张表**：category, product, product_sku, stock_record, sys_order, order_item, sys_user
- **product ↔ product_sku 分离** — 商品主表存基本信息，SKU 表存颜色/尺码/条码/库存
- **乐观锁** — `product_sku.version` 字段（MyBatis-Plus `@Version`），防止并发扣库存超卖
- **流水冗余** — `stock_record` 和 `order_item` 冗余 `product_name` + `sku_spec`
- **枚举列** — MySQL ENUM 类型匹配 MyBatis-Plus `@EnumValue` 映射（非 VARCHAR）
- **`sys_order`** — 加 `sys_` 前缀避免 MySQL `order` 保留字

### 订单单号
- 格式：`POS` + yyyyMMdd + 4 位序号
- 序号 = COUNT + 1（非 MAX(id)），`synchronized` 防并发

### 条码
- 自生成规则：`HUAXING` + yyyyMMdd + 三位序号，`synchronized` 保护

### ESC/POS 小票打印
- ESC/POS 指令（GBK 编码），写 USB 端口；失败降级到 `receipt_debug.txt`
- `@ConditionalOnProperty("app.printer.enabled")`，打印机未连接时不阻塞启动

### 备份
- `mysqldump` + `--defaults-extra-file` 传递密码（不暴露在进程列表）
- 文件 `backup_yyyyMMdd_HHmmss.sql`，保留 7 天

### Docker 注意事项
- **多阶段构建** — 后端 Dockerfile（Maven 编译 → JRE 运行），前端 Dockerfile（Node 构建 → nginx 托管），`docker compose up --build` 一把梭
- MySQL schema.sql + data.sql 挂载到 `/docker-entrypoint-initdb.d/`，容器首次启动自动初始化
- Docker 环境使用 `application-docker.yml`（`spring.sql.init.mode: never`），避免 Spring Boot 重复执行初始化脚本
- `backend` 容器依赖 MySQL healthcheck 通过后才启动；所有服务 `restart: unless-stopped`
- `uploads/` 和 `backup/` 目录使用 Docker volume 持久化
- 无需预先安装 JDK/Maven/Node.js，全部在 Docker 镜像内完成

## 配置文件

| 文件 | 用途 |
|------|------|
| `application.yml` | MySQL 生产配置（端口 8080） |
| `application-docker.yml` | Docker 环境配置（sql.init.mode: never） |
| `docker-compose.yml` | Docker 三服务编排 |
| `frontend/nginx.conf` | nginx 反向代理 + SPA 回退 |
| `frontend/Dockerfile` | 前端多阶段构建（node build + nginx serve） |
| `Dockerfile` | 后端多阶段构建（maven build + JRE run） |

## 包结构

```
com.huaxing
├── config/      # Security, JWT, CORS, WebMvc, MyBatisPlus, MetaObjectHandler
├── controller/  # REST 控制器（7 个）
├── dto/         # 请求/响应 DTO（10 个）
├── entity/      # MyBatis-Plus 实体（7 个）
├── enums/       # PayMethod, StockType, UserRole
├── mapper/      # MyBatis-Plus Mapper 接口（7 个）
├── printer/     # ESC/POS 打印服务
└── service/     # BackupService
```

## 已知注意事项

- JDBC URL `characterEncoding=UTF-8`（非 `utf8mb4`，需 Java 字符集名）
- schema.sql ENUM 值需与 Java 枚举名称完全一致（大写）
- Lombok 版本 1.18.46（兼容 JDK 26+）
- MyBatis-Plus 3.5.7，分页查询 `Page.current` 从 1 开始（非 0-based），Controller 中使用 `page + 1` 转换
- 实体间关联（`@ManyToOne`/`@OneToMany`）已迁移为 MyBatis-Plus 的 `@TableField(exist = false)` + 手动查询
- 级联保存（如 Product → SKU）需手动分步 insert，使用 `@Transactional` 保证原子性
- 前端 dist 和后端 target 目录不提交到 git
