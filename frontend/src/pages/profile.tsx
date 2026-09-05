import {Link as InertiaLink} from '@inertiajs/react'
import {Avatar, Button, CounterLabel, Heading, Stack, Text} from '@primer/react'
import {ClockIcon, RepoIcon, ShieldIcon} from '@primer/octicons-react'

import {Empty} from '@/components/Empty'
import {Page, Subhead} from '@/components/Page'
import {Pagination} from '@/components/Pagination'
import {RepoRow, Rows} from '@/components/Rows'
import {WithSidebar} from '@/components/Sidebar'
import {TimeAgo} from '@/components/TimeAgo'
import type {PageInfo, Repo, UserRecord} from '@/lib/types'

type Props = {
  profile: UserRecord
  repos: Repo[]
  repoCount: number
  isSelf: boolean
  page?: PageInfo
}

export default function Profile({profile, repos, repoCount, isSelf, page}: Props) {
  return (
    <Page>
      <WithSidebar
        position="start"
        aside={
          <Stack gap="normal">
            {/* Fluid, not a fixed square: on a phone the sidebar is the full width. */}
            <Avatar src={`/avatars/${profile.username}`} size={260} className="profile-avatar" />

            <div>
              <Heading as="h1" variant="medium">
                {profile.displayName ?? profile.username}
              </Heading>
              <Text as="p" className="u-muted profile-username">
                {profile.username}
              </Text>
              {profile.bio ? <Text as="p">{profile.bio}</Text> : null}
            </div>

            {isSelf ? (
              <Button as={InertiaLink} href="/settings/keys" block>
                Edit profile
              </Button>
            ) : null}

            <Stack gap="condensed" className="u-muted profile-facts">
              <Stack direction="horizontal" align="center" gap="condensed">
                <RepoIcon />
                <span>
                  <Text weight="semibold">{repoCount}</Text> repositories
                </span>
              </Stack>
              {profile.createdAt ? (
                <Stack direction="horizontal" align="center" gap="condensed">
                  <ClockIcon />
                  <span>
                    Joined <TimeAgo at={profile.createdAt} />
                  </span>
                </Stack>
              ) : null}
              {profile.admin ? (
                <Stack direction="horizontal" align="center" gap="condensed">
                  <ShieldIcon />
                  <span>Site administrator</span>
                </Stack>
              ) : null}
            </Stack>
          </Stack>
        }
        main={
          <>
            <Subhead title="Repositories" actions={<CounterLabel>{repoCount}</CounterLabel>} />

            {repos.length > 0 ? (
              <Rows>
                {repos.map(repo => (
                  <RepoRow key={repo.id} owner={profile.username} repo={repo} />
                ))}
              </Rows>
            ) : (
              <Empty
                icon={RepoIcon}
                title="No repositories to show"
                action={
                  isSelf ? (
                    <Button as={InertiaLink} href="/new" variant="primary">
                      New repository
                    </Button>
                  ) : undefined
                }
              >
                {isSelf
                  ? 'Repositories you create will appear here.'
                  : 'This user has no repositories you can see.'}
              </Empty>
            )}

            <Pagination page={page} path={`/@/${profile.username}`} />
          </>
        }
      />
    </Page>
  )
}
