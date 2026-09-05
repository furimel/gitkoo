import {useForm, router} from '@inertiajs/react'
import {Button, Flash, FormControl, Text, TextInput, Textarea} from '@primer/react'
import {KeyIcon} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import {Empty} from '@/components/Empty'
import {FormCard} from '@/components/FormCard'
import {Page, Section, Subhead} from '@/components/Page'
import {Row, Rows} from '@/components/Rows'
import {TimeAgo} from '@/components/TimeAgo'

type SshKey = {
  id: number
  title: string | null
  fingerprint: string
  keyType: string | null
  createdAt: string | null
}

type Props = {keys: SshKey[]; error?: string; success?: string; title?: string; publicKey?: string}

export default function SshKeys({keys, error, success}: Props) {
  const {data, setData, post, processing, reset} = useForm({title: '', publicKey: ''})

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    post('/settings/keys', {onSuccess: () => reset()})
  }

  return (
    <Page width="form">
      <Subhead title="SSH keys" description="Public keys that may push and pull over SSH." />

      {error && <Flash variant="danger">{error}</Flash>}
      {success && <Flash variant="success">{success}</Flash>}

      <Section>
        {keys.length > 0 ? (
          <Rows>
            {keys.map(key => (
              <Row key={key.id} align="start">
                <KeyIcon className="u-muted" />
                <div className="u-flex-auto">
                  <Text weight="semibold">{key.title ?? '(untitled)'}</Text>
                  <Text as="div" size="small" className="u-muted u-mono">
                    {key.fingerprint}
                  </Text>
                  <Text as="div" size="small" className="u-muted">
                    {key.keyType ? `${key.keyType} · ` : ''}
                    Added <TimeAgo at={key.createdAt} />
                  </Text>
                </div>
                <Button
                  size="small"
                  variant="danger"
                  onClick={() => {
                    if (window.confirm('Delete this SSH key? Anything using it will lose access.')) {
                      router.post(`/settings/keys/${key.id}/delete`)
                    }
                  }}
                >
                  Delete
                </Button>
              </Row>
            ))}
          </Rows>
        ) : (
          <Empty icon={KeyIcon} title="No SSH keys yet">
            Add a public key below to push and pull over SSH.
          </Empty>
        )}
      </Section>

      <FormCard
        title="Add a new SSH key"
        onSubmit={submit}
        submitLabel="Add SSH key"
        submitting={processing}
      >
        <FormControl>
          <FormControl.Label>Title</FormControl.Label>
          <TextInput
            block
            placeholder="My laptop"
            value={data.title}
            onChange={e => setData('title', e.target.value)}
          />
        </FormControl>

        <FormControl required>
          <FormControl.Label>Key</FormControl.Label>
          <Textarea
            block
            rows={6}
            className="u-mono"
            placeholder="ssh-ed25519 AAAA..."
            value={data.publicKey}
            onChange={e => setData('publicKey', e.target.value)}
          />
          <FormControl.Caption>
            Paste the contents of your <code>~/.ssh/id_*.pub</code> file.
          </FormControl.Caption>
        </FormControl>
      </FormCard>
    </Page>
  )
}
