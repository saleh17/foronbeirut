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

**Phase 1, the vertical slice**, is in. One customer, zaatar only: take dough,
flatten, spread, into the furn, pull it on the green band, hand it over, get
paid. No day timer, no queue, no upgrades, no menu — on purpose. The point of
this phase is to find out whether the core loop is fun before anything is built
on top of it.

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

Phase 2 is the shift: a 90-second day, a queue with patience meters, coins that
expire on the counter, and an end-of-day summary. Phases are listed in
[GAME_DESIGN.md](GAME_DESIGN.md#4-phases).
