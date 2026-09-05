import {RelativeTime} from '@primer/react'

/**
 * "3 hours ago", with the exact stamp in the tooltip.
 *
 * Primer wraps GitHub's own relative-time element, so this updates as the page
 * stays open and formats in the reader's locale rather than the server's.
 */
export function TimeAgo({at}: {at: string | null | undefined}) {
  if (!at) return null
  const date = new Date(at)
  if (Number.isNaN(date.getTime())) return null
  return <RelativeTime date={date} />
}
