import {useForm} from '@inertiajs/react'
import {Flash, FormControl, TextInput} from '@primer/react'
import type {FormEvent} from 'react'

import {FormCard} from '@/components/FormCard'
import {Page, Subhead} from '@/components/Page'

type Props = {error?: string}

export default function NewTeam({error}: Props) {
  const {data, setData, post, processing} = useForm({name: '', displayName: '', description: ''})

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    post('/teams/new')
  }

  return (
    <Page width="form">
      <Subhead
        title="Create a new team"
        description="Teams group people so you can grant repository access once."
      />

      {error && <Flash variant="danger">{error}</Flash>}

      <FormCard onSubmit={submit} submitLabel="Create team" submitting={processing}>
        <FormControl required>
          <FormControl.Label>Team name</FormControl.Label>
          <TextInput
            block
            autoFocus
            placeholder="e.g. platform"
            value={data.name}
            onChange={e => setData('name', e.target.value)}
          />
          <FormControl.Caption>Used in URLs. Lowercase, no spaces.</FormControl.Caption>
        </FormControl>

        <FormControl>
          <FormControl.Label>Display name</FormControl.Label>
          <TextInput
            block
            placeholder="Platform Team"
            value={data.displayName}
            onChange={e => setData('displayName', e.target.value)}
          />
        </FormControl>

        <FormControl>
          <FormControl.Label>Description</FormControl.Label>
          <TextInput
            block
            placeholder="What this team is responsible for"
            value={data.description}
            onChange={e => setData('description', e.target.value)}
          />
        </FormControl>
      </FormCard>
    </Page>
  )
}
