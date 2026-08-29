package com.foronbeirut.manakich

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.sin

/** The design canvas is 844 x 390 units, so the app uses the same numbers the brief does. */
private const val STAGE_W = 844f
private const val STAGE_H = 390f

/**
 * The tap grammar, now that a bench holds more than one thing:
 * pick one up (tap it), dress it (tap khodra), hand it over (tap the customer).
 */
@Composable
fun StationScreen(state: GameState, params: GameParams, onAction: (Action) -> Unit) {
    var selected by remember { mutableStateOf(0) }
    val chosen = selected.coerceIn(0, (state.bench.size - 1).coerceAtLeast(0))

    Box(Modifier.fillMaxSize().background(Palette.FurnMouth), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val scale = minOf(maxWidth.value / STAGE_W, maxHeight.value / STAGE_H)
            Box(
                Modifier
                    .requiredSize(STAGE_W.dp, STAGE_H.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(Palette.Wall)
            ) {
                Wall()
                Furn(state, params, onIn = { onAction(Action.IntoFurn) }, onOut = { onAction(Action.OutOfFurn) })
                Queue(state, state.bench.getOrNull(chosen)?.khodra.orEmpty()) {
                    if (state.bench.isNotEmpty()) onAction(Action.Serve(chosen))
                }
                DayClock(state, params)
                Purse(state)
                Counter()

                Bin(x = 14, w = 56) {
                    if (state.bench.isNotEmpty() && state.board == Board.Empty) onAction(Action.BinBaked(chosen))
                    else onAction(Action.BinBoard)
                }
                Peel(state, params, x = 78, w = 104) { onAction(Action.IntoFurn) }
                Bench(state, chosen, x = 190, w = 186) { selected = it }
                KhodraBox(state, chosen, x = 384, w = 126) { onAction(Action.AddKhodra(chosen, it)) }
                WorkBoard(state, x = 518, w = 118) {
                    when (state.board) {
                        Board.Empty -> onAction(Action.TakeDough)
                        Board.Ball -> onAction(Action.Flatten)
                        Board.Flat -> Unit
                        is Board.Topped -> onAction(Action.LoadPeel)
                    }
                }
                Tray(Topping.ZAATAR, x = 644, w = 56) { onAction(Action.Spread(Topping.ZAATAR)) }
                Tray(Topping.JIBNEH, x = 706, w = 56) { onAction(Action.Spread(Topping.JIBNEH)) }
                DoughBowl(x = 770, w = 60) { onAction(Action.TakeDough) }

                Drops(state) { onAction(Action.Collect(it)) }
                Hint(state)
                Curtain(state) { onAction(Action.OpenShop) }
            }
        }
    }
}

// ---------------------------------------------------------------- room

@Composable
private fun Wall() {
    Box(
        Modifier
            .requiredSize(STAGE_W.dp, 252.dp)
            .background(Brush.verticalGradient(listOf(Palette.Wall, Palette.WallDark)))
    )
}

@Composable
private fun Counter() {
    Box(
        Modifier
            .offset(0.dp, 250.dp)
            .requiredSize(STAGE_W.dp, 140.dp)
            .background(Brush.verticalGradient(listOf(Palette.CounterTop, Palette.CounterEdge)))
    )
}

// ---------------------------------------------------------------- the furn

@Composable
private fun Furn(state: GameState, params: GameParams, onIn: () -> Unit, onOut: () -> Unit) {
    val loaded = state.furn
    Box(
        Modifier
            .offset(20.dp, 44.dp)
            .requiredSize(200.dp, 200.dp)
            .noRippleClickable { if (loaded == null) onIn() else onOut() }
    ) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(Palette.SteelDark).padding(10.dp)
        ) {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Palette.FurnMouth),
                contentAlignment = Alignment.Center,
            ) {
                Flames(hot = loaded != null)
                if (loaded != null) {
                    BakeRings(loaded.items.distinct(), loaded.elapsed, params)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        loaded.items.forEach {
                            Manousheh(size = 40.dp, topping = it, doneness = params.donenessAt(it, loaded.elapsed))
                        }
                    }
                }
            }
        }
    }
    Label("FURN", x = 20, y = 246, w = 200)
}

@Composable
private fun Flames(hot: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .requiredSize(150.dp, if (hot) 34.dp else 18.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Palette.Flame, Palette.FlameHot)),
                    RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                )
        )
    }
}

/**
 * One ring per topping in the load, sharing a single needle. That is the whole
 * mixed-load problem drawn: two green bands, one hand, and they never line up.
 */
