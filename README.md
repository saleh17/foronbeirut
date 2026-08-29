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

