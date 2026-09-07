import api from '../index'

// P1-7 programmatic / tabular workflow modeling (JSON + Excel)
export const workflowImportApi = {
  importJson: (data) => api.post('/api/workflow-import/json', data).then(r => r.data.data),
  exportJson: (id) => api.get(`/api/workflow-import/${id}/export-json`).then(r => r.data.data),
  importExcel: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/api/workflow-import/excel', fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data.data)
  },
  // Excel export: download as a file (uses the authed axios client -> blob)
  exportExcel: async (id) => {
    const res = await api.get(`/api/workflow-import/${id}/export-excel`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = `workflow_${id}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }
}