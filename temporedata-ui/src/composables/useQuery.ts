/**
 * Thin wrappers around @tanstack/vue-query for consistent server-state usage.
 */
import {
  useQuery as vqUseQuery,
  useMutation as vqUseMutation,
  type QueryKey,
  type UseQueryOptions,
} from '@tanstack/vue-query'
import { type Ref, toValue, type MaybeRefOrGetter } from 'vue'

/** Query wrapper: key may be a ref so it re-fetches when inputs change. */
export function useQuery<T>(
  key: MaybeRefOrGetter<QueryKey>,
  fetcher: () => Promise<T>,
  options?: Partial<Omit<UseQueryOptions<T, Error, T, QueryKey>, 'queryKey' | 'queryFn'>>,
) {
  return vqUseQuery<T, Error>({
    queryKey: key as Ref<QueryKey>,
    queryFn: () => fetcher(),
    ...(options || {}),
  })
}

/** Mutation wrapper with built-in error toast via http layer. */
export function useMutation<TVars, TResult>(
  fn: (vars: TVars) => Promise<TResult>,
  options?: Parameters<typeof vqUseMutation<TResult, Error, TVars>>[0],
) {
  return vqUseMutation<TResult, Error, TVars>({
    mutationFn: (vars: TVars) => fn(vars),
    ...(options || {}),
  })
}

export { toValue }
