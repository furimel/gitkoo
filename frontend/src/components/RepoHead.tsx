import {Link as InertiaLink, router, usePage} from '@inertiajs/react'
import {Button, Label, Link, UnderlineNav} from '@primer/react'
import {
  CodeIcon,
  EyeClosedIcon,
  EyeIcon,
  GearIcon,
  GitPullRequestIcon,
  HistoryIcon,
  IssueOpenedIcon,
  LockIcon,
  PlayIcon,
  RepoForkedIcon,
  RepoIcon,
  StarFillIcon,
  StarIcon,
} from '@primer/octicons-react'

import type {RepoHeader, SharedPageProps} from '@/lib/types'

type Tab = 'code' | 'issues' | 'pulls' | 'actions' | 'activity' | 'settings'

/**
 * The repository title band and tab bar.
 *
 * Every repository page renders this with the same props, which come from one
 * server-side component (RepoChrome). Six controllers used to decide separately
 * what the band knew, so the issue counter appeared on some pages and not others.
 */
export function RepoHead({tab, header}: {tab: Tab; header: RepoHeader}) {
  const {auth} = usePage<SharedPageProps>().props
  const {owner, repo, starred, watching, starCount, watcherCount} = header
  const base = `/${owner}/${repo.name}`

  return (
    <div className="repo-head">
      <div className="page page--wide page--flush">
        <div className="repo-title">
          {repo.visibility === 'PRIVATE' ? (
            <LockIcon className="u-muted" />
          ) : (
            <RepoIcon className="u-muted" />
          )}
          <Link as={InertiaLink} href={`/@/${owner}`} muted>
            {owner}
          </Link>
          <span className="repo-title-separator">/</span>
          <Link as={InertiaLink} href={base} className="repo-title-name">
            {repo.name}
          </Link>
          <Label variant="secondary">{repo.visibility.toLowerCase()}</Label>
          {header.forkedFrom ? (
            <span className="u-muted repo-title-fork">
              forked from{' '}
              <Link as={InertiaLink} href={`/${header.forkedFrom}`} muted>
                {header.forkedFrom}
              </Link>
            </span>
          ) : null}

          <div className="repo-title-actions">
            {auth.user ? (
              <>
                <Button
                  size="small"
                  leadingVisual={watching ? EyeClosedIcon : EyeIcon}
                  count={watcherCount}
                  onClick={() => router.post(`${base}/watch`)}
                >
                  {watching ? 'Unwatch' : 'Watch'}
                </Button>
                <Button
                  size="small"
                  leadingVisual={RepoForkedIcon}
                  onClick={() => router.post(`${base}/fork`)}
                >
                  Fork
                </Button>
                <Button
                  size="small"
                  leadingVisual={starred ? StarFillIcon : StarIcon}
                  count={starCount}
                  onClick={() => router.post(`${base}/star`)}
                >
                  {starred ? 'Starred' : 'Star'}
                </Button>
              </>
            ) : (
              <Button as="a" href="/login" size="small" leadingVisual={StarIcon} count={starCount}>
                Star
              </Button>
            )}
          </div>
        </div>

        <UnderlineNav aria-label="Repository">
          <UnderlineNav.Item
            as={InertiaLink}
            href={base}
            icon={CodeIcon}
            aria-current={tab === 'code' ? 'page' : undefined}
          >
            Code
          </UnderlineNav.Item>
          <UnderlineNav.Item
            as={InertiaLink}
            href={`${base}/issues`}
            icon={IssueOpenedIcon}
            counter={header.openIssueCount || undefined}
            aria-current={tab === 'issues' ? 'page' : undefined}
          >
            Issues
          </UnderlineNav.Item>
          <UnderlineNav.Item
            as={InertiaLink}
            href={`${base}/pulls`}
            icon={GitPullRequestIcon}
            counter={header.openPrCount || undefined}
            aria-current={tab === 'pulls' ? 'page' : undefined}
          >
            Pull requests
          </UnderlineNav.Item>
          <UnderlineNav.Item
            as={InertiaLink}
            href={`${base}/actions`}
            icon={PlayIcon}
            aria-current={tab === 'actions' ? 'page' : undefined}
          >
            Actions
          </UnderlineNav.Item>
          <UnderlineNav.Item
            as={InertiaLink}
            href={`${base}/activity`}
            icon={HistoryIcon}
            aria-current={tab === 'activity' ? 'page' : undefined}
          >
            Activity
          </UnderlineNav.Item>
          {/* Hidden from anonymous visitors, for whom it only leads to the login page. */}
          {auth.user ? (
            <UnderlineNav.Item
              as={InertiaLink}
              href={`${base}/settings`}
              icon={GearIcon}
              aria-current={tab === 'settings' ? 'page' : undefined}
            >
              Settings
            </UnderlineNav.Item>
          ) : null}
        </UnderlineNav>
      </div>
    </div>
  )
}
