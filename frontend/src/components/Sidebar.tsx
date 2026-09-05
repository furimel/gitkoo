import type {ReactNode} from 'react'
import {PageLayout} from '@primer/react'

/**
 * The two-column page.
 *
 * Primer's own layout, with its default sidebar width - which is GitHub's 296px.
 * The previous markup added a "wide" modifier that made every sidebar in the app
 * 40px fatter than the thing it was copying.
 */
export function WithSidebar({
  position = 'end',
  main,
  aside,
}: {
  position?: 'start' | 'end'
  main: ReactNode
  aside: ReactNode
}) {
  return (
    <PageLayout padding="none" columnGap="normal" rowGap="normal">
      <PageLayout.Content>{main}</PageLayout.Content>
      <PageLayout.Pane position={position} resizable={false}>
        {aside}
      </PageLayout.Pane>
    </PageLayout>
  )
}
