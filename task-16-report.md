# Task 16 Report: 前端收银台页面 (POS)

## 完成状态: 已完成

## 文件变更

| 操作 | 文件 |
|------|------|
| 修改 | `frontend/src/views/pos/PosView.vue` |

## 实现内容

### 1. 扫码输入交互

- 扫码输入框始终自动聚焦（`onMounted` + `nextTick`）
- 输入条码后按回车自动调用 `queryByBarcode` API 查询商品
- 已存在的条码数量 +1，不重复查询接口
- 查询失败或未找到时有友好的 ElMessage 提示

### 2. 交易明细表格

| 列 | 说明 |
|----|------|
| 序号 | 自动编号 |
| 商品名称 | 从 API 返回的 productName |
| 规格 | skuSpec（颜色/尺码），无则显示 "-" |
| 单价 | sellPrice，格式化 ¥xx.xx |
| 数量 | el-input-number，最小 1，最大 999，可手动调整 |
| 单品折扣 | el-input-number，0 到 (单价 x 数量)，精度 2 位 |
| 小计 | 自动计算 = 单价 x 数量 - 单品折扣 |
| 删除 | 删除该行 |

### 3. 底部结算栏

- **合计**：所有行小计之和（自动计算）
- **整单折扣**：el-input-number，0 到合计金额
- **应收**：合计 - 整单折扣（自动计算）
- **「结账 F12」按钮**：触发结账弹窗

### 4. 结账弹窗

| 字段 | 类型 | 说明 |
|------|------|------|
| 应收金额 | 只读 input | display 格式 ¥xx.xx |
| 实收金额 | 可编辑 input | 默认 = 应收，输入后自动计算找零 |
| 找零 | 只读 input | 自动 = max(实收 - 应收, 0) |
| 支付方式 | 下拉选择 | CASH / WECHAT / ALIPAY，默认 CASH |
| 打印小票 | 复选框 | 默认勾选 |

### 5. 确认收款流程

- 校验实收 >= 应收
- 调用 `request.post('/order', orderData)` 提交订单
- 请求体包含：items、totalAmount、discountAmount、receivableAmount、receivedAmount、changeAmount、paymentMethod、printReceipt
- 成功后关闭结账弹窗，显示「收款成功」弹窗（订单号 + 交易时间 + 实收金额 + 找零）
- 点击「开始下一单」清空所有状态，重新聚焦扫码框

### 6. F12 快捷键

- `onMounted` 注册 `keydown` 监听 F12 键
- `onUnmounted` 移除监听
- 按 F12 等同于点击「结账 F12」按钮

### 7. API 使用

- `queryByBarcode` 来自 `@/api/product`（已封装）
- `request.post('/order', data)` 来自 `@/api/request`（已封装的 axios 实例，自动附带 Bearer token）

## 构建验证

```
npm run build → vue-tsc --noEmit && vite build → 成功
PosView-CzvQvWH8.js  8.93 kB (gzip: 3.59 kB)
PosView-BlKKHb8A.css 1.76 kB (gzip: 0.61 kB)
```

## Commit

```
9b54e77 feat: Task 16 - 前端收银台页面 PosView.vue
```
