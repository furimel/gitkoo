import {router} from '@inertiajs/react'
import {useState} from 'react'
import {ActionList, ActionMenu, Button, TextInput} from '@primer/react'
import {GitBranchIcon, SearchIcon} from '@primer/octicons-react'

/**
 * Switch branches.
 *
 * The filter is real. A plain menu stops being usable at about twenty branches, and
 * the previous version had no way to search at all - which is the kind of control
 * that looks finished and is not.
 */
export function BranchPicker({
  branches,
  current,
  hrefPrefix,
}: {
  branches: string[]
  current: string
  hrefPrefix: string
}) {
  const [filter, setFilter] = useState('')
  const needle = filter.trim().toLowerCase()
  const shown = needle ? branches.filter(b => b.toLowerCase().includes(needle)) : branches

  return (
    <ActionMenu onOpenChange={open => !open && setFilter('')}>
      <ActionMenu.Anchor>
        <Button leadingVisual={GitBranchIcon} trailingAction={undefined}>
          {current}
        </Button>
      </ActionMenu.Anchor>
      <ActionMenu.Overlay width="medium">
        {branches.length > 8 ? (
          <div className="branch-filter">
            <TextInput
              block
              size="small"
              leadingVisual={SearchIcon}
              placeholder="Filter branches"
              aria-label="Filter branches"
              value={filter}
              onChange={e => setFilter(e.target.value)}
            />
          </div>
        ) : null}
        <ActionList selectionVariant="single">
          {shown.map(branch => (
            <ActionList.Item
              key={branch}
              selected={branch === current}
              onSelect={() => router.visit(`${hrefPrefix}${branch}`)}
            >
              {branch}
            </ActionList.Item>
          ))}
          {shown.length === 0 ? <ActionList.Item disabled>No branches matched</ActionList.Item> : null}
        </ActionList>
      </ActionMenu.Overlay>
    </ActionMenu>
  )
}
