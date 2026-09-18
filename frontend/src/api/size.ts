import request from './request'

export interface SizeConfigDTO {
  id?: number
  // luohuai codeX  modify: expose immutable barcode codes and lifecycle status.
  code?: string
  name?: string
  enabled?: boolean
  sortOrder?: number
  createTime?: string
}

/** 尺码标签只读列表（所有登录用户可用，SKU 下拉选择） */
export function listSizes(): Promise<SizeConfigDTO[]> {
  return request.get('/sizes').then((res) => res.data)
}

/** 尺码标签管理列表（BOSS 专属，系统设置页） */
export function listSizesForSetting(): Promise<SizeConfigDTO[]> {
  return request.get('/setting/sizes').then((res) => res.data)
}

/** 新增尺码标签 */
export function createSize(data: { name: string; sortOrder?: number }): Promise<SizeConfigDTO> {
  return request.post('/setting/sizes', data).then((res) => res.data)
}

/** 更新尺码标签 */
export function updateSize(
  id: number,
  data: { name?: string; sortOrder?: number; enabled?: boolean }
): Promise<SizeConfigDTO> {
  return request.put(`/setting/sizes/${id}`, data).then((res) => res.data)
}

/** 删除尺码标签 */
export function deleteSize(id: number): Promise<{ message: string }> {
  return request.delete(`/setting/sizes/${id}`).then((res) => res.data)
}
