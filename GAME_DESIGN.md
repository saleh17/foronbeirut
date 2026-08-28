# Forn Beirut — design spec

A single-station cooking game: one saj, a queue of customers, a
90-second day, and an upgrade tree that keeps you coming back.

Reference point: **Shawarma Legend** (`com.company.shaw`, Play Store, July 2024,
also on Steam) — a shawarma restaurant sim. Two of its design decisions are
worth taking, and the rest is dressing:

1. **Multi-touch parallelism is the skill ceiling.** Good players use several
   fingers at once — filling two trays, building three items, wrapping them
   together. No single step is hard; the optimal play is pipelining. That is
   the whole reason it feels good.
2. **Collecting money is a manual action.** Coins left on the counter when the
   timer hits zero are lost. Cheap to implement, and it is the main source of
   tension.

Mechanics are not protectable; art and copy are. All assets here are original.

### Presentation: landscape, and a scene rather than a UI

**The game is played horizontally.** Everything below assumes a landscape phone
(~844x390 at 19.5:9).

**And the screen is a diorama, not an interface.** This is the single biggest
lesson from the reference: there are no floating panels, cards or app chrome.
The shop *is* the UI —

- the day counter is a **calendar nailed to the wall**,
- the timer is a **wooden sign** beside it,
- the coin total sits alone in the **top-right corner**, with a pause button,
- ingredients live in **metal trays on the counter**, not in a tool bar,
- customers lean through a **window behind the counter**, each with an order
  card floating beside their head,
- and a **bin** sits at the end of the counter for anything burnt.

Anything drawn as a rounded card on a flat background is wrong for this game.

### Two more mechanics worth taking

- **The order card is a live checklist.** Each item on it gets a green check the
  moment it is satisfied, so the player reads progress off the customer rather
  than off a HUD.
- **Patience is a heart, not a bar.** One icon per customer that drains in
  colour. Cheaper to read at a glance mid-rush than a meter.

## 1. The station

    dough tray -> rolling surface -> topping bar -> saj -> wrap & serve

Manakish suits this better than shawarma: the prep is a clean ordered pipeline
with a natural failure mode (the oven).

### Order grammar

Keep it small — this is the data model.

    base:     dough
    topping:  zaatar | cheese (akkawi) | half-and-half | kishk | lahm bi ajin
    extras:   tomato, cucumber, mint, olives, labneh   (0..n)
    fold:     rolled | flat
    side:     ayran, tea, orange juice

### Interaction map

| Step          | Gesture                             | Fail state                    |
|---------------|-------------------------------------|-------------------------------|
| Take dough    | tap / drag from tray                | wrong count                   |
| Flatten       | press-and-hold, or short circular drag | too thin tears, too thick doughy |
| Spread        | drag the ladle across the disc      | patchy coverage, lower rating |
| Bake          | drop on the saj, timing ring        | raw / perfect / burnt         |
| Extras        | drag onto the baked piece           | wrong or missing item         |
| Fold + wrap   | swipe to roll, tap paper            | —                             |
| Serve+collect | drag to customer, tap the coins     | uncollected coins expire      |

**Scoring:** accuracy (order match) x doneness (bake window) x speed
-> coins + tip, three stars per customer. That is the whole economy.

### Zaatar vs cheese is not a skin

Cheese needs a longer bake than zaatar — akkawi has to melt. One ingredient,
two timing windows, and the player is juggling two oven timers. That also makes
**half-and-half** a real skill unlock rather than a new sprite: one disc, two
correct bake times, pick a compromise.

## 2. Unlocks

### The core rule: every upgrade removes a tap or raises a ceiling

Count the taps in one manousheh on day 1:

    take dough -> flatten -> spread -> place on saj -> pull off -> serve -> collect  = 7

That number is the progression curve. By late game it should be ~3 while
throughput triples. Every equipment upgrade should be expressible as "this
deletes one of the seven" — a single honest test for whether it deserves to
exist.

Split the tree into two jobs and alternate them:

- **Labor upgrades** — same work, easier (faster oven, more slots).
- **Ceiling upgrades** — same work, worth more (new toppings, better
  ingredients).

Labor-only gets boring; ceiling-only gets exhausting.

### The faster-oven trap

