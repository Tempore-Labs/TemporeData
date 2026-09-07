import http from '../../services/http'

export function fetchOverview() {
  return http.get('/dashboard/overview')
}