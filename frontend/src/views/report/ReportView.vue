<template>
  <div class="report-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-bar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :shortcuts="shortcuts"
          style="width: 320px"
        />
        <el-radio-group
          v-model="granularity"
          class="granularity-group"
          @change="loadAll"
        >
          <el-radio-button value="day">按日</el-radio-button>
          <el-radio-button value="month">按月</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="loadAll">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="4" v-for="card in statCards" :key="card.label">
        <el-card shadow="never" class="stat-card">
          <el-statistic
            :title="card.label"
            :value="card.value"
            :precision="card.precision"
            :prefix="card.prefix"
            :suffix="card.suffix"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>销售趋势</template>
          <div class="chart-container" v-loading="loading">
            <div ref="trendChartRef" class="chart"></div>
            <el-empty v-if="showEmpty" description="暂无数据" class="chart-empty" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>分类占比</template>
          <div class="chart-container" v-loading="loading">
            <div ref="categoryChartRef" class="chart"></div>
            <el-empty v-if="showEmpty" description="暂无数据" class="chart-empty" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 畅销款 -->
    <el-card shadow="never">
      <template #header>畅销款 TOP10</template>
      <el-table
        :data="topProducts"
        stripe
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ row }">{{ row.rank ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="销量" width="100" align="center">
          <template #default="{ row }">{{ row.qty ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="销售额" width="150" align="right">
          <template #default="{ row }">{{ fmtMoney(row.sales) }}</template>
        </el-table-column>
        <el-table-column label="毛利" width="150" align="right">
          <template #default="{ row }">
            <span :style="{ color: (row.grossProfit ?? 0) >= 0 ? '#67c23a' : '#f56c6c' }">
              {{ fmtMoney(row.grossProfit) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  getSummary,
  getTopProducts,
  getCategory,
  type ReportSummary,
  type TopProductItem,
  type CategoryItem
} from '@/api/report'

// echarts 按需注册（不引入 vue-echarts）
echarts.use([LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// ---- 日期工具 ----
function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// ---- 筛选状态 ----
const today = new Date()
const defaultStart = new Date()
defaultStart.setDate(today.getDate() - 29)
// 默认近 30 天（今天及前 29 天）
const dateRange = ref<string[]>([formatDate(defaultStart), formatDate(today)])
const granularity = ref('day')

// 日期快捷项
const shortcuts = [
  {
    text: '今日',
    value: () => [new Date(), new Date()]
  },
  {
    text: '昨日',
    value: () => {
      const d = new Date()
      d.setDate(d.getDate() - 1)
      return [d, d]
    }
  },
  {
    text: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(end.getDate() - 6)
      return [start, end]
    }
  },
  {
    text: '近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(end.getDate() - 29)
      return [start, end]
    }
  },
  {
    text: '本月',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return [start, now]
    }
  },
  {
    text: '上月',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      const end = new Date(now.getFullYear(), now.getMonth(), 0)
      return [start, end]
    }
  }
]

// ---- 报表数据 ----
const summaryData = ref<ReportSummary | null>(null)
const topProducts = ref<TopProductItem[]>([])
const categoryData = ref<CategoryItem[]>([])
const loading = ref(false)

// 无数据（订单数为 0）时图表区显示覆盖式空态遮罩
const showEmpty = computed(
  () => !loading.value && (summaryData.value?.totals?.orderCount ?? 0) === 0
)

// 金额格式化
const fmtMoney = (n?: number) => `¥${(n ?? 0).toFixed(2)}`

// 统计卡片
const statCards = computed(() => {
  const t = summaryData.value?.totals ?? {}
  return [
    { label: '订单数', value: t.orderCount ?? 0, precision: 0, prefix: '', suffix: '' },
    { label: '销售额', value: t.salesAmount ?? 0, precision: 2, prefix: '¥', suffix: '' },
    { label: '实收', value: t.payAmount ?? 0, precision: 2, prefix: '¥', suffix: '' },
    { label: '毛利', value: t.grossProfit ?? 0, precision: 2, prefix: '¥', suffix: '' },
    { label: '毛利率', value: t.grossProfitRate ?? 0, precision: 2, prefix: '', suffix: '%' },
    { label: '客单价', value: t.avgOrderValue ?? 0, precision: 2, prefix: '¥', suffix: '' }
  ]
})

