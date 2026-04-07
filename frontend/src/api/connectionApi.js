import http from './http'

export function testConnection(type, configJson) {
  return http.post('/api/connection/test', { type, configJson }).then((r) => r.data)
}
