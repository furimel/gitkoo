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

## Layout fragments

Defined in `templates/fragments/common.html`:

| Fragment | Purpose |
| --- | --- |
| `head(title)` | `<head>`: stylesheets, no-flash theme script, scripts |
| `header` | The dark top bar: search, create menu, notifications, theme, account |
| `repoHead(owner, repo, tab)` | Repository title and `UnderlineNav`; `tab` selects the active item |
| `footer` | Page footer, plus the icon sprite |

Every page is `head` -> `header` -> `<main class="container-xl">` -> `footer`.
Use `container-xl` (1280px) for app pages; Primer's `container-lg` is only 1012px.

## Keeping templates and CSS in sync

A class used in a template but defined nowhere fails silently - the original
stylesheet drifted this way, leaving `rounded-full` and `markdown-body` used on
every page and defined nowhere, so no badge was a pill and rendered Markdown had
no spacing at all. Guard against it:

```bash
tools/check-css-classes.sh
```

## Static assets

Served under `/assets/**` (via `WebMvcConfig`), which avoids route conflicts with
the `/{username}/{name}` repository URL pattern. No Node, no npm, no build step;
all CSS and JS are committed directly.
