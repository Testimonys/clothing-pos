import request from './request'

// ---- 类型定义 ----

export interface ReportTotals {
  orderCount?: number
  salesAmount?: number
  payAmount?: number
  discountAmount?: number
  grossProfit?: number
  grossProfitRate?: number
  avgOrderValue?: number
}

export interface ReportTrendItem {
  key?: string
  sales?: number
  orders?: number
  grossProfit?: number
}

export interface ReportSummary {
  totals?: ReportTotals
  trend?: ReportTrendItem[]
}

export interface TopProductItem {
  rank?: number
  productName?: string
  qty?: number
  sales?: number
  grossProfit?: number
}

export interface CategoryItem {
  categoryName?: string
  sales?: number
  percent?: number
}

// ---- API 函数 ----

/** 销售汇总 + 趋势（按日/按月） */
export function getSummary(params: {
  start?: string
  end?: string
  granularity?: string
}): Promise<ReportSummary> {
  return request.get('/report/summary', { params }).then((res) => res.data)
}

/** 畅销款 TOP N（默认按销量倒序前 10） */
export function getTopProducts(params: {
  start?: string
  end?: string
  limit?: number
  orderBy?: string
}): Promise<TopProductItem[]> {
  return request.get('/report/top-products', { params }).then((res) => res.data)
}

/** 分类销售占比 */
export function getCategory(params: {
  start?: string
  end?: string
}): Promise<CategoryItem[]> {
  return request.get('/report/category', { params }).then((res) => res.data)
}