// ---- 图表实例 ----
const trendChartRef = ref<HTMLDivElement | null>(null)
const categoryChartRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null

function renderTrendChart() {
  const el = trendChartRef.value
  if (!el) return
  if (!trendChart) {
    trendChart = echarts.init(el)
  }
  const trend = summaryData.value?.trend ?? []
  trendChart.setOption(
    {
      tooltip: {
        trigger: 'axis',
        valueFormatter: (value: unknown) => fmtMoney(Number(value))
      },
      legend: { data: ['销售额', '毛利'] },
      grid: { left: 16, right: 16, top: 40, bottom: 16, containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: trend.map((t) => t.key ?? '')
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '销售额',
          type: 'line',
          smooth: true,
          data: trend.map((t) => t.sales ?? 0)
        },
        {
          name: '毛利',
          type: 'line',
          smooth: true,
          data: trend.map((t) => t.grossProfit ?? 0)
        }
      ]
    },
    { notMerge: true }
  )
}

function renderCategoryChart() {
  const el = categoryChartRef.value
  if (!el) return
  if (!categoryChart) {
    categoryChart = echarts.init(el)
  }
  categoryChart.setOption(
    {
      tooltip: {
        trigger: 'item',
        formatter: (params: { name?: string; value?: unknown; percent?: number }) =>
          `${params.name ?? '-'}<br/>销售额：${fmtMoney(Number(params.value))}<br/>占比：${params.percent ?? 0}%`
      },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: '65%',
          data: categoryData.value.map((c) => ({
            name: c.categoryName ?? '未分类',
            value: c.sales ?? 0
          }))
        }
      ]
    },
    { notMerge: true }
  )
}

function renderCharts() {
  renderTrendChart()
  renderCategoryChart()
}

// ---- 加载数据 ----
async function loadAll() {
  loading.value = true
  try {
    const [summary, top, category] = await Promise.all([
      getSummary({
        start: dateRange.value[0],
        end: dateRange.value[1],
        granularity: granularity.value
      }),
      getTopProducts({
        start: dateRange.value[0],
        end: dateRange.value[1],
        limit: 10,
        orderBy: 'qty'
      }),
      getCategory({
        start: dateRange.value[0],
        end: dateRange.value[1]
      })
    ])
    summaryData.value = summary
    topProducts.value = top
    categoryData.value = category
    await nextTick()
    renderCharts()
  } catch {
    ElMessage.error('报表加载失败')
  } finally {
    loading.value = false
  }
}

// ---- 生命周期 ----
let trendObserver: ResizeObserver | null = null
let categoryObserver: ResizeObserver | null = null

onMounted(() => {
  loadAll()
  if (trendChartRef.value) {
    trendObserver = new ResizeObserver(() => trendChart?.resize())
    trendObserver.observe(trendChartRef.value)
  }
  if (categoryChartRef.value) {
    categoryObserver = new ResizeObserver(() => categoryChart?.resize())
    categoryObserver.observe(categoryChartRef.value)
  }
})

onBeforeUnmount(() => {
  trendObserver?.disconnect()
  categoryObserver?.disconnect()
  trendChart?.dispose()
  categoryChart?.dispose()
  trendChart = null
  categoryChart = null
})
</script>

<style scoped>
.report-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.granularity-group {
  margin-left: 16px;
}

.filter-bar .el-button {
  margin-left: 16px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  margin-bottom: 16px;
}

.chart-row {
  margin-bottom: 16px;
}

.chart-container {
  position: relative;
  height: 320px;
}

.chart {
  width: 100%;
  height: 320px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: rgba(255, 255, 255, 0.9);
}
</style>
