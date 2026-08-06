# 销售统计报表 — 功能设计

> 华兴服装店三个新功能之功能二。状态：设计已获用户批准（2026-08-06）。
> 前置：[[return-exchange-design]]（功能一，另行实现）。后续：功能三 Excel 批量导入。

## 背景与需求

店主（老板）需要掌握销售情况，做进销存决策。在既有订单数据基础上提供**销售汇总日报/月报 + 畅销款/毛利分析**两类报表视图。

报表**全部数据来源现有表**，无需新增任何数据库表：
- `sys_order`：total_amount / discount / pay_amount / create_time / cashier_id / pay_method
- `order_item`：unit_price / qty / discount / sub_total / product_name / sku_spec / sku_id
- `product`：cost_price（毛利计算用）/ category_id
- `product_sku`：product_id / color / size（串联订单明细与商品成本）

## 已确认的决策

| 决策点 | 结论 |
|--------|------|
| 展示形式 | 图表 + 表格，引入 **echarts**（前端目前无图表库） |
| 查看权限 | **仅老板（BOSS）**，对应 `/api/report/**` 需 `ROLE_BOSS` |
| 报表维度 | 销售汇总（日报/月报）+ 畅销款 TOP N + 毛利 + 分类销售占比 |
| 验证方式 | 不做自动化测试/curl 冒烟，用户部署后在页面点击验收 |

## 后端接口设计

新增 `ReportMapper`（MyBatis-Plus Mapper 接口，写聚合 SQL）+ `ReportController`（`@RestController`，`@RequestMapping("/api/report")`）。

**权限**：`SecurityConfig` 增加一行 `.requestMatchers("/api/report/**").hasRole("BOSS")`（仿照现有 `/api/setting/**`）。

### ① GET `/api/report/summary` — 销售汇总 + 趋势

```
参数: start  (LocalDate, 可选, 默认近30天起)
      end    (LocalDate, 可选, 默认今天)
      granularity = day | month   (默认 day)
返回:
  totals: { orderCount, salesAmount, payAmount, discountAmount, grossProfit, grossProfitRate, avgOrderValue }
  trend:  [ { key: "2026-08-01" | "2026-08", sales, orders, grossProfit } ]
```

- 汇总 SQL：`GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')`（day）或 `'%Y-%m'`（month），范围 `create_time >= start 00:00:00 AND create_time < end+1 00:00:00`
- `salesAmount` = 订单 total_amount 之和；`payAmount` = 订单 pay_amount 之和（实收）；`discountAmount` = discount 之和（让利）
- **毛利口径**：`毛利 = Σ(pay_amount) − Σ(qty × cost_price)`
  - 实收按订单 `pay_amount`（已是打折后的实际到账金额）
  - 成本按订单明细行：`order_item JOIN product_sku ON sku_id → JOIN product ON product_id`，取 `product.cost_price`
  - **已知局限**：历史订单毛利按**当前** `product.cost_price` 计算（成本价可能被改过，非下单时成本）。2-3 人小店可接受，不做下单时成本快照（YAGNI）
- `grossProfitRate` = 毛利 / 实收；`avgOrderValue` = 实收 / 订单数
- `trend` 每一格 = 该日/月内上述合计（sales、orders、grossProfit）

### ② GET `/api/report/top-products` — 畅销款 TOP N

```
参数: start, end（同上）; limit = 10; orderBy = qty | sales | profit（默认 qty）
返回: [ { rank, productName, skuSpec, qty, sales, grossProfit } ]
```

- `GROUP BY product_name, sku_spec`（利用 order_item 冗余字段，畅销款分组不需 JOIN 商品表）
- **按商品名合并**：同款不同颜色/尺码（sku_spec 不同）合并为一行，销量/销售额/毛利求和——聚焦"哪款卖得好"。SKU 级展开本期不做（YAGNI）
- 毛利用 `JOIN product_sku → product` 取 `cost_price` 计算
- 排序字段由 `orderBy` 决定（销量/销售额/毛利）

### ③ GET `/api/report/category` — 分类销售占比

```
参数: start, end（同上）
返回: [ { categoryName, sales, percent } ]
```

- `order_item JOIN product_sku → product → category`，`GROUP BY category.name`
- `percent` = 该类销售额 / 总销售额（后端算好，前端直接画饼图）
- 无分类的商品归入「未分类」（category_id 为 NULL 时显示"未分类"）

## 前端页面设计

### 路由与菜单
- 新增路由 `/report` → `views/report/ReportView.vue`，`meta: { title: '销售报表', icon: 'TrendCharts', requiresAuth: true, bossOnly: true }`
- `MainLayout.vue` 侧边栏加「销售报表」菜单项，`v-if="authStore.isBoss"` 控制仅老板可见
- 新增 `frontend/src/api/report.ts`（类型 + 3 个请求函数）

### 页面布局（单页上下结构）

```
┌─────────────────────────────────────────────────────┐
│ [日期范围选择器 + 快捷项]        [按日 ▾ | 按月 ▾]    │
├─────────────────────────────────────────────────────┤
│  订单数    销售额     实收      毛利     毛利率  客单价 │   ← 统计卡片（el-statistic）
├────────────────────────┬────────────────────────────┤
│  销售趋势图（折线，双系列） │  分类销售占比（饼图）       │
├────────────────────────┴────────────────────────────┤
│  畅销款 TOP10 表格：排名/商品/规格/销量/销售额/毛利     │
└─────────────────────────────────────────────────────┘
```

### 交互
- **日期范围**：`el-date-picker` type="daterange"，快捷项：今日 / 昨日 / 近 7 天 / 近 30 天 / 本月 / 上月 / 自定义
- **聚合粒度切换**（按日/按月）：切换时重新请求 `summary`，趋势图 x 轴 `2026-08-06` ↔ `2026-08`；月粒度适合看长周期走势
- **空态**：无数据时统计卡片显示 `0` / `¥0.00`，图表显示"暂无数据"提示，表格空
- **默认范围**：进入页面默认近 30 天、按日粒度

### echarts 集成
- `npm install echarts`（唯一新增前端依赖）
- **按需引入** `echarts/core`：`LineChart` + `PieChart` + `GridComponent` + `TooltipComponent` + `LegendComponent` + `CanvasRenderer`，控制打包体积
- **不引入 vue-echarts**，`ReportView.vue` 内手动 `init` / `setOption` / `dispose`（与项目少依赖风格一致）
- 图表容器固定高度；`ResizeObserver` 监听窗口/侧栏变化调 `chart.resize()`；`onBeforeUnmount` 时 `dispose`
- 趋势图：折线图**双系列**（销售额 + 毛利）；饼图：分类销售额占比

## 错误处理

- `start > end` 或日期格式非法 → `400 Bad Request`
- start/end 缺省 → 默认近 30 天
- 无数据 → 返回零值/空数组（不报错，前端显示空态）
- 权限不足（CLERK 访问 `/api/report/**`）→ 由现有 JWT 安全链返回 403

## 验证方式

- 后端：编译通过（`mvn clean package -DskipTests`），不写自动化测试
- 前端：`vue-tsc --noEmit` 类型检查 + `npm run build` 构建通过
- 最终验收：**用户部署后自己在页面点击确认**（统计数字、趋势图、饼图、畅销款、权限过滤）

## 明确不做（YAGNI）

- 不做下单时成本快照（毛利按当前 cost_price 算）
- 不做畅销款 SKU 级展开/下钻
- 不做预聚合统计表 / 定时任务
- 不做报表导出（Excel/打印）
- 不做会员/收银员维度的交叉分析
