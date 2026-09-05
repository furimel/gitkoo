import {useEffect, useRef} from 'react'
import hljs from 'highlight.js/lib/common'
import {FileIcon} from '@primer/octicons-react'
import {CounterLabel, Heading, Stack, Text} from '@primer/react'

import type {DiffFile} from '@/lib/types'
import {Empty} from '@/components/Empty'

/** "+40 -12", in the two colours the whole app uses for added and removed. */
function Churn({additions, deletions}: {additions: number; deletions: number}) {
  return (
    <Text size="small">
      <Text weight="semibold" className="u-success">
        +{additions}
      </Text>{' '}
      <Text weight="semibold" className="u-danger">
        &minus;{deletions}
      </Text>
    </Text>
  )
}

/** "Showing 3 changed files  +40 -12" */
export function DiffSummary({
  files,
  additions,
  deletions,
  verb,
}: {
  files: number
  additions: number
  deletions: number
  verb: string
}) {
  return (
    <Heading as="h2" variant="small">
      <Stack direction="horizontal" align="center" gap="condensed" wrap="wrap">
        <FileIcon className="u-muted" />
        <span>{verb}</span>
        <CounterLabel>{files}</CounterLabel>
        <span>{files === 1 ? 'changed file' : 'changed files'}</span>
        <Churn additions={additions} deletions={deletions} />
      </Stack>
    </Heading>
  )
}

/**
 * A unified diff.
 *
 * This markup existed in three verbatim copies before, so a fix to one silently left
 * the other two behind. There is one copy now, and it is a component rather than a
 * template fragment, which means the compiler catches a caller passing the wrong
 * shape instead of the page rendering half-blank at runtime.
 */
export function Diff({files}: {files: DiffFile[]}) {
  if (files.length === 0) {
    return (
      <Empty icon={FileIcon} title="No changes">
        These refs are identical.
      </Empty>
    )
  }
  return (
    <>
      {files.map(file => (
        <DiffFileView key={file.path} file={file} />
      ))}
    </>
  )
}

function DiffFileView({file}: {file: DiffFile}) {
  const ref = useRef<HTMLTableSectionElement>(null)

  useEffect(() => {
    const body = ref.current
    if (!body || !file.language) return
    // Each line is highlighted on its own: a hunk is not valid source, so
    // highlighting the block as a whole produces nonsense.
    for (const cell of body.querySelectorAll<HTMLElement>('td.diff-code')) {
      const source = cell.textContent ?? ''
      if (!source) continue
      cell.innerHTML = hljs.highlight(source, {
        language: file.language,
        ignoreIllegals: true,
      }).value
    }
  }, [file])

  return (
    <div className="diff-file">
      <div className="diff-file-header">
        <FileIcon className="u-muted" />
        <Text weight="semibold">{file.path}</Text>
        <span className="u-ml-auto">
          <Churn additions={file.additions} deletions={file.deletions} />
        </span>
      </div>

      {file.binary ? (
        <Text as="p" className="u-muted diff-binary">
          Binary file not shown.
        </Text>
      ) : (
        <table className="diff-table">
          <tbody ref={ref}>
            {file.lines.map((line, index) => (
              <tr key={index} className={rowClass(line.hunk, line.addition, line.deletion)}>
                {line.hunk ? (
                  <td colSpan={3}>{line.content}</td>
                ) : (
                  <>
                    <td className="diff-num">{line.oldNumber ?? ''}</td>
                    <td className="diff-num">{line.newNumber ?? ''}</td>
                    <td className="diff-code">{line.content}</td>
                  </>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

function rowClass(hunk: boolean, addition: boolean, deletion: boolean) {
  if (hunk) return 'diff-line--hunk'
  if (addition) return 'diff-line--add'
  if (deletion) return 'diff-line--del'
  return undefined
}
