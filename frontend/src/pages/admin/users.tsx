import {Link as InertiaLink} from '@inertiajs/react'
import {Avatar, Label, Link, Text} from '@primer/react'

import {AdminNav} from '@/components/AdminNav'
import {Page, Subhead} from '@/components/Page'
import {Pagination} from '@/components/Pagination'
import {Row, Rows} from '@/components/Rows'
import {WithSidebar} from '@/components/Sidebar'
import type {PageInfo, UserRecord} from '@/lib/types'

type Props = {users: UserRecord[]; totalUsers: number; page?: PageInfo}

export default function AdminUsers({users, totalUsers, page}: Props) {
  return (
    <Page>
      <Subhead title="Users" description={`${totalUsers} accounts on this instance.`} />
      <WithSidebar
        position="start"
        aside={<AdminNav active="users" />}
        main={
          <>
            <Rows>
              {users.map(user => (
                <Row key={user.id}>
                  <Avatar src={`/avatars/${user.username}`} size={32} />
                  <div className="u-flex-auto">
                    <Link as={InertiaLink} href={`/@/${user.username}`} className="repo-row-name">
                      {user.displayName ?? user.username}
                    </Link>
                    <Text as="div" size="small" className="u-muted">
                      {user.email}
                    </Text>
                  </div>
                  {user.admin ? <Label variant="accent">admin</Label> : null}
                  <Label variant={user.status === 'ACTIVE' ? 'success' : 'secondary'}>
                    {user.status.toLowerCase()}
                  </Label>
                </Row>
              ))}
            </Rows>
            <Pagination page={page} path="/admin/users" />
          </>
        }
      />
    </Page>
  )
}
