import type {ReactNode} from 'react'
import {ThemeProvider, BaseStyles} from '@primer/react'

import {AppHeader} from '@/layouts/AppHeader'
import {AppFooter} from '@/layouts/AppFooter'
import {useTheme} from '@/lib/useTheme'

/**
 * The frame around every page: header, content, footer, and a footer that stays at
 * the bottom of a short page rather than floating in the middle of one.
 *
 * Primer's ThemeProvider is given `preventSSRMismatch` deliberately off - the shell
 * HTML the server writes already sets the theme attributes before first paint, so
 * there is no flash to prevent and no mismatch to reconcile.
 */
export function Shell({children}: {children: ReactNode}) {
  const {colorMode, dayScheme, nightScheme} = useTheme()

  return (
    <ThemeProvider colorMode={colorMode} dayScheme={dayScheme} nightScheme={nightScheme}>
      <BaseStyles>
        <div className="app-shell">
          <AppHeader />
          <div className="app-main">{children}</div>
          <AppFooter />
        </div>
      </BaseStyles>
    </ThemeProvider>
  )
}

/** Sign-in, register and first-run setup: themed and styled, but no chrome. */
export function BareShell({children}: {children: ReactNode}) {
  const {colorMode, dayScheme, nightScheme} = useTheme()

  return (
    <ThemeProvider colorMode={colorMode} dayScheme={dayScheme} nightScheme={nightScheme}>
      <BaseStyles>
        <div className="app-shell">
          <div className="app-main">{children}</div>
          <AppFooter />
        </div>
      </BaseStyles>
    </ThemeProvider>
  )
}
