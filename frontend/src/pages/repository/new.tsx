import {useForm} from '@inertiajs/react'
import {Flash, FormControl, TextInput} from '@primer/react'
import type {FormEvent} from 'react'

import {FormCard} from '@/components/FormCard'
import {Page, Subhead} from '@/components/Page'
import {VisibilityChoice} from '@/components/VisibilityChoice'

type Props = {error?: string; name?: string; description?: string}

export default function NewRepository({error, name, description}: Props) {
  const {data, setData, post, processing} = useForm({
    name: name ?? '',
    description: description ?? '',
    visibility: 'PUBLIC',
    defaultBranch: 'main',
  })

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    post('/new')
  }

  return (
    <Page width="form">
      <Subhead
        title="Create a new repository"
        description="A repository contains all project files, including the revision history."
      />

      {error && <Flash variant="danger">{error}</Flash>}

      <FormCard onSubmit={submit} submitLabel="Create repository" submitting={processing}>
        <FormControl required>
          <FormControl.Label>Repository name</FormControl.Label>
          <TextInput
            block
            autoFocus
            placeholder="my-project"
            value={data.name}
            onChange={e => setData('name', e.target.value)}
          />
          <FormControl.Caption>Great repository names are short and memorable.</FormControl.Caption>
        </FormControl>

        <FormControl>
          <FormControl.Label>Description</FormControl.Label>
          <TextInput
            block
            placeholder="A short description of this repository"
            value={data.description}
            onChange={e => setData('description', e.target.value)}
          />
        </FormControl>

        <VisibilityChoice value={data.visibility} onChange={v => setData('visibility', v)} />

        <FormControl>
          <FormControl.Label>Default branch</FormControl.Label>
          <TextInput
            value={data.defaultBranch}
            onChange={e => setData('defaultBranch', e.target.value)}
          />
        </FormControl>
      </FormCard>
    </Page>
  )
}
