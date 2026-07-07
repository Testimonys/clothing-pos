# Task 10 库存管理 API 实现报告

## 完成内容

### 新建文件
- **`src/main/java/com/huaxing/dto/InboundRequest.java`** — 入库请求 DTO，包含 `items: List<{skuId, qty}>`
- **`src/main/java/com/huaxing/controller/StockController.java`** — 库存管理 Controller

### 修改文件
- **`src/main/java/com/huaxing/repository/StockRecordRepository.java`** — 新增 `findBySkuIdOrderByCreateTimeDesc()` 方法

## 接口说明

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/stock?page=0&size=20` | 分页查询库存列表 |
| GET | `/api/stock/{skuId}/flow` | 查询指定 SKU 的库存流水 |
| POST | `/api/stock/inbound` | 入库操作（事务） |

### 入库接口核心逻辑
1. 参数校验：`items` 不能为空，每个 item 的 `skuId` 和 `qty` 必须合法
2. 从 `@AuthenticationPrincipal` 获取当前操作用户（`SysUser`）的 ID
3. 遍历每个入库项：
   - 根据 `skuId` 查找 `ProductSku`
   - 获取 `productName`（从关联的 Product）和 `skuSpec`（color + size）冗余到流水记录
   - 更新 `stockQty = beforeQty + qty`
   - 写入 `StockRecord`（type=INBOUND）
4. 整个方法使用 `@Transactional` 保证事务一致性

## 编译结果
`mvn compile` 通过，无错误。

## 提交信息
```
commit 689ca7b
feat: 实现Task10-库存管理API（InboundRequest DTO + StockController + 流水查询）
```
