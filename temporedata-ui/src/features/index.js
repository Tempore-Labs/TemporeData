// Aggregates all feature route tables into one array for the v2.0 router.
import { routes as dashboardRoutes } from '../features/dashboard/routes'
import { routes as jobRoutes } from '../features/jobs/routes'
import { routes as clusterRoutes } from '../features/cluster/routes'
import { routes as monitoringRoutes } from '../features/monitoring/routes'
import { routes as incidentRoutes } from '../features/incidents/routes'
import { routes as computeRoutes } from '../features/compute/routes'
import { routes as loggingRoutes } from '../features/logging/routes'
import { routes as automationRoutes } from '../features/automation/routes'
import { routes as dataRoutes } from '../features/data/routes'
import { routes as adminRoutes } from '../features/admin/routes'

export const featureRoutes = [
  ...dashboardRoutes,
  ...clusterRoutes,
  ...jobRoutes,
  ...monitoringRoutes,
  ...incidentRoutes,
  ...computeRoutes,
  ...loggingRoutes,
  ...automationRoutes,
  ...dataRoutes,
  ...adminRoutes
]