import request from './request'

// ---- 类型定义 ----

export interface OrderItemDTO {
  id?: number
  skuId?: number
  productName?: string
  skuSpec?: string
  unitPrice?: number
  qty?: number
  discount?: number
  subTotal?: number
  barcode?: string
}

export interface OrderSummaryDTO {
  id?: number
  orderNo?: string
  totalAmount?: number
  discount?: number
  payAmount?: number
  receiveAmount?: number
  changeAmount?: number
  payMethod?: string
  cashierName?: string
  createTime?: string
}

export interface OrderDetailDTO extends OrderSummaryDTO {
  items?: OrderItemDTO[]
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

// ---- API 函数 ----

/** 订单列表：分页 + 时间范围 + 支付方式筛选，按时间倒序 */
export function listOrders(params: {
  page?: number
  size?: number
  startTime?: string
  endTime?: string
  payMethod?: string
}): Promise<PageResult<OrderSummaryDTO>> {
  return request.get('/order', { params }).then((res) => res.data)
}

/** 订单详情（含 items） */
export function getOrderDetail(id: number): Promise<OrderDetailDTO> {
  return request.get(`/order/${id}`).then((res) => res.data)
}

/** 补打小票 */
export function reprintOrder(id: number): Promise<{ message: string }> {
  return request.post(`/order/${id}/print`).then((res) => res.data)
}
