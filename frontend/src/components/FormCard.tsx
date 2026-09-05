import type {FormEvent, ReactNode} from 'react'
import {Button, Heading, Stack} from '@primer/react'

/**
 * A form as a card: optional header, body, and a footer holding the submit.
 *
 * There used to be two conventions for the same thing - some forms in a card and
 * some floating bare on the canvas - and the bare ones then needed a hardcoded
 * max-width to stop their fields spanning the whole page. One convention now.
 */
export function FormCard({
  title,
  onSubmit,
  submitLabel,
  submitting,
  children,
  footer,
}: {
  title?: ReactNode
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  submitLabel: string
  submitting?: boolean
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <form className="card" onSubmit={onSubmit}>
      {title ? (
        <div className="card-header">
          <Heading as="h2" variant="small">
            {title}
          </Heading>
        </div>
      ) : null}

      <div className="card-body">
        <Stack gap="normal">{children}</Stack>
      </div>

      <div className="card-footer">
        {footer}
        <Button type="submit" variant="primary" disabled={submitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}

/** The same card without a form, for grouping read-only detail. */
export function Card({title, children}: {title?: ReactNode; children: ReactNode}) {
  return (
    <div className="card">
      {title ? (
        <div className="card-header">
          <Heading as="h2" variant="small">
            {title}
          </Heading>
        </div>
      ) : null}
      <div className="card-body">{children}</div>
    </div>
  )
}
