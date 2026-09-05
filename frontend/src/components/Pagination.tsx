import {router} from '@inertiajs/react'
import {Pagination as PrimerPagination} from '@primer/react'

import type {PageInfo} from '@/lib/types'

/**
 * Hidden entirely when everything fits on one page, which is the common case on a
 * self-hosted instance and where a control that is always drawn looks like clutter.
 */
export function Pagination({page, path}: {page?: PageInfo; path: string}) {
  if (!page || page.single) return null

  return (
    <PrimerPagination
      pageCount={page.totalPages}
      currentPage={page.page}
      hrefBuilder={n => buildHref(path, n)}
      onPageChange={(event, n) => {
        event.preventDefault()
        router.visit(buildHref(path, n), {preserveScroll: false})
      }}
    />
  )
}

/** Keeps any filter already in the path, so paging a closed-issues list stays closed. */
function buildHref(path: string, n: number) {
  const [base, query = ''] = path.split('?')
  const params = new URLSearchParams(query)
  params.set('page', String(n))
  return `${base}?${params.toString()}`
}
