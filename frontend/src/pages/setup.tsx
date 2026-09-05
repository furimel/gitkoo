import {useForm} from '@inertiajs/react'
import {Button, Flash, FormControl, Heading, Stack, Text, TextInput} from '@primer/react'
import {MarkGithubIcon} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import {Page} from '@/components/Page'
import {BareShell} from '@/layouts/Shell'
import type {PageComponent} from '@/lib/types'

type Props = {error?: string; username?: string; email?: string}

/** First run: create the administrator account, and nothing else. */
const Setup: PageComponent = function Setup({error, username, email}: Props) {
  const {data, setData, post, processing} = useForm({
    username: username ?? '',
    email: email ?? '',
    password: '',
    confirmPassword: '',
  })

  function submit(event: FormEvent) {
    event.preventDefault()
    post('/setup')
  }

  return (
    <Page width="auth">
      <div className="auth-masthead">
        <MarkGithubIcon size={48} />
        <Heading as="h1" variant="medium">
          Welcome to GitKoo
        </Heading>
        <Text className="u-muted">
          Create the administrator account to finish setting up this instance.
        </Text>
      </div>

      {error && <Flash variant="danger">{error}</Flash>}

      <form onSubmit={submit} className="auth-card">
        <Stack gap="normal">
          <FormControl required>
            <FormControl.Label>Username</FormControl.Label>
            <TextInput
              block
              autoComplete="username"
              autoFocus
              placeholder="e.g. minh"
              value={data.username}
              onChange={e => setData('username', e.target.value)}
            />
          </FormControl>

          <FormControl required>
            <FormControl.Label>Email</FormControl.Label>
            <TextInput
              block
              type="email"
              autoComplete="email"
              value={data.email}
              onChange={e => setData('email', e.target.value)}
            />
          </FormControl>

          <FormControl required>
            <FormControl.Label>Password</FormControl.Label>
            <TextInput
              block
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={data.password}
              onChange={e => setData('password', e.target.value)}
            />
            <FormControl.Caption>Use at least 8 characters.</FormControl.Caption>
          </FormControl>

          <FormControl required>
            <FormControl.Label>Confirm password</FormControl.Label>
            <TextInput
              block
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={data.confirmPassword}
              onChange={e => setData('confirmPassword', e.target.value)}
            />
          </FormControl>

          <Button type="submit" variant="primary" block disabled={processing}>
            Create administrator account
          </Button>
        </Stack>
      </form>
    </Page>
  )
}

Setup.layout = children => <BareShell>{children}</BareShell>
export default Setup
