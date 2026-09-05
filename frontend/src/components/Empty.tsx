import {Blankslate} from '@primer/react/experimental'
import type {Icon} from '@primer/octicons-react'
import type {ReactNode} from 'react'

/**
 * The one empty state.
 *
 * There were three competing conventions before - bordered on the canvas, bare
 * inside a card, and a large variant - which is why an empty list looked different
 * on every screen. A blankslate is never inside a card: it *is* the container for
 * the empty case, and when a list has rows this does not render at all.
 *
 * `action` takes a whole node rather than a label and href, because the actions
 * differ: some are links, some submit a form.
 */
export function Empty({
  icon: IconComponent,
  title,
  children,
  action,
}: {
  icon: Icon
  title: string
  children?: ReactNode
  action?: ReactNode
}) {
  return (
    <Blankslate border>
      <Blankslate.Visual>
        <IconComponent size={24} />
      </Blankslate.Visual>
      <Blankslate.Heading>{title}</Blankslate.Heading>
      {children ? <Blankslate.Description>{children}</Blankslate.Description> : null}
      {action ? <div style={{marginTop: 16}}>{action}</div> : null}
    </Blankslate>
  )
}
