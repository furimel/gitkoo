import type {ReactNode} from 'react'
import {Avatar, Heading, Stack, Text, Timeline} from '@primer/react'

import {Markdown} from '@/components/Markdown'
import {StateBadge} from '@/components/StateBadge'
import {TimeAgo} from '@/components/TimeAgo'

/**
 * Issue and pull request conversation.
 *
 * A comment is a card, but it hangs off the timeline rail - and that rail is what
 * stops a column of comments reading as a stack of loose rectangles. The pull
 * request page had five free-floating cards for exactly this reason: the merge panel
 * and the review form had escaped the rail.
 */
export function ConversationHeader({
  title,
  number,
  kind,
  status,
  byline,
}: {
  title: string
  number: number
  kind: 'issue' | 'pr'
  status: 'OPEN' | 'CLOSED' | 'MERGED'
  byline: ReactNode
}) {
  return (
    <div className="section">
      <Heading as="h1" variant="large">
        {title} <Text className="u-muted conversation-number">#{number}</Text>
      </Heading>
      <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
        <StateBadge kind={kind} status={status} />
        <Text className="u-muted">{byline}</Text>
      </Stack>
    </div>
  )
}

/** A comment bubble on the rail. */
export function CommentItem({
  username,
  displayName,
  verb,
  at,
  bodyHtml,
}: {
  username: string | null | undefined
  displayName: string | null | undefined
  verb: string
  at: string | null | undefined
  bodyHtml: string
}) {
  return (
    <Timeline.Item>
      <Timeline.Badge>
        <Avatar src={`/avatars/${username ?? 'ghost'}`} size={24} />
      </Timeline.Badge>
      <Timeline.Body>
        <div className="card">
          <div className="card-header u-muted">
            <Text weight="semibold" className="comment-author">
              {displayName ?? username ?? 'Unknown'}
            </Text>
            <span>{verb}</span>
            <TimeAgo at={at} />
          </div>
          <div className="card-body">
            <Markdown html={bodyHtml} />
          </div>
        </div>
      </Timeline.Body>
    </Timeline.Item>
  )
}

/** A state change on the rail: closed, merged, reviewed. Not a card. */
export function EventItem({
  icon,
  tone,
  children,
}: {
  icon: ReactNode
  tone?: 'success' | 'danger' | 'done'
  children: ReactNode
}) {
  return (
    <Timeline.Item>
      <Timeline.Badge className={tone ? `timeline-badge--${tone}` : undefined}>
        {icon}
      </Timeline.Badge>
      <Timeline.Body>{children}</Timeline.Body>
    </Timeline.Item>
  )
}

/** The composer, as the last item on the rail - which is where GitHub puts it. */
export function ComposerItem({username, children}: {username: string; children: ReactNode}) {
  return (
    <Timeline.Item>
      <Timeline.Badge>
        <Avatar src={`/avatars/${username}`} size={24} />
      </Timeline.Badge>
      <Timeline.Body>{children}</Timeline.Body>
    </Timeline.Item>
  )
}
