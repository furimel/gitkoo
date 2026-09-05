import {PeopleIcon} from '@primer/octicons-react'
import {Text} from '@primer/react'

import {AdminNav} from '@/components/AdminNav'
import {Page, Subhead} from '@/components/Page'
import {Row, Rows} from '@/components/Rows'
import {WithSidebar} from '@/components/Sidebar'

type Props = {userCount: number}

export default function AdminIndex({userCount}: Props) {
  return (
    <Page>
      <Subhead title="Site administration" />
      <WithSidebar
        position="start"
        aside={<AdminNav active="overview" />}
        main={
          <Rows>
            <Row>
              <PeopleIcon className="u-muted" />
              <span className="u-flex-auto">Registered users</span>
              <Text weight="semibold">{userCount}</Text>
            </Row>
          </Rows>
        }
      />
    </Page>
  )
}
