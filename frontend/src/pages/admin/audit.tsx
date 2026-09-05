import {ShieldIcon} from '@primer/octicons-react'
import {Stack, Text} from '@primer/react'

import {AdminNav} from '@/components/AdminNav'
import {Empty} from '@/components/Empty'
import {Page, Subhead} from '@/components/Page'
import {Row, Rows} from '@/components/Rows'
import {WithSidebar} from '@/components/Sidebar'
import {TimeAgo} from '@/components/TimeAgo'

type AuditEvent = {
  id: number
  action: string
  targetType: string | null
  targetId: number | null
  ip: string | null
  createdAt: string | null
}

type Props = {events: AuditEvent[]}

export default function AdminAudit({events}: Props) {
  return (
    <Page>
      <Subhead title="Audit log" description="Security-relevant actions recorded on this instance." />
      <WithSidebar
        position="start"
        aside={<AdminNav active="audit" />}
        main={
          events.length > 0 ? (
            <Rows>
              {events.map(event => (
                <Row key={event.id} align="start">
                  <ShieldIcon className="u-muted" />
                  <div className="u-flex-auto">
                    <Text weight="semibold">{event.action}</Text>{' '}
                    {event.targetType ? (
                      <Text className="u-muted">
                        {event.targetType} #{event.targetId}
                      </Text>
                    ) : null}
                    <Stack direction="horizontal" gap="condensed" className="u-muted audit-meta">
                      <TimeAgo at={event.createdAt} />
                      {event.ip ? <span className="u-mono">{event.ip}</span> : null}
                    </Stack>
                  </div>
                </Row>
              ))}
            </Rows>
          ) : (
            <Empty icon={ShieldIcon} title="No audit events">
              Events appear here when security-relevant actions occur.
            </Empty>
          )
        }
      />
    </Page>
  )
}
