import {Link as InertiaLink} from '@inertiajs/react'
import {
  Avatar,
  Button,
  CounterLabel,
  IconButton,
  Label,
  Link,
  ProgressBar,
  Stack,
  Text,
} from '@primer/react'
import {
  BookIcon,
  DatabaseIcon,
  FileDirectoryFillIcon,
  FileIcon,
  GearIcon,
  GitBranchIcon,
  HistoryIcon,
  LawIcon,
  LockIcon,
  RepoIcon,
  TagIcon,
} from '@primer/octicons-react'

import {BranchPicker} from '@/components/BranchPicker'
import {CloneField, CloneMenu} from '@/components/CloneMenu'
import {Empty} from '@/components/Empty'
import {Markdown} from '@/components/Markdown'
import {Page, Section} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import {WithSidebar} from '@/components/Sidebar'
import {TimeAgo} from '@/components/TimeAgo'
import {bytes, percent} from '@/lib/format'
import type {CommitInfo, RepoHeader, TreeEntry} from '@/lib/types'

type LanguageShare = {name: string; color: string; bytes: number; percent: number}
type Contributor = {name: string; email: string; commits: number}
type Tag = {name: string; sha: string; createdAtIso: string}

type Insight = {
  languages: LanguageShare[]
  contributors: Contributor[]
  sizeBytes: number
  license: string | null
  tags: Tag[]
}

type Props = RepoHeader & {
  empty?: boolean
  ref?: string
  path?: string
  parentPath?: string
  entries?: TreeEntry[]
  entryCommits?: Record<string, CommitInfo>
  branches: string[]
  totalCommits?: number
  latestCommit?: CommitInfo | null
  readmeHtml?: string | null
  readmeName?: string | null
  cloneUrl: string
  sshCloneUrl?: string | null
  insight?: Insight
  topics?: string[]
  error?: string
}

