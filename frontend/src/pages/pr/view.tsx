import {router, useForm, usePage} from '@inertiajs/react'
import {
  BranchName,
  Button,
  FormControl,
  Heading,
  Radio,
  RadioGroup,
  Stack,
  Text,
  Textarea,
  Timeline,
} from '@primer/react'
import {
  AlertIcon,
  CheckIcon,
  CommentIcon,
  FileIcon,
  GitCommitIcon,
  GitMergeIcon,
  XIcon,
} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import {CommentItem, ComposerItem, ConversationHeader, EventItem} from '@/components/Conversation'
import {Diff, DiffSummary} from '@/components/Diff'
import {Markdown} from '@/components/Markdown'
import {Page, Section} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import {WithSidebar} from '@/components/Sidebar'
import type {
  CommitInfo,
  DiffFile,
  PullRequest,
  RepoHeader,
  Review,
  SharedPageProps,
  UserRecord,
} from '@/lib/types'

type Props = RepoHeader & {
  pr: PullRequest
  prBodyHtml: string
  author: UserRecord | null
  reviews: Review[]
  reviewers: (UserRecord | null)[]
  reviewBodies: (string | null)[]
  commits: CommitInfo[]
  diffFiles: DiffFile[]
  additions: number
  deletions: number
  mergesCleanly: boolean
}

/**
 * The pull request page.
 *
 * This used to be a ladder of five stacked cards - description, review, merge panel,
 * merged panel, review form - which read as a column of identical rectangles rather
 * than a conversation. Everything now hangs off the timeline rail, and the two
 * mutually exclusive merge panels are one.
 */