@Composable
private fun BakeRings(toppings: List<Topping>, elapsed: Double, params: GameParams) {
    val span = 12.0
    Canvas(Modifier.requiredSize(160.dp)) {
        toppings.forEachIndexed { index, topping ->
            val recipe = params.recipe(topping)
            val stroke = Stroke(width = 8.dp.toPx())
            val inset = stroke.width / 2 + index * 13.dp.toPx()
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            fun band(from: Double, to: Double, colour: Color) {
                val start = (from / span * 360.0).toFloat()
                val sweep = ((to - from) / span * 360.0).toFloat()
                drawArc(colour, -90f + start, sweep, false, topLeft, arc, style = stroke)
            }

            val half = recipe.perfectWindow / 2
            band(0.0, span, Color(0x40000000))
            band(recipe.bakeSeconds - half, recipe.bakeSeconds + half, Palette.Good)
            band(recipe.bakeSeconds + half, recipe.bakeSeconds + half + recipe.graceWindow, Palette.Coin)
            band(0.0, elapsed.coerceAtMost(span), toppingInk(topping).copy(alpha = 0.95f))
        }
    }
}

// ---------------------------------------------------------------- counter

@Composable
private fun Peel(state: GameState, params: GameParams, x: Int, w: Int, onSend: () -> Unit) {
    val full = state.peel.size >= params.peelSlots
    Box(
        Modifier
            .offset(x.dp, 258.dp)
            .requiredSize(w.dp, 100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(Palette.Steel, Palette.SteelDark)))
            .border(if (full) 4.dp else 3.dp, if (full) Palette.Select else Palette.Ink, RoundedCornerShape(8.dp))
            .noRippleClickable { if (state.peel.isNotEmpty()) onSend() },
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(params.peelSlots) { slot ->
                val item = state.peel.getOrNull(slot)
                if (item == null) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape)
                            .border(2.dp, Color(0x552A1B12), CircleShape)
                    )
                } else {
                    Manousheh(size = 28.dp, topping = item, doneness = null, topped = true)
                }
            }
        }
    }
    Label(if (state.peel.isEmpty()) "PEEL" else "TAP: INTO THE FURN", x, 360, w)
}

