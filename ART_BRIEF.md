# Manakich Beirut — art brief

Everything an illustrator needs to replace the prototype's CSS-and-SVG
placeholder art with finished assets. The playable prototype is the layout
reference and the source of truth for sizes:
<https://claude.ai/code/artifact/2ae1c617-d592-4513-bf2d-7aee4b42c011>

Read [GAME_DESIGN.md](GAME_DESIGN.md) first for what the game actually does.
This document is only about how it looks.

---

## 1. The one-paragraph version

A Lebanese bakery, one morning, seen from behind the counter. You take dough
from a bowl, roll it flat, spread zaatar or cheese, load it onto a peel, slide
the peel into a brick furn, pull it out at the right second, add vegetables,
roll it in paper and hand it to whoever is at the window — while a queue builds,
a clock runs down and Fairuz plays on a radio. It should feel warm, busy and
specific to Beirut, not like a generic cartoon kitchen.

## 2. Canvas and delivery

| | |
|---|---|
| Design canvas | **844 × 390** logical units, landscape only |
| Aspect | 19.5 : 9 — a modern phone held sideways |
| Ship at | **3×** (2532 × 1170) for raster; vector preferred where possible |
| Safe area | keep nothing critical within 24 units of the left and right edges (notch and home indicator) |
| Format | layered source (`.psd`, `.ai`, `.procreate` or `.riv`) **plus** exported PNGs with transparency, one file per asset per state |
| Naming | `zone_object_state@3x.png` — e.g. `furn_deck_perfect@3x.png` |
| Colour | sRGB |

All coordinates below are in design units, measured from the top-left of the
canvas, and match the prototype exactly. **Match the slot; the composition is
already balanced.**

## 3. Art direction

**The screen is a diorama, not an interface.** There is no UI chrome anywhere.
The day counter is a calendar nailed to the wall, the timer is a wooden sign,
the prices are a chalk menu, the ingredients are trays on a real counter. If
something looks like a button, it is wrong.

**Style.** Hand-drawn cartoon with confident, uneven outlines — closer to a
painted shopfront sign than to flat vector. Ink outlines in a warm near-black
`#2A1B12`, never pure black, heavier on silhouettes (≈3.5 units at 1×) and
lighter on interior detail (≈2). Cel shading: one clear shadow shape per form
rather than soft gradients, with a second warm bounce light on anything near the
furn.

**Light.** One source: **the furn, low and to the left.** Everything on the left
half carries a warm rim; everything on the right falls into cooler shadow, until
the door on the far right, where **daylight** cuts back in. Get this right and
half the work is done.

**Palette.**

| Role | Hex |
|---|---|
| Ink | `#2A1B12` |
| Warm cream / paper | `#FDFBF4` → `#E4DAC4` |
| Wall stone | `#E7DCC2` → `#A99878` |
| Wall tile (Levantine diamond band) | `#5F7F5C` / `#47624A` |
| Counter marble | `#EDE5D4` → `#C3B79E` |
| Brick (furn) | `#BE7A5E` → `#9E5C44` |
| Fire | `#FFEFA8` → `#FF8A16` → `#C0561A` |
| Bread crust | `#F3E4C0` → `#8E6337` when burnt |
| Zaatar | `#6E6634` → `#2E2712` — olive-brown, **never green** |
| Cheese (akkawi) | `#FAF0CE` → `#8E6B33` |
| Terracotta accent | `#D4643F` / `#B04A2C` |
| Gold / coins | `#F7D477` → `#B77C1F` |
| Signal green (perfect bake, ticks) | `#7CA646` / `#4E7A2E` |

**Type.** Alexandria (display) and Cairo (body), both of which carry Arabic.
Arabic and Latin sit side by side throughout and Arabic is never an
afterthought — the calendar says اليوم, the menu says المنيو, the door says
مفتوح.

**Do not** copy any existing game's assets, characters or logos. The mechanics
were studied from a shawarma game; none of its art may be referenced.

---

## 4. Scene map

The wall runs from y 0 to y 196. The **counter lip** is a 24-unit band at
y 196–220, and the counter surface fills y 220–390.

### Wall

