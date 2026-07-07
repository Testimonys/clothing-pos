# 华兴服装店库存管理与收银系统 — 设计文档

> 日期：2026-07-07 | 状态：已确认

## 一、系统概览

| 项目 | 内容 |
|------|------|
| 名称 | 华兴服装店库存管理与收银系统 |
| 定位 | 自有服装商店，本地部署，局域网访问 |
| 用户 | 2-3 人，角色：老板（BOSS）/ 店员（CLERK） |
| 部署 | 一台电脑装 JDK 17 + MySQL 8.0，`java -jar` 启动 |
| 访问 | 局域网浏览器打开 `http://主机IP:8080` |
| 硬件 | 已有电脑 + USB 扫码枪 + USB 小票打印机 |

## 二、技术选型

| 层 | 选型 | 说明 |
|----|------|------|
| 前端 | Vue 3 + Vue Router + Pinia + Element Plus | SPA，Element Plus 提供表格/表单组件 |
| 条码生成 | JsBarcode（前端库） | 生成 Code128 条码图 |
| 后端 | Spring Boot 3.x + Spring Security + JPA | 单体 fat jar 部署 |
| 数据库 | MySQL 8.0 | 本地安装，InnoDB 引擎 |
| 扫码 | 扫码枪模拟键盘输入 | 前端监听 input 回车键，无需驱动 |
| 小票打印 | 后端 ESC/POS 指令 + USB 直发 | 静默打印，无弹窗 |
| 条码标签打印 | 前端 JsBarcode + `window.print()` | 浏览器打印条码标签 |

## 三、功能模块

```
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────┐
│ 收银台   │ │ 商品管理 │ │ 库存管理 │ │ 销售记录 │ │  系统设置  │
│ /pos    │ │/product │ │ /stock  │ │ /orders │ │ /setting │
├─────────┤ ├─────────┤ ├─────────┤ ├─────────┤ ├──────────┤
│扫码收银  │ │商品CRUD │ │库存查询 │ │订单列表  │ │用户管理👤│
│交易明细  │ │SKU管理  │ │入库录入 │ │订单详情  │ │分类管理  │
│单品折扣  │ │条码管理 │ │库存流水 │ │补打小票  │ │备份管理  │
│整单折扣  │ │条码打印 │ │         │ │         │ │         │
│自定义实收 │ │         │ │         │ │         │ │         │
│找零计算  │ │         │ │         │ │         │ │         │
│小票打印  │ │         │ │         │ │         │ │         │
├─────────┤ ├─────────┤ ├─────────┤ ├─────────┤ ├──────────┤
│   全员   │ │  全员    │ │  全员    │ │  全员    │ │ 👤仅老板  │
└─────────┘ └─────────┘ └─────────┘ └─────────┘ └──────────┘
```

## 四、数据库设计

### 表结构

#### category（商品分类）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| name | VARCHAR(50) | 分类名称 |
| parent_id | BIGINT | 父级分类，NULL 表示顶级 |
| sort_order | INT | 排序 |
| create_time | DATETIME | 创建时间 |

#### product（商品主表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| category_id | BIGINT FK | 分类 |
| name | VARCHAR(200) | 商品名称 |
| image_url | VARCHAR(500) | 图片地址 |
| cost_price | DECIMAL(10,2) | 默认进价 |
| sell_price | DECIMAL(10,2) | 默认售价 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### product_sku（SKU 规格）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| product_id | BIGINT FK | 所属商品 |
| color | VARCHAR(50) | 颜色 |
| size | VARCHAR(50) | 尺码 |
| barcode | VARCHAR(100) UNIQUE | 条码，唯一 |
| stock_qty | INT DEFAULT 0 | 当前库存数量 |
| create_time | DATETIME | 创建时间 |

#### stock_record（库存流水）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| sku_id | BIGINT FK | SKU |
| product_name | VARCHAR(200) | 冗余：交易时商品名称 |
| sku_spec | VARCHAR(100) | 冗余：交易时规格，如"白色/M" |
| type | VARCHAR(20) | INBOUND（入库）/ OUTBOUND（出库） |
| qty | INT | 变化数量 |
| before_qty | INT | 变化前库存 |
| after_qty | INT | 变化后库存 |
| operator_id | BIGINT FK | 操作人 |
| create_time | DATETIME | 操作时间 |