@Composable
private fun Bench(state: GameState, chosen: Int, x: Int, w: Int, onPick: (Int) -> Unit) {
    Box(
        Modifier
            .offset(x.dp, 258.dp)
            .requiredSize(w.dp, 100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Paper)
            .border(3.dp, Palette.Ink, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (state.bench.isEmpty()) {
            BasicText(
                "nothing baked",
                style = TextStyle(color = Color(0xFFA89A80), fontSize = 11.sp),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.bench.forEachIndexed { index, baked ->
                    BenchItem(baked, picked = index == chosen) { onPick(index) }
                }
            }
        }
    }
    Label("PICK ONE UP", x, 360, w)
}

@Composable
private fun BenchItem(baked: Baked, picked: Boolean, onPick: () -> Unit) {
    Box(
        Modifier
            .size(54.dp)
            .then(if (picked) Modifier.border(3.dp, Palette.Select, CircleShape) else Modifier)
            .noRippleClickable(onPick),
        contentAlignment = Alignment.Center,
    ) {
        Manousheh(size = 46.dp, topping = baked.topping, doneness = baked.doneness)
        // What is already on it, as beads round the rim — readable without a legend.
        Row(
            Modifier.offset(0.dp, 20.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            baked.khodra.forEach {
                Box(Modifier.size(7.dp).clip(CircleShape).background(khodraInk(it)).border(1.dp, Palette.Ink, CircleShape))
            }
        }
    }
}

@Composable
private fun KhodraBox(state: GameState, chosen: Int, x: Int, w: Int, onAdd: (Khodra) -> Unit) {
    val on = state.bench.getOrNull(chosen)?.khodra.orEmpty()
    val wanted = state.front?.khodra.orEmpty()
    Box(
        Modifier
            .offset(x.dp, 258.dp)
            .requiredSize(w.dp, 100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Steel)
            .border(3.dp, Palette.SteelDark, RoundedCornerShape(8.dp))
            .padding(5.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Khodra.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier
                                .requiredSize(36.dp, 41.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(khodraInk(k))
                                .then(
                                    when {
                                        k in on -> Modifier.border(2.5.dp, Palette.Good, RoundedCornerShape(4.dp))
                                        k in wanted -> Modifier.border(2.5.dp, Palette.Select, RoundedCornerShape(4.dp))
                                        else -> Modifier
                                    }
                                )
                                .noRippleClickable { onAdd(k) },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            BasicText(
                                k.arabic,
                                style = TextStyle(color = Color(0xE6FFFFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
    Label("KHODRA  +2 EACH", x, 360, w)
}

@Composable
private fun WorkBoard(state: GameState, x: Int, w: Int, onTap: () -> Unit) {
    Box(
        Modifier
            .offset(x.dp, 258.dp)
            .requiredSize(w.dp, 100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFC49A6A), Color(0xFF9A754A))))
            .border(3.dp, Palette.Ink, RoundedCornerShape(10.dp))
            .noRippleClickable(onTap),
        contentAlignment = Alignment.Center,
    ) {
        when (state.board) {
            Board.Empty -> Unit
            Board.Ball -> Box(
                Modifier.size(38.dp).clip(CircleShape).background(Palette.DoughPale)
                    .border(3.dp, Palette.Ink, CircleShape)
            )
            Board.Flat -> Manousheh(size = 72.dp, topping = null, doneness = null)
            is Board.Topped -> Manousheh(
                size = 72.dp,
                topping = (state.board as Board.Topped).topping,
                doneness = null,
                topped = true,
            )
        }
    }
    Label(
        when (state.board) {
            Board.Empty -> "BOARD"
            Board.Ball -> "TAP TO FLATTEN"
            Board.Flat -> "PICK A TOPPING"
            is Board.Topped -> "TAP: ONTO THE PEEL"
        },
        x, 360, w,
    )
}

@Composable
private fun Tray(topping: Topping, x: Int, w: Int, onTap: () -> Unit) {
    Box(
        Modifier
            .offset(x.dp, 272.dp)
            .requiredSize(w.dp, 72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.Steel)
            .border(3.dp, Palette.SteelDark, RoundedCornerShape(8.dp))
            .padding(6.dp)
            .noRippleClickable(onTap),
    ) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                .background(Brush.verticalGradient(listOf(toppingInk(topping), toppingShade(topping)))),
            contentAlignment = Alignment.BottomCenter,
        ) {
            BasicText(
                topping.arabic,
                style = TextStyle(
                    color = if (topping == Topping.JIBNEH) Palette.Ink else Color(0xE6FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
    Label(topping.label.uppercase(), x, 348, w)
}

@Composable
private fun DoughBowl(x: Int, w: Int, onTap: () -> Unit) {
    Box(
        Modifier
            .offset(x.dp, 272.dp)
            .requiredSize(w.dp, 72.dp)
            .clip(RoundedCornerShape(12.dp, 12.dp, 40.dp, 40.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFC07C51), Color(0xFF63361F))))
            .border(4.dp, Palette.Ink, RoundedCornerShape(12.dp, 12.dp, 40.dp, 40.dp))
            .noRippleClickable(onTap),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(2) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(Palette.DoughPale)
                        .border(2.dp, Palette.Ink, CircleShape)
                )
            }
        }
    }
    Label("DOUGH", x, 348, w)
}

@Composable
private fun Bin(x: Int, w: Int, onTap: () -> Unit) {
    Box(
        Modifier
            .offset(x.dp, 272.dp)
            .requiredSize(w.dp, 82.dp)
            .clip(RoundedCornerShape(6.dp, 6.dp, 12.dp, 12.dp))
            .background(Color(0xFF5A5F65))
            .border(3.dp, Palette.Ink, RoundedCornerShape(6.dp, 6.dp, 12.dp, 12.dp))
            .noRippleClickable(onTap)
    )
    Label("BIN", x, 358, w)
}

// ---------------------------------------------------------------- the queue

@Composable
private fun Queue(state: GameState, inHand: Set<Khodra>, onServe: () -> Unit) {
    state.queue.take(3).forEachIndexed { index, customer ->
        val front = index == 0
        Box(
            Modifier
                .offset((296 + index * 134).dp, (34 + index * 6).dp)
                .requiredSize(128.dp, 214.dp)
                .then(if (front) Modifier.noRippleClickable(onServe) else Modifier)
                .graphicsLayer(alpha = if (front) 1f else 0.7f),
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Ticket(customer, front, if (front) inHand else emptySet())
                Heart(customer.patience.toFloat())
                Box(
                    Modifier.size(if (front) 50.dp else 44.dp).clip(CircleShape)
                        .background(Color(0xFFC89A6E)).border(3.dp, Palette.Ink, CircleShape)
                )
                Box(
                    Modifier
                        .requiredSize(if (front) 88.dp else 76.dp, if (front) 66.dp else 58.dp)
                        .background(Color(0xFF6FA3C4), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                        .border(3.dp, Palette.Ink, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                )
            }
        }
    }
}

/**
 * The order as a ticket rather than a row of swatches: what they want in words,
 * and the khodra block only when khodra was asked for.
 */
@Composable
private fun Ticket(customer: Customer, front: Boolean, onSelected: Set<Khodra>) {
    Box(
        Modifier
            .background(Palette.Paper, RoundedCornerShape(6.dp))
            .border(if (front) 2.5.dp else 1.5.dp, if (front) Color(0xFFC2593C) else Palette.Ink, RoundedCornerShape(6.dp))
            .padding(6.dp, 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Manousheh(size = 18.dp, topping = customer.wants, doneness = Doneness.PERFECT)
                BasicText(
                    "  ${customer.wants.label}",
                    style = TextStyle(color = Palette.Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
            if (customer.khodra.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    customer.khodra.forEach { k ->
                        val met = k in onSelected
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(8.dp).clip(CircleShape).background(khodraInk(k))
                                    .border(1.dp, if (met) Palette.Good else Palette.Ink, CircleShape)
                            )
                            BasicText(
                                " ${k.label}",
                                style = TextStyle(
                                    color = if (met) Palette.Good else Color(0xFF7B6852),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
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
    Canvas(Modifier.requiredSize(26.dp, 24.dp).padding(bottom = 4.dp)) {
        val path = heartPath(size.width, size.height)
        drawPath(path, Color(0x33000000))
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

@Composable
private fun DayClock(state: GameState, params: GameParams) {
    val fraction = (state.timeLeft / params.dayLength).coerceIn(0.0, 1.0).toFloat()
    val urgent = state.timeLeft <= 10.0 && state.phase == DayPhase.RUNNING
    Box(
        Modifier.offset(18.dp, 10.dp).requiredSize(200.dp, 26.dp)
            .background(Palette.HintBg, RoundedCornerShape(13.dp)).padding(4.dp),
    ) {
        Box(
            Modifier
                .requiredSize((192 * fraction).dp.coerceAtLeast(0.dp), 18.dp)
                .background(if (urgent) Palette.Warn else Palette.Good, RoundedCornerShape(9.dp))
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText(
                "${state.timeLeft.toInt()}s",
                style = TextStyle(color = Palette.HintInk, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Purse(state: GameState) {
    Box(
        Modifier.offset(650.dp, 10.dp).requiredSize(176.dp, 40.dp)
            .background(Palette.HintBg, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(Palette.Coin))
            BasicText(
                "  ${state.purse}   ·   ${state.served} served" + if (state.streak > 1) "   ·   x${state.streak}" else "",
                style = TextStyle(color = Palette.HintInk, fontSize = 13.sp, fontWeight = FontWeight.Bold),
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
                .offset((292 + (drop.id % 4) * 62).dp, (248 + bob).dp)
                .requiredSize(44.dp, 44.dp)
                .graphicsLayer(alpha = if (fading) 0.45f + 0.55f * (drop.left / 2.0).toFloat() else 1f)
                .noRippleClickable { onCollect(drop.id) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Palette.Coin)
                    .border(3.dp, Palette.Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    "${drop.amount}",
                    style = TextStyle(color = Palette.Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
                )
            }
        }
    }
}

@Composable
private fun Hint(state: GameState) {
    Box(
        Modifier.offset(0.dp, 372.dp).requiredSize(STAGE_W.dp, 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.background(Palette.HintBg, RoundedCornerShape(9.dp)).padding(12.dp, 2.dp)) {
            BasicText(
                state.note,
                style = TextStyle(color = Palette.HintInk, fontSize = 12.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Curtain(state: GameState, onOpen: () -> Unit) {
    if (state.phase == DayPhase.RUNNING) return
    Box(
        Modifier.requiredSize(STAGE_W.dp, STAGE_H.dp).background(Color(0xE61C120A)).noRippleClickable(onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val report = state.report
            if (report == null) {
                BasicText(
                    "Sabah el kheir",
                    style = TextStyle(color = Palette.Paper, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
                )
                BasicText(
                    "Ninety seconds. Tap to open the shop.",
                    style = TextStyle(color = Palette.HintInk, fontSize = 15.sp),
                )
            } else {
                BasicText(
                    "That is the morning gone",
                    style = TextStyle(color = Palette.Paper, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                )
                Box(Modifier.requiredSize(1.dp, 14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Tally("SERVED", "${report.served}")
                    Tally("WALKED OUT", "${report.walkedOut}", warn = report.walkedOut > 0)
                    Tally("BINNED", "${report.binned}", warn = report.binned > 0)
                    Tally("BEST RUN", "${report.bestStreak}")
                }
                Box(Modifier.requiredSize(1.dp, 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Tally("IN THE PURSE", "${report.collected}", big = true)
                    Tally("LEFT ON THE COUNTER", "${report.dropped}", warn = report.dropped > 0)
                }
                Box(Modifier.requiredSize(1.dp, 18.dp))
                BasicText(
                    "Tap for another day",
                    style = TextStyle(color = Palette.HintInk, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                )
            }
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

private fun toppingShade(topping: Topping): Color =
    if (topping == Topping.JIBNEH) Palette.JibnehDark else Palette.ZaatarDark

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
    Box(Modifier.offset(x.dp, y.dp).requiredSize(w.dp, 13.dp), contentAlignment = Alignment.Center) {
        BasicText(
            text,
            style = TextStyle(
                color = Palette.Ink,
                fontSize = 8.5.sp,
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
