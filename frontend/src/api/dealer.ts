import request from './request'

// luohuai codeX generate: dealer contracts serve both the catalog selector and administrator page.
export interface DealerDTO {
  id?: number
  code?: string
  name?: string
  contactName?: string
  phone?: string
  address?: string
  remark?: string
  enabled?: boolean
  createTime?: string
  updateTime?: string
}

export function listDealers(): Promise<DealerDTO[]> {
  return request.get('/dealers').then(res => res.data)
}

export function listDealersForSetting(): Promise<DealerDTO[]> {
  return request.get('/setting/dealers').then(res => res.data)
}

export function createDealer(data: DealerDTO): Promise<DealerDTO> {
  return request.post('/setting/dealers', data).then(res => res.data)
}

export function updateDealer(id: number, data: DealerDTO): Promise<DealerDTO> {
  return request.put(`/setting/dealers/${id}`, data).then(res => res.data)
}

export function deleteDealer(id: number): Promise<{ message: string }> {
  return request.delete(`/setting/dealers/${id}`).then(res => res.data)
}
