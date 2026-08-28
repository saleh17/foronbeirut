# Manakich Beirut — design spec

A single-station cooking game: one saj, a queue of customers, a
90-second day, and an upgrade tree that keeps you coming back.

Reference point: **Shawarma Legend** (`com.company.shaw`, Play Store, July 2024,
also on Steam) — a shawarma restaurant sim. Two of its design decisions are
worth taking, and the rest is dressing:

1. **Multi-touch parallelism is the skill ceiling.** Good players use several
   fingers at once — filling two trays, building three items, wrapping them
   together. No single step is hard; the optimal play is pipelining. That is
   the whole reason it feels good.
2. **Collecting money is a manual action.** Coins land on the **counter lip in
   front of the customer who paid**, up to three side by side, popping in and
   bobbing so they cannot be missed, and they sit there for six seconds. Coins
   left on the counter when the timer hits zero are lost. Cheap to implement, and it is the main source of
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

**The oven is a furn, not a saj** — the arched brick mouth a real manakish shop
bakes in. **You can see inside it**: the mouth is a lit tunnel in perspective —
a fire-brick floor scorched with soot, brick walls in courses running back on
both sides, a low ceiling, and the far wall glowing orange. Nothing in there is
black.

**Two gas burner pipes run the length of it**, one along the foot of each wall,
converging toward the back like a road in a tunnel: steel with a highlight down
the top, tapering with distance, a feed elbow at the front of each. **Six
drilled holes down each pipe, evenly spaced**, and a flame standing up out of
every one — twelve jets in strict formation, each licking on its own offset so
the line never pulses as a block.

Everything shrinks together with depth — pipe, holes, flames — and that is what
sells the space. **The manakish lie flat on the deck**, foreshortened into
ellipses with a contact shadow under each, inside the narrower floor rather than
floating upright across the mouth. Their timing rings squash with them, so the
rings alone can no longer carry the timing — which is why the timing moved.

**The bake timer is the deck lip.** The metal strip across the front of the
mouth is the readout: a bar that fills as the load bakes, with a hatched green
band marking each topping's perfect window and a bright head riding the front
edge. A mixed load shows **two green bands**, which is the clearest possible
statement of the problem — there is no single moment that satisfies both, and
you can see it. Underneath it in words: FURN EMPTY, Baking, **Perfect — pull!**,
Careful, Burnt.

The fire is a state readout, not decoration, and the three states are far apart
on purpose:

| Furn | Flames |
|---|---|
| Empty | half height, slow, deep red-orange, the tunnel dim |
| Baking | nearly triple that, quick, yellow, the whole left side lit |
| Peel entering or leaving | taller again, whitest, fastest |

The jump when the bread goes in is meant to be unmissable — you should catch it
in the corner of your eye while your hands are on the other side of the counter.
A glance at the fire tells you whether the furn is working without reading a
label. It stands
on the **right**, with the topping trays under it and the dough bowl under those.

**Jeddo leans in from the archway on the right**, behind the counter. He is
decor and personality, not a control.

### Drawing a manousheh that looks like one

Worth writing down because the first pass got it wrong: **a manousheh is not a
green disc.** It is a pale golden flatbread with a **raised, visibly separate
rim** — the topping never reaches the edge — and the zaatar itself is a dark
**olive-brown**, not green: dried thyme and sumac bound in olive oil, closer to
khaki than to grass. On top of that field sit **sesame seeds** (pale, scattered,
individually visible) and **flecks of sumac** (rust red), with an **oil sheen**
catching the light off-centre.

And the detail that makes it unmistakable, taken from a photo of the real thing:
**the dough puffs up in golden domes that poke through the zaatar**, a ring of
them around the inside of the rim and a few more scattered across the middle,
each catching the light on its top-left. Without those it is a flat green
circle; with them it is a manousheh. The edge is never a perfect circle either —
it waves.

So every manousheh in the game renders as two layers — bread underneath, topping
field inset on top — and both brown independently as it bakes: the rim goes
gold then blistered then charred, the zaatar darkens and dries. Cheese is the
same construction with a pale melted field and golden-brown bubbles. The splash
and the serve moment use the full drawing: wavy crust, seventeen puffed domes,
some three hundred sesame seeds.

