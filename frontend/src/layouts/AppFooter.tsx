import {Link} from '@primer/react'

import {Logo} from '@/components/Logo'

/** Thin on purpose: two links and a mark, 16px of padding, not 48. */
export function AppFooter() {
  return (
    <footer className="app-footer">
      <div className="app-footer-inner page page--wide">
        <Logo size={20} className="color-fg-subtle" />
        <span>GitKoo &mdash; self-hosted Git forge</span>
        <span className="app-footer-links">
          <Link href="/health" muted>
            Status
          </Link>
          <Link href="https://github.com/furimeo/gitkoo" muted rel="noopener">
            Source
          </Link>
        </span>
      </div>
    </footer>
  )
}
