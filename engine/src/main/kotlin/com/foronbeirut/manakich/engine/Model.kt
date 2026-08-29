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

public data class Customer(val id: Int, val wants: Topping)

/**
 * Everything the game is, at one instant. No behaviour lives here — [step] is the
 * only thing that produces a new one, which is what makes the rules testable.
 */
public data class GameState(
    val board: Board = Board.Empty,
    val furn: Bake? = null,
    val bench: Baked? = null,
    val customer: Customer = Customer(id = 1, wants = Topping.ZAATAR),
    val coins: Int = 0,
    val served: Int = 0,
    val binned: Int = 0,
    val note: String = "Tap the dough bowl",
)

/**
 * The tuning, pulled out of the engine so upgrades can compile into a snapshot of
 * it later without the engine knowing upgrades exist.
 */
public data class GameParams(
    val bakeSeconds: Double = 6.0,
    val perfectWindow: Double = 1.4,
    val graceWindow: Double = 0.8,
    val price: Int = 8,
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
}

/** Everything the player can do. One per gesture, nothing implicit. */
public sealed interface Action {
    public data object TakeDough : Action
    public data object Flatten : Action
    public data class Spread(val topping: Topping) : Action
    public data object IntoFurn : Action
    public data object OutOfFurn : Action
    public data object Serve : Action
    public data object Bin : Action
}