#### order（销售订单）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| order_no | VARCHAR(30) UNIQUE | 单号，如 POS202607070013 |
| total_amount | DECIMAL(10,2) | 原价合计 |
| discount | DECIMAL(10,2) | 整单折扣金额 |
| pay_amount | DECIMAL(10,2) | 实付金额 |
| receive_amount | DECIMAL(10,2) | 实收金额 |
| change_amount | DECIMAL(10,2) | 找零 |
| pay_method | VARCHAR(20) | CASH / WECHAT / ALIPAY |
| cashier_id | BIGINT FK | 收款人 |
| member_id | BIGINT | 预留：会员 ID |
| create_time | DATETIME(0) | 创建时间，精确到秒 |

#### order_item（订单明细）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| order_id | BIGINT FK | 所属订单 |
| sku_id | BIGINT FK | SKU |
| product_name | VARCHAR(200) | 冗余：交易时商品名称 |
| sku_spec | VARCHAR(100) | 冗余：交易时规格 |
| unit_price | DECIMAL(10,2) | 单价 |
| qty | INT | 数量 |
| discount | DECIMAL(10,2) | 单品折扣金额 |
| sub_total | DECIMAL(10,2) | 小计 |
| barcode | VARCHAR(100) | 交易时条码 |

#### sys_user（用户）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| username | VARCHAR(50) UNIQUE | 登录名 |
| password | VARCHAR(200) | BCrypt 加密 |
| display_name | VARCHAR(50) | 显示名称 |
| role | VARCHAR(20) | BOSS / CLERK |
| enabled | TINYINT(1) | 启用状态 |
| create_time | DATETIME | 创建时间 |

### ER 关系图

```
category ──→ product ──→ product_sku
                              │
              ┌───────────────┼───────────────┐
              │               │               │
         stock_record    order_item          │
                              │               │
                           order ──→ sys_user
```

## 五、后端 API

### 认证 `/api/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /login | 登录，返回 token |
| POST | /logout | 登出 |
| GET | /current-user | 当前用户信息 |

### 商品 `/api/product`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 商品列表（搜索/分类/分页） |
| GET | /{id} | 商品详情（含 SKU 列表） |
| POST | / | 新增商品（含 SKU） |
| PUT | /{id} | 编辑商品 |
| DELETE | /{id} | 删除商品 |
| GET | /barcode/{code} | 扫码查询（收银台核心接口） |

### SKU `/api/product/{productId}/sku`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | / | 添加 SKU |
| PUT | /{skuId} | 编辑 SKU |
| DELETE | /{skuId} | 删除 SKU |
| POST | /{skuId}/barcode | 生成条码并返回条码号 |

### 库存 `/api/stock`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | / | 库存查询（筛选） |
| GET | /{skuId}/flow | SKU 库存流水 |
| POST | /inbound | 入库（支持多条 SKU 一次提交） |

### 订单 `/api/order`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | / | 创建订单（收银结账） |
| GET | / | 订单列表（筛选/分页） |
| GET | /{id} | 订单详情 |
| POST | /{id}/print | 补打小票 |

### 文件上传 `/api/upload`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /image | 上传商品图片，返回图片 URL |

### 设置 `/api/setting`（老板权限）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /users | 用户列表 |
| POST | /users | 新增用户 |
| PUT | /users/{id} | 编辑用户 |
| DELETE | /users/{id} | 删除用户 |
| GET | /categories | 分类列表 |
| POST | /categories | 新增分类 |
| PUT | /categories/{id} | 编辑分类 |
| POST | /backup | 数据库备份 |
| GET | /backup/list | 备份文件列表 |
| GET | /backup/download/{name} | 下载备份文件 |

## 六、关键业务流程

### 收银流程

```
扫码/输入条码 → 查询商品信息 → 加入当前交易明细
→ 继续扫码或手动调整数量/单品折扣
→ 点「结账」→ 弹窗选择支付方式
→ 输入实收金额（默认等于应收）→ 自动计算找零
→ 确认是否打印小票（默认勾选）
→ 点击「确认收款」→ 后端一个事务：
    ① 扣减库存
    ② 写入 stock_record（出库流水）
    ③ 保存 order + order_item
    ④ 若勾选打印，发送 ESC/POS 指令到 USB 打印机
→ 弹窗显示成功 + 单号 + "开始下一单"
```

