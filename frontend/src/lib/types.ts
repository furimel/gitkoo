import type {ComponentType, ReactNode} from 'react'

/**
 * The props every page receives, whatever its controller adds.
 *
 * These come from SharedProps on the server, so they are guaranteed present rather
 * than threaded through thirty controllers by hand.
 */
export type SharedPageProps = {
  auth: {user: Account | null}
  /** Flash set by RedirectAttributes on the request before this one. */
  error?: string
  success?: string
}

export type Account = {
  username: string
  displayName: string | null
  admin: boolean
  unread: number
}

/**
 * A page that supplies its own layout.
 *
 * Only the three pages with no app chrome need this; everything else gets the
 * default Shell assigned in main.tsx.
 */
/* eslint-disable @typescript-eslint/no-explicit-any */
// `any` matches Inertia's own ReactComponent, which a page has to be assignable to.
// Pages still type their own props on the function signature, where it matters.
export type PageComponent = ComponentType<any> & {
  layout?: (children: ReactNode) => ReactNode
}

// ── Server-side records, as they arrive over the wire ─────────────────────

export type Repo = {
  id: number
  name: string
  description: string | null
  visibility: 'PUBLIC' | 'PRIVATE' | 'INTERNAL'
  defaultBranch: string
  archived: boolean
  forkOfId: number | null
  homepage: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type UserRecord = {
  id: number
  username: string
  displayName: string | null
  email: string | null
  bio: string | null
  status: string
  admin: boolean
  createdAt: string | null
}

export type Issue = {
  id: number
  number: number
  title: string
  body: string | null
  status: 'OPEN' | 'CLOSED'
  createdAt: string | null
  closedAt: string | null
}

export type PullRequest = {
  id: number
  number: number
  title: string
  body: string | null
  status: 'OPEN' | 'CLOSED' | 'MERGED'
  sourceBranch: string
  targetBranch: string
  mergeCommitSha: string | null
  createdAt: string | null
}

export type Label = {
  id: number
  name: string
  description: string | null
  color: string
}

export type Comment = {
  id: number
  body: string
  createdAt: string | null
}

export type Review = {
  id: number
  state: 'COMMENT' | 'APPROVE' | 'REQUEST_CHANGES'
  body: string | null
  createdAt: string | null
}

export type CommitInfo = {
  sha: string
  authorName: string
  authorEmail: string
  subject: string
  committerDateIso: string | null
}

export type TreeEntry = {
  mode: string
  type: string
  sha: string
  name: string
  directory: boolean
}

export type DiffLine = {
  /** "hunk" | "add" | "del" | "context", as DiffParser.Line records it. */
  type: string
  content: string
  oldNumber: number | null
  newNumber: number | null
  hunk: boolean
  addition: boolean
  deletion: boolean
}

export type DiffFile = {
  path: string
  additions: number
  deletions: number
  binary: boolean
  language: string | null
  lines: DiffLine[]
}

/** Mirrors the Java record `web.Page`, derived accessors included. */
export type PageInfo = {
  page: number
  size: number
  total: number
  totalPages: number
  hasPrevious: boolean
  hasNext: boolean
  previousPage: number
  nextPage: number
  single: boolean
  empty: boolean
}

export type RepoHeader = {
  owner: string
  repo: Repo
  starCount: number
  watcherCount: number
  starred: boolean
  watching: boolean
  openIssueCount: number
  openPrCount: number
  forkedFrom: string | null
}