/** The repository overview: branch picker, file tree, README, and the About rail. */
export default function Code(props: Props) {
  const {owner, repo, branches, cloneUrl, sshCloneUrl, empty, topics = []} = props
  const base = `/${owner}/${repo.name}`
  const currentRef = props.ref ?? repo.defaultBranch
  const insight = props.insight

  return (
    <>
      <RepoHead tab="code" header={props} />
      <Page>
        <WithSidebar
          main={
            empty ? (
              <Empty icon={RepoIcon} title="This repository is empty">
                <Stack gap="normal">
                  <span>Push your first commit to get started.</span>
                  <div className="empty-clone">
                    <CloneField label="Clone URL" value={cloneUrl} />
                  </div>
                </Stack>
              </Empty>
            ) : (
              <Stack gap="normal">
                <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
                  <BranchPicker
                    branches={branches}
                    current={currentRef}
                    hrefPrefix={`${base}/tree/`}
                  />
                  <Link as={InertiaLink} href={`${base}/activity`} muted className="branch-count">
                    <GitBranchIcon />
                    <Text weight="semibold">{branches.length}</Text>
                    <span>{branches.length === 1 ? 'Branch' : 'Branches'}</span>
                  </Link>

                  <div className="u-ml-auto">
                    <Stack direction="horizontal" gap="condensed">
                      <Button
                        as={InertiaLink}
                        href={`${base}/activity`}
                        leadingVisual={HistoryIcon}
                        count={props.totalCommits}
                      >
                        Commits
                      </Button>
                      <CloneMenu cloneUrl={cloneUrl} sshCloneUrl={sshCloneUrl} />
                    </Stack>
                  </div>
                </Stack>

                <div className="card">
                  {props.latestCommit ? (
                    <div className="commit-tease">
                      <Avatar src={`/avatars/${props.latestCommit.authorName}`} size={20} />
                      <Text weight="semibold">{props.latestCommit.authorName}</Text>
                      <Link
                        as={InertiaLink}
                        href={`${base}/commit/${props.latestCommit.sha}`}
                        muted
                        className="u-truncate"
                      >
                        {props.latestCommit.subject}
                      </Link>
                      <span className="u-ml-auto u-nowrap u-muted commit-tease-meta">
                        <Link
                          as={InertiaLink}
                          href={`${base}/commit/${props.latestCommit.sha}`}
                          muted
                          className="u-mono"
                        >
                          {props.latestCommit.sha.slice(0, 7)}
                        </Link>{' '}
                        <TimeAgo at={props.latestCommit.committerDateIso} />
                      </span>
                    </div>
                  ) : null}

                  {props.path ? (
                    <div className="tree-row">
                      <div className="tree-row-name">
                        <FileDirectoryFillIcon className="tree-icon--dir" />
                        <Link
                          as={InertiaLink}
                          href={`${base}/tree/${currentRef}${props.parentPath ? `/${props.parentPath}` : ''}`}
                        >
                          ..
                        </Link>
                      </div>
                      <div className="tree-row-commit" />
                      <div className="tree-row-age" />
                    </div>
                  ) : null}

                  {(props.entries ?? []).map(entry => {
                    // entryCommits comes from one history walk, not a git log per row.
                    const commit = props.entryCommits?.[entry.name]
                    const target = `${base}/${entry.directory ? 'tree' : 'blob'}/${currentRef}/${
                      props.path ? `${props.path}/` : ''
                    }${entry.name}`
                    return (
                      <div className="tree-row" key={entry.sha + entry.name}>
                        <div className="tree-row-name">
                          {entry.directory ? (
                            <FileDirectoryFillIcon className="tree-icon--dir" />
                          ) : (
                            <FileIcon className="tree-icon--file" />
                          )}
                          <Link as={InertiaLink} href={target}>
                            {entry.name}
                          </Link>
                        </div>
                        <div className="tree-row-commit">
                          {commit ? (
                            <Link
                              as={InertiaLink}
                              href={`${base}/commit/${commit.sha}`}
                              muted
                              title={commit.subject}
                            >
                              {commit.subject}
                            </Link>
                          ) : null}
                        </div>
                        <div className="tree-row-age">
                          {commit ? <TimeAgo at={commit.committerDateIso} /> : null}
                        </div>
                      </div>
                    )
                  })}
                </div>

                {props.readmeHtml ? (
                  <div className="card" id="readme">
                    <div className="card-header">
                      <BookIcon className="u-muted" />
                      <Text weight="semibold">{props.readmeName}</Text>
                    </div>
                    <div className="markdown-pane">
                      <Markdown html={props.readmeHtml} />
                    </div>
                  </div>
                ) : null}
              </Stack>
            )
          }
          aside={
            <>
              <Section
                title="About"
                actions={
                  <IconButton
                    as={InertiaLink}
                    href={`${base}/settings`}
                    icon={GearIcon}
                    aria-label="Edit repository details"
                    variant="invisible"
                    size="small"
                  />
                }
              >
                <Stack gap="condensed">
                  {repo.description ? (
                    <Text>{repo.description}</Text>
                  ) : (
                    <Text className="u-muted">
                      <em>No description provided.</em>
                    </Text>
                  )}

                  {topics.length > 0 ? (
                    <Stack direction="horizontal" gap="condensed" wrap="wrap">
                      {topics.map(topic => (
                        <Label key={topic} variant="accent">
                          {topic}
                        </Label>
                      ))}
                    </Stack>
                  ) : null}

                  <Stack gap="condensed" className="u-muted about-facts">
                    {props.readmeName ? (
                      <Fact icon={BookIcon}>
                        <Link href="#readme" muted>
                          {props.readmeName}
                        </Link>
                      </Fact>
                    ) : null}
                    {insight?.license ? <Fact icon={LawIcon}>{insight.license} license</Fact> : null}
                    <Fact icon={repo.visibility === 'PRIVATE' ? LockIcon : RepoIcon}>
                      {repo.visibility.charAt(0) + repo.visibility.slice(1).toLowerCase()}
                    </Fact>
                    {insight && insight.sizeBytes > 0 ? (
                      <Fact icon={DatabaseIcon}>{bytes(insight.sizeBytes)}</Fact>
                    ) : null}
                  </Stack>
                </Stack>
              </Section>

              {insight && insight.contributors.length > 0 ? (
                <Section
                  title="Contributors"
                  actions={<CounterLabel>{insight.contributors.length}</CounterLabel>}
                >
                  <Stack direction="horizontal" gap="condensed" wrap="wrap">
                    {insight.contributors.slice(0, 8).map(person => (
                      <Avatar
                        key={person.email || person.name}
                        src={`/avatars/${person.name}`}
                        size={24}
                        alt={`${person.name} - ${person.commits} commits`}
                      />
                    ))}
                  </Stack>
                </Section>
              ) : null}

              {insight && insight.tags.length > 0 ? (
                <Section title="Tags" actions={<CounterLabel>{insight.tags.length}</CounterLabel>}>
                  <Stack gap="condensed">
                    {insight.tags.map(tag => (
                      <Stack key={tag.sha} direction="horizontal" align="center" gap="condensed">
                        <TagIcon className="u-muted" />
                        <Link as={InertiaLink} href={`${base}/tree/${tag.name}`} muted>
                          {tag.name}
                        </Link>
                      </Stack>
                    ))}
                  </Stack>
                </Section>
              ) : null}

              {insight && insight.languages.length > 0 ? (
                <Section title="Languages">
                  <ProgressBar aria-label="Language breakdown" className="language-bar">
                    {insight.languages.map(language => (
                      <ProgressBar.Item
                        key={language.name}
                        progress={language.percent}
                        style={{backgroundColor: language.color}}
                        aria-label={`${language.name} ${percent(language.percent)}`}
                      />
                    ))}
                  </ProgressBar>
                  <Stack direction="horizontal" gap="condensed" wrap="wrap" className="language-legend">
                    {insight.languages.map(language => (
                      <Stack key={language.name} direction="horizontal" align="center" gap="condensed">
                        <span className="lang-dot" style={{backgroundColor: language.color}} />
                        <Text weight="semibold">{language.name}</Text>
                        <Text className="u-muted">{percent(language.percent)}</Text>
                      </Stack>
                    ))}
                  </Stack>
                </Section>
              ) : null}
            </>
          }
        />
      </Page>
    </>
  )
}

function Fact({icon: IconComponent, children}: {icon: typeof BookIcon; children: React.ReactNode}) {
  return (
    <Stack direction="horizontal" align="center" gap="condensed">
      <IconComponent />
      <span>{children}</span>
    </Stack>
  )
}
