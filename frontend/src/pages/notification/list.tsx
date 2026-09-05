import {router} from '@inertiajs/react'
import {Button, Stack, Text} from '@primer/react'
import {CheckIcon, DotFillIcon, InboxIcon} from '@primer/octicons-react'

import {Empty} from '@/components/Empty'
import {Page, Subhead} from '@/components/Page'
import {Pagination} from '@/components/Pagination'
import {TimeAgo} from '@/components/TimeAgo'
import type {PageInfo} from '@/lib/types'

type Notification = {
  id: number
  message: string
  read: boolean
  createdAt: string | null
}

type Props = {notifications: Notification[]; unreadCount: number; page?: PageInfo}

/** An inbox is one of the few lists GitHub really does draw as a card. */
export default function Notifications({notifications, unreadCount, page}: Props) {
  return (
    <Page>
      <Subhead
        title="Notifications"
        actions={
          unreadCount > 0 ? (
            <Button size="small" leadingVisual={CheckIcon} onClick={() => router.post('/notifications/read-all')}>
              Mark all as read
            </Button>
          ) : undefined
        }
      />

      {notifications.length > 0 ? (
        <div className="card">
          <div className="card-header">
            <InboxIcon className="u-muted" />
            <Text weight="semibold">{unreadCount} unread</Text>
          </div>
          {notifications.map(item => (
            <div
              key={item.id}
              className={item.read ? 'card-row' : 'card-row notification--unread'}
            >
              {item.read ? (
                <InboxIcon className="u-muted" />
              ) : (
                <DotFillIcon className="notification-dot" />
              )}
              <div className="u-flex-auto">
                <Text weight={item.read ? 'normal' : 'semibold'} className={item.read ? 'u-muted' : undefined}>
                  {item.message}
                </Text>
                <Text as="div" size="small" className="u-muted">
                  <TimeAgo at={item.createdAt} />
                </Text>
              </div>
              {item.read ? null : (
                <Button size="small" onClick={() => router.post(`/notifications/${item.id}/read`)}>
                  Mark read
                </Button>
              )}
            </div>
          ))}
        </div>
      ) : (
        <Empty icon={InboxIcon} title="All caught up">
          You have no notifications.
        </Empty>
      )}

      <Stack className="notifications-pagination">
        <Pagination page={page} path="/notifications" />
      </Stack>
    </Page>
  )
}
