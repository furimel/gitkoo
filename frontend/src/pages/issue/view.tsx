import {router, useForm, usePage} from '@inertiajs/react'
import {
  ActionList,
  ActionMenu,
  Button,
  FormControl,
  IconButton,
  Label,
  Stack,
  Text,
  TextInput,
  Timeline,
  Textarea,
} from '@primer/react'
import {
  CommentIcon,
  GearIcon,
  IssueClosedIcon,
  IssueOpenedIcon,
  PersonIcon,
  XIcon,
} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import {
  CommentItem,
  ComposerItem,
  ConversationHeader,
  EventItem,
} from '@/components/Conversation'
import {Page, Section} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import {WithSidebar} from '@/components/Sidebar'
import {TimeAgo} from '@/components/TimeAgo'
import type {
  Comment,
  Issue,
  Label as LabelRecord,
  RepoHeader,
  SharedPageProps,
  UserRecord,
} from '@/lib/types'

type Props = RepoHeader & {
  issue: Issue
  issueBodyHtml: string
  author: UserRecord | null
  comments: Comment[]
  commentAuthors: (UserRecord | null)[]
  commentBodies: string[]
  attachedLabels: LabelRecord[]
  availableLabels: LabelRecord[]
}

export default function IssueView(props: Props) {
  const {auth} = usePage<SharedPageProps>().props
  const {owner, repo, issue, author} = props
  const base = `/${owner}/${repo.name}/issues/${issue.number}`

  const comment = useForm({body: ''})
  const newLabel = useForm({name: '', description: '', color: '#0969da', returnNumber: issue.number})

  function postComment(event: FormEvent) {
    event.preventDefault()
    comment.post(`${base}/comment`, {onSuccess: () => comment.reset()})
  }

  return (
    <>
      <RepoHead tab="issues" header={props} />
      <Page>
        <ConversationHeader
          title={issue.title}
          number={issue.number}
          kind="issue"
          status={issue.status}
          byline={
            <>
              <Text weight="semibold">{author?.displayName ?? author?.username ?? 'Unknown'}</Text>{' '}
              opened this issue <TimeAgo at={issue.createdAt} /> &middot; {props.comments.length}{' '}
              {props.comments.length === 1 ? 'comment' : 'comments'}
            </>
          }
        />

        <WithSidebar
          main={
            <Timeline>
              <CommentItem
                username={author?.username}
                displayName={author?.displayName}
                verb="commented"
                at={issue.createdAt}
                bodyHtml={props.issueBodyHtml}
              />

              {props.comments.map((item, index) => (
                <CommentItem
                  key={item.id}
                  username={props.commentAuthors[index]?.username}
                  displayName={props.commentAuthors[index]?.displayName}
                  verb="commented"
                  at={item.createdAt}
                  bodyHtml={props.commentBodies[index] ?? ''}
                />
              ))}

              {issue.status === 'CLOSED' ? (
                <EventItem icon={<IssueClosedIcon />} tone="done">
                  <Text className="u-muted">
                    This issue was closed <TimeAgo at={issue.closedAt} />
                  </Text>
                </EventItem>
              ) : null}

              {auth.user ? (
                <ComposerItem username={auth.user.username}>
                  {issue.status === 'OPEN' ? (
                    <form className="card" onSubmit={postComment}>
                      <div className="card-header">
                        <Text weight="semibold">Add a comment</Text>
                      </div>
                      <div className="card-body">
                        <Textarea
                          block
                          rows={5}
                          placeholder="Leave a comment (Markdown supported)"
                          value={comment.data.body}
                          onChange={e => comment.setData('body', e.target.value)}
                        />
                      </div>
                      <div className="card-footer">
                        <Button
                          type="button"
                          leadingVisual={IssueClosedIcon}
                          onClick={() => router.post(`${base}/close`)}
                        >
                          Close issue
                        </Button>
                        <Button type="submit" variant="primary" disabled={comment.processing}>
                          Comment
                        </Button>
                      </div>
                    </form>
                  ) : (
                    <Button
                      leadingVisual={IssueOpenedIcon}
                      onClick={() => router.post(`${base}/reopen`)}
                    >
                      Reopen issue
                    </Button>
                  )}
                </ComposerItem>
              ) : null}
            </Timeline>
          }
          aside={
            <>
              <Section
                title="Labels"
                actions={
                  auth.user ? (
                    <ActionMenu>
                      <ActionMenu.Anchor>
                        <IconButton
                          icon={GearIcon}
                          aria-label="Manage labels"
                          variant="invisible"
                          size="small"
                        />
                      </ActionMenu.Anchor>
                      <ActionMenu.Overlay width="medium">
                        {props.availableLabels.length > 0 ? (
                          <ActionList>
                            {props.availableLabels.map(label => (
                              <ActionList.Item
                                key={label.id}
                                onSelect={() =>
                                  router.post(`${base}/labels/add`, {labelName: label.name})
                                }
                              >
                                <ActionList.LeadingVisual>
                                  <span
                                    className="lang-dot"
                                    style={{backgroundColor: label.color}}
                                  />
                                </ActionList.LeadingVisual>
                                {label.name}
                              </ActionList.Item>
                            ))}
                          </ActionList>
                        ) : null}

                        <form
                          className="label-create"
                          onSubmit={event => {
                            event.preventDefault()
                            newLabel.post(`/${owner}/${repo.name}/labels/new`, {
                              onSuccess: () => newLabel.reset(),
                            })
                          }}
                        >
                          <FormControl required>
                            <FormControl.Label>New label</FormControl.Label>
                            <TextInput
                              block
                              size="small"
                              placeholder="Label name"
                              value={newLabel.data.name}
                              onChange={e => newLabel.setData('name', e.target.value)}
                            />
                          </FormControl>
                          <Stack direction="horizontal" gap="condensed" align="center">
                            <input
                              type="color"
                              aria-label="Label colour"
                              className="color-swatch"
                              value={newLabel.data.color}
                              onChange={e => newLabel.setData('color', e.target.value)}
                            />
                            <Button type="submit" size="small" block>
                              Create
                            </Button>
                          </Stack>
                        </form>
                      </ActionMenu.Overlay>
                    </ActionMenu>
                  ) : undefined
                }
              >
                {props.attachedLabels.length > 0 ? (
                  <Stack direction="horizontal" gap="condensed" wrap="wrap">
                    {props.attachedLabels.map(label => (
                      <span key={label.id} className="label-chip">
                        <Label style={{backgroundColor: label.color, color: '#fff'}}>
                          {label.name}
                        </Label>
                        {auth.user ? (
                          <IconButton
                            icon={XIcon}
                            size="small"
                            variant="invisible"
                            aria-label={`Remove the ${label.name} label`}
                            onClick={() => router.post(`${base}/labels/remove`, {labelId: label.id})}
                          />
                        ) : null}
                      </span>
                    ))}
                  </Stack>
                ) : (
                  <Text size="small" className="u-muted">
                    None yet
                  </Text>
                )}
              </Section>

              <Section>
                <Stack gap="condensed" className="u-muted about-facts">
                  <Stack direction="horizontal" align="center" gap="condensed">
                    <PersonIcon />
                    <span>
                      Opened by <Text weight="semibold">{author?.username ?? 'unknown'}</Text>
                    </span>
                  </Stack>
                  <Stack direction="horizontal" align="center" gap="condensed">
                    <CommentIcon />
                    <span>{props.comments.length} comments</span>
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
