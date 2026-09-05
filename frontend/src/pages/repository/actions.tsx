import {Button, Text} from '@primer/react'
import {CheckIcon, ClockIcon, LinkExternalIcon, PlayIcon, XIcon} from '@primer/octicons-react'

import {Empty} from '@/components/Empty'
import {Page, Subhead} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import {Row, Rows} from '@/components/Rows'
import type {RepoHeader} from '@/lib/types'

type Run = {
  id: number
  event: string
  ref: string | null
  status: 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED'
}

type Props = RepoHeader & {runs: Run[]}

export default function Actions(props: Props) {
  const {owner, repo, runs} = props
  const base = `/${owner}/${repo.name}`

  return (
    <>
      <RepoHead tab="actions" header={props} />
      <Page>
        <Subhead title="Workflow runs" />

        {runs.length > 0 ? (
          <Rows>
            {runs.map(run => (
              <Row key={run.id}>
                <StatusIcon status={run.status} />
                <div className="u-flex-auto">
                  <Text weight="semibold">{run.event}</Text>{' '}
                  {run.ref ? <Text className="u-muted">{run.ref}</Text> : null}
                  <Text as="div" size="small" className="u-muted">
                    Run #{run.id} &middot; {titleCase(run.status)}
                  </Text>
                </div>
                <Button
                  as="a"
                  size="small"
                  href={`${base}/actions/${run.id}/logs`}
                  target="_blank"
                  rel="noopener"
                  trailingVisual={LinkExternalIcon}
                >
                  View logs
                </Button>
              </Row>
            ))}
          </Rows>
        ) : (
          <Empty icon={PlayIcon} title="No workflow runs yet">
            Add a .gitkoo/workflows/build.koo file and push to trigger a run.
          </Empty>
        )}
      </Page>
    </>
  )
}

function StatusIcon({status}: {status: Run['status']}) {
  if (status === 'SUCCESS') return <CheckIcon className="u-success" />
  if (status === 'FAILED') return <XIcon className="u-danger" />
  return <ClockIcon className="u-muted" />
}

function titleCase(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase()
}
