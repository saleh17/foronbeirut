package com.foronbeirut.manakich

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foronbeirut.manakich.engine.Action
import com.foronbeirut.manakich.engine.Baked
import com.foronbeirut.manakich.engine.Board
import com.foronbeirut.manakich.engine.Customer
import com.foronbeirut.manakich.engine.DayPhase
import com.foronbeirut.manakich.engine.Doneness
import com.foronbeirut.manakich.engine.GameParams
import com.foronbeirut.manakich.engine.GameState
import com.foronbeirut.manakich.engine.Khodra
import com.foronbeirut.manakich.engine.Topping
import com.foronbeirut.manakich.engine.Upgrade
import kotlin.math.sin

/**
 * The station.
 *
 * The shop itself is not drawn here: it is `station_bg.webp`, rendered straight
 * out of the design canvas by tools/render-artboard.mjs. Hand-porting an
 * illustrated scene into Compose guarantees drift, so the art is the art and this
 * file only places what moves on top of it — at the canvas's own coordinates, in
 * its own 844 x 390 units.
 */
private const val STAGE_W = 844f
private const val STAGE_H = 390f

// Measured off the rendered artboard, so every overlay lands on its prop.
val FURN = Rect4(16, 30, 186, 172)
val PEEL = Rect4(78, 212, 62, 178)
val BIN = Rect4(4, 292, 70, 92)
val BOARD = Rect4(262, 300, 130, 82)
val KHODRA = Rect4(410, 300, 126, 82)
val DOUGH = Rect4(554, 300, 124, 82)
val TRAY_ZAATAR = Rect4(648, 234, 96, 50)
val TRAY_JIBNEH = Rect4(748, 234, 96, 50)
val BENCH = Rect4(688, 298, 152, 84)
val CUSTOMER_X = listOf(218, 336, 454)
val CALENDAR = Rect4(252, 4, 58, 64)
val TIMER = Rect4(318, 6, 80, 44)
val COINS = Rect4(712, 8, 118, 32)

data class Rect4(val x: Int, val y: Int, val w: Int, val h: Int)

private fun Modifier.at(r: Rect4) = offset(r.x.dp, r.y.dp).requiredSize(r.w.dp, r.h.dp)

/**
 * What is currently in the hand. Held here rather than in the engine, because
 * carrying something is not a game state — it is a gesture in progress that
 * resolves into exactly the action the equivalent tap would have sent.
 */
class DragHost {
    var carry: Carry? by mutableStateOf(null)
        private set
    var x: Float by mutableStateOf(0f)
        private set
    var y: Float by mutableStateOf(0f)
        private set

    fun begin(what: Carry, atX: Float, atY: Float) {
        carry = what
        x = atX
        y = atY
    }

    fun moveTo(atX: Float, atY: Float) {
        x = atX
        y = atY
    }

    fun release(): Carry? = carry.also { carry = null }
}

/**
 * Makes a prop something you can pick up. Sits alongside the tap handler rather
 * than replacing it: a press that never moves is still a tap.
 */
@Composable
private fun Modifier.dragSource(origin: Rect4, host: DragHost, pick: () -> Carry?, onDrop: (Carry, Float, Float) -> Unit): Modifier {
    val density = LocalDensity.current.density
    return this.pointerInput(origin, host) {
        detectDragGestures(
            onDragStart = { at ->
                pick()?.let { host.begin(it, origin.x + at.x / density, origin.y + at.y / density) }
            },
            onDrag = { change, _ ->
                if (host.carry != null) {
                    change.consume()
                    host.moveTo(origin.x + change.position.x / density, origin.y + change.position.y / density)
                }
            },
            onDragEnd = { host.release()?.let { onDrop(it, host.x, host.y) } },
            onDragCancel = { host.release() },
        )
    }
}

