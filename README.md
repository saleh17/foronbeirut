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

**Phases 1 to 4 are in.** The loop runs inside a ninety-second day with a queue,
patience, expiring coins and a shop between days. Jibneh alongside zaatar, a peel
carrying a shared-clock load, khodra as add-ons, six upgrades, and a save.

Rules that are the game rather than features on it:

- **One clock, two windows.** Zaatar wants 6.0s, jibneh 7.5s, and their perfect
  windows never overlap — there is a test asserting exactly that. A whole peel
  bakes on one timer, so a mixed load has no right moment.
- **A better furn is always a gift.** Bake time goes down and the window goes up
  at every level, tested at every level. A faster oven that narrowed the window
  would be a stealth difficulty rise the player pays for and cannot articulate.
- **Difficulty is a longer order, not a faster clock.** Khodra pays +2 each and
  each one missed takes 22% off; how often tickets carry khodra grows with the
  day. The bake and the day length never shorten.
- **Collecting is a move you spend.** Serving drops coins on the counter. Earned
  and collected are reported apart, and the tip jar that removes the chore is the
  most expensive thing in the shop for that reason.

Priced by simulation, not arithmetic: a headless bot plays whole days at three
skill levels, and the tree is set against what it actually earns.

### On the art

The shop is **not drawn in Compose**. It is `station_bg.webp`, rendered straight
out of `design/Main.dc.html` by `tools/render-artboard.mjs` — a small headless
renderer for the artboard format that runs the component, expands its template
and screenshots the result at 3x. Hand-porting an illustrated scene into drawing
code guarantees drift; this way the app's art *is* the design, by construction,
and re-exporting after a design change is one command:

```bash
node tools/render-artboard.mjs design/Main.dc.html station_bg.png 3
```

The twelve regulars come across the same way but as vector drawables
(`app/src/main/res/drawable/cust_*.xml`), converted from the canvas's inline SVG
by `tools/svg2vd.py`, so they stay crisp and can be positioned and animated
individually. Only their mouths are drawn live, because that is the one part
that answers to how long they have been waiting.

`Station.kt` therefore contains no scenery at all. It places what moves — the
customers, the food, the fire when the furn is working, the coins, the numbers —
on top of the plate, at the canvas's own coordinates in its own 844 x 390 units,
measured off the render rather than guessed.

Two things on the plate are scenery for now because the engine has not reached
them: the fatayer plate and the ayran dispenser. They arrive with the menu.

