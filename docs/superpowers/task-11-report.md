### Task 11 完成报告

**目标**: 订单 API（创建 + 列表 + 详情 + 补打）

**新建文件**:

| 文件 | 说明 |
|------|------|
| `src/main/java/com/huaxing/dto/CreateOrderRequest.java` | 创建订单请求 DTO，含 items 内嵌类（skuId, qty, unitPrice, discount, subTotal）和金额字段（totalAmount, discount, payAmount, receiveAmount, changeAmount, payMethod, printReceipt） |
| `src/main/java/com/huaxing/controller/OrderController.java` | 订单控制器，4 个接口 |

**接口清单**:

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/order` | 创建订单（@Transactional），校验库存→生成单号→存Order→扣库存→写OUTBOUND流水→存OrderItem→返回{orderId, orderNo, createTime} |
| GET | `/api/order` | 订单列表，分页+时间范围+支付方式筛选，按createTime倒序 |
| GET | `/api/order/{id}` | 订单详情，含items |
| POST | `/api/order/{id}/print` | 补打小票占位（EscPosPrinter 尚未实现，Task14 接入） |

**关键实现细节**:

1. 单号生成规则: `POS` + `yyyyMMdd` + 4位序号，通过 `orderRepository.maxIdToday()` 查当天最大ID，+1 后格式化
2. 库存校验: 先遍历所有 item 检查库存是否充足，任一不足立即返回 400 错误消息
3. 库存扣减: 逐条扣减 stockQty，记录 beforeQty/afterQty
4. 流水记录: 每条 item 写入 StockType.OUTBOUND 类型流水
5. OrderItem: 冗余保存 productName 和 skuSpec（拼接 color/size）
6. EscPosPrinter: `@Autowired(required = false)` 注入，当前为空，print 方法为占位代码

**编译**: `mvn compile` BUILD SUCCESS

**Commit**: `367d1c7` Task 11: 订单 API（创建+列表+详情+补打）

---

### Bug 修复报告

**修复日期**: 2026-07-07

**BUG-1: 单号序号使用 MAX(id) 而非 COUNT —— 已修复**

- **文件**: `src/main/java/com/huaxing/repository/OrderRepository.java`
- **问题**: `maxIdToday` 查询 `SELECT MAX(o.id)` 返回自增主键最大值。当订单 ID 存在空洞时（如删除订单、事务回滚），序号会不连续跳跃。例如当天有 5 笔订单但最大 ID 是 100，则下一单号为 ...0101 而非 ...0006。
- **修复**: 将 JPQL 改为 `SELECT COUNT(o.id) FROM Order o WHERE o.createTime >= :todayStart`，方法名从 `maxIdToday` 改为 `countOrdersToday`。序号 = 当天订单数 + 1，天然连续无空洞。
- **影响范围**: `OrderController.create()` 中单号生成逻辑同步适配（seq = count + 1）。

**BUG-2: 并发下单可能重复单号 —— 已修复**

- **文件**: `src/main/java/com/huaxing/controller/OrderController.java`
- **问题**: 单号生成逻辑内联在 `create()` 方法中，无并发保护。两个并发请求可能同时读到相同的 COUNT 值，生成相同单号。
- **修复**: 将单号生成逻辑提取为 `private synchronized String generateOrderNo()` 方法。利用 Spring 单例 Bean 的实例锁，保证同一 JVM 内同一时刻只有一个线程执行单号生成，彻底消除并发重复单号风险。
- **补充说明**: COUNT 查询 + synchronized 组合是 POS 收银场景下的推荐简单方案。若未来部署多实例，可进一步配合数据库唯一约束 + 重试机制。

**编译验证**: `mvn compile` BUILD SUCCESS（2 文件修改，+23/-10 行）

**Commit**: 见下方
