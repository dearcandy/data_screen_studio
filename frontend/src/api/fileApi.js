import axios from 'axios'

export function uploadExcel(file) {
  const fd = new FormData()
  fd.append('file', file)
  return axios
    .post('/api/files/excel', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
    .then((res) => {
      const body = res.data
      if (body && typeof body.code === 'number' && body.code !== 0) {
        return Promise.reject(new Error(body.message || '上传失败'))
      }
      return body.data
    })
}
