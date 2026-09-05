import {Link as InertiaLink} from '@inertiajs/react'
import {Label, Link, Stack, Text} from '@primer/react'
import {LockIcon, RepoIcon, SearchIcon} from '@primer/octicons-react'

import {Empty} from '@/components/Empty'
import {Page, Subhead} from '@/components/Page'
import {Row, Rows} from '@/components/Rows'

/** Mirrors SearchController.SearchResult. */
type Result = {
  name: string
  description: string | null
  owner: string | null
  visibility: string
}

type Props = {query: string | null; results: Result[]}

/**
 * Search results.
 *
 * The controller filters by visibility before this ever runs - an earlier version
 * ran a bare LIKE over the whole table, so any signed-in user could enumerate the
 * names of every private repository on the instance.
 */
export default function Search({query, results}: Props) {
  return (
    <Page>
      <Subhead
        title="Search"
        description={
          query
            ? `${results.length} ${results.length === 1 ? 'result' : 'results'} for "${query}"`
            : 'Type a query in the bar above.'
        }
      />

      {results.length > 0 ? (
        <Rows>
          {results.map(result => (
            <Row key={`${result.owner}/${result.name}`} align="start">
              {result.visibility === 'PRIVATE' ? (
                <LockIcon className="u-muted" />
              ) : (
                <RepoIcon className="u-muted" />
              )}
              <div className="u-flex-auto">
                <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
                  <Link
                    as={InertiaLink}
                    href={`/${result.owner}/${result.name}`}
                    className="repo-row-name"
                  >
                    {result.owner}/{result.name}
                  </Link>
                  <Label variant="secondary">{result.visibility.toLowerCase()}</Label>
                </Stack>
                {result.description ? (
                  <Text as="p" className="u-muted repo-row-description">
                    {result.description}
                  </Text>
                ) : null}
              </div>
            </Row>
          ))}
        </Rows>
      ) : query ? (
        <Empty icon={SearchIcon} title="No repositories matched">
          Try a different name, or check the spelling.
        </Empty>
      ) : null}
    </Page>
  )
}
