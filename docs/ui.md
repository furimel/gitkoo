# UI

The client is React 19 with [Primer React](https://primer.style/react), GitHub's own
design system, rendered through [Inertia](https://inertiajs.com). It lives in
`frontend/` and is compiled into the jar by Gradle.

## How a page reaches the screen

A Spring controller returns a view name and a model, exactly as it always did:

```java
model.addAttribute("repos", repos);
return "dashboard";
```

`InertiaViewResolver` turns that into a page object - `{component, props, url,
version}` - and `main.tsx` loads `src/pages/dashboard.tsx` to render it. The view
name *is* the component path, so there is no routing table on the client to fall out
of step with the server.

A first visit gets an HTML shell with the page object in a JSON script tag. Every
navigation after that is the same page object as JSON, and the client swaps the
component in place.

**Why not a REST API.** Every authorization decision in this product lives in a
controller: `requireRead`, `requireWrite`, the visibility filter in search, the
anonymous-read rule for public repositories. An API would have needed a second copy
of all of it in the browser. Three authorization holes have already shipped here;
duplicating the rules is how a fourth arrives.

## Working on it

```
cd frontend
npm install
npm run dev        # rebuilds on change into build/frontend
```

Run the server in another terminal with `./gradlew bootRun`, and reload the browser.
There is no dev server and no hot reload: the Java side serves the compiled bundle,
and `npm run dev` is Vite in watch mode writing into the place the jar reads from.

`./gradlew bootJar` runs the whole thing - `npm ci`, `vite build`, then copies the
output into the jar. Node is needed to *build*; the jar that comes out needs only a
JVM and `git`.

## Structure

```
frontend/src
  main.tsx              entry: resolves a component name to a page
  pages/                one file per controller view name
  layouts/              Shell (header + footer), BareShell (auth pages)
  components/           shared pieces
  lib/                  types mirroring the server records, theme, formatting
  styles/app.css        the app's own layer on top of Primer
```

Imports use the `@/` alias, never `../..`. It is configured in both `vite.config.ts`
and `tsconfig.json`, so a file reads the same wherever it sits.

## Conventions

**Page widths belong to `<Page>`.** A page picks `wide`, `form`, `narrow` or `auth`
and never writes a container class. The previous markup repeated one container chain
across twenty-four files, and they drifted apart.

**A card is a card.** `.card` is for a group that needs a header bar, a footer bar,
or a background different from the page. Everything else is `<Rows>`: rows separated
by hairlines, no outer border. A page never opens with a card - it opens with a
`<Subhead>`.

**An empty list is not rendered.** `<Empty>` is the whole empty case, never nested
inside a card, and never shown beside a list that has rows.

**Primer 38 has no `sx` prop.** It moved to CSS Modules. Layout goes through
`<Stack>` and anything else through a class. The utility layer in `app.css` is nine
rules with a `u-` prefix, and it is meant to stay that size.

## Two traps worth knowing

**Themes.** Primer only defines tokens for a light mode paired with a light theme, or
a dark mode paired with a dark one. Naming a dark theme under light mode matches no
rule at all, so every token falls back to nothing: transparent background, black
text, a page that looks broken rather than differently themed. `useTheme` derives the
mode from the theme's family for exactly this reason, and the server's shell script
does the same before first paint.

**The whitelabel error view.** Spring registers its fallback error page as a `View`
bean named, literally, `error` - and `BeanNameViewResolver` runs ahead of the Inertia
resolver, so it wins the view name our own error page uses. It is switched off with
`spring.web.error.whitelabel.enabled: false`. That key moved in Boot 4; setting the
old `server.error.whitelabel` path does nothing, and looks exactly like the setting
being ignored.

## Guard rails

**`PageRenderTest`** requests every route with the `X-Inertia` header and asserts
three things about the page object, each of which has caught a real bug:

1. the named component exists as a file under `frontend/src/pages` - a controller
   renamed without its counterpart is otherwise a blank screen at runtime;
2. every prop serialises - one unserialisable object fails the whole response;
3. no password hash, token hash or filesystem path appears in the JSON.

The third matters more than it used to. A template chose its fields one at a time;
props are the *whole model*, so a getter that was merely unused is now shipped to the
browser. `User.passwordHash`, `AccessToken.tokenHash` and `Repository.storagePath`
carry `@JsonIgnore` on both the field and the getter - Jackson merges the two into
one logical property, so annotating only one still suppresses it and makes the test
look like it is guarding something it is not. Verified by removing both annotations:
seven routes failed.

**`tsc --noEmit`** runs as part of `npm run build`, so a type error fails
`./gradlew bootJar`. This is the replacement for the class of bug the rewrite was
done to kill: a fragment given the wrong arguments used to render half a page at
runtime, and is now a compile error.

### What tests cannot check

Whether React actually mounted, and whether the result has any visible text. A blank
page returns 200 with correct props.

Checked by hand in headless Chrome across all thirty routes at 390px and 1440px,
watching for an empty root element, console errors, horizontal overflow, and a footer
floating above the bottom of a short page. That pass found the two worst bugs in this
rewrite: the page object written as a data attribute, which Inertia 3 does not read,
so nothing mounted anywhere; and a `.blob-table td` padding reset that outranked
`.blob-num` and left every line number touching its line of code.
