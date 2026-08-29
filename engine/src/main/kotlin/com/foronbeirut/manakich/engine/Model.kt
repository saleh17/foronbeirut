package com.foronbeirut.manakich.engine

import kotlin.math.roundToInt

public enum class Topping(public val label: String) {
    ZAATAR("Zaatar"),
}

public enum class Doneness { RAW, PERFECT, DONE, BURNT }

/** What is sitting on the work board. The chain only ever moves forward. */
public sealed interface Board {
    public data object Empty : Board
    public data object Ball : Board
    public data object Flat : Board
    public data class Topped(val topping: Topping) : Board
}

public data class Bake(val topping: Topping, val elapsed: Double)

public data class Baked(val topping: Topping, val doneness: Doneness)

/**
 * Patience is one heart that drains, not a bar — [left] over [max] is the fill.
 * When it empties they walk, and that costs more than the coins did.
 */
public data class Customer(
    val id: Int,
    val wants: Topping,
    val left: Double,
    val max: Double,
) {
    public val patience: Double get() = if (max <= 0.0) 0.0 else (left / max).coerceIn(0.0, 1.0)
}

/**
 * Coins land on the counter where the customer stood and have to be picked up.
 * That is a deliberate cost, not a flourish: it is the tax the tip-jar upgrade
 * eventually buys you out of.
 */
public data class CoinDrop(val id: Int, val amount: Int, val left: Double)

public enum class DayPhase { READY, RUNNING, OVER }

public data class DayReport(
    val served: Int,
    val walkedOut: Int,
    val binned: Int,
    val earned: Int,
    val collected: Int,
    val bestStreak: Int,
) {
    /** What fell on the floor because nobody picked it up. */
    public val dropped: Int get() = earned - collected
}

/**
 * Everything the game is, at one instant. No behaviour lives here — [step] is the
 * only thing that produces a new one, which is what makes the rules testable.
 */
public data class GameState(
    val phase: DayPhase = DayPhase.READY,
    val timeLeft: Double = 0.0,
    val board: Board = Board.Empty,
    val furn: Bake? = null,
    val bench: Baked? = null,
    val queue: List<Customer> = emptyList(),
    val drops: List<CoinDrop> = emptyList(),
    val purse: Int = 0,
    val earned: Int = 0,
    val served: Int = 0,
    val walkedOut: Int = 0,
    val binned: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val report: DayReport? = null,
    val note: String = "Tap to open the shop",
    val nextCustomerId: Int = 1,
    val nextDropId: Int = 1,
    val spawnIn: Double = 0.0,
) {
    public val front: Customer? get() = queue.firstOrNull()
}

/**
 * The tuning, pulled out of the engine so upgrades can compile into a snapshot of
 * it later without the engine knowing upgrades exist.
 */
public data class GameParams(
    val bakeSeconds: Double = 6.0,
    val perfectWindow: Double = 1.4,
    val graceWindow: Double = 0.8,
    val price: Int = 8,
    val dayLength: Double = 90.0,
    val patienceSeconds: Double = 24.0,
    val queueMax: Int = 3,
    val firstCustomerAfter: Double = 1.0,
    val spawnEvery: Double = 8.0,
    val coinLife: Double = 6.0,
    val maxTip: Int = 4,
) {
    /**
     * The perfect window straddles [bakeSeconds]; [graceWindow] is the slack after it
     * where the manousheh is dark but still sellable. Past that it is charcoal.
     */
    public fun donenessAt(elapsed: Double): Doneness {
        val half = perfectWindow / 2.0
        return when {
            elapsed < bakeSeconds - half -> Doneness.RAW
            elapsed <= bakeSeconds + half -> Doneness.PERFECT
            elapsed <= bakeSeconds + half + graceWindow -> Doneness.DONE
            else -> Doneness.BURNT
        }
    }

    public fun payoutFor(doneness: Doneness): Int = when (doneness) {
        Doneness.PERFECT -> price
        Doneness.DONE -> (price * 0.6).roundToInt()
        Doneness.RAW, Doneness.BURNT -> 0
    }

    /** Speed is paid as a tip, so being quick reads as a bonus rather than as the wage. */
    public fun tipFor(patience: Double): Int = (maxTip * patience.coerceIn(0.0, 1.0)).roundToInt()
}

/** Everything the player can do. One per gesture, nothing implicit. */
public sealed interface Action {
    public data object OpenShop : Action
    public data object TakeDough : Action
    public data object Flatten : Action
    public data class Spread(val topping: Topping) : Action
    public data object IntoFurn : Action
    public data object OutOfFurn : Action
    public data object Serve : Action
    public data object Bin : Action
    public data class Collect(val dropId: Int) : Action
}
