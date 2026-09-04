# UI Design

GitKoo's interface is built on [Primer](https://primer.style), GitHub's own
design system, so the forge reads as familiar to anyone who has used GitHub.

## Stylesheets

Two files, loaded in this order, both committed as static assets:

| File | What it is |
| --- | --- |
| `static/css/primer.css` | `@primer/css` 21.5.1, vendored verbatim (MIT). The design system. |
| `static/css/gitkoo.css` | A small supplement for what Primer does not ship. |

Primer 21.5.1 is deliberate: it is the last release that still carries the full
component set GitHub's own UI uses. Version 22 dropped `Box-row`, `State`,
`UnderlineNav`, `TimelineItem`, `blankslate` and `Truncate` in favour of
Primer React, which is not an option here.

Never edit `primer.css`. To upgrade, re-download the same path from the CDN:

```bash
curl -sSL -o src/main/resources/static/css/primer.css \
  https://cdn.jsdelivr.net/npm/@primer/css@21.5.1/dist/primer.css
```

### What Primer provides

`Header`, `Layout` / `Layout-main` / `Layout-sidebar`, `Box` and its rows,
`btn` and variants, `State`, `Label`, `Counter`, `UnderlineNav`, `TimelineItem`,
`avatar`, `blankslate`, `Subhead`, `flash`, `SelectMenu`, `Popover`,
`details-overlay` dropdowns, `branch-name`, `markdown-body`, and the whole
utility layer (`d-flex`, `color-fg-muted`, `f4`, `mt-3`, `width-full`, ...).

### What `gitkoo.css` adds

Only the code-browser chrome Primer has no component for:

- `.file-navigation`, `.commit-tease`, `.tree-row*` - the repository file list
- `.blob-table`, `.blob-num`, `.blob-code` - file viewer with a line-number gutter
- `.diff-table`, `.diff-num`, `.diff-code`, `.diff-line--add|del|hunk` - unified diffs
- `.breadcrumb`, `.repo-head`, `.repo-title` - page chrome
- `.gap-1` .. `.gap-4` - Primer 21 spaces flex rows with margins and ships no gap utilities

Everything there is written against Primer's own custom properties
(`var(--fgColor-muted, ...)`, `var(--diffBlob-addition-bgColor-line, ...)`), so
it follows every theme automatically. **Never hardcode a hex value.**

## Icons

