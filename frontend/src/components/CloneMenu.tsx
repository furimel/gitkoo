import {ActionMenu, Button, FormControl, TextInput} from '@primer/react'
import {CodeIcon, CopyIcon} from '@primer/octicons-react'

/** The green Code button and the two clone URLs behind it. */
export function CloneMenu({cloneUrl, sshCloneUrl}: {cloneUrl: string; sshCloneUrl?: string | null}) {
  return (
    <ActionMenu>
      <ActionMenu.Anchor>
        <Button variant="primary" leadingVisual={CodeIcon}>
          Code
        </Button>
      </ActionMenu.Anchor>
      <ActionMenu.Overlay width="medium">
        <div className="clone-menu">
          <CloneField label="HTTPS" value={cloneUrl} />
          {sshCloneUrl ? <CloneField label="SSH" value={sshCloneUrl} /> : null}
        </div>
      </ActionMenu.Overlay>
    </ActionMenu>
  )
}

export function CloneField({label, value}: {label: string; value: string}) {
  return (
    <FormControl>
      <FormControl.Label>{label}</FormControl.Label>
      <TextInput
        block
        readOnly
        size="small"
        className="u-mono"
        value={value}
        onFocus={event => event.currentTarget.select()}
        trailingAction={
          <TextInput.Action
            icon={CopyIcon}
            aria-label={`Copy the ${label} clone URL`}
            onClick={() => navigator.clipboard?.writeText(value)}
          />
        }
      />
    </FormControl>
  )
}
