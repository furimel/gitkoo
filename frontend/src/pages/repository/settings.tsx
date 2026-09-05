import {useForm} from '@inertiajs/react'
import {Flash, FormControl, Label, Stack, TextInput} from '@primer/react'
import type {FormEvent} from 'react'

import {FormCard} from '@/components/FormCard'
import {Page, Subhead} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import {VisibilityChoice} from '@/components/VisibilityChoice'
import type {RepoHeader} from '@/lib/types'

type Props = RepoHeader & {success?: string; error?: string; topics?: string[]}

export default function RepositorySettings(props: Props) {
  const {owner, repo, success, error, topics = []} = props
  const base = `/${owner}/${repo.name}`

  const general = useForm({
    description: repo.description ?? '',
    defaultBranch: repo.defaultBranch,
    visibility: repo.visibility,
  })

  const topicForm = useForm({topics: topics.join(' ')})

  function saveGeneral(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    general.post(`${base}/settings`)
  }

  function saveTopics(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    topicForm.post(`${base}/settings/topics`)
  }

  return (
    <>
      <RepoHead tab="settings" header={props} />
      <Page width="form">
        {success && <Flash variant="success">{success}</Flash>}
        {error && <Flash variant="danger">{error}</Flash>}

        <Subhead title="General" />

        <Stack gap="normal">
          <FormCard
            onSubmit={saveGeneral}
            submitLabel="Save changes"
            submitting={general.processing}
          >
            <FormControl disabled>
              <FormControl.Label>Repository name</FormControl.Label>
              <TextInput block value={repo.name} readOnly />
              <FormControl.Caption>The repository name cannot be changed.</FormControl.Caption>
            </FormControl>

            <FormControl>
              <FormControl.Label>Description</FormControl.Label>
              <TextInput
                block
                placeholder="A short description"
                value={general.data.description}
                onChange={e => general.setData('description', e.target.value)}
              />
            </FormControl>

            <FormControl>
              <FormControl.Label>Default branch</FormControl.Label>
              <TextInput
                value={general.data.defaultBranch}
                onChange={e => general.setData('defaultBranch', e.target.value)}
              />
              <FormControl.Caption>
                The branch shown when someone opens this repository.
              </FormControl.Caption>
            </FormControl>

            <VisibilityChoice
              value={general.data.visibility}
              onChange={v => general.setData('visibility', v as typeof general.data.visibility)}
            />
          </FormCard>

          <FormCard
            title="Topics"
            onSubmit={saveTopics}
            submitLabel="Save topics"
            submitting={topicForm.processing}
          >
            <FormControl>
              <FormControl.Label>Topics</FormControl.Label>
              <TextInput
                block
                placeholder="java spring self-hosted"
                value={topicForm.data.topics}
                onChange={e => topicForm.setData('topics', e.target.value)}
              />
              <FormControl.Caption>
                Separated by spaces or commas. Lowercased, at most 20.
              </FormControl.Caption>
            </FormControl>

            {topics.length > 0 ? (
              <Stack direction="horizontal" gap="condensed" wrap="wrap">
                {topics.map(topic => (
                  <Label key={topic} variant="accent">
                    {topic}
                  </Label>
                ))}
              </Stack>
            ) : null}
          </FormCard>
        </Stack>
      </Page>
    </>
  )
}