[Octicons](https://primer.style/octicons) v19 (MIT), inlined as an SVG sprite in
`templates/fragments/icons.html` and referenced with:

```html
<svg class="octicon" width="16" height="16" aria-hidden="true">
  <use href="#octicon-repo"></use>
</svg>
```

The sprite is emitted at the **end** of `<body>` by the `footer` fragment, not
the top. It is around 20 KB, and emitting it first pushed page content past the
response buffer before the first `<form>`, which made session creation fail with
*"Cannot create a session after the response has been committed"*.
`EagerCsrfTokenFilter` now resolves the CSRF token before rendering as well, so
page length can never break form rendering again.

## Theming

Primer's own mechanism, on the `<html>` element:

```html
<html data-color-mode="auto" data-light-theme="light" data-dark-theme="dark">
```

`gitkoo.js` cycles the stored preference through `auto -> light -> dark ->
dark_dimmed`, persisted to `localStorage` under `gitkoo-theme`. An inline script
in `<head>` applies it before first paint, so a dark-mode reader never sees a
white flash.

**The trap.** Primer only ships selectors that pair a mode with a theme of the
same family - `[data-color-mode=light][data-light-theme=light*]` and
`[data-color-mode=dark][data-dark-theme=dark*]`. Naming a dark theme under light
mode matches *no rule at all*, so every design token falls back to nothing: a
transparent body, black text on whatever the browser paints, and a page that
looks broken rather than differently themed. The bootstrap script therefore picks
the mode from the theme family, not from a light/dark toggle. Verified by reading
the computed `background-color` in a real browser in all four settings.

## Layout fragments

A template never names its own container. It hands its body to a shell, which
decides the width and the top gap - so the `container-xl px-3 px-md-4 px-lg-5
mt-4` chain that used to be copied into two dozen files cannot come back and
drift apart. `TemplateConventionsTest` fails the build if a page names a
container.

```html
<main th:replace="~{fragments/layout :: page('wide', ~{::content})}">
  <th:block th:fragment="content"> ... </th:block>
</main>
```

| Fragment file | What it owns |
| --- | --- |
| `layout.html` | `page(width, content)`, `withSidebar(position, main, aside)`, `repoHead`, `adminNav` |
| `ui.html` | `icon`, `subhead`, `emptyState`, `stateBadge`, `timeAgo`, `repoRow` |
| `form.html` | `field`, `password`, `textarea`, `radioOption`, `card`, `authCard` |
| `conversation.html` | `header`, `comment`, `event`, `commentForm`, `sidebarSection` |
| `diff.html` | `summary`, `files` |
| `repo.html` | `branchPicker`, `cloneMenu`, `repoActions`, `languageBar`, `contributors` |
| `common.html` | `head`, `header`, `footer`, `avatar`, `avatarProfile`, `pagination` |

Widths: `wide` (1280, app pages), `form` (768, settings and creation),
`narrow` (768), `auth` (544).

### Naming a fragment

**Never name a fragment after an HTML tag.** `~{::body}` selects by fragment
name *or* by tag name, so a fragment called `body` passed into another template
resolves to that template's own `<body>` - and Thymeleaf recurses until it hits
its 100-deep inclusion limit. The same applies to `nav`, `meta`, `header`,
`main`, `summary`. Use `adminBody`, `adminRail`, `metaRail`.

### `th:if` never guards `th:replace`

On one element, `th:replace` has precedence 100 and `th:if` has 300, so the
fragment is resolved before the condition is ever evaluated. An optional
fragment parameter needs the empty fragment instead:

```html
<th:block th:replace="${action} ?: ~{}"></th:block>
```

Likewise `th:each` (200) runs *after* `th:replace`, so a loop that renders a
fragment per item needs a `th:block` wrapper around the element that replaces.

## De-boxing

`.Box` is a card. Use it when a group needs a header bar, a footer bar, or a
background different from the page. Everything else is `.list`: rows separated by
hairlines, no outer border, no rounded corners.

- A page never *opens* with a `Box`; it opens with a `Subhead`.
- A `blankslate` is never inside a `Box` - an empty list is not rendered at all.
- Three sibling cards is a bug, and the build fails on it. Two is allowed,
  because two is what GitHub itself draws in the two places that survive here:
  the sign-in form above its "create an account" callout, and the file tree above
  the README.

## Guard rails

Four test classes hold the UI to its conventions. All run under `./gradlew
test`, which matters: CI runs only `test` and `bootJar`, so a shell script would
never have executed anywhere but a developer machine.

**`PageRenderTest`** renders every route and asserts the body closes with the
html end tag. Status alone is not enough - a Thymeleaf error thrown after the
response buffer flushes leaves the request at 200 with truncated HTML, so a sweep
over status codes reports success on a visibly broken page. It covers signed-in,
anonymous and empty-repository variants, because the empty branch of a list is
where a null model attribute hides.

**`TemplateConventionsTest`** enforces the structural rules, and all of them are
now absolute rather than ratchets:

1. every class used in a template is defined in a stylesheet
2. every decorative octicon carries `aria-hidden`
3. no literal `style="..."` (a `th:style` carrying `${...}` is fine - a label
   colour or a progress width genuinely comes from data)
4. no page names its own container
5. no three sibling `.Box` elements

The ratchets reached zero during the redesign. A ratchet budget must equal the
true count, never a round number above it: set loosely it silently permits new
debt, which is exactly what happened the first time these were written.

**`MinifiedAssetsTest`** compares the packaged stylesheet against the source and
fails if a selector went missing or the braces stopped balancing. The YUI
compressor is a regular expression, not a parser; if it ever meets syntax it
cannot follow, the source is right, the development server is right, and only the
jar is wrong. Probing it with CSS nesting, `@layer`, `color-mix()` and `calc()`
found none of them mangled by 2.4.8, so this guards against a regression rather
than working around a known break.

**`MarkdownServiceTest`** covers the GFM extensions.

### What tests cannot check

Spacing rhythm, optical alignment and overflow need a browser. Measured in
headless Chrome over CDP at 390/768/1024/1440 px across eleven pages:

- icon-to-text distance takes exactly three values - 4, 8 and 16 px - and all
  three are tokens on the scale, not accidents
- no element is vertically mis-centred against its label, apart from the list-row
  icons that are deliberately top-aligned against a two-line block, as GitHub's are
- `scrollWidth == clientWidth` at every width
- the footer sits flush to the bottom of a short page

Two bugs came out of that pass that no unit test would have found: a one-pixel
overflow from a button row that could not wrap, and the `State` pill's icon
sitting 1.5 px high because Primer renders the pill `inline-block`.

## Static assets

Served under `/assets/**` (via `WebMvcConfig`), which avoids route conflicts with
the `/{username}/{name}` repository URL pattern. No Node and no npm; the sources
are committed plain and readable.

`gitkoo.css` and `gitkoo.js` are minified into the jar by the `minifyAssets`
Gradle task, so no comment is ever served to a browser. `primer.css` is left
alone: response compression already takes it from 745 KB to 65 KB on the wire,
and running a 2013-era minifier over a modern design system to save a few more
KB is a bad trade.
