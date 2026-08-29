package com.foronbeirut.manakich.engine

import kotlin.math.pow
import kotlin.math.roundToInt

public enum class Topping(public val label: String, public val arabic: String) {
    ZAATAR("Zaatar", "زعتر"),
    JIBNEH("Jibneh", "جبنة"),
}

/** Six separate sources, six separate drags. A customer asks for them by name. */
public enum class Khodra(public val label: String, public val arabic: String) {
    TOMATO("Tomato", "بندورة"),
    CUCUMBER("Cucumber", "خيار"),
    OLIVES("Olives", "زيتون"),
    PICKLES("Pickles", "كبيس"),
    MINT("Mint", "نعناع"),
    LABNEH("Labneh", "لبنة"),
}

public enum class Doneness { RAW, PERFECT, DONE, BURNT }

/** One topping's whole character: how long it wants, how forgiving it is, what it fetches. */
public data class Recipe(
    val bakeSeconds: Double,
    val perfectWindow: Double,
    val graceWindow: Double,
    val price: Int,
)

/** What is sitting on the work board. The chain only ever moves forward. */
public sealed interface Board {
    public data object Empty : Board
    public data object Ball : Board
    public data object Flat : Board
    public data class Topped(val topping: Topping) : Board
}

/**
 * A whole peel-load in the furn under **one** elapsed clock. Each manousheh is
 * judged against its own recipe, which is the entire point: a mixed load has no
 * moment that is right for both.
 */
public data class Bake(val items: List<Topping>, val elapsed: Double)

public data class Baked(
    val id: Int,
    val topping: Topping,
    val doneness: Doneness,
    val khodra: Set<Khodra> = emptySet(),
)

/**
 * Patience is one heart that drains, not a bar — [left] over [max] is the fill.
 * [khodra] is the rest of their order, and it is read off the ticket, not guessed.
 */
public data class Customer(
    val id: Int,
    val wants: Topping,
    val khodra: Set<Khodra> = emptySet(),
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
 *
 * [seed] makes the arrivals deterministic: the engine never calls a global random,
 * so a day replays identically and a balance sweep is reproducible.
 */
public data class GameState(
    val phase: DayPhase = DayPhase.READY,
    val timeLeft: Double = 0.0,
    val board: Board = Board.Empty,
    val peel: List<Topping> = emptyList(),
    val furn: Bake? = null,
    val bench: List<Baked> = emptyList(),
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
    val nextBakedId: Int = 1,
    val spawnIn: Double = 0.0,
    val seed: Long = 20260829L,
) {
    public val front: Customer? get() = queue.firstOrNull()
}

/**
 * The tuning, pulled out of the engine so upgrades can compile into a snapshot of
 * it later without the engine knowing upgrades exist.
 */
public data class GameParams(
    val recipes: Map<Topping, Recipe> = mapOf(
        // Zaatar is quick and forgiving. Akkawi has to actually melt, so jibneh
        // wants a second and a half longer — one ingredient, two clocks.
        Topping.ZAATAR to Recipe(bakeSeconds = 6.0, perfectWindow = 1.4, graceWindow = 0.8, price = 8),
        Topping.JIBNEH to Recipe(bakeSeconds = 7.5, perfectWindow = 1.5, graceWindow = 1.1, price = 14),
    ),
    val peelSlots: Int = 3,
    val dayLength: Double = 90.0,
    val patienceSeconds: Double = 30.0,
    val queueMax: Int = 3,
    val firstCustomerAfter: Double = 1.0,
    val spawnEvery: Double = 8.0,
    val coinLife: Double = 6.0,
    val maxTip: Int = 4,
    val khodraBonus: Int = 2,
    val khodraMissPenalty: Double = 0.22,
    val jibnehShare: Double = 0.4,
) {
    public fun recipe(topping: Topping): Recipe = recipes.getValue(topping)

    /**
     * The perfect window straddles the bake time; the grace is the slack after it
     * where the manousheh is dark but still sellable. Past that it is charcoal.
     */
    public fun donenessAt(topping: Topping, elapsed: Double): Doneness {
        val r = recipe(topping)
        val half = r.perfectWindow / 2.0
        return when {
            elapsed < r.bakeSeconds - half -> Doneness.RAW
            elapsed <= r.bakeSeconds + half -> Doneness.PERFECT
            elapsed <= r.bakeSeconds + half + r.graceWindow -> Doneness.DONE
            else -> Doneness.BURNT
        }
    }

    public fun payoutFor(topping: Topping, doneness: Doneness): Int = when (doneness) {
        Doneness.PERFECT -> recipe(topping).price
        Doneness.DONE -> (recipe(topping).price * 0.6).roundToInt()
        Doneness.RAW, Doneness.BURNT -> 0
    }

    /** Speed is paid as a tip, so being quick reads as a bonus rather than as the wage. */
    public fun tipFor(patience: Double): Int = (maxTip * patience.coerceIn(0.0, 1.0)).roundToInt()

    /**
     * Asked-for khodra that turned up pays [khodraBonus] each; every one you missed
     * takes [khodraMissPenalty] off the lot. Khodra nobody asked for is free and
     * harmless — putting olives on someone who did not want them costs nothing.
     */
    public fun settle(
        topping: Topping,
        doneness: Doneness,
        wanted: Set<Khodra>,
        given: Set<Khodra>,
        patience: Double,
    ): Int {
        val matched = wanted.intersect(given).size
        val missed = wanted.size - matched
        val gross = payoutFor(topping, doneness) + khodraBonus * matched + tipFor(patience)
        val after = gross * (1.0 - khodraMissPenalty).pow(missed)
        return after.roundToInt().coerceAtLeast(1)
    }
}

/** Everything the player can do. One per gesture, nothing implicit. */
public sealed interface Action {
    public data object OpenShop : Action
    public data object TakeDough : Action
    public data object Flatten : Action
    public data class Spread(val topping: Topping) : Action
    public data object LoadPeel : Action
    public data object IntoFurn : Action
    public data object OutOfFurn : Action
    public data class AddKhodra(val benchIndex: Int, val khodra: Khodra) : Action
    public data class Serve(val benchIndex: Int) : Action
    public data class BinBaked(val benchIndex: Int) : Action
    public data object BinBoard : Action
    public data class Collect(val dropId: Int) : Action
}
