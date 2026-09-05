import {useForm} from '@inertiajs/react'
import {Button, Flash, FormControl, Heading, Link, Stack, TextInput} from '@primer/react'
import {MarkGithubIcon} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import {Page} from '@/components/Page'
import {BareShell} from '@/layouts/Shell'
import type {PageComponent} from '@/lib/types'

type Props = {error?: string; username?: string; email?: string}

const Register: PageComponent = function Register({error, username, email}: Props) {
  const {data, setData, post, processing} = useForm({
    username: username ?? '',
    email: email ?? '',
    password: '',
    confirmPassword: '',
  })

  function submit(event: FormEvent) {
    event.preventDefault()
    post('/register')
  }

  return (
    <Page width="auth">
      <div className="auth-masthead">
        <MarkGithubIcon size={48} />
        <Heading as="h1" variant="medium">
          Create your account
        </Heading>
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
            Create account
          </Button>
        </Stack>
      </form>

      <div className="auth-alt">
        Already have an account? <Link href="/login">Sign in</Link>
      </div>
    </Page>
  )
}

Register.layout = children => <BareShell>{children}</BareShell>
export default Register