### The menu board

The green board on the wall is the shop's menu, the way every furn in Beirut has
one: المنيو across the top, then each item with its swatch, a dotted leader and
a fixed price — Zaatar 8, Jibneh 14, Sbanekh 12, Khodra +4. The prices are read
straight off the recipe table, so the board can never drift from what the game
actually pays.

Under a rule, a **قريباً / COMING SOON** block lists what the shop does not sell
yet — mini pizza, kishk, lahm bi ajin — dimmed, each with the day it arrives.
The locked tray on the counter carries a matching **SOON** badge. It costs
nothing to show and it does the work an upgrade tree cannot: the player sees the
whole menu they are working towards on day one, in the fiction, on a board on
the wall.

The playing hint moved to a small strip along the bottom of the screen — it is
scaffolding for testing, not shop furniture, and it should not be occupying the
best prop in the room.

### Khodra is an add-on, not a step

A **six-compartment box, 3x2**, sitting on the counter right beside the dough
bowl — tomato, cucumber, olives, pickles, mint, and labneh in the sixth cell.
It goes **on the baked manousheh, before the paper**, exactly where it goes in
life, and it sits beside the dough because those are the two things the right
hand reaches for without looking.

**Each vegetable is its own ingredient.** Six separate sources, six separate
drags, named on the compartment in Arabic — banadura, khiar, zaytoun, kabees,
naanaa, labneh. A customer asks for one or two *by name*, their ticket lists
exactly which, and each line takes its own tick the moment that vegetable lands
on a manousheh on the counter. **+2 coins each**; every requested vegetable you
miss costs 22% of the payout.

This is where the difficulty should come from as the days go on — not a faster
timer, but a longer order. "Zaatar" is one drag. "Zaatar, tomato, mint" is
three, and you have to read which three. It is never required: it adds **+4 coins** to anything it touches, and
roughly a third of customers ask for it. Serve one of those without khodra and
they pay 30% less; put it on someone who did not ask and nobody minds.

The tray does not run out. Zaatar and cheese already carry the scarcity, and a
second thing to run to jeddo for would turn a bonus into a chore.

### Sbanekh comes off a plate, not out of the furn

**Drinks are cut.** Ayran, tea and juice each needed their own cooler, their own
pour, their own serve — a whole second interaction grammar for one tap of value,
and it made the busiest moment of the day worse.

**Pastries replace them**, and they work differently from each other:

- **Fatayer sbanekh sit finished on a plate on the counter**, folded that
  morning like the kibbeh tray in any real shop. They are sold as a **set of
  four**, wrapped together, and they go out **plain — never with khodra**. One
  tap takes a whole set straight to a customer: no dough, no topping, no furn,
  no wrap. It is the item you reach
  for when the queue is about to walk, and it is the reason the plate is the
  only thing on the counter with a **stock count**: eight a day, and when they
  are gone they are gone. Without that limit a one-tap 12 coins would beat
  everything else on the menu.
- **Mini pizza is baked to order** like a manousheh — a topping on the tray row,
  onto the peel, into the furn. Unlocks day 6.

So the menu now has two rhythms: five taps for a manousheh, one for a fatayer.
That contrast is what makes the plate feel like a lifeline rather than a
shortcut.

The fatayer is **folded into a triangle**, which is worth honouring in the art —
it is the one item on the menu that is not a disc, and that reads instantly both
on the plate and in a queue of order cards.

### The cast

The queue is a Beirut morning, not a row of generic customers. Six regulars,
cycling — each readable by silhouette alone at phone size, which is the whole
test:

