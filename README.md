# Sabah el Saj

A manakish cooking game for Android and iOS — open the bakery, beat the morning
rush, keep every manousheh out of the fire.

Zaatar and cheese to start, a saj that punishes bad timing, and an upgrade tree
that makes the same seven taps steadily easier.

- **Design spec:** [GAME_DESIGN.md](GAME_DESIGN.md) — order grammar, upgrade
  tree, tuning tables, build phases.
- **UI prototype:** a playable canvas of the five screens lives at
  <https://claude.ai/code/artifact/2ae1c617-d592-4513-bf2d-7aee4b42c011>.
  Its artboard sources are in [`design/`](design/).

The name is a working title.

## Stack

Kotlin Multiplatform + Compose Multiplatform. The game engine is pure Kotlin in
`commonMain` with no Compose imports, so the whole economy and every bake window
is unit-testable without an emulator.

Nothing is scaffolded yet — the design comes first.
