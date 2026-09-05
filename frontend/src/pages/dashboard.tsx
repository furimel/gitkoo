import {Link as InertiaLink} from '@inertiajs/react'
import {Button} from '@primer/react'
import {RepoIcon} from '@primer/octicons-react'

import {Page, Subhead} from '@/components/Page'
import {Empty} from '@/components/Empty'
import {Pagination} from '@/components/Pagination'
import {RepoRow, Rows} from '@/components/Rows'
import type {PageInfo, Repo, SharedPageProps} from '@/lib/types'

type Props = SharedPageProps & {
  repos: Repo[]
  page?: PageInfo
}

/**
 * The signed-in landing page.
 *
 * The template this replaces rendered the repository list *and* the "no
 * repositories yet" panel at the same time, because `th:if` cannot guard a
 * `th:replace` on the same element - the fragment resolves at precedence 100 and
 * the condition is evaluated at 300. That whole category of bug is gone: here the
 * empty branch is an else, and the compiler knows it.
 */
export default function Dashboard({repos, page, auth}: Props) {
  return (
    <Page>
      <Subhead
        title="Your repositories"
        actions={
          <Button as={InertiaLink} href="/new" variant="primary" size="small" leadingVisual={RepoIcon}>
            New
          </Button>
        }
      />

      {repos.length > 0 ? (
        <Rows>
          {repos.map(repo => (
            <RepoRow key={repo.id} owner={auth.user?.username ?? ''} repo={repo} />
          ))}
        </Rows>
      ) : (
        <Empty
          icon={RepoIcon}
          title="No repositories yet"
          action={
            <Button as={InertiaLink} href="/new" variant="primary">
              Create a repository
            </Button>
          }
        >
          Repositories you create will show up here.
        </Empty>
      )}

      <Pagination page={page} path="/" />
    </Page>
  )
}
