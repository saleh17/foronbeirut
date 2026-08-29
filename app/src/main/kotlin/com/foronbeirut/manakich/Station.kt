package com.foronbeirut.manakich

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foronbeirut.manakich.engine.Action
import com.foronbeirut.manakich.engine.Board
import com.foronbeirut.manakich.engine.Doneness
import com.foronbeirut.manakich.engine.GameParams
import com.foronbeirut.manakich.engine.GameState
import com.foronbeirut.manakich.engine.Topping

/** The design canvas is 844 x 390 units, so the app uses the same numbers the brief does. */
private const val STAGE_W = 844f
private const val STAGE_H = 390f

@Composable
fun StationScreen(state: GameState, params: GameParams, onAction: (Action) -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.FurnMouth), contentAlignment = Alignment.Center) {
        BoxWithScale { scale ->
            Box(
                Modifier
                    .requiredSize(STAGE_W.dp, STAGE_H.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(Palette.Wall)
            ) {
                Wall()
                Furn(state, params) { onAction(Action.IntoFurn) }
                FurnOut(state) { onAction(Action.OutOfFurn) }
                CustomerBox { onAction(Action.Serve) }
                Coins(state)
                Counter()
                Bin { onAction(Action.Bin) }
                Bench(state) { onAction(Action.Serve) }
                WorkBoard(state) {
                    when (state.board) {
                        Board.Ball -> onAction(Action.Flatten)
                        is Board.Topped -> onAction(Action.IntoFurn)
                        else -> onAction(Action.TakeDough)
                    }
                }
                ZaatarTray { onAction(Action.Spread(Topping.ZAATAR)) }
                DoughBowl { onAction(Action.TakeDough) }
                Hint(state)
            }
        }
    }
}

@Composable
private fun BoxWithScale(content: @Composable (Float) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val scale = minOf(maxWidth.value / STAGE_W, maxHeight.value / STAGE_H)
        content(scale)
    }
}

// ---------------------------------------------------------------- scene

@Composable
private fun Wall() {
    Box(
        Modifier
            .offset(0.dp, 0.dp)
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

@Composable
private fun Furn(state: GameState, params: GameParams, onTap: () -> Unit) {
    Prop(x = 20, y = 40, w = 200, h = 210, label = "FURN", onTap = onTap) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.SteelDark)
                .padding(10.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.FurnMouth),
                contentAlignment = Alignment.Center,
            ) {
                Flames(hot = state.furn != null)
                val baking = state.furn
                if (baking != null) {
                    BakeRing(elapsed = baking.elapsed, params = params)
                    Manousheh(size = 74.dp, doneness = params.donenessAt(baking.elapsed))
                }
            }
        }
    }
}

/** An invisible second target over the furn: tap it to pull the load back out. */
@Composable
private fun FurnOut(state: GameState, onTap: () -> Unit) {
    if (state.furn == null) return
    Box(
        Modifier
            .offset(20.dp, 40.dp)
            .requiredSize(200.dp, 210.dp)
            .noRippleClickable(onTap)
    )
}

@Composable
private fun Flames(hot: Boolean) {
    val height = if (hot) 34.dp else 18.dp
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .requiredSize(150.dp, height)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Palette.Flame, Palette.FlameHot)
                    ),
                    RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                )
        )
    }
}

@Composable
private fun BakeRing(elapsed: Double, params: GameParams) {
    // The whole timing rule, drawn: the green band is the perfect window, the amber
    // tail is the grace, and the needle is where this one actually is.
    val total = (params.bakeSeconds + params.perfectWindow / 2 + params.graceWindow + 1.5)
    Canvas(Modifier.requiredSize(118.dp)) {
        val stroke = Stroke(width = 9.dp.toPx())
        val inset = stroke.width / 2
        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
        val topLeft = Offset(inset, inset)

        fun sweep(from: Double, to: Double, color: Color) {
            val a = (from / total * 360.0).toFloat()
            val b = ((to - from) / total * 360.0).toFloat()
            drawArc(color, -90f + a, b, false, topLeft, arcSize, style = stroke)
        }

        val half = params.perfectWindow / 2
        sweep(0.0, total, Color(0x33000000))
        sweep(params.bakeSeconds - half, params.bakeSeconds + half, Palette.Good)
        sweep(
            params.bakeSeconds + half,
            params.bakeSeconds + half + params.graceWindow,
            Palette.Coin,
        )
        sweep(0.0, elapsed.coerceAtMost(total), Color(0xCCF7F1E2))
    }
}

@Composable
private fun CustomerBox(onTap: () -> Unit) {
    Prop(x = 300, y = 54, w = 130, h = 196, label = "WAITING", onTap = onTap) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC89A6E))
                    .border(3.dp, Palette.Ink, CircleShape)
            )
            Box(
                Modifier
                    .requiredSize(96.dp, 92.dp)
                    .background(
                        Color(0xFF6FA3C4),
                        RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
                    )
                    .border(
                        3.dp,
                        Palette.Ink,
                        RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Manousheh(size = 40.dp, doneness = Doneness.PERFECT)
            }
        }
    }
}

