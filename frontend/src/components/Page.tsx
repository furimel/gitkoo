import type {ReactNode} from 'react'
import {Heading, Stack, Text} from '@primer/react'

type Width = 'wide' | 'form' | 'narrow' | 'auth'

/**
 * The page container.
 *
 * A page picks a width and hands over its body; it never writes a container class
 * itself. That rule is what keeps the left edge of every screen on the same line.
 */
export function Page({width = 'wide', children}: {width?: Width; children: ReactNode}) {
  return <main className={`page page--${width}`}>{children}</main>
}

/** Page heading with an optional description and right-aligned actions. */
export function Subhead({
  title,
  description,
  actions,
}: {
  title: ReactNode
  description?: ReactNode
  actions?: ReactNode
}) {
  return (
    <div className="section">
      <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
        <Heading as="h1" variant="large" className="u-flex-auto">
          {title}
        </Heading>
        {actions}
      </Stack>
      {description ? (
        <Text as="p" className="u-muted subhead-description">
          {description}
        </Text>
      ) : null}
    </div>
  )
}

/** A titled block in a sidebar rail. */
export function Section({
  title,
  actions,
  children,
}: {
  title?: ReactNode
  actions?: ReactNode
  children: ReactNode
}) {
  return (
    <div className="section">
      {title ? (
        <div className="section-title">
          <span>{title}</span>
          {actions ? <span className="section-title-actions">{actions}</span> : null}
        </div>
      ) : null}
      {children}
    </div>
  )
}