**结账弹窗结构：**
- 应收金额（只读）= 明细合计 - 整单折扣
- 实收金额（可编辑，默认等于应收）
- 找零（自动计算）= 实收 - 应收
- 实收 < 应收 时弹出警告提示
- 支付方式：现金 / 微信 / 支付宝
- 打印小票勾选框（默认勾选）

### 入库流程

```
扫码/输入条码 → 判断条码是否存在
  ├─ 已存在 → 数量累加 +1
  └─ 不存在 → 弹窗选择已有商品+规格 或 创建新商品

「手动添加」按钮 → 处理无条码商品
→ 待入库明细列表
→ 点击「确认入库」→ 后端一个事务：
    ① 更新 product_sku.stock_qty
    ② 写入 stock_record（入库流水）

→ 确认后弹窗：无条码 SKU 提示生成条码
  → 条码规则：HUAXING + YYYYMMDD + 三位序号
  → 选中可打印条码标签
```

## 七、打印设计

### 小票打印
- 技术：后端生成 ESC/POS 指令，USB 端口直发打印机，静默打印
- 模板信息：店名（华兴服装店）、单号、时间（精确到秒）、商品明细、合计、折扣、实付、支付方式

### 条码标签打印
- 技术：前端 JsBarcode 生成 Code128 → `window.print()` 浏览器打印
- 标签内容：条码图 + 条码号 + 商品名称 + 规格

### 小票模板示例
```
══════════════════════════
     华兴服装店
══════════════════════════
单号: POS202607060013
时间: 2026-07-06 15:23:17
──────────────────────────
商品          数量   小计
纯棉T恤 白/M   ×1  ¥99.00
牛仔裤 蓝/L    ×2 ¥358.00
──────────────────────────
合计:         ¥457.00
整单折扣:      9.0折
实付:         ¥411.30
支付方式:     微信
──────────────────────────
谢谢惠顾！
══════════════════════════
```

## 八、数据备份
- 位置：系统设置 → 备份管理（仅老板可见）
- 一键备份：后端调用 `mysqldump` 生成 SQL 文件
- 文件命名：`backup_YYYYMMDD_HHmmss.sql`
- 自动备份：每天首次启动应用时自动执行，保留最近 7 天
- 支持下载备份文件

## 九、扩展预留
| 预留项 | 方式 |
|--------|------|
| SKU 独立管理 | 表结构已分离 product / product_sku |
| Excel 批量导入 | API 层预留接口 |
| 会员管理 | order 表预留 member_id |
| 盘点 | stock_record 已记录完整变化前后值 |

## 十、本期不做
- 退货/换货系统处理
- 盘点单功能
- 供应商管理
- 多维度销售统计分析

## 十一、图片管理

- 上传：`POST /api/upload/image`，接受 multipart/form-data，文件存到 `./uploads/` 目录
- 访问：Spring Boot 配置静态资源映射，`/uploads/**` → `file:./uploads/`
- 图片 URL 存入 `product.image_url`，如 `/uploads/20260707_153000_abc.jpg`
- 前端：商品表单中使用 `el-upload` 组件上传，支持预览

## 十二、项目结构（规划）

```
clothing-pos/
├── frontend/                    # Vue 3 前端
│   └── src/
│       ├── views/
│       │   ├── PosView.vue      # 收银台
│       │   ├── ProductView.vue  # 商品管理
│       │   ├── StockView.vue    # 库存管理
│       │   ├── OrderView.vue    # 销售记录
│       │   └── SettingView.vue  # 系统设置
│       ├── router/
│       ├── stores/
│       ├── api/
│       └── components/
├── src/main/java/com/huaxing/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── config/
│   │   └── SecurityConfig.java
│   └── printer/
│       └── EscPosPrinter.java   # 小票打印服务
├── src/main/resources/
│   ├── application.yml
│   └── static/                   # Vue build 产物
└── pom.xml
```
