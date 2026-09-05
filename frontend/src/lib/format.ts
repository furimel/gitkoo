/**
 * Display formatting.
 *
 * On the client rather than the server, for two reasons: Jackson serialises a Java
 * record's components and nothing else, so a `sizeLabel()` helper would simply be
 * missing from the props and render as "undefined"; and formatting here uses the
 * reader's locale instead of whichever one the server happens to run under.
 */

/** "62.4%" */
export function percent(value: number) {
  return `${value.toFixed(1)}%`
}

/** "1.2 MB", the way a person reads a repository size. */
export function bytes(value: number) {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${Math.round(value / 1024)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}
