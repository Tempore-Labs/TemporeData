// v2.0 route table (NOT yet wired into main router — see src/app/README.md).
// Feature routes are assembled from each `features/*/routes.js`.
import { featureRoutes } from '../features'
import { NAV } from '../layouts/navigation'

export const v2RootRoute = {
  path: '/v2',
  name: 'V2Root',
  component: () => import('../layouts/AppLayoutV2.vue'),
  children: [
    { path: '', redirect: NAV.dashboard.path },
    ...featureRoutes
  ]
}

// Convenience: full route list for permission/menu tables.
export const v2AllRoutes = featureRoutes.map((r) => ({ path: r.path, name: r.name, meta: r.meta }))