import {useForm} from '@inertiajs/react'
import {Button, Flash, FormControl, Select, Stack, Text, TextInput, Textarea} from '@primer/react'
import {ArrowRightIcon, GitMergeIcon} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import {Page, Subhead} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import type {RepoHeader} from '@/lib/types'

type Props = RepoHeader & {branches: string[]; error?: string}

/**
 * One card, not two. The branch pair is the card header - which is where GitHub puts
 * it - so the page no longer reads as two unrelated panels. The branches are a list
 * from the repository rather than two free-text inputs, which were a typo away from
 * an empty diff.
 */
export default function NewPullRequest(props: Props) {
  const {owner, repo, branches, error} = props
  const {data, setData, post, processing} = useForm({
    targetBranch: repo.defaultBranch,
    sourceBranch: branches.find(b => b !== repo.defaultBranch) ?? repo.defaultBranch,
    title: '',
    body: '',
  })

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    post(`/${owner}/${repo.name}/pulls/new`)
  }

  return (
    <>
      <RepoHead tab="pulls" header={props} />
      <Page width="form">
        <Subhead
          title="Open a pull request"
          description="Propose the commits on one branch for merging into another."
        />

        {error && <Flash variant="danger">{error}</Flash>}

        <form className="card" onSubmit={submit}>
          <div className="card-header">
            <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
              <GitMergeIcon className="u-muted" />
              <Text weight="semibold">base:</Text>
              <Select
                value={data.targetBranch}
                onChange={e => setData('targetBranch', e.target.value)}
                aria-label="Base branch"
              >
                {branches.map(branch => (
                  <Select.Option key={branch} value={branch}>
                    {branch}
                  </Select.Option>
                ))}
              </Select>
              <ArrowRightIcon className="u-muted" />
              <Text weight="semibold">compare:</Text>
              <Select
                value={data.sourceBranch}
                onChange={e => setData('sourceBranch', e.target.value)}
                aria-label="Compare branch"
              >
                {branches.map(branch => (
                  <Select.Option key={branch} value={branch}>
                    {branch}
                  </Select.Option>
                ))}
              </Select>
            </Stack>
          </div>

          <div className="card-body">
            <Stack gap="normal">
              <FormControl required>
                <FormControl.Label visuallyHidden>Title</FormControl.Label>
                <TextInput
                  block
                  size="large"
                  placeholder="Title"
                  value={data.title}
                  onChange={e => setData('title', e.target.value)}
                />
              </FormControl>
              <FormControl>
                <FormControl.Label visuallyHidden>Description</FormControl.Label>
                <Textarea
                  block
                  rows={10}
                  placeholder="Describe the change (Markdown supported)"
                  value={data.body}
                  onChange={e => setData('body', e.target.value)}
                />
              </FormControl>
            </Stack>
          </div>

          <div className="card-footer">
            <Button type="submit" variant="primary" disabled={processing}>
              Create pull request
            </Button>
          </div>
        </form>
      </Page>
    </>
  )
}