If bake time shortens without the perfect window widening, a faster oven is a
stealth difficulty increase — the player pays coins to make the game harder and
cannot articulate why it feels bad. Bake time goes **down** while the window
goes **up**, always. Split into two visible upgrades so the trade reads:
**Heat** (shorter bake) and **Thermostat/stone** (wider window, longer grace).

| Oven level        | Cost  | Bake | Perfect window | Burn grace |
|-------------------|------:|-----:|---------------:|-----------:|
| Clay saj (start)  |     — | 6.0s |           1.4s |       0.8s |
| Seasoned saj      |   350 | 5.2s |           1.4s |       0.9s |
| Gas furn          |   900 | 4.4s |           1.5s |       1.1s |
| Stone furn        | 2,200 | 3.6s |           1.6s |       1.4s |
| Pro deck oven     | 5,000 | 3.0s |           1.8s |       2.0s |

Bake time -50%, forgiveness +29%. The upgrade always feels like a gift.

### Equipment — deletes taps

| Upgrade        | What it removes                      | Levels        | Cost            |
|----------------|--------------------------------------|---------------|-----------------|
| Saj slots      | serialization — bake in parallel     | 2>3>4>6       | 500/1,400/4,000 |
| Oven           | waiting (table above)                | 5             | 350 -> 5,000    |
| Burn guard     | panic — holds at perfect longer      | 3             | 600/1,800/4,500 |
| Dough press    | flatten becomes one tap              | 1             | 800             |
| Wide ladle     | spread in one stroke, not three      | 3             | 400/1,100/2,800 |
| Prep buffer    | pre-flattened discs in a rack        | 3             | 900/2,400/6,000 |
| Serving tray   | batch-serve three customers          | 2             | 1,600/4,200     |
| Tip jar        | auto-collects coins after 2s         | 2             | 7,000/15,000    |

**Sell the tip jar last and expensive.** Manual coin collection is the best
pressure source in the game; the moment it goes, the game relaxes permanently.
It should read as the reward for mastery, not an early convenience.

### Menu — raises the ceiling

| Item                    | Unlocks | Price | Extra work                    |
|-------------------------|---------|------:|-------------------------------|
| Zaatar                  | start   |     8 | —                             |
| Cheese (akkawi)         | day 2   |    14 | longer bake                   |
| Half and half           | day 4   |    18 | two spreads, split disc       |
| Kishk                   | day 7   |    16 | needs topping-bar upgrade     |
| Lahm bi ajin            | day 10  |    22 | meat prep step                |
| Sides (ayran, tea, kaak)| day 3+  |   5-9 | one tap, pure margin          |

### Ingredients and shop

- Better olive oil / fresh akkawi / stone-ground zaatar: price x1.15 per tier,
  and customers wait longer.
- Bigger dough: same taps, more coins.
- Awning, chairs, radio, tiled counter, sign: faster customer spawn, more
  patience, and the visible reward players screenshot.

### Staff — the late-game idle turn

Assistant (auto-collects money), baker (pulls at perfect), prep boy (keeps the
dough rack full). Each converts an action into automation. Day 15+; this is what
keeps someone playing in week two, and the natural end of the tree.

### Pacing

- Cost curve: `base * 1.7^level`.
- First meaningful upgrade within ~3 minutes of play, second by ~8, then
  stretch the intervals.
- Day 1: ~10 customers x ~8 coins = ~90 coins, so the 350-coin oven lands at the
  end of day 3 — right when the timer starts feeling tight. That "one more day"
  pull comes entirely from this number being right.
- **Gate scale, never fun.** No upgrade should be required to pass a day, or it
  reads as a paywall.

### Retention around the tree

1. A persistent **next-unlock progress bar** on the HUD and the end-of-day
   screen ("240 coins to Saj slot 3"). Highest-impact element in the genre and
   it is a progress indicator.
2. **Daily goal** ("serve 20 cheese today") for a bonus — a reason to play a day
   you would otherwise skip.
3. **Stars per day (1-3), best persisted, replayable** — turns a bad run into a
   retry instead of a loss.
4. **Recipe book** that fills in as toppings unlock.
5. **Streak bonus** across consecutive days.

Anti-patterns: no energy timers, no lives, no forced-ad gates. The pleasure here
is uninterrupted flow and every one of those breaks it.

## 3. Architecture

