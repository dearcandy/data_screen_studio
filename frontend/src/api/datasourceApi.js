import http from './http'

export function listDataSources() {
  return http.get('/api/datasources').then((r) => r.data)
}

export function saveDataSource(payload) {
  const { id, ...rest } = payload
  if (id) {
    return http.put(`/api/datasources/${id}`, rest).then((r) => r.data)
  }
  return http.post('/api/datasources', rest).then((r) => r.data)
}

export function removeDataSource(id) {
  return http.delete(`/api/datasources/${id}`).then((r) => r.data)
}
