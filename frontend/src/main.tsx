import {createInertiaApp} from '@inertiajs/react'
import {createRoot} from 'react-dom/client'
import {StrictMode} from 'react'

import '@primer/primitives/dist/css/primitives.css'
import '@primer/primitives/dist/css/functional/themes/light.css'
import '@primer/primitives/dist/css/functional/themes/dark.css'
import '@primer/primitives/dist/css/functional/themes/dark-dimmed.css'
import './styles/app.css'

import {Shell} from '@/layouts/Shell'
import type {PageComponent} from '@/lib/types'

/*
 * The server sends a component name that matches its own view name, so
 * `return "repository/code"` in a Spring controller loads
 * pages/repository/code.tsx here. Keeping the two names identical means there is no
 * routing table to fall out of sync - the controller decides what renders, exactly
 * as it did with server-side templates, and the client never has to duplicate a
 * permission rule to know which page a user is allowed to see.
 */
type PageModule = {default: PageComponent}

const pages = import.meta.glob<PageModule>('./pages/**/*.tsx')

createInertiaApp({
  progress: {color: 'var(--fgColor-accent)'},

  /*
   * Send every request body as form data, not JSON.
   *
   * The Spring controllers read their input with @RequestParam, which the servlet
   * container fills from a parsed form body - it does not parse JSON. Inertia
   * defaults to JSON, so without this every form in the application answers 400
   * with "Required request parameter 'username' is not present", and the page has
   * no way to explain itself.
   *
   * It goes in `defaults` rather than a `config.set` call before this one:
   * createInertiaApp starts with `config.replace(defaults)`, which discards
   * anything configured beforehand. That failure is silent - the setting is
   * present in the bundle, and simply gone by the time the first request is made.
   *
   * And here rather than at each call site, because there are more than thirty of
   * those and one forgotten is one broken form that nobody finds until a person
   * tries to use it.
   */
  defaults: {visitOptions: () => ({forceFormData: true})},

  resolve: async name => {
    const loader = pages[`./pages/${name}.tsx`]
    if (!loader) {
      throw new Error(`No page component for "${name}". Expected src/pages/${name}.tsx.`)
    }
    const page = (await loader()).default
    // Every page is wrapped in the app chrome unless it opts out - sign-in and
    // first-run setup have no header, because there is nothing to navigate to yet.
    page.layout ??= children => <Shell>{children}</Shell>
    return page
  },

  setup({el, App, props}) {
    createRoot(el).render(
      <StrictMode>
        <App {...props} />
      </StrictMode>,
    )
  },
})
