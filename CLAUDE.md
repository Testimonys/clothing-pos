# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

华兴服装店库存管理与收银系统 — 本地部署的 Spring Boot 3.2 + Vue 3 全栈应用，服务于自有服装商店（2-3 人使用，老板 + 店员角色）。

## 构建与运行

```bash
# 前端开发（独立运行，API 代理到 localhost:8080）
cd frontend && npm run dev          # → http://localhost:5173

# 前端构建（输出到 ../src/main/resources/static/）
cd frontend && npm run build

# 后端编译
mvn compile

# 完整打包（前端 build + 后端 package → fat jar）
cd frontend && npm run build && cd .. && mvn clean package -DskipTests

# 启动（需先创建 MySQL 数据库）
java -jar target/clothing-pos-1.0.0.jar

# H2 内存数据库开发模式（无需 MySQL）
java -jar target/clothing-pos-1.0.0.jar --spring.profiles.active=h2
# 默认端口 8181，H2 控制台: http://localhost:8181/h2-console
```

**默认账号：** `admin` / `admin123`（老板），`user` / `user123`（店员）

## 技术架构

```
浏览器 → Vue 3 SPA (Vite + Element Plus)
           ↓ REST /api/*
       Spring Boot 3.2 (fat jar)
           ↓ JPA/Hibernate
       MySQL 8.0 (或 H2 开发模式)
```

- **前后端分离开发，部署合并** — 开发时 Vite dev server 代理 API；部署时 Vue build 产物放入 `src/main/resources/static/`，打成一个 fat jar
- **无状态 JWT 认证** — jjwt 0.12.x，Bearer token，24h 过期；SecurityConfig 保护 `/api/**`，前端静态资源全放行
- **BOSS/CLERK 角色控制** — SecurityConfig 控制 `/api/setting/**` 需 ROLE_BOSS；前端路由守卫 `meta.bossOnly` 拦截非老板用户

## 关键设计决策

### 数据库
- **7 张表**：category, product, product_sku, stock_record, sys_order, order_item, sys_user
- **product ↔ product_sku 分离** — 商品主表存基本信息，SKU 表存颜色/尺码/条码/库存，预留独立 SKU 管理扩展
- **乐观锁** — `product_sku.version` 字段（`@Version`），防止并发扣库存超卖
- **流水冗余** — `stock_record` 和 `order_item` 冗余 `product_name` + `sku_spec`，历史记录不随商品改名而变
- **枚举列** — schema.sql 中 `type`、`pay_method`、`role` 使用 MySQL ENUM 类型（非 VARCHAR），匹配 Hibernate 映射
- **`sys_order`** — 表名加 `sys_` 前缀避免 MySQL `order` 保留字冲突
- **H2 模式** — 需 `spring.jpa.defer-datasource-initialization: true` 确保 DDL 先于 data-h2.sql 执行

### 订单单号
- 格式：`POS` + yyyyMMdd + 4 位序号
- 序号 = 当日订单数 COUNT + 1（非 MAX(id)，避免 ID 空洞跳跃）
- 生成方法 `synchronized`，防止同 JVM 内并发重复

### 条码
- 厂家条码直接使用；自生成条码规则：`HUAXING` + yyyyMMdd + 三位序号
- 生成方法 `synchronized` 保护，DB 层 `barcode UNIQUE` 约束兜底

### ESC/POS 小票打印
- 后端生成 ESC/POS 指令（GBK 编码中文），直接写 USB 端口
- macOS 端口 `/dev/cu.usbprinter`，Linux `/dev/usb/lp0`，Windows 需配置
- 写入失败自动降级输出到 `receipt_debug.txt`

### 备份
- 后端调用 `mysqldump`，通过 `--defaults-extra-file` 临时文件传递密码（不在进程列表中暴露）
- 文件名 `backup_yyyyMMdd_HHmmss.sql`，自动清理 7 天前的备份
- 路径穿越防护：`normalize()` + startsWith 校验

### 图片上传
- `POST /api/upload/image`，校验 jpg/png/gif，存储到 `./uploads/`
- 通过 WebMvcConfig 映射 `/uploads/**` → 本地目录
- SecurityConfig 中 `/uploads/**` 已放行

### 前端
- Vue Router history 模式，WebMvcConfig 配置 SPA 回退：非 `/api/` 路径 → `forward:/index.html`
- Token 存 localStorage，Axios 拦截器自动附加 Authorization header
- 收银台输入框自动聚焦，F12 快捷键结账
- 扫码枪模拟键盘输入 + 回车，前端监听回车事件

## 配置文件

- `application.yml` — MySQL 生产配置（端口 8080）
- `application-h2.yml` — H2 开发配置（端口 8181）
- 自定义配置前缀 `app.*`：jwt, store, backup, printer, upload

## 包结构

```
com.huaxing
├── config/      # Security, JWT, CORS, WebMvc
├── controller/  # REST 控制器（7 个）
├── dto/         # 请求/响应 DTO（10 个）
├── entity/      # JPA 实体（7 个）+ @PrePersist/@PreUpdate
├── enums/       # PayMethod, StockType, UserRole
├── printer/     # ESC/POS 打印服务
├── repository/  # Spring Data JPA 接口（7 个）
└── service/     # BackupService
```

## 已知注意事项

- JDBC URL 中 `characterEncoding=UTF-8`（非 `utf8mb4`，MySQL Connector/J 需要 Java 字符集名）
- schema.sql 中的 ENUM 值需与 Java 枚举名称完全一致（大写）
- `@Autowired(required = false)` 用于 EscPosPrinter（打印机可能未连接）
- 构建后 jar 约 54MB，需 JDK 17+ 运行
- Lombok 版本手动指定为 1.18.46（兼容 JDK 26+），非 Spring Boot parent 默认版本
