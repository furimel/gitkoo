import {Link as InertiaLink} from '@inertiajs/react'
import {BranchName, Button, Link, Stack, Text} from '@primer/react'
import {ArrowRightIcon, CheckIcon, GitMergeIcon, GitPullRequestIcon} from '@primer/octicons-react'

import {Empty} from '@/components/Empty'
import {Page} from '@/components/Page'
import {Pagination} from '@/components/Pagination'
import {RepoHead} from '@/components/RepoHead'
import {TimeAgo} from '@/components/TimeAgo'
import type {PageInfo, PullRequest, RepoHeader} from '@/lib/types'

type Props = RepoHeader & {
  pulls: PullRequest[]
  filter: 'open' | 'closed'
  openCount: number
  closedCount: number
  page?: PageInfo
}

/** openCount and closedCount come from the controller: counting here would only
 *  count the current page. */
export default function PullRequestList(props: Props) {
  const {owner, repo, pulls, filter} = props
  const base = `/${owner}/${repo.name}`

  return (
    <>
      <RepoHead tab="pulls" header={props} />
      <Page>
        <Stack gap="normal">
          <Stack direction="horizontal" justify="end">
            <Button as={InertiaLink} href={`${base}/pulls/new`} variant="primary">
              New pull request
            </Button>
          </Stack>

          <div className="card">
            <div className="card-header">
              <Stack direction="horizontal" gap="normal">
                <Link
                  as={InertiaLink}
                  href={`${base}/pulls`}
                  muted={filter !== 'open'}
                  className="filter-link"
                >
                  <GitPullRequestIcon /> {props.openCount} Open
                </Link>
                <Link
                  as={InertiaLink}
                  href={`${base}/pulls?state=closed`}
                  muted={filter !== 'closed'}
                  className="filter-link"
                >
                  <CheckIcon /> {props.closedCount} Closed
                </Link>
              </Stack>
            </div>

            {pulls.length > 0 ? (
              pulls.map(pull => (
                <div className="card-row" key={pull.id} style={{alignItems: 'flex-start'}}>
                  {pull.status === 'MERGED' ? (
                    <GitMergeIcon className="u-done" />
                  ) : (
                    <GitPullRequestIcon
                      className={pull.status === 'OPEN' ? 'u-success' : 'u-danger'}
                    />
                  )}
                  <div className="u-flex-auto">
                    <Link
                      as={InertiaLink}
                      href={`${base}/pulls/${pull.number}`}
                      className="repo-row-name"
                    >
                      {pull.title}
                    </Link>
                    <Stack
                      direction="horizontal"
                      align="center"
                      gap="condensed"
                      wrap="wrap"
                      className="u-muted pr-row-meta"
                    >
                      <Text size="small">#{pull.number}</Text>
                      <TimeAgo at={pull.createdAt} />
                      <BranchName href="#">{pull.sourceBranch}</BranchName>
                      <ArrowRightIcon size={12} />
                      <BranchName href="#">{pull.targetBranch}</BranchName>
                    </Stack>
                  </div>
                </div>
              ))
            ) : (
              <div className="card-body">
                <Empty
                  icon={GitPullRequestIcon}
                  title={filter === 'closed' ? 'No closed pull requests' : 'No open pull requests'}
                  action={
                    filter === 'closed' ? undefined : (
                      <Button as={InertiaLink} href={`${base}/pulls/new`} variant="primary">
                        New pull request
                      </Button>
                    )
                  }
                >
                  {filter === 'closed'
                    ? 'Pull requests you merge or close will be listed here.'
                    : 'Push a branch, then open a pull request to propose changes.'}
                </Empty>
              </div>
            )}
          </div>

          <Pagination
            page={props.page}
            path={`${base}/pulls${filter === 'closed' ? '?state=closed' : ''}`}
          />
        </Stack>
      </Page>
    </>
  )
}
