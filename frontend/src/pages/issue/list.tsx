import {Link as InertiaLink} from '@inertiajs/react'
import {Button, Label, Link, Stack, Text} from '@primer/react'
import {IssueClosedIcon, IssueOpenedIcon} from '@primer/octicons-react'

import {Empty} from '@/components/Empty'
import {Page} from '@/components/Page'
import {Pagination} from '@/components/Pagination'
import {RepoHead} from '@/components/RepoHead'
import {TimeAgo} from '@/components/TimeAgo'
import type {Issue, Label as LabelRecord, PageInfo, RepoHeader} from '@/lib/types'

type Props = RepoHeader & {
  issues: Issue[]
  issueLabels: Record<number, LabelRecord[]>
  filter: 'open' | 'closed'
  openCount: number
  closedCount: number
  page?: PageInfo
}

/** GitHub really does draw the issue list as a card, so this one keeps it. */
export default function IssueList(props: Props) {
  const {owner, repo, issues, filter} = props
  const base = `/${owner}/${repo.name}`

  return (
    <>
      <RepoHead tab="issues" header={props} />
      <Page>
        <Stack gap="normal">
          <Stack direction="horizontal" justify="end">
            <Button as={InertiaLink} href={`${base}/issues/new`} variant="primary">
              New issue
            </Button>
          </Stack>

          <div className="card">
            <div className="card-header">
              <Stack direction="horizontal" gap="normal">
                <Link
                  as={InertiaLink}
                  href={`${base}/issues`}
                  muted={filter !== 'open'}
                  className="filter-link"
                >
                  <IssueOpenedIcon /> {props.openCount} Open
                </Link>
                <Link
                  as={InertiaLink}
                  href={`${base}/issues?state=closed`}
                  muted={filter !== 'closed'}
                  className="filter-link"
                >
                  <IssueClosedIcon /> {props.closedCount} Closed
                </Link>
              </Stack>
            </div>

            {issues.length > 0 ? (
              issues.map(issue => (
                <div className="card-row" key={issue.id} style={{alignItems: 'flex-start'}}>
                  {issue.status === 'OPEN' ? (
                    <IssueOpenedIcon className="u-success" />
                  ) : (
                    <IssueClosedIcon className="u-done" />
                  )}
                  <div className="u-flex-auto">
                    <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
                      <Link
                        as={InertiaLink}
                        href={`${base}/issues/${issue.number}`}
                        className="repo-row-name"
                      >
                        {issue.title}
                      </Link>
                      {/* Batch-loaded in the controller, so this costs no query per row. */}
                      {(props.issueLabels[issue.id] ?? []).map(label => (
                        <Label
                          key={label.id}
                          title={label.description ?? undefined}
                          style={{backgroundColor: label.color, color: '#fff'}}
                        >
                          {label.name}
                        </Label>
                      ))}
                    </Stack>
                    <Text as="div" size="small" className="u-muted">
                      #{issue.number} {issue.status === 'OPEN' ? 'opened' : 'closed'}{' '}
                      <TimeAgo at={issue.createdAt} />
                    </Text>
                  </div>
                </div>
              ))
            ) : (
              <div className="card-body">
                <Empty
                  icon={IssueOpenedIcon}
                  title={filter === 'closed' ? 'No closed issues' : 'No open issues'}
                  action={
                    filter === 'closed' ? undefined : (
                      <Button as={InertiaLink} href={`${base}/issues/new`} variant="primary">
                        New issue
                      </Button>
                    )
                  }
                >
                  {filter === 'closed'
                    ? 'Issues you close will be listed here.'
                    : 'Issues are a good way to track bugs and tasks.'}
                </Empty>
              </div>
            )}
          </div>

          <Pagination
            page={props.page}
            path={`${base}/issues${filter === 'closed' ? '?state=closed' : ''}`}
          />
        </Stack>
      </Page>
    </>
  )
}
