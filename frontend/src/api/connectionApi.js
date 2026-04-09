import http from './http'

export function testConnection(type, configJson) {
  return http.post('/api/datasources/test', { type, configJson })
}
