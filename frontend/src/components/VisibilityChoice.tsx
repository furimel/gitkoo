import {FormControl, Radio, RadioGroup, Stack, Text} from '@primer/react'
import {LockIcon, RepoIcon} from '@primer/octicons-react'

/**
 * Public / Private.
 *
 * Written out twice in the templates this replaces, which is exactly how the two
 * copies came to describe the same choice in different words.
 */
export function VisibilityChoice({
  value,
  onChange,
}: {
  value: string
  onChange: (value: string) => void
}) {
  return (
    <RadioGroup name="visibility" aria-label="Visibility">
      <RadioGroup.Label>Visibility</RadioGroup.Label>
      <FormControl>
        <Radio
          value="PUBLIC"
          checked={value === 'PUBLIC'}
          onChange={event => onChange(event.target.value)}
        />
        <FormControl.Label>
          <Stack direction="horizontal" align="center" gap="condensed">
            <RepoIcon className="u-muted" />
            <Text>Public</Text>
          </Stack>
        </FormControl.Label>
        <FormControl.Caption>Anyone on this server can see this repository.</FormControl.Caption>
      </FormControl>
      <FormControl>
        <Radio
          value="PRIVATE"
          checked={value === 'PRIVATE'}
          onChange={event => onChange(event.target.value)}
        />
        <FormControl.Label>
          <Stack direction="horizontal" align="center" gap="condensed">
            <LockIcon className="u-muted" />
            <Text>Private</Text>
          </Stack>
        </FormControl.Label>
        <FormControl.Caption>
          You choose who can see and commit to this repository.
        </FormControl.Caption>
      </FormControl>
    </RadioGroup>
  )
}
