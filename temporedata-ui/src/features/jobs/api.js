import http from '../../services/http'

export function listJobs(params) {
  return http.get('/jobs', { params })
}
export function getJob(id) {
  return http.get(`/jobs/${id}`)
}
// /api/v1 contract: GET /jobs/{id}/executions (job:read). Backend v1 not ready yet:
// fall back to the job detail so the page state machine still resolves (empty list).
export async function getJobExecutions(jobId, params) {
  try {
    return await http.get(`/jobs/${jobId}/executions`, { params })
  } catch (e) {
    const job = await getJob(jobId)
    return (job && job.executions) || []
  }
}
// /api/v1 contract: execution detail (Observability contract: executionId/jobId/traceId).
// Same backend-v1-not-ready fallback -> resolve placeholder so the page shows state machine.
export async function getJobExecution(jobId, executionId) {
  try {
    return await http.get(`/jobs/${jobId}/executions/${executionId}`)
  } catch (e) {
    const job = await getJob(jobId)
    const exes = (job && job.executions) || []
    return exes.find((x) => String(x.id) === String(executionId)) || null
  }
}