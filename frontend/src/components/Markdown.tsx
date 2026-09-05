import {useEffect, useRef} from 'react'
import hljs from 'highlight.js/lib/common'

/**
 * Server-rendered Markdown.
 *
 * The HTML comes from commonmark on the server, which is also where it is
 * sanitised - doing it here would mean trusting the browser to protect itself from
 * content the server already had a chance to clean.
 *
 * Fenced code blocks are highlighted after mount rather than on the server, so the
 * text is readable with JavaScript off and the theme's own syntax palette applies.
 */
export function Markdown({html}: {html: string}) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const root = ref.current
    if (!root) return
    for (const block of root.querySelectorAll('pre code')) {
      hljs.highlightElement(block as HTMLElement)
    }
  }, [html])

  return <div ref={ref} className="markdown-body" dangerouslySetInnerHTML={{__html: html}} />
}
