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
  // luohuai codeX  modify: preserve store article numbers as text and expose selling units.
  productCode?: string | null
  unit?: string
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

/** luohuai codeX generate: submit product metadata and color/size specifications as one atomic edit. */
export function updateProductCatalog(id: number, data: ProductDTO): Promise<ProductDTO> {
  return request.put(`/product/${id}/catalog`, data).then(res => res.data)
}

// luohuai codeX generate: preview contracts exclude stock so historical last-inbound quantities cannot become inventory.
export type PreviewStatus = 'READY' | 'NEEDS_SPEC' | 'CONFLICT' | 'INVALID'
export interface ProductImportRow {
  sheet: string
  sourceRow: number
  productCode: string
  name: string
  unit: string
  barcode: string
  color: string
  size: string
  categoryName: string
  costPrice: number | null
  sellPrice: number | null
  status: PreviewStatus
  errors: string[]
  conflicts: string[]
  warnings: string[]
}
export interface ProductImportPreview {
  totalRows: number
  productCount: number
  readyRows: number
  needsSpecRows: number
  conflictRows: number
  invalidRows: number
  notes: string[]
  rows: ProductImportRow[]
}
export function previewProductImport(file: File): Promise<ProductImportPreview> {
  const data = new FormData()
  data.append('file', file)
  return request.post('/product/import/preview', data, { timeout: 60000 }).then(res => res.data)
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

/** 生成下一个可用条码（HUAXING 规则，供新增 SKU 自动填充） */
export function generateNextBarcode(): Promise<{ barcode: string }> {
  return request.post('/product/barcode/generate').then((res) => res.data)
}

/** 获取分类列表（所有登录用户可用） */
export function listCategories(): Promise<CategoryDTO[]> {
  return request.get('/categories').then((res) => res.data)
}