@Composable
fun StationScreen(state: GameState, params: GameParams, fx: Effects, onAction: (Action) -> Unit) {
    var selected by remember { mutableStateOf(0) }
    val chosen = selected.coerceIn(0, (state.bench.size - 1).coerceAtLeast(0))
    val host = remember { DragHost() }
    val drop: (Carry, Float, Float) -> Unit = { carried, x, y ->
        actionFor(carried, hitTest(x, y, state.bench.size), state)?.let(onAction)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1C120A)), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val scale = minOf(maxWidth.value / STAGE_W, maxHeight.value / STAGE_H)
            Box(
                Modifier
                    .requiredSize(STAGE_W.dp, STAGE_H.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            ) {
                Image(
                    painter = painterResource(R.drawable.station_bg),
                    contentDescription = null,
                    modifier = Modifier.requiredSize(STAGE_W.dp, STAGE_H.dp),
                    contentScale = ContentScale.FillBounds,
                )

                FurnZone(state, params, host, drop, onIn = { onAction(Action.IntoFurn) }, onOut = { onAction(Action.OutOfFurn) })
                Queue(state, state.bench.getOrNull(chosen)?.khodra.orEmpty()) {
                    if (state.bench.isNotEmpty()) onAction(Action.Serve(chosen))
                }
                Drops(state) { onAction(Action.Collect(it)) }

                PeelZone(state, params, host, drop) { onAction(Action.IntoFurn) }
                BoardZone(state, host, drop) {
                    when (state.board) {
                        Board.Empty -> onAction(Action.TakeDough)
                        Board.Ball -> onAction(Action.Flatten)
                        Board.Flat -> Unit
                        is Board.Topped -> onAction(Action.LoadPeel)
                    }
                }
                BenchZone(state, chosen, host, drop) { selected = it }
                KhodraZone(state, chosen, host, drop) { onAction(Action.AddKhodra(chosen, it)) }
                Tap(TRAY_ZAATAR, Modifier.dragSource(TRAY_ZAATAR, host, { Carry.Topping(Topping.ZAATAR) }, drop)) { onAction(Action.Spread(Topping.ZAATAR)) }
                Tap(TRAY_JIBNEH, Modifier.dragSource(TRAY_JIBNEH, host, { Carry.Topping(Topping.JIBNEH) }, drop)) { onAction(Action.Spread(Topping.JIBNEH)) }
                Tap(DOUGH, Modifier.dragSource(DOUGH, host, { Carry.Dough }, drop)) { onAction(Action.TakeDough) }
                Tap(BIN) {
                    if (state.bench.isNotEmpty() && state.board == Board.Empty) onAction(Action.BinBaked(chosen))
                    else onAction(Action.BinBoard)
                }

                Hud(state)
                DragLayer(host, state)
                FxLayer(fx)
                Hint(state)
                Curtain(state, onAction)
            }
        }
    }
}

/** An invisible hit target over a prop that is already painted in the background. */
@Composable
private fun Tap(r: Rect4, extra: Modifier = Modifier, onTap: () -> Unit) {
    Box(Modifier.at(r).noRippleClickable(onTap).then(extra))
}

// ---------------------------------------------------------------- the furn

@Composable
private fun FurnZone(state: GameState, params: GameParams, host: DragHost, drop: (Carry, Float, Float) -> Unit, onIn: () -> Unit, onOut: () -> Unit) {
    val load = state.furn
    Box(
        Modifier.at(FURN).noRippleClickable { if (load == null) onIn() else onOut() }
            .dragSource(FURN, host, { carryFrom(Zone.FURN, state) }, drop)
    ) {
        // The plate is painted with the fire low. A load makes the furn work harder.
        if (load != null) {
            Canvas(Modifier.fillMaxSize()) {
                val u = size.width / FURN.w
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x66FF9A3C), Color(0x00FF9A3C)),
                        center = Offset(93f * u, 130f * u),
                        radius = 120f * u,
                    )
                )
                for (i in 0 until 8) {
                    val x = 30f + i * 18f
                    val h = 22f + (i % 3) * 6f
                    val flame = Path().apply {
                        moveTo(x * u, (140f - h) * u)
                        cubicTo((x + 7f) * u, (140f - h * .5f) * u, (x + 5f) * u, 140f * u, x * u, 140f * u)
                        cubicTo((x - 5f) * u, 140f * u, (x - 7f) * u, (140f - h * .5f) * u, x * u, (140f - h) * u)
                        close()
                    }
                    drawPath(
                        flame,
                        Brush.verticalGradient(
                            listOf(Palette.FlameHot, Palette.Flame, Color(0x00D2541E)),
                            startY = (140f - h) * u, endY = 140f * u,
                        ),
                    )
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BakeRings(load.items.distinct(), load.elapsed, params)
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(Modifier.offset(0.dp, 22.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    load.items.forEach {
                        Manousheh(size = 34.dp, topping = it, doneness = params.donenessAt(it, load.elapsed))
                    }
                }
            }
        }
    }
}

/**
 * One ring per topping in the load, sharing a single needle. That is the whole
 * mixed-load problem drawn: two green bands, one hand, and they never line up.
 */