| Asset | x | y | w | h | Notes |
|---|---:|---:|---:|---:|---|
| Back wall | 0 | 0 | 844 | 214 | Plaster and stone, with a **green Levantine diamond tile band** at y 92–132 |
| Left stone wall | 0 | 0 | 224 | 390 | Frames the furn |
| Right stone wall | 700 | 0 | 144 | 390 | Frames the door |
| Hanging zaatar bunch | 210 | 0 | 34 | 64 | Tied with string to a nail |
| Wall calendar | 252 | 4 | 58 | 64 | Red header reading **اليوم**, big day number below, two nails |
| Timer sign | 318 | 6 | 80 | 44 | Wooden board, dark glass, glowing digits |
| **Lebanese flag** | 402 | 2 | 142 | 58 | On a brass rod. Red-white-red with the cedar. Give it real cloth folds |
| Radio | 596 | 8 | 52 | 38 | Wooden 1960s set, speaker grille, dial, two knobs. Dial lights when on |
| Menu board | 556 | 60 | 148 | 132 | Chalkboard in a wooden frame. **المنيو**, priced rows, a قريباً block below a rule |
| **Shop door** | 708 | 30 | 130 | 172 | Stone frame, glass-and-wood leaf standing open inward, brass bell above, مفتوح / OPEN card, daylight pouring through |

### The furn — the hero prop

| Asset | x | y | w | h |
|---|---:|---:|---:|---:|
| Furn body | 16 | 30 | 186 | 172 |
| Mouth opening | +24 | +28 | 138 | 116 |
| Bake bar (deck lip) | 22 | bottom 24 | 142 | 16 |

An **arched brick mouth**, courses visible, soot-darkened above the opening.
Inside it is a **lit tunnel in one-point perspective** — nothing black:

- **floor**: scorched fire-brick, soot patches, receding to a vanishing band at
  50% height
- **both walls**: brick courses running back, warmer on the left
- **ceiling**: low, dark, warm
- **far wall**: glowing orange
- **two steel burner pipes**, one along the foot of each wall, tapering to the
  back, with **six drilled holes each** and a **flame standing out of every
  hole** — jets shrink with distance, white-hot at the base

The **bake bar** is the metal strip across the front of the mouth and doubles as
the timer: it must read as machined steel when idle and as a filling gauge when
lit. Deliver it as: idle strip, fill overlay, hatched green window band, bright
leading-edge head.

### Counter — the working row (y 212–384)

| Asset | x | y | w | h | Notes |
|---|---:|---:|---:|---:|---|
| Bin | 8 | 294 | 62 | 88 | Steel, swing lid, dark mouth |
| **Peel** | 78 | 212 | 62 | 178 | Stands on its end. Steel paddle 62×156, turned wooden handle below. Three manakish stack **up** the paddle |
| Wrapping bench | 156 | 296 | 88 | 88 | Square wooden block, deep border, **three sheets of paper stacked askew** inside |
| Work board | 262 | 300 | 130 | 82 | Floured wood, grain, a dusting of flour |
| Sbanekh plate | 296 | 230 | 118 | 58 | Ceramic oval, fatayer triangles piled on it, count badge |
| **Ayran dispenser** | 424 | 212 | 112 | 84 | Chrome drinks machine, blue **عيران** panel, level window, nozzle, grated drip tray. Cups 28×34 under the spout |
| Khodra box | 410 | 300 | 126 | 82 | Steel box, **3 × 2 compartments**: tomato, cucumber, olives, pickles, mint, labneh |
| Dough bowl | 554 | 300 | 124 | 82 | Wooden bowl, five dough balls, flour |
| Zaatar tray | 648 | 234 | 96 | 50 | Gastronorm pan. Contents drain from full to empty |
| Cheese tray | 748 | 234 | 96 | 50 | As above |
| Pizza tray (locked) | 548 | 234 | 96 | 50 | Empty pan, dull, `SOON` badge |

### Queue and money

| Asset | x | y | w | h |
|---|---:|---:|---:|---:|
| Customer slots | 218, 336, 454 | 58 | 118 | 158 |
| Character art within slot | — | — | **56 × 102** | |
| Order ticket | beside the head | — | 58 wide | |
| Coin drop slots | 236, 354, 472 | 186 | 58 | 58 |

---

## 5. The manousheh — the most important object in the game

It appears at six sizes and every state must be recognisable at **30 units**.

**Construction, always two layers:**

1. **Bread** — a pale golden flatbread with a **raised rim the topping never
   reaches**, edge slightly wavy, never a perfect circle, and — this is the
   detail that makes it real — **golden puffed domes poking up through the
   topping**, a ring of them inside the rim plus a few across the middle, each
   catching light on its top-left.
2. **Topping field** — inset, never touching the edge.
   - **Zaatar**: dark **olive-brown**, thyme and sumac bound in olive oil, with
     scattered pale **sesame seeds**, rust-red **sumac flecks** and an
     off-centre **oil sheen**. It is not green.
   - **Cheese**: pale melted akkawi, bubbled, with golden-brown blisters.

**Doneness states — deliver all five, for both toppings:**

| State | Bread | Topping |
|---|---|---|
| Raw | very pale, no colour | wet, bright, oil pooling |
| Baking | straw | darkening |
| **Perfect** | golden, first blisters | dried, aromatic, deepest colour |
| Overdone | deep amber, dark blisters | edges going black |
| Burnt | dark brown to black | charred |

