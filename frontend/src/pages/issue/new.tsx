import {useForm} from '@inertiajs/react'
import {FormControl, TextInput, Textarea} from '@primer/react'
import type {FormEvent} from 'react'

import {FormCard} from '@/components/FormCard'
import {Page, Section} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import {WithSidebar} from '@/components/Sidebar'
import type {RepoHeader} from '@/lib/types'

type Props = RepoHeader

export default function NewIssue(props: Props) {
  const {owner, repo} = props
  const {data, setData, post, processing} = useForm({title: '', body: ''})

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    post(`/${owner}/${repo.name}/issues/new`)
  }

  return (
    <>
      <RepoHead tab="issues" header={props} />
      <Page>
        <WithSidebar
          main={
            <FormCard onSubmit={submit} submitLabel="Submit new issue" submitting={processing}>
              <FormControl required>
                <FormControl.Label visuallyHidden>Title</FormControl.Label>
                <TextInput
                  block
                  autoFocus
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
                  rows={12}
                  placeholder="Leave a comment (Markdown supported)"
                  value={data.body}
                  onChange={e => setData('body', e.target.value)}
                />
              </FormControl>
            </FormCard>
          }
          aside={
            <Section title="Writing tips">
              <ul className="tips-list u-muted">
                <li>Markdown is supported.</li>
                <li>
                  Reference other issues with <code>#123</code>.
                </li>
                <li>
                  Use <code>- [ ]</code> for task lists.
                </li>
              </ul>
            </Section>
          }
        />
      </Page>
    </>
  )
}
