import {useEffect, useRef} from 'react'
import {Link as InertiaLink} from '@inertiajs/react'
import hljs from 'highlight.js/lib/common'
import {Breadcrumbs, Button, IconButton, Link, Stack, Text} from '@primer/react'
import {CopyIcon} from '@primer/octicons-react'

import {BranchPicker} from '@/components/BranchPicker'
import {Markdown} from '@/components/Markdown'
import {Page} from '@/components/Page'
import {RepoHead} from '@/components/RepoHead'
import type {RepoHeader} from '@/lib/types'

type Crumb = {name: string; path: string; last: boolean}

type Props = RepoHeader & {
  ref: string
  filePath: string
  fileName: string
  breadcrumbs: Crumb[]
  lines: string[]
  content: string
  byteSize: number
  language: string | null
  renderedHtml: string | null
  branches: string[]
}

/** One file: rendered when it is Markdown, source with a line gutter otherwise. */
export default function FileView(props: Props) {
  const {owner, repo, branches} = props
  const base = `/${owner}/${repo.name}`

  return (
    <>
      <RepoHead tab="code" header={props} />
      <Page>
        <Stack gap="normal">
          <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
            <BranchPicker branches={branches} current={props.ref} hrefPrefix={`${base}/tree/`} />
            <Breadcrumbs>
              <Breadcrumbs.Item as={InertiaLink} href={`${base}/tree/${props.ref}`}>
                {repo.name}
              </Breadcrumbs.Item>
              {/* Each crumb carries its own prefix, so parent directories are clickable. */}
              {props.breadcrumbs.map(crumb => (
                <Breadcrumbs.Item
                  key={crumb.path}
                  as={InertiaLink}
                  href={`${base}/tree/${props.ref}/${crumb.path}`}
                  selected={crumb.last}
                >
                  {crumb.name}
                </Breadcrumbs.Item>
              ))}
            </Breadcrumbs>
          </Stack>

          <div className="card">
            <div className="card-header">
              <Text size="small" className="u-muted">
                {props.lines.length} lines &middot; {props.byteSize} Bytes
              </Text>
              <span className="u-ml-auto">
                <Stack direction="horizontal" gap="condensed">
                  <Button
                    as="a"
                    size="small"
                    href={`${base}/raw/${props.ref}/${props.filePath}`}
                  >
                    Raw
                  </Button>
                  <IconButton
                    icon={CopyIcon}
                    size="small"
                    aria-label="Copy file contents"
                    onClick={() => navigator.clipboard?.writeText(props.content)}
                  />
                </Stack>
              </span>
            </div>

            {props.renderedHtml ? (
              <div className="markdown-pane">
                <Markdown html={props.renderedHtml} />
              </div>
            ) : (
              <Source lines={props.lines} language={props.language} />
            )}
          </div>
        </Stack>
      </Page>
    </>
  )
}

/**
 * Source is written as plain text and highlighted after mount, so the file is
 * readable with JavaScript disabled and the theme's own palette applies.
 */
function Source({lines, language}: {lines: string[]; language: string | null}) {
  const ref = useRef<HTMLTableSectionElement>(null)

  useEffect(() => {
    const body = ref.current
    if (!body || !language) return
    for (const cell of body.querySelectorAll<HTMLElement>('td.blob-code')) {
      const source = cell.textContent ?? ''
      if (!source) continue
      cell.innerHTML = hljs.highlight(source, {language, ignoreIllegals: true}).value
    }
  }, [lines, language])

  return (
    <div className="blob-wrapper">
      <table className="blob-table">
        <tbody ref={ref}>
          {lines.map((line, index) => (
            <tr key={index}>
              <td className="blob-num" id={`L${index + 1}`}>
                <Link href={`#L${index + 1}`} muted>
                  {index + 1}
                </Link>
              </td>
              <td className="blob-code">{line}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
