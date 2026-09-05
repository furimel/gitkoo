/**
 * The GitKoo mark.
 *
 * Drawn here rather than taken from Octicons: `MarkGithubIcon` is GitHub's
 * trademark, and shipping another forge under it would be passing this off as
 * GitHub. Primer's components are MIT-licensed and free to use; the logo is not
 * part of that.
 *
 * The shape is a commit graph - a trunk with a branch leaving and merging back -
 * which is what the product is about, and is a form nobody owns.
 */
export function Logo({size = 32, className}: {size?: number; className?: string}) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      className={className}
      role="img"
      aria-label="GitKoo"
    >
      {/* The trunk. */}
      <path
        d="M6 3.5v17"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      {/* A branch out of the trunk and back into it. */}
      <path
        d="M6 8.5h6a4 4 0 0 1 4 4v0a4 4 0 0 1-4 4H6"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Commits: the two ends of the trunk and the tip of the branch. */}
      <circle cx="6" cy="3.5" r="2.25" fill="currentColor" />
      <circle cx="6" cy="20.5" r="2.25" fill="currentColor" />
      <circle cx="19" cy="12.5" r="2.25" fill="currentColor" />
    </svg>
  )
}