@Composable
private fun BakeRings(toppings: List<Topping>, elapsed: Double, params: GameParams) {
    val span = 12.0
    Canvas(Modifier.requiredSize(132.dp)) {
        toppings.forEachIndexed { index, topping ->
            val recipe = params.recipe(topping)
            val stroke = Stroke(width = 7.dp.toPx())
            val inset = stroke.width / 2 + index * 11.dp.toPx()
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            fun band(from: Double, to: Double, colour: Color) {
                drawArc(
                    colour, -90f + (from / span * 360.0).toFloat(), ((to - from) / span * 360.0).toFloat(),
                    false, topLeft, arc, style = stroke,
                )
            }
            val half = recipe.perfectWindow / 2
            band(0.0, span, Color(0x59000000))
            band(recipe.bakeSeconds - half, recipe.bakeSeconds + half, Palette.Good)
            band(recipe.bakeSeconds + half, recipe.bakeSeconds + half + recipe.graceWindow, Palette.Coin)
            band(0.0, elapsed.coerceAtMost(span), Color(0xF2FFF4DC))
        }
    }
}

// ---------------------------------------------------------------- the counter

@Composable
private fun PeelZone(state: GameState, params: GameParams, host: DragHost, drop: (Carry, Float, Float) -> Unit, onSend: () -> Unit) {
    Box(
        Modifier.at(PEEL).noRippleClickable { if (state.peel.isNotEmpty()) onSend() }
            .dragSource(PEEL, host, { carryFrom(Zone.PEEL, state) }, drop),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.offset(0.dp, 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(params.peelSlots) { slot ->
                val item = state.peel.getOrNull(slot)
                if (item != null) Manousheh(size = 44.dp, topping = item, doneness = null, topped = true)
                else Box(Modifier.size(44.dp).clip(CircleShape).border(2.dp, Color(0x44FFFFFF), CircleShape))
            }
        }
    }
    Label(if (state.peel.isEmpty()) "PEEL" else "TAP: INTO THE FURN", PEEL.x - 12, 392, PEEL.w + 24)
}

@Composable
private fun BoardZone(state: GameState, host: DragHost, drop: (Carry, Float, Float) -> Unit, onTap: () -> Unit) {
    Box(
        Modifier.at(BOARD).noRippleClickable(onTap)
            .dragSource(BOARD, host, { carryFrom(Zone.BOARD, state) }, drop),
        contentAlignment = Alignment.Center,
    ) {
        when (state.board) {
            Board.Empty -> Unit
            Board.Ball -> Box(
                Modifier.size(38.dp).clip(CircleShape).background(Palette.DoughPale)
                    .border(3.dp, Palette.Ink, CircleShape)
            )
            Board.Flat -> Manousheh(size = 68.dp, topping = null, doneness = null)
            is Board.Topped -> Manousheh(
                size = 68.dp, topping = (state.board as Board.Topped).topping, doneness = null, topped = true,
            )
        }
    }
    Label(
        when (state.board) {
            Board.Empty -> "TAP FOR DOUGH"
            Board.Ball -> "TAP TO FLATTEN"
            Board.Flat -> "PICK A TOPPING"
            is Board.Topped -> "TAP: ONTO THE PEEL"
        },
        BOARD.x, 384, BOARD.w,
    )
}

/**
 * The baked ones wait on the clear stretch of counter at the right. The design's
 * wrap bench only has room for one, and a peel-load is three.
 */
@Composable
private fun BenchZone(state: GameState, chosen: Int, host: DragHost, drop: (Carry, Float, Float) -> Unit, onPick: (Int) -> Unit) {
    Box(Modifier.at(BENCH), contentAlignment = Alignment.CenterStart) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.bench.forEachIndexed { index, baked ->
                BenchItem(baked, index == chosen, Modifier.dragSource(BENCH, host, { Carry.BakedItem(index) }, drop)) { onPick(index) }
            }
        }
    }
    Label(if (state.bench.isEmpty()) "" else "PICK ONE UP", BENCH.x, 384, BENCH.w)
}

@Composable
private fun BenchItem(baked: Baked, picked: Boolean, extra: Modifier, onPick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .then(if (picked) Modifier.border(3.dp, Palette.Select, CircleShape) else Modifier)
            .noRippleClickable(onPick)
            .then(extra),
        contentAlignment = Alignment.Center,
    ) {
        Manousheh(size = 42.dp, topping = baked.topping, doneness = baked.doneness)
        Row(Modifier.offset(0.dp, 19.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            baked.khodra.forEach {
                Box(
                    Modifier.size(7.dp).clip(CircleShape).background(khodraInk(it))
                        .border(1.dp, Palette.Ink, CircleShape)
                )
            }
        }
    }
}