**Also needed:** dough ball; flattened disc; topped disc; the **fatayer sbanekh
triangle** (the one item on the menu that is not a disc) as a single piece and
as a set of four; **mini pizza**; the **wrapped roll** — a tube in paper with
the spiral cross-section showing at the open end; and the six khodra items as
loose garnish on a baked disc.

---

## 6. Character sheet

Eight regulars cycle through the queue. **Each must be identifiable by
silhouette alone at 56 units wide** — that is the test, not the face.

| # | Who | Reads by | Palette |
|---|---|---|---|
| 1 | **Abou Elias** — service driver | Sunglasses pushed up on his head, thick grey moustache, cigarette behind the ear, open collar with a gold chain, blue **masbaha** in his hand | sky-blue shirt, olive skin |
| 2 | **Yara** — on her way to school | High ponytail with a red scrunchie, backpack straps, freckles, **cedar badge** on the pinafore | navy uniform, white collar |
| 3 | **Im Georges** — the teta | Headscarf tied under the chin, round glasses, cardigan, thin gold chain, smile lines | cream scarf, mauve cardigan |
| 4 | **Ziad** — delivery rider | Open-face helmet with the visor up, chin strap, hi-vis vest | white helmet, lime vest, dark tee |
| 5 | **Karim** — gym before work | Gelled quiff, sunglasses on, gold chain, one earbud | black fitted tee |
| 6 | **Nour** — late for the office | Hair in a bun, blazer over a white shirt, hoop earrings, lanyard | navy blazer, deeper skin tone |
| 7 | **Hanna** — the painter | Cap on backwards, paint-spattered whites, pencil behind the ear, **keffiyeh** at the neck | off-white overalls |
| 8 | **Rita** — with the little one | The toddler on her shoulder in yellow | terracotta top |

**Per character, deliver:**

- three-quarter **front** view, neutral — the queue pose
- three-quarter **front**, impatient — leaning in, brow down
- three-quarter **back** view — the walk-out, with their wrapped order in hand
- head at 2× for the serve-moment close-up

**Craft notes.** Tapered jaws, not circles. Real necks, collars and shoulders —
the counter crops them at the waist. Almond eyes with a lid line and a highlight;
brows carry the expression. Ears. Hair with volume and one highlight. A cel
shadow down the side away from the furn. Vary ages, skin tones and body shapes.

**Warm, never caricature.** Draw each one the way you would draw a neighbour.
No exaggerated ethnic features, no costume-shop "Middle East" — the specifics
above (a masbaha, a scrunchie, a keffiyeh, a helmet) do all the work.

Also needed: **five to eight extra silhouettes** for later days, and the
**baker's own hands** if hands are ever shown reaching for the dough.

---

## 7. Animation

Prototype timings. Nothing may run long enough to delay the next input —
**the peel is the one deliberate exception.**

| Move | Length | What happens |
|---|---:|---|
| Dough rolled flat | 0.72 s | Wooden pin sweeps across; the ball squashes out, overshoots, settles |
| Peel into the furn | 0.95 s | Dips, **rises straight up** into the mouth, holds, comes back down |
| Peel out | 0.95 s | Same path, returning with the baked pieces already browned |
| Fire — idle | 1.9 s loop | Low, slow, deep red-orange |
| Fire — baking | 0.68 s loop | Up, quick, yellow, whole left side lit |
| Fire — peel moving | 0.4 s loop | Roaring, near-white |
| Wrap | 0.85 s | Paper flap folds over; the roll **tightens from one end** while the rolled leading edge keeps its width and travels across; spiral end appears |
| Customer walks in | 0.95 s | Eight-step bobbing path in from the door |
| Queue steps up | 0.62 s | Same footfall rhythm |
| Customer walks out | 1.15 s | Out to the door, fading into the daylight |
| Coin collected | 0.55 s | Flies to the counter in the top-right and shrinks |
| Ayran refill | 2.0 s | Liquid rises up the glass |
| Tray refill | 1.3 s | Level climbs back |
| Idle breathing | 3.8 s loop | Per character, offset so the queue never syncs |
| Blink | 5.6 s loop | Offset per character |
| Heartbeat (patience) | 1.5 s loop | Faster as patience drops |

Sprite sheets or **Rive / Spine** rigs both fine; rigs preferred for the
characters so extra regulars can be added cheaply later.

---

## 8. Priority

If the budget only reaches part of it, this is the order that buys the most:

1. **The manousheh, all states** — it is on screen constantly and at every size
2. **The furn** — hero prop, the light source, the tension
3. **The eight characters**
4. **Counter props** — peel, bowl, trays, khodra box, dispenser, bench
5. **Wall dressing** — flag, menu, calendar, radio, door
6. Splash and end-of-day screens
