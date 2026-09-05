import {useForm} from '@inertiajs/react'
import {Button, Flash, FormControl, Heading, Link, Stack, TextInput} from '@primer/react'
import type {FormEvent} from 'react'

import {Logo} from '@/components/Logo'
import {Page} from '@/components/Page'
import {BareShell} from '@/layouts/Shell'
import type {PageComponent} from '@/lib/types'

type Props = {success?: string}

/**
 * Sign in.
 *
 * Posts to the same /login endpoint Spring Security has always listened on, so the
 * session, the redirect back to the page the visitor asked for, and the failure
 * handling are unchanged - only the rendering moved.
 */
const Login: PageComponent = function Login({success}: Props) {
  const {data, setData, post, processing} = useForm({username: '', password: ''})
  const params = new URLSearchParams(window.location.search)

  function submit(event: FormEvent) {
    event.preventDefault()
    post('/login')
  }

  return (
    <Page width="auth">
      <div className="auth-masthead">
        <Logo size={48} />
        <Heading as="h1" variant="medium">
          Sign in to GitKoo
        </Heading>
      </div>

      {params.has('logout') && <Flash variant="success">You have been signed out.</Flash>}
      {params.has('error') && <Flash variant="danger">Invalid username or password.</Flash>}
      {success && <Flash>{success}</Flash>}

      <form onSubmit={submit} className="auth-card">
        <Stack gap="normal">
          <FormControl required>
            <FormControl.Label>Username</FormControl.Label>
            <TextInput
              block
              name="username"
              autoComplete="username"
              autoFocus
              value={data.username}
              onChange={e => setData('username', e.target.value)}
            />
          </FormControl>

          <FormControl required>
            <FormControl.Label>Password</FormControl.Label>
            <TextInput
              block
              type="password"
              name="password"
              autoComplete="current-password"
              value={data.password}
              onChange={e => setData('password', e.target.value)}
            />
          </FormControl>

          <Button type="submit" variant="primary" block disabled={processing}>
            Sign in
          </Button>
        </Stack>
      </form>

      <div className="auth-alt">
        New to GitKoo? <Link href="/register">Create an account</Link>
      </div>
    </Page>
  )
}

Login.layout = children => <BareShell>{children}</BareShell>
export default Login
