# Nova Brand Identity

## Logo concept

The mark is two overlapping N strokes: one at full weight in indigo
(`#5B6CFF`), one mirrored horizontally, offset, and layered at 78% opacity
in mint (`#9FE8D6`). The two diagonals cross through the center — that
crossing is the mark, not a decorative afterthought. It reads as "N" at
any size without leaning on a wallet, coin, or bank-cliché symbol.

## Files

| File | Use |
|---|---|
| `logo/nova-mark.svg` | Primary mark, square, two-color. App icon, favicon, square social profiles. |
| `logo/nova-mark-mono-light.svg` | Single N, white. Dark backgrounds, single-color contexts, status bar. |
| `logo/nova-mark-mono-dark.svg` | Single N, near-black. Light/print backgrounds only — the app itself is dark-only. |
| `logo/nova-logo-lockup.svg` | Mark + "Nova" wordmark. Anywhere wider-than-square is available. |

Android-native copies of the mark (`ic_launcher_foreground.xml`,
`ic_launcher_monochrome.xml`, `ic_notification.xml`) live under
`app/src/main/res/drawable/` — see their file-level comments for how each
one derives from the SVGs above.

## Clearspace

Minimum clearspace around the mark, on any side, is equal to the width of
one N stroke (14 units on the 108-unit grid the mark is drawn on). Nothing
— text, other UI, edge of frame — enters that margin.

## Minimum size

- Two-color mark: do not render below 24dp. Below that the mint pass loses
  legibility against the indigo.
- Mono mark: usable down to 16dp (status bar icon is the practical floor).

## Color

- Never recolor the mark outside the defined palette (indigo/mint on dark,
  or single-color mono).
- Never place the two-color mark on a light background — it was built
  against `#0B0B10` and the mint pass has no separation on light surfaces.
  Use `nova-mark-mono-dark.svg` there instead.

## Don't

- Don't separate the two N strokes further apart than the source files —
  the crossing is the mark; without it, it's just two unrelated N shapes.
- Don't add a bounding shape (circle, rounded square) behind the mark
  outside of the Android adaptive-icon background layer, which already
  handles that per-launcher.
- Don't italicize, outline, or add a drop shadow to the wordmark.
- Don't use the two-color mark as a notification or status-bar icon —
  use the mono version; the platform will flatten a two-color icon to a
  single opaque blob regardless.
