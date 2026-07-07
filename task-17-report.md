# Task 17 Report: 前端库存管理 + 销售记录页面

## 完成项

### 1. API 层

- **`frontend/src/api/stock.ts`** (新建)
  - `listStock()` - GET /api/stock 分页查询库存列表
  - `getStockFlow(skuId)` - GET /api/stock/{skuId}/flow 查询库存流水
  - `inbound()` - POST /api/stock/inbound 入库操作
  - 类型定义：`StockItemDTO`, `StockRecordDTO`, `InboundRequest`, `InboundItem`, `PageResult<T>`

- **`frontend/src/api/order.ts`** (新建)
  - `listOrders()` - GET /api/order 订单列表（时间范围 + 支付方式筛选 + 分页）
  - `getOrderDetail(id)` - GET /api/order/{id} 订单详情（含商品明细）
  - `reprintOrder(id)` - POST /api/order/{id}/print 补打小票
  - 类型定义：`OrderSummaryDTO`, `OrderDetailDTO`, `OrderItemDTO`, `PageResult<T>`

### 2. StockView.vue (`frontend/src/views/stock/StockView.vue`)

- **库存查询表格**：商品名称、规格(颜色/尺码)、条码、库存数，库存数用 el-tag 颜色区分（0=红色，<=10=警告色，其他=绿色）
- **分页**：支持 pageSize 切换(10/20/50)，页码跳转
- **入库按钮** -> 入库弹窗：
  - 扫码输入区（自动聚焦，回车添加），调用 `queryByBarcode` 查询商品
  - 已存在的 SKU 自动 qty+1
  - 入库明细列表：序号、商品名称、规格、条码、入库数量（可调整）、删除操作
  - 「确认入库」-> POST /api/stock/inbound
- **点击 SKU 行** -> 库存流水弹窗：
  - 展示该 SKU 的 stock_record 列表
  - 列：时间、变动类型(入库/出库 tag)、变动数量(+/-)、变动前、变动后、商品名称

### 3. OrdersView.vue (`frontend/src/views/orders/OrdersView.vue`)

- **筛选条件**：日期时间范围选择器 + 支付方式下拉(现金/微信/支付宝) + 搜索/重置按钮
- **订单列表表格**：订单号、交易时间、订单金额、实收金额、支付方式(tag)、收银员、操作(详情/补打小票)
- **分页**：按时间倒序，支持 pageSize 切换(10/20/50)
- **点击订单行** -> 订单详情弹窗：
  - 订单基本信息（el-descriptions）：订单号、交易时间、支付方式、收银员
  - 商品明细表格：序号、商品名称、规格、单价、数量、折扣、小计
  - 金额汇总：合计金额、整单折扣、应付金额、实收金额、找零
- **「补打小票」按钮**（列表和详情弹窗均可用）-> POST /api/order/{id}/print

## 验证

- `npm run build` (vue-tsc --noEmit + vite build) 通过，无 TypeScript 错误，无构建错误

## 文件清单

| 文件 | 操作 |
|------|------|
| `frontend/src/api/stock.ts` | 新建 |
| `frontend/src/api/order.ts` | 新建 |
| `frontend/src/views/stock/StockView.vue` | 替换占位 |
| `frontend/src/views/orders/OrdersView.vue` | 替换占位 |
