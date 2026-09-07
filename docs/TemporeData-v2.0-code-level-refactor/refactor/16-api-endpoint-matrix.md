# 16 API Endpoint Matrix

| Domain | Method | Endpoint | Permission |
|---|---|---|---|
| Cluster | GET | /api/v1/clusters | cluster:read |
| Cluster | POST | /api/v1/clusters | cluster:create |
| Cluster | GET | /api/v1/clusters/{id} | cluster:read |
| Cluster | POST | /api/v1/clusters/{id}/actions/refresh | cluster:operate |
| Job | GET | /api/v1/jobs | job:read |
| Job | POST | /api/v1/jobs | job:create |
| Job | POST | /api/v1/jobs/{id}/actions/run | job:execute |
| Job | POST | /api/v1/jobs/{id}/actions/stop | job:execute |
| Job | GET | /api/v1/jobs/{id}/executions | job:read |
| Monitoring | GET | /api/v1/metrics | metric:read |
| Monitoring | GET | /api/v1/events | event:read |
| Incident | GET | /api/v1/incidents | incident:read |
| Incident | POST | /api/v1/incidents/{id}/ack | incident:operate |
| Incident | POST | /api/v1/incidents/{id}/resolve | incident:operate |
| Automation | GET | /api/v1/runbooks | runbook:read |
| Automation | POST | /api/v1/runbooks/{id}/execute | runbook:execute |
| Automation | POST | /api/v1/action-approvals/{id}/approve | action:approve |
| Data | GET | /api/v1/datasources | datasource:read |
| Data | GET | /api/v1/metadata | metadata:read |
| Data | GET | /api/v1/lineage | lineage:read |
