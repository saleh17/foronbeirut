# Manakich Beirut

A manakish cooking game for Android — open the bakery, beat the morning rush,
keep every manousheh out of the fire.

Zaatar and cheese to start, a furn that punishes bad timing, and an upgrade tree
that makes the same seven taps steadily easier.

- **Design spec:** [GAME_DESIGN.md](GAME_DESIGN.md) — order grammar, upgrade
  tree, tuning tables, build phases.
- **Art brief:** [ART_BRIEF.md](ART_BRIEF.md) — canvas, palette, scene map,
  ingredients, the twelve regulars, animation timings.
- **UI prototype:** a playable canvas of the five screens lives at
  <https://claude.ai/code/artifact/2ae1c617-d592-4513-bf2d-7aee4b42c011>.
  Its artboard sources are in [`design/`](design/).

مناقيش بيروت.

## Where it is

**Phases 1 to 3 are in.** A ninety-second day, a queue of three with a heart
each, coins that land on the counter and are gone in six seconds, and now the
depth: **jibneh alongside zaatar**, the **peel carrying three under one shared
clock**, and **khodra as add-ons**.

Four rules in there are the game, rather than features on it:

- **One clock, two windows.** Zaatar wants 6.0s, jibneh 7.5s, and their perfect
  windows never overlap — there is a test that asserts exactly that. A whole peel
  bakes on a single timer, so a mixed load has no right moment: pull for the
  zaatar and the jibneh is raw, wait for the jibneh and the zaatar has gone dark.
  Loading the peel is the decision; the furn just charges you for it.
- **Difficulty is a longer order, not a faster clock.** Khodra pays +2 a piece
  and each one you were asked for and missed takes 22% off the lot. Khodra
  nobody asked for is free and harmless.
- **Collecting is a move you spend.** Serving drops coins on the counter; picking
  them up is a separate action. Earned and collected are reported apart.
- **Speed is a tip, not the wage.** And the bake puts a ceiling on it — nobody
  can be served faster than their manousheh bakes, so the top coin is unreachable
  by construction.

The tap grammar on the counter: **pick one up** (tap it on the bench), **dress
it** (tap khodra), **hand it over** (tap the customer).

Still no upgrades, no fatayer, no mini pizza. That is phase 4.

## Layout

```
engine/   the game, as pure Kotlin — no Android, no Compose, no coroutines
app/      the Android app: Compose UI over the engine, nothing more
design/   the .dc.html artboards behind the published canvas
```

`engine` is the important boundary. It exposes one function —

```kotlin
step(state, params, dt, actions) -> state
```

— and holds every rule: what can follow what, when a manousheh is perfect, what
a customer will accept, what it pays. It has no clock of its own; the caller says
how much time passed. That is what makes the whole game testable in milliseconds,
and it is why `GameParams` is a separate object: upgrades will compile into a
snapshot of it without the engine ever learning that upgrades exist.

`app` owns the frame clock, the taps and the pixels, and is allowed to know
nothing else.

## Building

Android Studio (Ladybug or newer), or the command line with `ANDROID_HOME` set:

```bash
./gradlew :app:installDebug     # onto a connected device or emulator
./gradlew :engine:test          # the rules, no Android toolchain needed
```

The engine tests need only a JDK — no SDK, no emulator, about ten seconds:

```bash
gradle :engine:test --configure-on-demand
```

There is deliberately no root `build.gradle.kts` declaring plugins with
`apply false`; that would drag the Android Gradle Plugin into every build,
including engine-only ones. Each module declares its own plugins from
`gradle/libs.versions.toml`.

## Next

Phase 4 is the economy: the upgrade tree, save and load, and day progression —
the point where the coins the player has been collecting start buying the taps
back. Phases are listed in [GAME_DESIGN.md](GAME_DESIGN.md#4-phases).
