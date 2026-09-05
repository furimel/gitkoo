import {NavList} from '@primer/react'
import {GearIcon, HistoryIcon, PeopleIcon, ShieldIcon} from '@primer/octicons-react'

type Section = 'overview' | 'users' | 'system' | 'audit'

const ITEMS = [
  {key: 'overview', href: '/admin', label: 'Overview', icon: ShieldIcon},
  {key: 'users', href: '/admin/users', label: 'Users', icon: PeopleIcon},
  {key: 'system', href: '/admin/system', label: 'System', icon: GearIcon},
  {key: 'audit', href: '/admin/audit', label: 'Audit log', icon: HistoryIcon},
] as const

/**
 * Site administration navigation.
 *
 * Three loose buttons on the overview page and nothing at all on the other three is
 * not navigation; this is the same rail on all four.
 */
export function AdminNav({active}: {active: Section}) {
  return (
    <NavList aria-label="Administration">
      {ITEMS.map(item => (
        <NavList.Item key={item.key} href={item.href} aria-current={item.key === active ? 'page' : undefined}>
          <NavList.LeadingVisual>
            <item.icon />
          </NavList.LeadingVisual>
          {item.label}
        </NavList.Item>
      ))}
    </NavList>
  )
}
