import {Label} from '@primer/react'
import {
  GitMergeIcon,
  GitPullRequestIcon,
  IssueClosedIcon,
  IssueOpenedIcon,
} from '@primer/octicons-react'

type Status = 'OPEN' | 'CLOSED' | 'MERGED'

/** The Open / Closed / Merged pill, in the colours GitHub uses for each. */
export function StateBadge({kind, status}: {kind: 'issue' | 'pr'; status: Status}) {
  const Icon =
    kind === 'pr'
      ? status === 'MERGED'
        ? GitMergeIcon
        : GitPullRequestIcon
      : status === 'OPEN'
        ? IssueOpenedIcon
        : IssueClosedIcon

  const variant = status === 'OPEN' ? 'success' : status === 'MERGED' ? 'done' : 'danger'

  return (
    <Label variant={variant} size="large">
      <Icon size={16} />
      <span className="state-badge-text">
        {status.charAt(0) + status.slice(1).toLowerCase()}
      </span>
    </Label>
  )
}
