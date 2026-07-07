# Task 14: ESC/POS 小票打印服务 -- 完成报告

## 概述

实现 ESC/POS 热敏小票打印服务，支持在创建订单后自动打印小票，并提供补打接口。

## 变更清单

### 新增文件

| 文件 | 说明 |
|------|------|
| `src/main/java/com/huaxing/printer/EscPosPrinter.java` | ESC/POS 打印机服务组件 |

### 修改文件

| 文件 | 变更内容 |
|------|---------|
| `src/main/java/com/huaxing/controller/OrderController.java` | 注入 EscPosPrinter，调用打印 |
| `src/main/resources/application.yml` | 新增 `app.printer.port` 配置 |
| `src/main/resources/application-h2.yml` | 新增 `app.printer.port` 配置 |

## 实现细节

### EscPosPrinter.java

- **注解**: `@Component` + `@ConditionalOnProperty("app.printer.enabled")`
- **printOrder(Order, List<OrderItem>)**: 构建 ESC/POS 指令序列 → USB 端口写入
- **ESC/POS 指令序列**:
  1. `ESC @` -- 初始化打印机
  2. `ESC a 1` + 店名 -- 居中打印店名（从 `app.store.name` 读取）
  3. `ESC a 0` + 单号/时间 -- 左对齐，时间精确到秒
  4. 分隔线（32个 `-`）
  5. 商品明细表头（品名 / 数量 / 小计）
  6. 分隔线
  7. 每行明细（品名+规格 / 数量 / 小计）
  8. 分隔线 → 合计 / 折扣 / 实付 → 支付方式（中文：现金/微信/支付宝）
  9. 居中 "谢谢惠顾！欢迎下次光临"
  10. `GS V 0` -- 全切纸
- **USB端口写入**: 优先使用 `app.printer.port` 配置值，macOS 自动检测 `/dev/cu.usbprinter`，Linux 自动检测 `/dev/usb/lp0`，Windows 需显式配置（如 `COM3`）
- **降级方案**: USB 写入失败则追加输出到 `receipt_debug.txt` 文件
- **编码**: 文本使用 GBK 编码，支持中文打印
- **视觉宽度**: 中文按 2 字符、英文按 1 字符计算对齐

### OrderController.java

- 将 `@Autowired(required=false) private Object escPosPrinter` 改为 `EscPosPrinter` 类型
- `create()`: `printReceipt=true` 时调用 `escPosPrinter.printOrder(order, order.getItems())`，异常不影响订单创建
- `print()`: `/api/order/{id}/print` 补打接口完善，加载订单和 items 后调用打印

## 配置说明

```yaml
app:
  printer:
    enabled: true              # 启用打印服务
    port: /dev/cu.usbprinter   # 打印机端口（macOS 默认，Windows 需设为 COM3）
  store:
    name: 华兴服装店            # 小票上的店名
```

## 编译验证

`mvn compile` 通过，无编译错误。
