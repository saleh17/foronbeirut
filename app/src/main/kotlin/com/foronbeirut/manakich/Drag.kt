package com.foronbeirut.manakich

import com.foronbeirut.manakich.engine.Action
import com.foronbeirut.manakich.engine.GameState
import com.foronbeirut.manakich.engine.Khodra

/**
 * Drag, as a second way to say the same things.
 *
 * The engine gains nothing from this file: a drop resolves to exactly the
 * [Action] the corresponding tap would have sent. Drag is what makes it feel
 * like cooking; tap is what makes it playable one-handed on a bus, and both stay
 * available because they are the same grammar underneath.
 */
sealed interface Carry {
    data object Dough : Carry
    data class Topping(val topping: com.foronbeirut.manakich.engine.Topping) : Carry
    data class Topped(val topping: com.foronbeirut.manakich.engine.Topping) : Carry
    data object PeelLoad : Carry
    data object FurnLoad : Carry
    data class BakedItem(val index: Int) : Carry
    data class Veg(val khodra: Khodra) : Carry
}

enum class Zone { BOARD, PEEL, FURN, BENCH, BIN, CUSTOMER, NOWHERE }

data class DropTarget(val zone: Zone, val index: Int = -1)

/** What is under the finger, in design units. */
fun hitTest(x: Float, y: Float, benchCount: Int): DropTarget {
    fun inside(r: Rect4) = x >= r.x && x <= r.x + r.w && y >= r.y && y <= r.y + r.h
    if (inside(BIN)) return DropTarget(Zone.BIN)
    if (inside(FURN)) return DropTarget(Zone.FURN)
    if (inside(PEEL)) return DropTarget(Zone.PEEL)
    if (inside(BOARD)) return DropTarget(Zone.BOARD)
    if (inside(BENCH)) {
        // Which of the three you are actually over, so khodra lands on the right one.
        val slot = ((x - BENCH.x) / 52f).toInt().coerceIn(0, (benchCount - 1).coerceAtLeast(0))
        return DropTarget(Zone.BENCH, if (benchCount == 0) -1 else slot)
    }
    CUSTOMER_X.forEachIndexed { i, cx ->
        if (x >= cx && x <= cx + 118 && y >= 40 && y <= 220) return DropTarget(Zone.CUSTOMER, i)
    }
    return DropTarget(Zone.NOWHERE)
}

/**
 * The whole drop grammar. Returning null means the drop is illegal, which is what
 * paints the target red — the rules get learned by moving rather than by being
 * told.
 */
fun actionFor(carry: Carry, target: DropTarget, state: GameState): Action? = when (carry) {
    Carry.Dough ->
        if (target.zone == Zone.BOARD) Action.TakeDough else null

    is Carry.Topping ->
        if (target.zone == Zone.BOARD) Action.Spread(carry.topping) else null

    is Carry.Topped -> when (target.zone) {
        Zone.PEEL -> Action.LoadPeel
        Zone.BIN -> Action.BinBoard
        else -> null
    }

    Carry.PeelLoad -> when (target.zone) {
        Zone.FURN -> Action.IntoFurn
        Zone.BIN -> Action.BinBoard
        else -> null
    }

    Carry.FurnLoad ->
        if (target.zone == Zone.BENCH || target.zone == Zone.PEEL) Action.OutOfFurn else null

    is Carry.BakedItem -> when {
        target.zone == Zone.BIN -> Action.BinBaked(carry.index)
        // Only the one at the counter can be served, and only what they asked for.
        target.zone == Zone.CUSTOMER && target.index == 0 -> Action.Serve(carry.index)
        else -> null
    }

    is Carry.Veg ->
        if (target.zone == Zone.BENCH && target.index >= 0 && target.index < state.bench.size) {
            Action.AddKhodra(target.index, carry.khodra)
        } else null
}

/** What the player can pick up from a place, given what is there right now. */
fun carryFrom(zone: Zone, state: GameState, index: Int = -1): Carry? = when (zone) {
    Zone.BOARD -> (state.board as? com.foronbeirut.manakich.engine.Board.Topped)?.let { Carry.Topped(it.topping) }
    Zone.PEEL -> if (state.peel.isNotEmpty()) Carry.PeelLoad else null
    Zone.FURN -> if (state.furn != null) Carry.FurnLoad else null
    Zone.BENCH -> if (index in state.bench.indices) Carry.BakedItem(index) else null
    else -> null
}

/** True when this carry could legally land here — what turns the target green. */
fun isLegal(carry: Carry, target: DropTarget, state: GameState): Boolean =
    actionFor(carry, target, state) != null

/** Where the highlight goes for a drop target. */
fun rectFor(target: DropTarget): Rect4? = when (target.zone) {
    Zone.BOARD -> BOARD
    Zone.PEEL -> PEEL
    Zone.FURN -> FURN
    Zone.BENCH -> BENCH
    Zone.BIN -> BIN
    Zone.CUSTOMER -> Rect4(CUSTOMER_X[target.index.coerceIn(0, 2)], 40, 118, 180)
    Zone.NOWHERE -> null
}
