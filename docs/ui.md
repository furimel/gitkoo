# UI Design

GitHub Primer-inspired design system.

## Design tokens

CSS custom properties in `gitkoo.css`:

- Colors: `--canvas-default`, `--canvas-subtle`, `--border-default`,
  `--fg-default`, `--fg-muted`, `--accent-fg`, `--success-fg`,
  `--danger-fg`, etc.
- Light theme in `:root`, dark theme in `[data-theme="dark"]`.
- Spacing scale: `--sp-0` through `--sp-7` (0 to 3rem).
- Radius: `--radius-sm` (4px), `--radius-md` (6px), `--radius-lg` (12px).
- Typography: system font stack + monospace stack.

## Classes

Three layers:

1. **Utility classes** (Tailwind-like syntax, hand-written): `.flex`,
   `.items-center`, `.gap-2`, `.mt-3`, `.px-4`, `.text-sm`, `.text-muted`,
   `.border`, `.rounded`, `.bg-subtle`, etc.
2. **Component classes**: `.app-header`, `.btn`, `.btn-primary`, `.input`,
   `.field`, `.alert`, `.card`, `.repo-header`, `.repo-tabs`, `.tree`,
   `.clone-box`, `.commit-item`, `.auth-card`, `.theme-toggle`,
   `.code-view`, `.empty-state`.
3. **Base reset** and element defaults.

## Dark mode

Toggled client-side via `gitkoo.js`. Persisted to `localStorage` under
`gitkoo-theme`. Respects `prefers-color-scheme` on first visit. Button in
the app header.

## Static assets

Served under `/assets/**` (via `WebMvcConfig`). This avoids route conflicts
with the `/{username}/{name}` repository URL pattern. No Node, no npm, no
build step. All CSS and JS are committed directly.
