import api from '../index'

// P1-5 task-level SQL lineage /api/workflow-lineage
export const workflowLineageApi = {
  parse: (sql) => api.post('/api/workflow-lineage/parse', { sql }).then(r => r.data.data),
  sync: (workflowId) => api.post(`/api/workflow-lineage/${workflowId}/sync`).then(r => r.data.data),
  graph: (workflowId) => api.get(`/api/workflow-lineage/${workflowId}`).then(r => r.data.data)
}