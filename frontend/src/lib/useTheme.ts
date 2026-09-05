import {useCallback, useSyncExternalStore} from 'react'

export const THEMES = ['auto', 'light', 'dark', 'dark_dimmed'] as const
export type ThemeName = (typeof THEMES)[number]

const KEY = 'gitkoo-theme'
const listeners = new Set<() => void>()

function read(): ThemeName {
  try {
    const stored = localStorage.getItem(KEY)
    if (stored && (THEMES as readonly string[]).includes(stored)) {
      return stored as ThemeName
    }
  } catch {
    // Private browsing throws on access rather than returning null.
  }
  return 'auto'
}

/**
 * Applies the preference to the document element.
 *
 * Primer only defines tokens for a light mode paired with a light theme and a dark
 * mode paired with a dark one. Naming a dark theme under light mode matches no rule
 * at all, so every token falls back to nothing and the page renders as black text on
 * a transparent background - which is what this app shipped until it was measured.
 * The mode is therefore derived from the theme's family, never toggled separately.
 */
function apply(theme: ThemeName) {
  const el = document.documentElement
  const dark = theme.startsWith('dark')
  el.setAttribute('data-color-mode', theme === 'auto' ? 'auto' : dark ? 'dark' : 'light')
  el.setAttribute('data-light-theme', dark ? 'light' : theme === 'auto' ? 'light' : theme)
  el.setAttribute('data-dark-theme', theme === 'auto' ? 'dark' : dark ? theme : 'dark')
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function setTheme(theme: ThemeName) {
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    // Not being able to remember it is not a reason to refuse to apply it.
  }
  apply(theme)
  for (const listener of listeners) listener()
}

/** The current preference, plus the three values Primer's ThemeProvider wants. */
export function useTheme() {
  const theme = useSyncExternalStore(subscribe, read, () => 'auto' as ThemeName)

  const cycle = useCallback(() => {
    setTheme(THEMES[(THEMES.indexOf(theme) + 1) % THEMES.length])
  }, [theme])

  const dark = theme.startsWith('dark')
  return {
    theme,
    cycle,
    colorMode: theme === 'auto' ? ('auto' as const) : dark ? ('night' as const) : ('day' as const),
    // Primer's scheme names match the theme names exactly: light, dark, dark_dimmed.
    dayScheme: dark || theme === 'auto' ? 'light' : theme,
    nightScheme: dark ? theme : 'dark',
  }
}
