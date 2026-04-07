import http from './http'

export function listDataSets() {
  return http.get('/api/datasets').then((r) => r.data)
}

export function saveDataSet(payload) {
  const { id, ...rest } = payload
  if (id) {
    return http.put(`/api/datasets/${id}`, rest).then((r) => r.data)
  }
  return http.post('/api/datasets', rest).then((r) => r.data)
}

export function removeDataSet(id) {
  return http.delete(`/api/datasets/${id}`).then((r) => r.data)
}

export function previewDataSet(id) {
  return http.post(`/api/datasets/${id}/preview`).then((r) => r.data)
}

export function regenerateToken(id) {
  return http.post(`/api/datasets/${id}/regenerate-token`).then((r) => r.data)
}
