package com.foronbeirut.manakich

import androidx.compose.runtime.mutableStateListOf
import com.foronbeirut.manakich.engine.GameState

/**
 * The half-second of feedback that tells you a tap registered.
 *
 * These live entirely on this side of the engine, and they are *observed* rather
 * than requested: the UI diffs one state against the next and spawns what that
 * change deserves. Nothing in the engine has to know an animation exists, and
 * nothing here can delay the next action — an effect is a thing being drawn, not
 * a thing being waited for. That is the rule from the design spec, and it is the
 * difference between a cooking game and a series of buttons.
 */
enum class FxKind { COIN, POP, PEEL_IN, LEAVE }

data class Fx(
    val kind: FxKind,
    val born: Double,
    val life: Double,
    val x: Float = 0f,
    val y: Float = 0f,
    val toX: Float = 0f,
    val toY: Float = 0f,
    val amount: Int = 0,
    val art: Int = 0,
    val happy: Boolean = true,
) {
    /** 0 at the moment it started, 1 when it is finished. */
    fun progress(now: Double): Float = ((now - born) / life).coerceIn(0.0, 1.0).toFloat()
}

class Effects {
    private val live = mutableStateListOf<Fx>()
    val all: List<Fx> get() = live

    var clock: Double = 0.0
        private set

    fun tick(dt: Double) {
        clock += dt
        if (live.isNotEmpty()) live.removeAll { clock - it.born >= it.life }
    }

    /** Reads what changed between two states and stages the feedback for it. */
    fun observe(was: GameState, now: GameState, dropAt: (Int) -> Pair<Float, Float>, slotAt: (Int) -> Float) {
        // A coin that was on the counter and is now in the purse flew there.
        if (now.purse > was.purse) {
            val gone = was.drops.filter { d -> now.drops.none { it.id == d.id } }
            gone.forEach { d ->
                val (x, y) = dropAt(d.id)
                live += Fx(FxKind.COIN, clock, 0.45, x, y, PURSE_X, PURSE_Y, amount = d.amount)
            }
        }
        // Something landed on the bench.
        if (now.bench.size > was.bench.size) {
            live += Fx(FxKind.POP, clock, 0.3)
        }
        // The peel went in. The one place the design wants a visible pause.
        if (was.furn == null && now.furn != null) {
            live += Fx(FxKind.PEEL_IN, clock, 0.5, amount = now.furn.items.size)
        }
        // Someone left — served or fed up, and they leave differently.
        val left = was.queue.filter { c -> now.queue.none { it.id == c.id } }
        left.forEach { c ->
            val index = was.queue.indexOfFirst { it.id == c.id }.coerceAtLeast(0)
            live += Fx(
                FxKind.LEAVE, clock, 0.6,
                x = slotAt(index), y = 56f,
                toX = DOOR, toY = 56f,
                art = c.id.mod(12),
                happy = now.served > was.served,
            )
        }
    }

    private companion object {
        const val PURSE_X = 740f
        const val PURSE_Y = 16f
        const val DOOR = 716f
    }
}