export default function PullRequestView(props: Props) {
  const {auth} = usePage<SharedPageProps>().props
  const {owner, repo, pr, author} = props
  const base = `/${owner}/${repo.name}/pulls/${pr.number}`

  const review = useForm({body: '', state: 'COMMENT'})

  function submitReview(event: FormEvent) {
    event.preventDefault()
    review.post(`${base}/review`, {onSuccess: () => review.reset()})
  }

  return (
    <>
      <RepoHead tab="pulls" header={props} />
      <Page>
        <ConversationHeader
          title={pr.title}
          number={pr.number}
          kind="pr"
          status={pr.status}
          byline={
            <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
              <Text weight="semibold">{author?.displayName ?? author?.username ?? 'Unknown'}</Text>
              <span>wants to merge</span>
              <Text weight="semibold">{props.commits.length}</Text>
              <span>{props.commits.length === 1 ? 'commit' : 'commits'} into</span>
              <BranchName href="#">{pr.targetBranch}</BranchName>
              <span>from</span>
              <BranchName href="#">{pr.sourceBranch}</BranchName>
            </Stack>
          }
        />

        <WithSidebar
          main={
            <Stack gap="normal">
              <Timeline>
                <CommentItem
                  username={author?.username}
                  displayName={author?.displayName}
                  verb="opened this pull request"
                  at={pr.createdAt}
                  bodyHtml={props.prBodyHtml}
                />

                {props.reviews.map((item, index) => (
                  <EventItem
                    key={item.id}
                    tone={
                      item.state === 'APPROVE'
                        ? 'success'
                        : item.state === 'REQUEST_CHANGES'
                          ? 'danger'
                          : undefined
                    }
                    icon={
                      item.state === 'APPROVE' ? (
                        <CheckIcon />
                      ) : item.state === 'REQUEST_CHANGES' ? (
                        <XIcon />
                      ) : (
                        <CommentIcon />
                      )
                    }
                  >
                    <Text className="u-muted">
                      <Text weight="semibold">
                        {props.reviewers[index]?.displayName ??
                          props.reviewers[index]?.username ??
                          'Unknown'}
                      </Text>{' '}
                      {item.state === 'APPROVE'
                        ? 'approved these changes'
                        : item.state === 'REQUEST_CHANGES'
                          ? 'requested changes'
                          : 'reviewed'}
                    </Text>
                    {props.reviewBodies[index] ? (
                      <div className="card review-body">
                        <div className="card-body">
                          <Markdown html={props.reviewBodies[index] as string} />
                        </div>
                      </div>
                    ) : null}
                  </EventItem>
                ))}

                {pr.status === 'MERGED' ? (
                  <EventItem icon={<GitMergeIcon />} tone="done">
                    <Heading as="h3" variant="small">
                      Pull request successfully merged
                    </Heading>
                    <Text size="small" className="u-muted">
                      Merged as{' '}
                      {pr.mergeCommitSha ? <code>{pr.mergeCommitSha.slice(0, 7)}</code> : null}
                    </Text>
                  </EventItem>
                ) : null}

                {pr.status === 'OPEN' && auth.user ? (
                  <EventItem
                    icon={props.mergesCleanly ? <GitMergeIcon /> : <AlertIcon />}
                    tone={props.mergesCleanly ? 'success' : 'danger'}
                  >
                    {/* Status reflects a real `git merge-tree` check, not an assumption. */}
                    <Heading as="h3" variant="small">
                      {props.mergesCleanly
                        ? 'This branch has no conflicts with the base branch'
                        : 'This branch has conflicts that must be resolved'}
                    </Heading>
                    <Text as="p" size="small" className="u-muted">
                      {props.mergesCleanly ? (
                        'Merging can be performed automatically.'
                      ) : (
                        <>
                          Merge <BranchName href="#">{pr.targetBranch}</BranchName> into{' '}
                          <BranchName href="#">{pr.sourceBranch}</BranchName> and push again.
                        </>
                      )}
                    </Text>
                    <Stack direction="horizontal" gap="condensed" wrap="wrap">
                      <Button
                        variant="primary"
                        leadingVisual={GitMergeIcon}
                        disabled={!props.mergesCleanly}
                        onClick={() => router.post(`${base}/merge`)}
                      >
                        Merge pull request
                      </Button>
                      <Button
                        variant="danger"
                        onClick={() => {
                          if (window.confirm('Close this pull request without merging?')) {
                            router.post(`${base}/close`)
                          }
                        }}
                      >
                        Close pull request
                      </Button>
                    </Stack>
                  </EventItem>
                ) : null}

                {pr.status === 'OPEN' && auth.user ? (
                  <ComposerItem username={auth.user.username}>
                    <form className="card" onSubmit={submitReview}>
                      <div className="card-header">
                        <Text weight="semibold">Submit a review</Text>
                      </div>
                      <div className="card-body">
                        <Stack gap="normal">
                          <Textarea
                            block
                            rows={4}
                            placeholder="Leave a comment (Markdown supported)"
                            value={review.data.body}
                            onChange={e => review.setData('body', e.target.value)}
                          />
                          <RadioGroup
                            name="state"
                            aria-label="Review type"
                            onChange={value => review.setData('state', value ?? 'COMMENT')}
                          >
                            <RadioGroup.Label visuallyHidden>Review type</RadioGroup.Label>
                            <FormControl>
                              <Radio value="COMMENT" defaultChecked />
                              <FormControl.Label>Comment</FormControl.Label>
                              <FormControl.Caption>
                                Submit general feedback without explicit approval.
                              </FormControl.Caption>
                            </FormControl>
                            <FormControl>
                              <Radio value="APPROVE" />
                              <FormControl.Label>Approve</FormControl.Label>
                              <FormControl.Caption>
                                Submit feedback and approve merging these changes.
                              </FormControl.Caption>
                            </FormControl>
                            <FormControl>
                              <Radio value="REQUEST_CHANGES" />
                              <FormControl.Label>Request changes</FormControl.Label>
                              <FormControl.Caption>
                                Submit feedback that must be addressed before merging.
                              </FormControl.Caption>
                            </FormControl>
                          </RadioGroup>
                        </Stack>
                      </div>
                      <div className="card-footer">
                        <Button type="submit" variant="primary" disabled={review.processing}>
                          Submit review
                        </Button>
                      </div>
                    </form>
                  </ComposerItem>
                ) : null}
              </Timeline>

              <Section>
                <DiffSummary
                  files={props.diffFiles.length}
                  additions={props.additions}
                  deletions={props.deletions}
                  verb="Changed"
                />
              </Section>
              <Diff files={props.diffFiles} />
            </Stack>
          }
          aside={
            <>
              <Section title="Reviewers">
                {props.reviews.length > 0 ? (
                  <Stack gap="condensed">
                    {props.reviews.map((item, index) => (
                      <Stack key={item.id} direction="horizontal" align="center" gap="condensed">
                        {item.state === 'APPROVE' ? (
                          <CheckIcon className="u-success" />
                        ) : item.state === 'REQUEST_CHANGES' ? (
                          <XIcon className="u-danger" />
                        ) : (
                          <CommentIcon className="u-muted" />
                        )}
                        <span>{props.reviewers[index]?.username ?? 'unknown'}</span>
                      </Stack>
                    ))}
                  </Stack>
                ) : (
                  <Text size="small" className="u-muted">
                    No reviews yet
                  </Text>
                )}
              </Section>

              <Section>
                <Stack gap="condensed" className="u-muted about-facts">
                  <Stack direction="horizontal" align="center" gap="condensed">
                    <GitCommitIcon />
                    <span>
                      {props.commits.length}{' '}
                      {props.commits.length === 1 ? 'commit' : 'commits'}
                    </span>
                  </Stack>
                  <Stack direction="horizontal" align="center" gap="condensed">
                    <FileIcon />
                    <span>
                      {props.diffFiles.length}{' '}
                      {props.diffFiles.length === 1 ? 'file changed' : 'files changed'}
                    </span>
                  </Stack>
                </Stack>
              </Section>
            </>
          }
        />
      </Page>
    </>
  )
}