/** Six compartments, already painted. This only marks what is asked for and what is on. */
@Composable
private fun KhodraZone(state: GameState, chosen: Int, host: DragHost, drop: (Carry, Float, Float) -> Unit, onAdd: (Khodra) -> Unit) {
    val on = state.bench.getOrNull(chosen)?.khodra.orEmpty()
    val wanted = state.front?.khodra.orEmpty()
    Box(Modifier.at(KHODRA).padding(5.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Khodra.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier
                                .requiredSize(35.dp, 33.dp)
                                .then(
                                    when {
                                        k in on -> Modifier.border(2.5.dp, Palette.Good, RoundedCornerShape(3.dp))
                                        k in wanted -> Modifier.border(2.5.dp, Palette.Select, RoundedCornerShape(3.dp))
                                        else -> Modifier
                                    }
                                )
                                .noRippleClickable { onAdd(k) }
                                .dragSource(KHODRA, host, { Carry.Veg(k) }, drop)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- the queue

@Composable
private fun Queue(state: GameState, inHand: Set<Khodra>, onServe: () -> Unit) {
    state.queue.take(3).forEachIndexed { index, customer ->
        val front = index == 0
        val slotX = CUSTOMER_X[index].toFloat()

        // All of this comes off state the engine already holds, so the motion needs
        // no clock of its own and cannot drift out of step with the game.
        val since = (customer.max - customer.left).toFloat()
        val walk = (since / 0.75f).coerceIn(0f, 1f)
        val eased = 1f - (1f - walk) * (1f - walk)
        val x = DOOR_X + (slotX - DOOR_X) * eased

        val fretting = customer.patience < 0.35
        val rate = if (fretting) 7.2f else 2.6f
        val bob = sin(state.timeLeft * rate + index * 1.7).toFloat() * (if (fretting) 2.4f else 1.4f)
        val lean = if (fretting) sin(state.timeLeft * 3.4 + index).toFloat() * 1.8f - 2.5f else 0f

        Box(
            Modifier
                .offset(x.dp, (56f + bob).dp)
                .requiredSize(118.dp, 162.dp)
                .graphicsLayer(
                    alpha = walk,
                    rotationZ = lean,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                )
                .then(if (front) Modifier.noRippleClickable(onServe) else Modifier),
            contentAlignment = Alignment.BottomCenter,
        ) {
            CustomerArt(customer, width = 84.dp)
            Box(Modifier.offset(0.dp, (-46).dp), contentAlignment = Alignment.Center) {
                Heart(customer.patience.toFloat())
            }
        }
        // The ticket sits over their chest, the way the design has it.
        Box(
            Modifier
                .offset((slotX + 6f).dp, 110.dp)
                .requiredSize(106.dp, 66.dp)
                .graphicsLayer(alpha = ((walk - 0.6f) / 0.4f).coerceIn(0f, 1f)),
        ) {
            Ticket(customer, front, if (front) inHand else emptySet())
        }
    }
}

/** They come in and go out through the door, so that is where a walk starts. */
private const val DOOR_X = 716f

/**
 * The regular, drawn once in the design canvas and converted straight to a vector
 * drawable — so the person on the phone is the person on the character sheet. Only
 * the mouth is live, because it is the one part that answers to how long they have
 * been waiting.
 */
@Composable
private fun CustomerArt(customer: Customer, width: Dp) {
    val art = CAST[customer.id.mod(CAST.size)]
    Box(Modifier.requiredSize(width, width * 200f / 110f)) {
        Image(painterResource(art), contentDescription = null, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val k = size.width / 110f
            val path = Path().apply {
                if (customer.patience > 0.45) {
                    moveTo(43f * k, 92f * k)
                    relativeCubicTo(5f * k, 8f * k, 19f * k, 8f * k, 24f * k, 0f)
                } else {
                    moveTo(43f * k, 99f * k)
                    relativeCubicTo(5f * k, -8f * k, 19f * k, -8f * k, 24f * k, 0f)
                }
            }
            drawPath(path, Palette.Ink, style = Stroke(width = 4.2f * k, cap = StrokeCap.Round))
        }
    }
}

private val CAST = intArrayOf(
    R.drawable.cust_a, R.drawable.cust_b, R.drawable.cust_c, R.drawable.cust_d,
    R.drawable.cust_e, R.drawable.cust_f, R.drawable.cust_g, R.drawable.cust_h,
    R.drawable.cust_i, R.drawable.cust_j, R.drawable.cust_k, R.drawable.cust_l,
)

@Composable
private fun Ticket(customer: Customer, front: Boolean, onSelected: Set<Khodra>) {
    Box(
        Modifier
            .background(Palette.Paper, RoundedCornerShape(6.dp))
            .border(if (front) 2.5.dp else 1.5.dp, if (front) Color(0xFFC2593C) else Palette.Ink, RoundedCornerShape(6.dp))
            .padding(5.dp, 3.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                if (front) "NOW" else "NEXT",
                style = TextStyle(color = Color(0xFFC2593C), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Manousheh(size = 16.dp, topping = customer.wants, doneness = Doneness.PERFECT)
                BasicText(
                    "  ${customer.wants.label}",
                    style = TextStyle(color = Palette.Ink, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
            customer.khodra.forEach { k ->
                val met = k in onSelected
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(7.dp).clip(CircleShape).background(khodraInk(k))
                            .border(1.dp, if (met) Palette.Good else Palette.Ink, CircleShape)
                    )
                    BasicText(
                        " ${k.label}",
                        style = TextStyle(
                            color = if (met) Palette.Good else Color(0xFF7B6852),
                            fontSize = 7.5.sp, fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

/** Patience is a heart, not a bar — readable in the corner of the eye mid-rush. */
@Composable
private fun Heart(patience: Float) {
    val colour = when {
        patience > 0.55f -> Palette.Good
        patience > 0.25f -> Palette.Coin
        else -> Palette.Warn
    }
    Canvas(Modifier.requiredSize(24.dp, 22.dp)) {
        val path = heartPath(size.width, size.height)
        drawPath(path, Color(0x40000000))
        clipRect(top = size.height * (1f - patience)) { drawPath(path, colour) }
        drawPath(path, Palette.Ink, style = Stroke(width = 2.5.dp.toPx()))
    }
}

private fun heartPath(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.5f, h * 0.96f)
    cubicTo(w * -0.10f, h * 0.62f, w * 0.06f, h * 0.02f, w * 0.5f, h * 0.30f)
    cubicTo(w * 0.94f, h * 0.02f, w * 1.10f, h * 0.62f, w * 0.5f, h * 0.96f)
    close()
}

// ---------------------------------------------------------------- hud

/** The calendar, the sign and the coin pill are painted. Only the numbers are live. */
@Composable
private fun Hud(state: GameState) {
    Box(Modifier.at(CALENDAR).offset(0.dp, 22.dp), contentAlignment = Alignment.Center) {
        BasicText(
            "${state.day}",
            style = TextStyle(color = Palette.Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
        )
    }
    val urgent = state.timeLeft <= 10.0 && state.phase == DayPhase.RUNNING
    Box(Modifier.at(TIMER), contentAlignment = Alignment.Center) {
        Box(
            Modifier.requiredSize(64.dp, 26.dp).background(Color(0xFF16120C), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val total = state.timeLeft.toInt().coerceAtLeast(0)
            BasicText(
                "%d:%02d".format(total / 60, total % 60),
                style = TextStyle(
                    color = if (urgent) Color(0xFFFF6B4A) else Color(0xFF8AECAE),
                    fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
    Box(Modifier.at(COINS), contentAlignment = Alignment.CenterStart) {
        BasicText(
            "${state.purse}",
            style = TextStyle(color = Color(0xFFFDF6E4), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
        )
    }
    if (state.streak > 1) {
        Box(Modifier.offset(712.dp, 42.dp)) {
            BasicText(
                "x${state.streak} clean",
                style = TextStyle(color = Palette.Coin, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Drops(state: GameState, onCollect: (Int) -> Unit) {
    state.drops.forEach { drop ->
        val bob = sin(drop.left * 7.0).toFloat() * 3f
        val fading = drop.left < 2.0
        Box(
            Modifier
                .offset((214 + (drop.id % 4) * 58).dp, (196 + bob).dp)
                .requiredSize(46.dp, 46.dp)
                .graphicsLayer(alpha = if (fading) 0.4f + 0.6f * (drop.left / 2.0).toFloat() else 1f)
                .noRippleClickable { onCollect(drop.id) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFF6D06A), Color(0xFFD9A02E))))
                    .border(3.dp, Color(0xFFC2593C), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    "${drop.amount}",
                    style = TextStyle(color = Color(0xFF6B4A18), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
        }
    }
}

/**
 * What is in the hand, and where it may land.
 *
 * The ghost is the thing, not a token for it — whatever you picked up is drawn
 * properly, and for the peel that means the peel, held by its handle with the
 * blade reaching out ahead toward the furn. The target under your finger goes
 * green when the drop is legal and red when it is not, so the rules get learned
 * by moving rather than by being told.
 */
@Composable
private fun DragLayer(host: DragHost, state: GameState) {
    val carry = host.carry ?: return
    val target = hitTest(host.x, host.y, state.bench.size)
    val legal = isLegal(carry, target, state)

    rectFor(target)?.let { r ->
        val ink = if (legal) Palette.Good else Palette.Warn
        Box(
            Modifier
                .at(r)
                .background(ink.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .border(3.dp, ink, RoundedCornerShape(10.dp))
        )
    }

    if (carry is Carry.PeelLoad) {
        // Held by the grip, blade ahead of it, swinging level as it nears the mouth.
        val tilt = ((host.x - (FURN.x + FURN.w / 2f)) * 0.05f).coerceIn(-16f, 16f)
        Box(
            Modifier
                .offset((host.x - 22f).dp, (host.y - 112f).dp)
                .requiredSize(44.dp, 120.dp)
                .graphicsLayer(alpha = 0.95f, rotationZ = tilt, transformOrigin = TransformOrigin(0.5f, 0.94f)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val u = size.width / 44f
                drawRoundRect(
                    Brush.horizontalGradient(listOf(Color(0xFFE4E8EC), Color(0xFF8E939A))),
                    size = Size(44f * u, 100f * u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * u),
                )
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFFC08E5C), Color(0xFF6F4A2E))),
                    topLeft = Offset(16f * u, 94f * u),
                    size = Size(12f * u, 26f * u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * u),
                )
            }
            Column(
                Modifier.fillMaxSize().padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                state.peel.forEach { Manousheh(size = 28.dp, topping = it, doneness = null, topped = true) }
            }
        }
        return
    }

    val size = when (carry) {
        is Carry.Topped -> 60.dp
        is Carry.BakedItem -> 46.dp
        Carry.FurnLoad -> 44.dp
        is Carry.Veg -> 30.dp
        else -> 46.dp
    }
    Box(
        Modifier
            .offset((host.x - size.value / 2f).dp, (host.y - size.value / 2f).dp)
            .graphicsLayer(alpha = 0.92f),
    ) {
        when (carry) {
            Carry.Dough -> Box(
                Modifier.size(size).clip(CircleShape).background(Palette.DoughPale)
                    .border(3.dp, Palette.Ink, CircleShape)
            )
            is Carry.Topping -> Box(
                Modifier.size(size).clip(CircleShape).background(toppingInk(carry.topping))
                    .border(3.dp, Palette.Ink, CircleShape)
            )
            is Carry.Topped -> Manousheh(size = size, topping = carry.topping, doneness = null, topped = true)
            Carry.FurnLoad -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                state.furn?.items?.forEach {
                    Manousheh(size = size, topping = it, doneness = state.params().donenessAt(it, state.furn?.elapsed ?: 0.0))
                }
            }
            is Carry.BakedItem -> state.bench.getOrNull(carry.index)?.let {
                Manousheh(size = size, topping = it.topping, doneness = it.doneness)
            }
            is Carry.Veg -> Box(
                Modifier.size(size).clip(CircleShape).background(khodraInk(carry.khodra))
                    .border(2.5.dp, Palette.Ink, CircleShape)
            )
            Carry.PeelLoad -> Unit
        }
    }
}

/**
 * Draws whatever feedback is currently in flight. Everything here is read-only:
 * it can never swallow a tap or hold up the next action.
 */
@Composable
private fun FxLayer(fx: Effects) {
    val now = fx.clock
    fx.all.forEach { e ->
        val t = e.progress(now)
        when (e.kind) {
            FxKind.COIN -> {
                val ease = t * t
                val x = e.x + (e.toX - e.x) * ease
                val y = e.y + (e.toY - e.y) * ease - 40f * t * (1f - t)
                Box(
                    Modifier
                        .offset(x.dp, y.dp)
                        .requiredSize(40.dp)
                        .graphicsLayer(alpha = 1f - t * t, scaleX = 1f - 0.6f * t, scaleY = 1f - 0.6f * t),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(0xFFF6D06A), Color(0xFFD9A02E))))
                            .border(3.dp, Color(0xFFC2593C), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            "+${e.amount}",
                            style = TextStyle(color = Color(0xFF6B4A18), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                        )
                    }
                }
            }

            FxKind.POP -> {
                val ring = 1f + t * 0.9f
                Box(
                    Modifier
                        .offset((BENCH.x + BENCH.w / 2 - 30).dp, (BENCH.y + 12).dp)
                        .requiredSize(60.dp)
                        .graphicsLayer(alpha = (1f - t) * 0.8f, scaleX = ring, scaleY = ring)
                        .border(3.dp, Palette.Paper, CircleShape)
                )
            }

            FxKind.PEEL_IN -> {
                // The peel travelling up into the mouth. The design wants this pause
                // visible — it is the one animation allowed to read as deliberate.
                val ease = 1f - (1f - t) * (1f - t)
                val x = PEEL.x + (FURN.x + FURN.w / 2f - PEEL.w / 2f - PEEL.x) * ease
                val y = PEEL.y + (FURN.y + 96f - PEEL.y) * ease
                Box(
                    Modifier
                        .offset(x.dp, y.dp)
                        .requiredSize(PEEL.w.dp, 96.dp)
                        .graphicsLayer(alpha = (1f - t).coerceAtMost(0.85f), rotationZ = -6f * ease),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(
                            Brush.horizontalGradient(listOf(Color(0xFFE4E8EC), Color(0xFF8E939A))),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        )
                    }
                }
            }

            FxKind.LEAVE -> {
                val ease = 1f - (1f - t) * (1f - t)
                val x = e.x + (e.toX - e.x) * ease
                Box(
                    Modifier
                        .offset(x.dp, (e.y + sin(t * 18.0).toFloat() * 2f).dp)
                        .requiredSize(84.dp, 153.dp)
                        .graphicsLayer(
                            alpha = 1f - ease * ease,
                            rotationZ = if (e.happy) 0f else 4f,
                            transformOrigin = TransformOrigin(0.5f, 1f),
                        ),
                ) {
                    Image(
                        painterResource(CAST[e.art.coerceIn(0, CAST.size - 1)]),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(state: GameState) {
    Box(
        Modifier.offset(0.dp, 366.dp).requiredSize(STAGE_W.dp, 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.background(Color(0xCC1C120A), RoundedCornerShape(10.dp)).padding(12.dp, 3.dp)) {
            BasicText(
                state.note,
                style = TextStyle(color = Color(0xFFF4EDDD), fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Curtain(state: GameState, onAction: (Action) -> Unit) {
    if (state.phase == DayPhase.RUNNING) return
    val report = state.report
    Box(
        Modifier
            .requiredSize(STAGE_W.dp, STAGE_H.dp)
            .background(Color(0xE61C120A))
            // Before the first day, anywhere opens up. After one, the shop is on
            // screen and a stray tap must not skip it.
            .then(if (report == null) Modifier.noRippleClickable { onAction(Action.OpenShop) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (report == null) {
            // The front of the shop, straight off the design canvas. Only the two
            // numbers on it are live.
            Image(
                painter = painterResource(R.drawable.home_bg),
                contentDescription = null,
                modifier = Modifier.requiredSize(STAGE_W.dp, STAGE_H.dp),
                contentScale = ContentScale.FillBounds,
            )
            Box(Modifier.offset(46.dp, 24.dp)) {
                BasicText(
                    "${state.purse}",
                    style = TextStyle(color = Color(0xFFFDF6E4), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
            Box(Modifier.offset(778.dp, 25.dp)) {
                BasicText(
                    "${state.day}",
                    style = TextStyle(color = Color(0xFFFDF6E4), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
            return@Box
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                "Day ${state.day} — that is the morning gone",
                style = TextStyle(color = Palette.Paper, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
            )
            Box(Modifier.requiredSize(1.dp, 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Tally("SERVED", "${report.served}")
                Tally("WALKED OUT", "${report.walkedOut}", warn = report.walkedOut > 0)
                Tally("BINNED", "${report.binned}", warn = report.binned > 0)
                Tally("BEST RUN", "${report.bestStreak}")
                Tally("LEFT BEHIND", "${report.dropped}", warn = report.dropped > 0)
                Tally("IN THE PURSE", "${state.purse}", big = true)
            }
            Box(Modifier.requiredSize(1.dp, 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Upgrade.entries.forEach { u -> ShopCard(u, state) { onAction(Action.Buy(u)) } }
            }
            Box(Modifier.requiredSize(1.dp, 10.dp))
            Box(
                Modifier
                    .background(Palette.Good, RoundedCornerShape(16.dp))
                    .border(2.dp, Palette.Ink, RoundedCornerShape(16.dp))
                    .noRippleClickable { onAction(Action.OpenShop) }
                    .padding(22.dp, 7.dp),
            ) {
                BasicText(
                    "Open day ${state.day + 1}",
                    style = TextStyle(color = Palette.Paper, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
            BasicText(
                state.note,
                style = TextStyle(color = Color(0xFF9A8A72), fontSize = 10.sp),
            )
        }
    }
}

/** One thing on the board: what it does, what it costs, how far you have taken it. */
@Composable
private fun ShopCard(upgrade: Upgrade, state: GameState, onBuy: () -> Unit) {
    val level = state.upgrades.level(upgrade)
    val price = state.upgrades.priceOf(upgrade)
    val affordable = price != null && state.purse >= price
    Box(
        Modifier
            .requiredSize(128.dp, 104.dp)
            .background(if (affordable) Color(0xFF2E2418) else Color(0xFF241C13), RoundedCornerShape(8.dp))
            .border(
                2.dp,
                if (affordable) Palette.Coin else Color(0xFF463829),
                RoundedCornerShape(8.dp),
            )
            .then(if (affordable) Modifier.noRippleClickable(onBuy) else Modifier)
            .padding(8.dp),
    ) {
        Column {
            BasicText(
                upgrade.label,
                style = TextStyle(
                    color = if (affordable) Palette.Paper else Color(0xFF8A7A62),
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                ),
            )
            BasicText(
                upgrade.blurb,
                style = TextStyle(color = Color(0xFF9A8A72), fontSize = 8.sp, lineHeight = 10.sp),
            )
            Box(Modifier.requiredSize(1.dp, 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(upgrade.maxLevel) { i ->
                    Box(
                        Modifier.size(7.dp).clip(CircleShape)
                            .background(if (i < level) Palette.Good else Color(0xFF463829))
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            BasicText(
                price?.let { "$it coins" } ?: "owned",
                style = TextStyle(
                    color = if (price == null) Palette.Good else if (affordable) Palette.Coin else Color(0xFF7A6A52),
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
}

@Composable
private fun Tally(label: String, value: String, warn: Boolean = false, big: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            value,
            style = TextStyle(
                color = if (warn) Color(0xFFE0805E) else if (big) Palette.Coin else Palette.Paper,
                fontSize = if (big) 32.sp else 26.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        BasicText(label, style = TextStyle(color = Color(0xFF9A8A72), fontSize = 9.sp, fontWeight = FontWeight.Bold))
    }
}

// ---------------------------------------------------------------- pieces

private fun toppingInk(topping: Topping): Color =
    if (topping == Topping.JIBNEH) Palette.Jibneh else Palette.Zaatar

private fun khodraInk(khodra: Khodra): Color = when (khodra) {
    Khodra.TOMATO -> Color(0xFFC33B26)
    Khodra.CUCUMBER -> Color(0xFF4E7A2E)
    Khodra.OLIVES -> Color(0xFF4A5828)
    Khodra.PICKLES -> Color(0xFFA8A038)
    Khodra.MINT -> Color(0xFF2F6B2A)
    Khodra.LABNEH -> Color(0xFFE8E1CE)
}

/** One manousheh. [doneness] null means it has not been in the furn yet. */
@Composable
private fun Manousheh(size: Dp, topping: Topping?, doneness: Doneness?, topped: Boolean = topping != null) {
    Canvas(Modifier.requiredSize(size)) {
        val bread = when (doneness) {
            null, Doneness.RAW -> Palette.BreadRaw
            Doneness.PERFECT -> Palette.BreadPerfect
            Doneness.DONE -> Palette.BreadDone
            Doneness.BURNT -> Palette.BreadBurnt
        }
        val r = this.size.minDimension / 2
        drawCircle(bread, radius = r)
        drawCircle(Palette.Ink, radius = r, style = Stroke(width = 2.5.dp.toPx()))
        if (topped && topping != null) {
            val fill = if (doneness == Doneness.BURNT) Color(0xFF241C0C) else toppingInk(topping)
            drawCircle(fill, radius = r * 0.78f)
            if (topping == Topping.ZAATAR) sesame(r)
        }
    }
}

/** A handful of seeds, placed rather than randomised so it never flickers per frame. */
private fun DrawScope.sesame(radius: Float) {
    val seeds = listOf(
        -0.34f to -0.30f, 0.16f to -0.44f, 0.42f to 0.06f,
        -0.06f to 0.10f, -0.44f to 0.20f, 0.10f to 0.46f, 0.36f to 0.38f,
    )
    val centre = Offset(size.width / 2, size.height / 2)
    for ((dx, dy) in seeds) {
        drawCircle(Palette.Sesame, radius = radius * 0.055f, center = centre + Offset(dx * radius, dy * radius))
    }
}

@Composable
private fun Label(text: String, x: Int, y: Int, w: Int) {
    if (text.isEmpty()) return
    Box(Modifier.offset(x.dp, y.dp).requiredSize(w.dp, 12.dp), contentAlignment = Alignment.Center) {
        BasicText(
            text,
            style = TextStyle(
                color = Color(0xFFF4EDDD),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interaction, indication = null, onClick = onClick)
}