| | Who | Reads by |
|---|---|---|
| **Abou Elias** | the service driver | moustache, sunglasses pushed up on his head, cigarette behind the ear |
| **Yara** | on her way to school | high ponytail with a red scrunchie, backpack straps, freckles |
| **Im Georges** | the teta from the building | headscarf tied under the chin, round glasses, cardigan |
| **Ziad** | the delivery rider | open-face helmet, hi-vis vest, chin strap |
| **Karim** | gym before work | gelled quiff, sunglasses on, gold chain, one earbud |
| **Nour** | late for the office | hair in a bun, blazer over a white shirt, hoops |
| **Hanna** | the painter | cap on backwards, paint-spattered whites, pencil behind the ear |
| **Rita** | with the little one | the toddler on her shoulder, in yellow |

Warm, not caricature: varied ages, skin tones and dress, each drawn with the
same affection you would draw a neighbour. Later regulars can carry standing
orders — Abou Elias always takes zaatar, and the player learns it.

**They are alive, not stamps.** Tapered jaws rather than circles, a shadow down
one side of every face lit from the furn, almond eyes with a lid line, brows
that carry the expression, ears, necks, collars. They breathe on a slow idle,
each offset from the next so the queue never moves in lockstep, and **they
blink**. When patience drops they switch to a faster, forward-leaning idle — you
feel the queue getting restless before you read a single meter.

### Sound: Fairuz, and the licensing problem

Fairuz in the morning is the ritual this game is about. It belongs in it.

**But her recordings are copyrighted** — the Rahbani catalogue is actively
administered, and shipping a track without a licence would pull the game from
the stores. Three routes, in the order I would take them:

1. **Commission original music in the Rahbani morning idiom** — oud, qanun,
   accordion, buzuq, a light 3/4 lilt. Cheap, clean, and it can be scored to
   the day timer: sparse at the open, busier as the queue builds, resolving on
   the end-of-day screen.
2. **Licence properly** through the rights holders, if the budget is ever there.
3. **Traditional folk melodies**, freshly arranged — the melodies are old, but
   any specific Rahbani arrangement is not, so this needs care.

Either way the music is **diegetic**: it comes from the radio on the shelf. Tap
it and the dial goes dark and the notes stop. That one detail does more for the
setting than a menu toggle ever would.

### Two more mechanics worth taking

- **The order card is a live checklist**, and it has to be legible in a glance
  taken between two other actions. So it is a proper ticket, not a row of
  swatches: a **NOW / NEXT / AFTER** tag so the queue order is never in doubt, a
  30px picture of the item, its **name in words** under it, the khodra block
  only when khodra was asked for, and **what it pays** at the bottom. The front
  one is bordered in terracotta with a gold halo — you always know who you are
  serving.

  Each requirement takes a **filled green tick** the moment something on the
  counter satisfies it, so progress is read off the customer rather than a HUD,
  and an unmet requirement is faint rather than absent — the difference between
  "not yet" and "not asked for" has to be visible.
- **Patience is a heart, not a bar.** One icon per customer that drains in
  colour. Cheaper to read at a glance mid-rush than a meter.

## 1. The station

    dough bowl -> topping trays -> PEEL -> furn -> khodra -> paper -> serve
    sbanekh plate ---------------------------------------------------> serve
    (laid out RIGHT to LEFT on screen — see "Layout")

Manakish suits this better than shawarma: the prep is a clean ordered pipeline
with a natural failure mode (the oven).

### Order grammar

Keep it small — this is the data model.

    base:     dough
    topping:  zaatar | cheese (akkawi) | half-and-half | kishk | lahm bi ajin
    pastry:   fatayer sbanekh | mini pizza (safiha)
    khodra:   tomato, cucumber, olives, pickles, mint, labneh  (add-on, +4)
    fold:     rolled | flat | triangle (fatayer)

### Interaction map

| Step          | Gesture                             | Fail state                    |
|---------------|-------------------------------------|-------------------------------|
| Take dough    | tap the bowl — a ball lands on the board | wrong count              |
| Roll it out   | tap the board — the pin rolls across | too thin, too thick          |
| Spread        | drag the ladle across the disc      | patchy coverage, lower rating |
| Load the peel | tap the peel — holds 3              | peel full, or left half empty |
| Send it in    | tap the peel again — it swings in   | sent half-loaded              |
| Refill        | tap jeddo when a tray runs dry      | caught empty mid-rush         |
| Bake          | slide the peel in, timing rings     | raw / perfect / burnt         |
| Khodra        | drag the veg tray onto a baked one  | order asked for it and got none |
| Wrap          | tap to roll it in a sheet of paper  | served bare, no tip           |
| Serve+collect | hand it over, tap the coins         | uncollected coins expire      |