@Composable
private fun Coins(state: GameState) {
    Box(
        Modifier
            .offset(676.dp, 16.dp)
            .requiredSize(150.dp, 44.dp)
            .background(Palette.HintBg, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(Palette.Coin))
            BasicText(
                "  ${state.coins}   ·   ${state.served} served",
                style = TextStyle(color = Palette.HintInk, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Bin(onTap: () -> Unit) {
    Prop(x = 20, y = 282, w = 70, h = 92, label = "BIN", onTap = onTap) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp, 6.dp, 12.dp, 12.dp))
                .background(Color(0xFF5A5F65))
                .border(3.dp, Palette.Ink, RoundedCornerShape(6.dp, 6.dp, 12.dp, 12.dp))
        )
    }
}

@Composable
private fun Bench(state: GameState, onTap: () -> Unit) {
    Prop(x = 110, y = 268, w = 130, h = 104, label = "HAND IT OVER", onTap = onTap) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Palette.Paper)
                .border(3.dp, Palette.Ink, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            state.bench?.let { Manousheh(size = 78.dp, doneness = it.doneness) }
        }
    }
}

@Composable
private fun WorkBoard(state: GameState, onTap: () -> Unit) {
    val label = when (state.board) {
        Board.Empty -> "BOARD"
        Board.Ball -> "TAP TO FLATTEN"
        Board.Flat -> "SPREAD THE ZAATAR"
        is Board.Topped -> "TAP FOR THE FURN"
    }
    Prop(x = 330, y = 268, w = 170, h = 104, label = label, onTap = onTap) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFC49A6A), Color(0xFF9A754A))))
                .border(3.dp, Palette.Ink, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when (state.board) {
                Board.Empty -> Unit
                Board.Ball -> Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Palette.DoughPale)
                        .border(3.dp, Palette.Ink, CircleShape)
                )
                Board.Flat -> Manousheh(size = 82.dp, doneness = null)
                is Board.Topped -> Manousheh(size = 82.dp, doneness = Doneness.RAW, topped = true)
            }
        }
    }
}

@Composable
private fun ZaatarTray(onTap: () -> Unit) {
    Prop(x = 530, y = 280, w = 120, h = 82, label = "ZAATAR", onTap = onTap) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Palette.Steel)
                .border(3.dp, Palette.SteelDark, RoundedCornerShape(8.dp))
                .padding(7.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(Palette.Zaatar, Palette.ZaatarDark)))
            )
        }
    }
}

@Composable
private fun DoughBowl(onTap: () -> Unit) {
    Prop(x = 680, y = 280, w = 140, h = 82, label = "DOUGH", onTap = onTap) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp, 12.dp, 54.dp, 54.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFC07C51), Color(0xFF63361F))))
                .border(4.dp, Palette.Ink, RoundedCornerShape(12.dp, 12.dp, 54.dp, 54.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Palette.DoughPale)
                            .border(2.dp, Palette.Ink, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(state: GameState) {
    Box(
        Modifier.offset(0.dp, 358.dp).requiredSize(STAGE_W.dp, 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.background(Palette.HintBg, RoundedCornerShape(15.dp)).padding(14.dp, 5.dp),
        ) {
            BasicText(
                state.note,
                style = TextStyle(color = Palette.HintInk, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

// ---------------------------------------------------------------- pieces

/**
 * One manousheh. [doneness] null means bare dough; [topped] paints the zaatar on.
 */
@Composable
private fun Manousheh(size: Dp, doneness: Doneness?, topped: Boolean = doneness != null) {
    Canvas(Modifier.requiredSize(size)) {
        val bread = when (doneness) {
            null, Doneness.RAW -> Palette.BreadRaw
            Doneness.PERFECT -> Palette.BreadPerfect
            Doneness.DONE -> Palette.BreadDone
            Doneness.BURNT -> Palette.BreadBurnt
        }
        val r = this.size.minDimension / 2
        drawCircle(bread, radius = r)
        drawCircle(Palette.Ink, radius = r, style = Stroke(width = 3.dp.toPx()))
        if (topped) {
            val zaatar = if (doneness == Doneness.BURNT) Color(0xFF241C0C) else Palette.Zaatar
            drawCircle(zaatar, radius = r * 0.78f)
            sesame(r)
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
        drawCircle(
            Palette.Sesame,
            radius = radius * 0.055f,
            center = centre + Offset(dx * radius, dy * radius),
        )
    }
}

/** A station on the counter: positioned in design units, tappable, with its label under it. */
@Composable
private fun Prop(
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    label: String,
    onTap: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.offset(x.dp, y.dp).requiredSize(w.dp, h.dp).noRippleClickable(onTap)) {
        content()
    }
    Box(
        Modifier.offset(x.dp, (y + h + 2).dp).requiredSize(w.dp, 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = Palette.Ink,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = androidx.compose.runtime.remember { MutableInteractionSource() }
    return this.clickable(interaction, indication = null, onClick = onClick)
}
