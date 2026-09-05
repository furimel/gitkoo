import type {ReactNode} from 'react'
import {Link as InertiaLink} from '@inertiajs/react'
import {Label, Link, Stack, Text} from '@primer/react'
import {GitBranchIcon, LockIcon, RepoIcon} from '@primer/octicons-react'

import type {Repo} from '@/lib/types'

/**
 * An unboxed list: rows separated by hairlines, no outer border.
 *
 * Primer has no borderless variant of its box, and wrapping every list in one made
 * each screen a stack of rectangles. This is the default; a card is the exception,
 * used only where a group needs a header bar, a footer bar, or its own background.
 */
export function Rows({children}: {children: ReactNode}) {
  return <ul className="rows">{children}</ul>
}

export function Row({
  children,
  align = 'center',
}: {
  children: ReactNode
  align?: 'center' | 'start'
}) {
  return (
    <li className="rows-item" style={{alignItems: align === 'start' ? 'flex-start' : 'center'}}>
      {children}
    </li>
  )
}

/**
 * One repository row.
 *
 * The dashboard, the profile and the search results each drew this three different
 * ways before, and they drifted apart exactly as you would expect.
 */
export function RepoRow({owner, repo}: {owner: string; repo: Repo}) {
  const Icon = repo.visibility === 'PRIVATE' ? LockIcon : RepoIcon

  return (
    <Row align="start">
      <Icon className="u-muted" />
      <div className="u-flex-auto">
        <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
          <Link as={InertiaLink} href={`/${owner}/${repo.name}`} className="repo-row-name">
            {repo.name}
          </Link>
          <Label variant="secondary">{repo.visibility.toLowerCase()}</Label>
        </Stack>
        {repo.description ? (
          <Text as="p" className="u-muted repo-row-description">
            {repo.description}
          </Text>
        ) : null}
      </div>
      <Text size="small" className="u-muted u-nowrap repo-row-branch">
        <GitBranchIcon size={16} />
        {repo.defaultBranch}
      </Text>
    </Row>
  )
}
