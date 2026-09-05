import {usePage} from '@inertiajs/react'
import {Button, Heading, Stack, Text} from '@primer/react'
import {AlertIcon, LockIcon, RepoIcon} from '@primer/octicons-react'

import {Page} from '@/components/Page'
import type {SharedPageProps} from '@/lib/types'

type Props = {status: number}

/**
 * One page for every error status.
 *
 * Only the number and the sentence differ. The server sends the status and nothing
 * else: an exception message like "Repository not found: minh/secret" would confirm
 * to a stranger that the repository exists.
 */
export default function ErrorPage({status}: Props) {
  const {auth} = usePage<SharedPageProps>().props
  const Icon = status === 404 ? RepoIcon : status === 403 ? LockIcon : AlertIcon

  return (
    <Page width="narrow">
      <Stack align="center" gap="normal" className="error-page">
        <Icon size={48} className="u-subtle" />
        <Heading as="h1" variant="large">
          {status}
        </Heading>
        <Text className="u-muted error-message">{message(status, Boolean(auth.user))}</Text>
        <Stack direction="horizontal" gap="condensed">
          <Button as="a" href="/" variant="primary">
            Go home
          </Button>
          {auth.user ? null : (
            <Button as="a" href="/login">
              Sign in
            </Button>
          )}
        </Stack>
      </Stack>
    </Page>
  )
}

function message(status: number, signedIn: boolean) {
  if (status === 404) return 'This is not the web page you are looking for.'
  if (status === 403) {
    return signedIn ? 'You do not have access to this.' : 'You may need to sign in to see this.'
  }
  return 'Something went wrong on the server.'
}
