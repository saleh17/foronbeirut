package com.foronbeirut.manakich

import androidx.compose.runtime.mutableStateListOf
import com.foronbeirut.manakich.engine.Board
import com.foronbeirut.manakich.engine.DayPhase
import com.foronbeirut.manakich.engine.GameState

/**
 * The half-second of feedback that tells you a tap registered.
 *
 * These live entirely on this side of the engine, and they are *observed* rather
 * than requested: the UI diffs one state against the next and stages what that
 * change deserves. Nothing in the engine knows an animation exists, and nothing
 * here can delay the next action — an effect is a thing being drawn, not a thing
 * being waited for.
 */
enum class FxKind { COIN, POP, PEEL_IN, PEEL_OUT, LEAVE, FLATTEN, WRAP }

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

    /** Set by the host so the same observed change can make a noise as well as a picture. */
    var onSound: ((SfxId) -> Unit)? = null

    var clock: Double = 0.0
        private set

    fun tick(dt: Double) {
        clock += dt
        if (live.isNotEmpty()) live.removeAll { clock - it.born >= it.life }
    }

    private fun say(id: SfxId) = onSound?.invoke(id)

    /** Reads what changed between two states and stages the feedback for it. */
    fun observe(was: GameState, now: GameState, dropAt: (Int) -> Pair<Float, Float>, slotAt: (Int) -> Float) {

        // ---- the board
        if (was.board == Board.Empty && now.board == Board.Ball) say(SfxId.DOUGH)
        if (was.board == Board.Ball && now.board == Board.Flat) {
            live += Fx(FxKind.FLATTEN, clock, 0.42)
            say(SfxId.DOUGH)
        }
        if (was.board == Board.Flat && now.board is Board.Topped) say(SfxId.TAP)

        // ---- the peel and the furn
        if (now.peel.size > was.peel.size) say(SfxId.TAP)
        // Both sides bound once: a public property from another module cannot be
        // smart-cast, and the engine is another module by design.
        val before = was.furn
        val after = now.furn
        if (before == null && after != null) {
            live += Fx(FxKind.PEEL_IN, clock, 0.5, amount = after.items.size)
            say(SfxId.SIZZLE)
        }
        if (before != null && after == null) {
            live += Fx(FxKind.PEEL_OUT, clock, 0.5, amount = before.items.size)
            say(SfxId.TAP)
        }

        // ---- the counter
        if (now.bench.size > was.bench.size) live += Fx(FxKind.POP, clock, 0.3)

        // ---- handing it over: paper first, then the coins
        if (now.served > was.served) {
            live += Fx(FxKind.WRAP, clock, 0.55, x = BENCH.x + 24f, y = BENCH.y.toFloat(), toX = slotAt(0), toY = 120f)
            say(SfxId.PAPER)
            say(SfxId.SERVE)
        }

        if (now.purse > was.purse) {
            was.drops.filter { d -> now.drops.none { it.id == d.id } }.forEach { d ->
                val (x, y) = dropAt(d.id)
                live += Fx(FxKind.COIN, clock, 0.45, x, y, PURSE_X, PURSE_Y, amount = d.amount)
            }
            say(SfxId.COIN)
        }

        // ---- someone left, served or fed up, and they leave differently
        was.queue.filter { c -> now.queue.none { it.id == c.id } }.forEach { c ->
            val index = was.queue.indexOfFirst { it.id == c.id }.coerceAtLeast(0)
            live += Fx(
                FxKind.LEAVE, clock, 0.6,
                x = slotAt(index), y = 56f, toX = DOOR, toY = 56f,
                art = c.id.mod(12), happy = now.served > was.served,
            )
        }

        // ---- the closing bell
        if (was.phase == DayPhase.RUNNING && now.phase == DayPhase.OVER) say(SfxId.BELL)
    }

    /** An action the engine refused: it only moved the hint line. */
    fun refused() = say(SfxId.NOPE)

    private companion object {
        const val PURSE_X = 740f
        const val PURSE_Y = 16f
        const val DOOR = 716f
    }
}
