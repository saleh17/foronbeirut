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

**Phases 1 and 2 are in.** The loop — take dough, flatten, spread, into the
furn, pull it on the green band, hand it over — now runs inside a **ninety-second
day**, with a queue of up to three, a **heart per customer** that drains while
they wait, **coins that land on the counter and are gone in six seconds** if you
do not pick them up, and an end-of-day board.

Two things that turn a loop into a shift and are easy to get wrong:

- **Collecting is a move you spend.** Coins are earned and collected separately,
  and the day report shows both, so leaving them on the counter is a visible
  mistake rather than a silent one. This is exactly the cost the tip-jar upgrade
  will later sell you out of.
- **Speed is a tip, not the wage.** The bake alone spends six of a customer's
  twenty-four seconds, so a full tip is unreachable by construction — being fast
  is a bonus on top of a fair price, never the price itself.

Still no upgrades, no cheese, no khodra, no menu. That is phase 3.

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

Phase 3 is depth: cheese and its longer bake window, the peel carrying three at
once under one shared timer, and khodra as add-ons. Phases are listed in
[GAME_DESIGN.md](GAME_DESIGN.md#4-phases).
