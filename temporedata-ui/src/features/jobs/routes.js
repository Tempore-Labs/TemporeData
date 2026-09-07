// Job feature route table (v2.0). Job state machine lives in the backend domain;
// here we only map status -> tone for StatusBadge.
export const JOB_TONE = { pending: 'info', queued: 'primary', running: 'primary', success: 'success', failed: 'danger', cancelled: 'info' }
export const EXECUTION_TONE = { queued: 'primary', running: 'primary', success: 'success', failed: 'danger', cancelled: 'info', paused: 'warning', unknown: 'info' }

export const routes = [
  { path: '/v2/jobs', name: 'V2.Jobs', component: () => import('./views/JobList.vue'), meta: { title: '作业', perm: 'job:read' } },
  { path: '/v2/jobs/:id', name: 'V2.JobDetail', component: () => import('./views/JobDetail.vue'), meta: { title: '作业详情', perm: 'job:read' } },
  { path: '/v2/jobs/:id/executions/:executionId', name: 'V2.JobExecution', component: () => import('./views/JobExecution.vue'), meta: { title: '作业执行', perm: 'job:read' } }
]