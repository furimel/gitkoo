import {BranchName, Stack} from '@primer/react'
import {ArrowRightIcon} from '@primer/octicons-react'

import {Diff, DiffSummary} from '@/components/Diff'
import {Page, Section, Subhead} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import type {DiffFile, RepoHeader} from '@/lib/types'

type Props = RepoHeader & {
  base: string
  head: string
  diffFiles: DiffFile[]
  additions: number
  deletions: number
}

/** Compare two refs. */
export default function Compare(props: Props) {
  return (
    <>
      <RepoHead tab="code" header={props} />
      <Page>
        <Subhead
          title={
            <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
              <span>Comparing</span>
              <BranchName href="#">{props.base}</BranchName>
              <ArrowRightIcon className="u-muted" />
              <BranchName href="#">{props.head}</BranchName>
            </Stack>
          }
        />

        <Section>
          <DiffSummary
            files={props.diffFiles.length}
            additions={props.additions}
            deletions={props.deletions}
            verb="Showing"
          />
        </Section>

        <Diff files={props.diffFiles} />
      </Page>
    </>
  )
}
