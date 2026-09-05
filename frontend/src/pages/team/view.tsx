import {Link as InertiaLink, useForm} from '@inertiajs/react'
import {Avatar, CounterLabel, FormControl, Label, Link, Select, Stack, TextInput} from '@primer/react'
import {PeopleIcon} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import {Empty} from '@/components/Empty'
import {FormCard} from '@/components/FormCard'
import {Page, Section, Subhead} from '@/components/Page'
import {Row, Rows} from '@/components/Rows'
import type {UserRecord} from '@/lib/types'

type Team = {id: number; name: string; displayName: string | null; description: string | null}
type Member = {id: number; userId: number; role: 'MEMBER' | 'MAINTAINER' | 'OWNER'}

type Props = {team: Team; members: Member[]; users: (UserRecord | null)[]}

export default function TeamView({team, members, users}: Props) {
  const {data, setData, post, processing, reset} = useForm({username: '', role: 'MEMBER'})

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    post(`/teams/${team.name}/members`, {onSuccess: () => reset()})
  }

  return (
    <Page width="form">
      <Subhead
        title={
          <Stack direction="horizontal" align="center" gap="condensed">
            <PeopleIcon className="u-muted" />
            <span>{team.displayName ?? team.name}</span>
          </Stack>
        }
        description={team.description ?? undefined}
      />

      <Section title="Members" actions={<CounterLabel>{members.length}</CounterLabel>}>
        {members.length > 0 ? (
          <Rows>
            {members.map((member, index) => {
              const user = users[index]
              return (
                <Row key={member.id}>
                  <Avatar src={`/avatars/${user?.username ?? 'ghost'}`} size={32} />
                  <Link
                    as={InertiaLink}
                    href={`/@/${user?.username ?? ''}`}
                    className="u-flex-auto repo-row-name"
                  >
                    {user?.displayName ?? user?.username ?? 'Unknown'}
                  </Link>
                  <Label variant={member.role === 'OWNER' ? 'accent' : 'secondary'}>
                    {member.role.toLowerCase()}
                  </Label>
                </Row>
              )
            })}
          </Rows>
        ) : (
          <Empty icon={PeopleIcon} title="No members yet">
            Add someone below to get started.
          </Empty>
        )}
      </Section>

      <FormCard title="Add a member" onSubmit={submit} submitLabel="Add member" submitting={processing}>
        <Stack direction="horizontal" gap="condensed" align="end" wrap="wrap">
          <FormControl required className="u-flex-auto">
            <FormControl.Label>Username</FormControl.Label>
            <TextInput
              block
              placeholder="username"
              value={data.username}
              onChange={e => setData('username', e.target.value)}
            />
          </FormControl>
          <FormControl>
            <FormControl.Label>Role</FormControl.Label>
            <Select value={data.role} onChange={e => setData('role', e.target.value)}>
              <Select.Option value="MEMBER">Member</Select.Option>
              <Select.Option value="MAINTAINER">Maintainer</Select.Option>
              <Select.Option value="OWNER">Owner</Select.Option>
            </Select>
          </FormControl>
        </Stack>
      </FormCard>
    </Page>
  )
}