### Everything drags

Every action is a drag as well as a tap: dough to the board, a topping across
it, the topped disc onto the peel, the peel into the furn, the baked ones back
out, khodra over them, then paper, then into a customer's hands — and anything
into the bin. A ghost of what you are carrying follows your finger and the
target you are over lights up **green when the drop is legal and red when it is
not**, so the rules are learned by moving rather than by being told.

Tapping still works for everything, and should keep working: it is the
accessible path, and the faster one once you know the layout. Drag is what makes
it feel like cooking; tap is what makes it playable one-handed on a bus.

**Nothing goes into the furn by hand.** Manakish are loaded onto the **peel** —
the long-handled paddle — up to three at a time, and the whole peel slides in
together. That is how a furn actually works, and it does the game a favour:
**one bake timer for the whole load**, so the player is not tracking three
independent clocks, they are choosing a moment that suits all three.

That makes a **mixed load a real decision**. Zaatar wants 6.0s, cheese wants
7.5s. Put both on the same peel and there is no moment that is perfect for
both — pull for the zaatar and the cheese is pale, wait for the cheese and the
zaatar is dark. Each piece still shows its own ring inside the mouth, so the
player can watch one band go green while the other has not, and learn the
lesson without being told it. Batching by topping is the skill the game is
quietly teaching, and the peel is what teaches it.

**The peel stands on its end at the far left**, manakish stacked up the paddle
rather than laid across it. Vertical, it takes a 62px column instead of 270px of
counter, which is most of the bottom-left corner given back to the rest of the
station — and it is how a peel is actually parked between loads.

**The peel is the verb.** One object, one finger: tap it to load the topped
manousheh from the board, tap it again once there is nothing left to load and it
swings into the furn. It glows gold and reads **TAP TO SEND IN** the moment it is
ready to go, so the second tap is never a guess. Tapping the furn still pulls
them out.

And it is a real swing, not a state change. The manakish sit **flat across the
paddle** while it waits on the counter; on the tap the peel **rotates up to
near-vertical**, pivoting on its head, and rises into the mouth with the fire
flaring behind it — then comes back down the same way, **carrying the baked ones
already browned** before they land on the board. Input is locked
for those nine-tenths of a second, which is exactly the beat the action needs —
you cannot spam the furn, and you can see what you pulled before you have to
decide what to do with it.

