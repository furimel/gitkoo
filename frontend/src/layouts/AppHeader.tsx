import {Link, router, usePage} from '@inertiajs/react'
import {ActionList, ActionMenu, Avatar, IconButton, Text, TextInput} from '@primer/react'
import {
  BellIcon,
  MarkGithubIcon,
  MoonIcon,
  PlusIcon,
  SearchIcon,
  SunIcon,
} from '@primer/octicons-react'
import type {FormEvent} from 'react'

import type {SharedPageProps} from '@/lib/types'
import {useTheme} from '@/lib/useTheme'

/**
 * The dark bar across the top of every signed-in page.
 *
 * Everything here is a real control: the search box submits, the create menu links
 * to routes that exist, and the bell shows the unread count the server sent. The
 * previous version had a notification dot that was styled but never wired.
 */
export function AppHeader() {
  const {auth} = usePage<SharedPageProps>().props
  const {theme, cycle} = useTheme()

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const q = new FormData(event.currentTarget).get('q')
    router.get('/search', {q: String(q ?? '')}, {preserveState: false})
  }

  return (
    <header className="app-header">
      <div className="app-header-inner page page--wide">
        <Link href="/" className="app-header-brand" aria-label="GitKoo home">
          <MarkGithubIcon size={32} />
          <Text weight="semibold" size="large">
            GitKoo
          </Text>
        </Link>

        <form className="app-header-search" onSubmit={search} role="search">
          <TextInput
            name="q"
            defaultValue={new URLSearchParams(window.location.search).get('q') ?? ''}
            leadingVisual={SearchIcon}
            placeholder="Search or jump to..."
            aria-label="Search repositories"
            size="small"
            block
          />
        </form>

        <div className="app-header-actions">
          <IconButton
            icon={theme.startsWith('dark') ? MoonIcon : SunIcon}
            aria-label={`Theme: ${theme.replace('_', ' ')}. Click to change.`}
            variant="invisible"
            onClick={cycle}
          />

          {auth.user ? (
            <>
              <ActionMenu>
                <ActionMenu.Anchor>
                  <IconButton icon={PlusIcon} aria-label="Create new" variant="invisible" />
                </ActionMenu.Anchor>
                <ActionMenu.Overlay align="end">
                  <ActionList>
                    <ActionList.LinkItem href="/new">New repository</ActionList.LinkItem>
                    <ActionList.LinkItem href="/teams/new">New team</ActionList.LinkItem>
                  </ActionList>
                </ActionMenu.Overlay>
              </ActionMenu>

              <Link href="/notifications" className="app-header-bell" aria-label="Notifications">
                <BellIcon />
                {auth.user.unread > 0 && (
                  <span className="app-header-unread" aria-label={`${auth.user.unread} unread`} />
                )}
              </Link>

              <ActionMenu>
                <ActionMenu.Anchor>
                  <button type="button" className="app-header-avatar" aria-label="Your account">
                    <Avatar src={`/avatars/${auth.user.username}`} size={20} square={false} />
                  </button>
                </ActionMenu.Anchor>
                <ActionMenu.Overlay align="end">
                  <ActionList>
                    <ActionList.Item disabled>
                      Signed in as <Text weight="semibold">{auth.user.username}</Text>
                    </ActionList.Item>
                    <ActionList.Divider />
                    <ActionList.LinkItem href={`/@/${auth.user.username}`}>
                      Your profile
                    </ActionList.LinkItem>
                    <ActionList.LinkItem href="/settings/keys">SSH keys</ActionList.LinkItem>
                    {auth.user.admin && (
                      <ActionList.LinkItem href="/admin">Site administration</ActionList.LinkItem>
                    )}
                    <ActionList.Divider />
                    <ActionList.Item
                      variant="danger"
                      onSelect={() => router.post('/logout')}
                    >
                      Sign out
                    </ActionList.Item>
                  </ActionList>
                </ActionMenu.Overlay>
              </ActionMenu>
            </>
          ) : (
            <Link href="/login" className="app-header-signin">
              Sign in
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
