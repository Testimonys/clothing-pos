import request from './request'

// ---- 类型定义 ----

export interface ProductSkuDTO {
  id?: number
  productId?: number
  color?: string
  size?: string
  barcode?: string
  stockQty?: number
  createTime?: string
}

export interface ProductDTO {
  id?: number
  categoryId?: number | null
  categoryName?: string
  name?: string
  imageUrl?: string
  costPrice?: number
  sellPrice?: number
  createTime?: string
  updateTime?: string
  skus?: ProductSkuDTO[]
}

export interface CategoryDTO {
  id?: number
  name?: string
  parentId?: number
  sortOrder?: number
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

export interface BarcodeResult {
  found: boolean
  skuId?: number
  productId?: number
  productName?: string
  color?: string
  size?: string
  sellPrice?: number
  stockQty?: number
  skuSpec?: string
}

// ---- API 函数 ----

/** 分页搜索商品列表 */
export function listProducts(params: {
  keyword?: string
  categoryId?: number
  page?: number
  size?: number
}): Promise<PageResult<ProductDTO>> {
  return request.get('/product', { params }).then((res) => res.data)
}

/** 商品详情（含SKU列表） */
export function getProduct(id: number): Promise<ProductDTO> {
  return request.get(`/product/${id}`).then((res) => res.data)
}

/** 新建商品（含SKU列表） */
export function createProduct(data: ProductDTO): Promise<ProductDTO> {
  return request.post('/product', data).then((res) => res.data)
}

/** 更新商品基本信息 */
export function updateProduct(id: number, data: ProductDTO): Promise<ProductDTO> {
  return request.put(`/product/${id}`, data).then((res) => res.data)
}

/** 删除商品及关联SKU */
export function deleteProduct(id: number): Promise<{ message: string }> {
  return request.delete(`/product/${id}`).then((res) => res.data)
}

/** 扫码查询（按条码查商品+SKU） */
export function queryByBarcode(code: string): Promise<BarcodeResult> {
  return request.get(`/product/barcode/${code}`).then((res) => res.data)
}

/** 为商品添加SKU */
export function addSku(
  productId: number,
  data: ProductSkuDTO
): Promise<ProductSkuDTO> {
  return request.post(`/product/${productId}/sku`, data).then((res) => res.data)
}

/** 更新SKU信息 */
export function updateSku(
  productId: number,
  skuId: number,
  data: ProductSkuDTO
): Promise<ProductSkuDTO> {
  return request
    .put(`/product/${productId}/sku/${skuId}`, data)
    .then((res) => res.data)
}

/** 删除SKU */
export function deleteSku(
  productId: number,
  skuId: number
): Promise<{ message: string }> {
  return request
    .delete(`/product/${productId}/sku/${skuId}`)
    .then((res) => res.data)
}

/** 为SKU生成条码 */
export function generateBarcode(
  productId: number,
  skuId: number
): Promise<{ barcode: string }> {
  return request
    .post(`/product/${productId}/sku/${skuId}/barcode`)
    .then((res) => res.data)
}

/** 获取分类列表 */
export function listCategories(): Promise<CategoryDTO[]> {
  return request.get('/setting/categories').then((res) => res.data)
}
