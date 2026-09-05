import {GitCommitIcon, HistoryIcon} from '@primer/octicons-react'
import {Text} from '@primer/react'

import {Empty} from '@/components/Empty'
import {Page, Subhead} from '@/components/Page'
import {Pagination} from '@/components/Pagination'
import {RepoHead} from '@/components/RepoHead'
import {Row, Rows} from '@/components/Rows'
import {TimeAgo} from '@/components/TimeAgo'
import type {PageInfo, RepoHeader} from '@/lib/types'

type Activity = {id: number; message: string; createdAt: string | null}
type Props = RepoHeader & {activities: Activity[]; page?: PageInfo}

export default function ActivityPage(props: Props) {
  const {owner, repo, activities} = props

  return (
    <>
      <RepoHead tab="activity" header={props} />
      <Page>
        <Subhead title="Activity" />

        {activities.length > 0 ? (
          <Rows>
            {activities.map(activity => (
              <Row key={activity.id}>
                <GitCommitIcon className="u-muted" />
                <span className="u-flex-auto">{activity.message}</span>
                <Text size="small" className="u-muted u-nowrap">
                  <TimeAgo at={activity.createdAt} />
                </Text>
              </Row>
            ))}
          </Rows>
        ) : (
          <Empty icon={HistoryIcon} title="No activity yet">
            Activity appears when someone pushes, opens issues, or merges pull requests.
          </Empty>
        )}

        <Pagination page={props.page} path={`/${owner}/${repo.name}/activity`} />
      </Page>
    </>
  )
}
