import {Link as InertiaLink} from '@inertiajs/react'
import {Avatar, Heading, Link, Stack, Text} from '@primer/react'

import {Diff, DiffSummary} from '@/components/Diff'
import {Page, Section} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import {TimeAgo} from '@/components/TimeAgo'
import type {DiffFile, RepoHeader} from '@/lib/types'

type CommitDetail = {
  sha: string
  authorName: string
  authorEmail: string
  message: string
  parent: string | null
  committerDateIso: string | null
}

type Props = RepoHeader & {
  sha: string
  commit: CommitDetail
  diffFiles: DiffFile[]
  additions: number
  deletions: number
}

export default function Commit(props: Props) {
  const {owner, repo, commit, sha} = props
  const base = `/${owner}/${repo.name}`
  const parents = commit.parent ? commit.parent.split(' ').filter(Boolean) : []

  return (
    <>
      <RepoHead tab="code" header={props} />
      <Page>
        <Stack gap="normal">
          <div className="card">
            <div className="card-body">
              <Heading as="h1" variant="small">
                {commit.message}
              </Heading>
            </div>
            <div className="card-row">
              <Avatar src={`/avatars/${commit.authorName}`} size={20} />
              <Text weight="semibold">{commit.authorName}</Text>
              <Text className="u-muted">committed</Text>
              <Text className="u-muted">
                <TimeAgo at={commit.committerDateIso} />
              </Text>
              <span className="u-ml-auto u-muted">
                {parents.length > 0 ? (
                  <>
                    parent{' '}
                    {parents.map(parent => (
                      <Link
                        key={parent}
                        as={InertiaLink}
                        href={`${base}/commit/${parent}`}
                        muted
                        className="u-mono"
                      >
                        {parent.slice(0, 7)}{' '}
                      </Link>
                    ))}
                  </>
                ) : null}
                <Text className="u-mono">commit {sha.slice(0, 7)}</Text>
              </span>
            </div>
          </div>

          <Section>
            <DiffSummary
              files={props.diffFiles.length}
              additions={props.additions}
              deletions={props.deletions}
              verb="Showing"
            />
          </Section>

          <Diff files={props.diffFiles} />
        </Stack>
      </Page>
    </>
  )
}