Compose Multiplatform, not a game engine: 2D, tap/drag, no physics, no scrolling
world. Compose gives gestures, animation and layout for free. Godot/Unity would
only be right if this later wants particles, skeletal animation and a scrolling
restaurant.

**Put the game in `commonMain` as pure Kotlin, zero Compose imports.**

    shared/src/commonMain/kotlin/.../game/
      model/    Order.kt, Topping.kt, Manakish.kt, Customer.kt, Upgrade.kt
      engine/   GameState.kt, GameEngine.kt, Scoring.kt
      economy/  Prices.kt, UpgradeTree.kt, GameParams.kt
      save/     SaveData.kt

The engine is `(state, params, dt, events) -> state`. Nothing else.

**Upgrades never touch engine logic.** They compile into a params snapshot the
engine reads:

```kotlin
data class GameParams(
    val sajSlots: Int,
    val bakeMs: Long,
    val perfectWindowMs: Long,
    val burnGraceMs: Long,
    val flattenTaps: Int,
    val spreadStrokes: Int,
    val prepBufferSize: Int,
    val batchServe: Int,
    val autoCollectAfterMs: Long?,   // null = manual
    val priceMultiplier: Float,
    val patienceMs: Long,
    val spawnIntervalMs: Long,
    val unlockedToppings: Set<Topping>
)

fun paramsFor(owned: Map<UpgradeId, Int>, day: Int): GameParams

data class UpgradeTier(
    val level: Int,
    val cost: Int,
    val requires: List<Requirement>,   // MinDay, TotalCustomers, OwnedAtLeast
    val effects: List<Effect>,
    val blurb: String                  // "Bake 25% faster" — player language
)
```

That makes the whole economy regression-testable in `commonTest`: *at day 7 on a
typical spend path, is tier 3 reachable?* Worth more than any amount of playing
it by hand.

### Compose side — the four APIs

- `withFrameMillis` in a `LaunchedEffect` for the clock. Not a `delay` loop; it
  drifts.
- `Modifier.pointerInput { awaitPointerEventScope { ... } }` — **not**
  `detectDragGestures`, which is single-pointer. Track `event.changes` by
  `PointerId` with a `Map<PointerId, DragTarget>`; that is what buys the
  multi-finger parallelism.
- `Canvas` for the manousheh: coverage is a list of painted blobs, doneness is a
  colour lerp pale -> golden -> burnt. The core object needs no art assets.
- `Animatable` / `AnimatedContent` for walk-ins, coin pops, star ratings.

**Assets:** layered PNGs or vectors per state (ball / disc / topped / baked /
burnt / wrapped). **Persistence:** DataStore, or an expect/actual key-value
store. **Sound:** `SoundPool` — a cooking game with no sizzle feels broken;
budget real time for it.

## 4. Phases

1. **Vertical slice.** One customer, zaatar only: flatten -> spread -> bake ->
   serve -> collect. No timer, no upgrades, no menu. If this is not fun, nothing
   after it will be.
2. **The shift.** 90-second day, queue with patience meters, expiring coin
   drops, end-of-day summary.
3. **Depth.** Cheese and half-and-half with distinct bake windows, extras,
   multi-touch batching, 2 -> 4 saj slots.
4. **Economy.** Upgrade tree, save/load, day progression.
5. **Polish + iOS.** Move the UI into `composeApp`, and the iOS target pays off.

Two tuning notes from the start: keep the perfect window generous (~1.2s on a 4s
bake) until phase 3, and make the day timer the only pressure source early —
layering patience meters on top too soon makes it stressful rather than
satisfying.

## 5. Layout

Landscape gives two thumbs, which settles the layout: the pipeline runs **left
to right** across the counter — dough, topping, saj, serve — the same direction
a baker actually works.

- Everything you only **read** sits above the counter line: day, timer, coins,
  the queue and its order cards.
- Everything you **touch** sits below it, inside one thumb arc or the other:
  saj on the left, trays and work board in the middle, ready/serve and the bin
  on the right.
- Coins land on the counter where the customer stood — reachable by either
  hand, never free.

The zone map on the prototype canvas's second page draws this out. Watch the
left thumb at three saj slots: that is the point where the prep buffer upgrade
starts to earn its cost.

## 6. Open decisions

- **Art.** The prototype's customers and ingredient icons are drawn
  placeholders.
