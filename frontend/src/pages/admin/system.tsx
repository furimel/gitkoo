import {Label, Text} from '@primer/react'
import type {ReactNode} from 'react'

import {AdminNav} from '@/components/AdminNav'
import {Card} from '@/components/FormCard'
import {Page, Subhead} from '@/components/Page'
import {WithSidebar} from '@/components/Sidebar'

type Props = {
  gitkooVersion: string
  javaVersion: string
  gitBinary: string
  dataPath: string
  ciEnabled: boolean
  ciWorkers: number
  sshEnabled: boolean
  sshPort: number
}

/** A settings panel is one of the few things that really is a card. */
export default function AdminSystem(props: Props) {
  return (
    <Page>
      <Subhead title="System" description="Runtime configuration of this instance." />
      <WithSidebar
        position="start"
        aside={<AdminNav active="system" />}
        main={
          <Card title="Runtime">
            <Detail label="GitKoo version" value={props.gitkooVersion} mono />
            <Detail label="Java version" value={props.javaVersion} mono />
            <Detail label="Git binary" value={props.gitBinary} mono />
            <Detail label="Data path" value={props.dataPath} mono />
            <Detail
              label="CI"
              value={
                <>
                  <Label variant={props.ciEnabled ? 'success' : 'secondary'}>
                    {props.ciEnabled ? 'enabled' : 'disabled'}
                  </Label>
                  <Text className="u-muted"> {props.ciWorkers} workers</Text>
                </>
              }
            />
            <Detail
              label="SSH"
              value={
                <>
                  <Label variant={props.sshEnabled ? 'success' : 'secondary'}>
                    {props.sshEnabled ? 'enabled' : 'disabled'}
                  </Label>
                  <Text className="u-muted"> port {props.sshPort}</Text>
                </>
              }
            />
          </Card>
        }
      />
    </Page>
  )
}

function Detail({label, value, mono}: {label: string; value: ReactNode; mono?: boolean}) {
  return (
    <div className="detail-row">
      <Text weight="semibold" className="u-flex-auto">
        {label}
      </Text>
      <span className={mono ? 'u-mono u-muted' : 'u-muted'}>{value}</span>
    </div>
  )
}