**Two capacities, both bought separately.** The peel is how many you can carry
in; the deck is how many the furn holds. The batch is whichever is smaller, so
buying one without the other does nothing — and the station says so ("buy more
deck, the peel has room"). Two upgrades, 500 and 650, each 2 -> 3 -> 4.

### Jeddo fills the trays

The zaatar and cheese trays hold **five manakish each**, and they visibly drain:
the topping level drops in the tray and a badge counts down, green to amber to
red. Empty, the tray is bare metal and the topping cannot be spread at all.

**Jeddo refills them, but only when asked.** Tap him and both trays go back to
five. He glows and a "!" pops over his head the moment one runs dry, so it is
never a mystery — but it is a tap, and it is a tap you have to spend during the
rush.

This is what he is for. Standing in the archway as decor was a waste of the best
character in the scene; now he is the reason you look right when your hands are
busy left, and the reason running two toppings hard costs you something.

**Every step you take is a step you can see.** The dough leaves the bowl as a
**ball** and lands on the board; tapping the board sends a **rolling pin** across
it and the ball squashes out into a disc under it. After the furn it is wrapped the way one actually is: the manousheh **folds in
half** with a crease down the middle, a **sheet of paper swings up** from below
and settles behind it, and a **paper band cinches** around the middle to hold
it. Three beats in under a second.

Those animations are not decoration either — each one is the half-second of
feedback that tells you the tap registered, and together they are most of what
separates "a cooking game" from "a series of buttons".

**Scoring:** doneness (bake window) x speed x khodra completeness -> coins +
tip, three stars per customer. That is the whole economy.

**A customer will not take something they did not order.** Wrong item at the
counter is refused outright, not accepted at reduced pay — accepting it makes
the cheapest item on the menu a farming exploit, since anything that clears the
queue is worth more than the coins it earns. Missing khodra is different: that
is a quality miss, so it serves at a penalty. The drop target says which is
which before you let go — the customer lights green only for a wrapped item they
actually asked for, red otherwise.

### Zaatar vs cheese is not a skin

Cheese needs a longer bake than zaatar — akkawi has to melt. One ingredient,
two timing windows, and the player is juggling two oven timers. That also makes
**half-and-half** a real skill unlock rather than a new sprite: one disc, two
correct bake times, pick a compromise.

## 2. Unlocks

### The core rule: every upgrade removes a tap or raises a ceiling

Count the taps in one manousheh on day 1:

    dough -> spread -> into furn -> pull out -> wrap in paper -> serve -> collect  = 7

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
| Wood furn (start) |     — | 6.0s |           1.4s |       0.8s |
| Lined furn        |   350 | 5.2s |           1.4s |       0.9s |
| Gas furn          |   900 | 4.4s |           1.5s |       1.1s |
| Stone furn        | 2,200 | 3.6s |           1.6s |       1.4s |
| Pro deck oven     | 5,000 | 3.0s |           1.8s |       2.0s |

Bake time -50%, forgiveness +29%. The upgrade always feels like a gift.

### Equipment — deletes taps

| Upgrade        | What it removes                      | Levels        | Cost            |
|----------------|--------------------------------------|---------------|-----------------|
| Wider peel     | carry more into the furn in one go   | 2>3>4         | 500/1,400/4,000 |
| Furn deck      | the deck holds more at a time        | 2>3>4         | 650/1,700/4,600 |
| Oven           | waiting (table above)                | 5             | 350 -> 5,000    |
| Burn guard     | panic — holds at perfect longer      | 3             | 600/1,800/4,500 |
| Paper feeder   | wrapping happens on pull-out         | 1             | 800             |
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
| Fatayer sbanekh ×4      | day 3   |    12 | a set of four off the plate, plain |
| Mini pizza (safiha)     | day 6   |    16 | shorter bake than cheese      |

### Ingredients and shop

- Better olive oil / fresh akkawi / stone-ground zaatar: price x1.15 per tier,
  and customers wait longer.
- Bigger sbanekh tray: 12 fatayer on the plate instead of 8.
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
   screen ("240 coins to a wider peel"). Highest-impact element in the genre and
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
    val peelCapacity: Int,
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

## 5. Splash

The title screen is one image: a **manousheh dead centre**, lit from behind, on
a sheet of wrapping paper, with the name over it and a single button under it.
No shop, no counter, no menu — the thing the game is about, at the size of a
poster. Everything else on that screen is a corner chip.

## 6. Layout

Landscape gives two thumbs, and the pipeline runs **right to left** across the
counter — dough bowl, topping trays, furn, then paper and serve — the direction
an Arabic reader's eye already moves.

- Everything you only **read** sits above the counter line: day, timer, coins,
  the queue and its order cards, and jeddo in his archway.
- Everything you **touch** sits below or beside it. **The right thumb cooks**
  (dough, toppings, furn); **the left thumb wraps and serves** (paper, ready
  board, bin). The two busiest actions never fight for one hand.
- Coins land on the counter where the customer stood — reachable by either
  hand, never free.

The zone map on the prototype canvas's second page draws this out, numbered 1-5
along the path. One thing to watch in testing: the furn sits high on the right,
which is the longest reach on the screen and the only tap with a deadline. If
that tests badly, drop it lower behind the counter.

## 7. Open decisions

- **Art.** The prototype's customers and ingredient icons are drawn
  placeholders.
