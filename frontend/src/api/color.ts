import request from './request'

// luohuai codeX generate: color configuration mirrors stable size-code lifecycle management.
export interface ColorConfigDTO {
  id?: number
  code?: string
  name?: string
  enabled?: boolean
  sortOrder?: number
  createTime?: string
}

export function listColors(): Promise<ColorConfigDTO[]> {
  return request.get('/colors').then(res => res.data)
}

export function listColorsForSetting(): Promise<ColorConfigDTO[]> {
  return request.get('/setting/colors').then(res => res.data)
}

export function createColor(data: { name: string; sortOrder?: number }): Promise<ColorConfigDTO> {
  return request.post('/setting/colors', data).then(res => res.data)
}

export function updateColor(id: number, data: Partial<ColorConfigDTO>): Promise<ColorConfigDTO> {
  return request.put(`/setting/colors/${id}`, data).then(res => res.data)
}

export function deleteColor(id: number): Promise<{ message: string }> {
  return request.delete(`/setting/colors/${id}`).then(res => res.data)
}
