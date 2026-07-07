import request from './request'

// ---- 类型定义 ----

export interface StockItemDTO {
  skuId?: number
  productId?: number
  productName?: string
  color?: string
  size?: string
  barcode?: string
  stockQty?: number
}

export interface StockRecordDTO {
  id?: number
  type?: string
  qty?: number
  beforeQty?: number
  afterQty?: number
  productName?: string
  skuSpec?: string
  operatorId?: number
  createTime?: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface InboundItem {
  skuId: number
  qty: number
}

export interface InboundRequest {
  items: InboundItem[]
}

// ---- API 函数 ----

/** 分页查询库存列表 */
export function listStock(params: {
  page?: number
  size?: number
}): Promise<PageResult<StockItemDTO>> {
  return request.get('/stock', { params }).then((res) => res.data)
}

/** 查询指定SKU的库存流水 */
export function getStockFlow(skuId: number): Promise<StockRecordDTO[]> {
  return request.get(`/stock/${skuId}/flow`).then((res) => res.data)
}

/** 入库操作 */
export function inbound(data: InboundRequest): Promise<{
  message: string
  items: { skuId: number; beforeQty: number; afterQty: number; qty: number }[]
}> {
  return request.post('/stock/inbound', data).then((res) => res.data)
}
