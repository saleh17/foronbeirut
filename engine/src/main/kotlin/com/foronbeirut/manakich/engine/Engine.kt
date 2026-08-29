package com.foronbeirut.manakich.engine

/**
 * The whole game, as one pure function: `(state, params, dt, actions) -> state`.
 *
 * Nothing in here touches a clock, a framework, a random or the screen. The caller
 * owns the frame loop and hands in how much time passed; the engine only decides
 * what that means. That is what lets the rules be tested in milliseconds and lets
 * the same code back a phone, a desktop preview or a headless balance sweep.
 */
public fun step(
    state: GameState,
    params: GameParams,
    dt: Double,
    actions: List<Action> = emptyList(),
): GameState {
    var next = if (state.phase == DayPhase.RUNNING && dt > 0.0) tick(state, params, dt) else state
    for (action in actions) next = apply(next, params, action)
    return next
}

// ---------------------------------------------------------------- time

private fun tick(state: GameState, params: GameParams, dt: Double): GameState {
    var next = state

    // The bake advances on the same frame the bell rings, so one left in the furn
    // at the close still takes that last moment of heat.
    next.furn?.let { next = next.copy(furn = it.copy(elapsed = it.elapsed + dt)) }

    next = drainPatience(next, dt)
    next = expireDrops(next, params, dt)
    next = spawn(next, params, dt)

    val timeLeft = next.timeLeft - dt
    return if (timeLeft > 0.0) next.copy(timeLeft = timeLeft) else close(next.copy(timeLeft = 0.0))
}

/** Everyone in the queue is waiting, not only the one at the counter. */
private fun drainPatience(state: GameState, dt: Double): GameState {
    if (state.queue.isEmpty()) return state
    val drained = state.queue.map { it.copy(left = it.left - dt) }
    val staying = drained.filter { it.left > 0.0 }
    val walked = drained.size - staying.size
    if (walked == 0) return state.copy(queue = drained)
    return state.copy(
        queue = staying,
        walkedOut = state.walkedOut + walked,
        streak = 0,
        note = if (walked == 1) "One walked out" else "$walked walked out",
    )
}

private fun expireDrops(state: GameState, params: GameParams, dt: Double): GameState {
    if (state.drops.isEmpty()) return state
    val aged = state.drops.map { it.copy(left = it.left - dt) }
    val after = params.autoCollectAfter
    if (after == null) return state.copy(drops = aged.filter { it.left > 0.0 })
    val (taken, waiting) = aged.partition { params.coinLife - it.left >= after }
    return state.copy(
        drops = waiting.filter { it.left > 0.0 },
        purse = state.purse + taken.sumOf { it.amount },
    )
}

private fun spawn(state: GameState, params: GameParams, dt: Double): GameState {
    val due = state.spawnIn - dt
    if (due > 0.0) return state.copy(spawnIn = due)
    // A full queue holds the door rather than banking arrivals, so a bad patch
    // never turns into a wave the player could not have prevented.
    if (state.queue.size >= params.queueMax) return state.copy(spawnIn = 0.0)

    var seed = state.seed
    fun roll(): Double {
        seed = seed * 6364136223846793005L + 1442695040888963407L
        return ((seed ushr 11).toDouble() / (1L shl 53).toDouble()).let { if (it < 0) it + 1.0 else it }
    }

    val menu = params.menu.ifEmpty { listOf(Topping.ZAATAR) }
    val wants = if (menu.size > 1 && roll() < params.jibnehShare) Topping.JIBNEH else menu.first()
    // Difficulty comes from a longer order, not a faster clock.
    val extras = roll().let { if (it < params.khodraOne) 1 else if (it < params.khodraOne + params.khodraTwo) 2 else 0 }
    val khodra = if (extras == 0) emptySet() else {
        val all = Khodra.entries
        buildSet { while (size < extras) add(all[(roll() * all.size).toInt().coerceIn(0, all.size - 1)]) }
    }

    return state.copy(
        queue = state.queue + Customer(
            id = state.nextCustomerId,
            wants = wants,
            khodra = khodra,
            left = params.patienceSeconds,
            max = params.patienceSeconds,
        ),
        nextCustomerId = state.nextCustomerId + 1,
        spawnIn = params.spawnEvery,
        seed = seed,
    )
}

private fun close(state: GameState): GameState = state.copy(
    phase = DayPhase.OVER,
    queue = emptyList(),
    drops = emptyList(),
    report = DayReport(
        served = state.served,
        walkedOut = state.walkedOut,
        binned = state.binned,
        earned = state.earned,
        collected = state.purse,
        bestStreak = state.bestStreak,
    ),
    note = "That is the morning gone",
)

// ---------------------------------------------------------------- actions

private fun apply(state: GameState, params: GameParams, action: Action): GameState {
    if (action is Action.OpenShop) {
        if (state.phase == DayPhase.RUNNING) return state
        // A finished day rolls over; a fresh one opens where it is. Coins, upgrades
        // and the day count are the only things that survive the night.
        val nextDay = if (state.phase == DayPhase.OVER) state.day + 1 else state.day
        return GameState(
            phase = DayPhase.RUNNING,
            day = nextDay,
            upgrades = state.upgrades,
            purse = state.purse,
            timeLeft = nextDay.let { state.upgrades.compile(it).dayLength },
            spawnIn = params.firstCustomerAfter,
            seed = state.seed + 1,
            note = if (nextDay > 1) "Day $nextDay — doors open" else "Doors open",
        )
    }

    // The shop is open between days, not during them.
    if (action is Action.Buy) {
        if (state.phase == DayPhase.RUNNING) return state.nag("Not while the queue is waiting")
        val price = state.upgrades.priceOf(action.upgrade) ?: return state.nag("Nothing left to buy there")
        if (state.purse < price) return state.nag("That is ${price - state.purse} short")
        return state.copy(
            upgrades = state.upgrades.bought(action.upgrade),
            purse = state.purse - price,
            note = "${action.upgrade.label} — bought",
        )
    }

    if (state.phase != DayPhase.RUNNING) return state

    return when (action) {

        Action.OpenShop, is Action.Buy -> state // handled above

        Action.TakeDough -> when (state.board) {
            Board.Empty -> state.copy(board = Board.Ball, note = "Press it flat")
            else -> state.nag("There is already something on the board")
        }

        Action.Flatten -> when (state.board) {
            Board.Ball -> state.copy(board = Board.Flat, note = "Zaatar or jibneh?")
            Board.Empty -> state.nag("Nothing to flatten — take a ball of dough")
            else -> state.nag("That one is already flat")
        }

        is Action.Spread -> when (state.board) {
            Board.Flat -> state.copy(board = Board.Topped(action.topping), note = "Onto the peel")
            Board.Ball -> state.nag("Press it flat first")
            Board.Empty -> state.nag("Nothing to spread it on")
            is Board.Topped -> state.nag("That one is already topped")
        }

        Action.LoadPeel -> when {
            state.board !is Board.Topped -> state.nag("Nothing topped to load")
            state.peel.size >= params.peelSlots -> state.nag("The peel is full — send it in")
            else -> {
                val peel = state.peel + (state.board as Board.Topped).topping
                state.copy(
                    board = Board.Empty,
                    peel = peel,
                    note = if (peel.size == params.peelSlots) "Full peel — into the furn" else
                        "${peel.size}/${params.peelSlots} on the peel",
                )
            }
        }

        Action.IntoFurn -> when {
            state.peel.isEmpty() -> state.nag("Load the peel first")
            state.furn != null -> state.nag("The furn is full — pull that load out first")
            else -> state.copy(
                peel = emptyList(),
                furn = Bake(items = state.peel, elapsed = 0.0),
                note = if (state.peel.distinct().size > 1)
                    "A mixed load — one clock, two windows" else "Watch it — pull on the green",
            )
        }

        Action.OutOfFurn -> outOfFurn(state, params)

        is Action.AddKhodra -> {
            val item = state.bench.getOrNull(action.benchIndex)
                ?: return state.nag("Nothing there to put it on")
            if (action.khodra in item.khodra) return state.nag("${action.khodra.label} is already on it")
            state.copy(
                bench = state.bench.replaceAt(
                    action.benchIndex,
                    item.copy(khodra = item.khodra + action.khodra),
                ),
                note = "${action.khodra.label} on",
            )
        }

        is Action.Serve -> serve(state, params, action.benchIndex)

        is Action.BinBaked -> {
            state.bench.getOrNull(action.benchIndex) ?: return state.nag("Nothing there to throw away")
            state.copy(
                bench = state.bench.filterIndexed { i, _ -> i != action.benchIndex },
                binned = state.binned + 1,
                note = "Gone. Start another",
            )
        }

        Action.BinBoard -> when {
            state.board != Board.Empty ->
                state.copy(board = Board.Empty, binned = state.binned + 1, note = "Gone. Start another")
            state.peel.isNotEmpty() ->
                state.copy(peel = emptyList(), binned = state.binned + state.peel.size, note = "Peel cleared")
            else -> state.nag("Nothing to throw away")
        }

        is Action.Collect -> {
            val drop = state.drops.firstOrNull { it.id == action.dropId } ?: return state
            state.copy(
                drops = state.drops.filterNot { it.id == drop.id },
                purse = state.purse + drop.amount,
            )
        }
    }
}

private fun outOfFurn(state: GameState, params: GameParams): GameState {
    val bake = state.furn ?: return state.nag("The furn is empty")
    if (state.bench.size + bake.items.size > params.peelSlots) {
        return state.nag("No room on the bench — hand some over first")
    }
    var id = state.nextBakedId
    val out = bake.items.map { topping ->
        Baked(id = id++, topping = topping, doneness = params.donenessAt(topping, bake.elapsed))
    }
    val perfect = out.count { it.doneness == Doneness.PERFECT }
    return state.copy(
        furn = null,
        bench = state.bench + out,
        nextBakedId = id,
        note = when {
            out.any { it.doneness == Doneness.BURNT } -> "Burnt ones straight in the bin"
            perfect == out.size -> if (out.size == 1) "Perfect" else "All ${out.size} perfect"
            else -> "$perfect of ${out.size} caught the window"
        },
    )
}

private fun serve(state: GameState, params: GameParams, index: Int): GameState {
    val baked = state.bench.getOrNull(index) ?: return state.nag("Nothing there to hand over")
    val customer = state.front ?: return state.nag("Nobody at the counter")
    // Wrong item is refused outright rather than sold cheap: anything that clears
    // the queue is worth more than the coins, so a discount would be an exploit.
    if (baked.topping != customer.wants) return state.nag("They asked for ${customer.wants.label}")
    if (baked.doneness == Doneness.RAW || baked.doneness == Doneness.BURNT) {
        return state.nag("They will not take that one — bin it")
    }

    val paid = params.settle(
        topping = baked.topping,
        doneness = baked.doneness,
        wanted = customer.khodra,
        given = baked.khodra,
        patience = customer.patience,
    )
    val missed = (customer.khodra - baked.khodra).size
    // A clean one is the right item, caught in the window, with everything they asked for.
    val clean = baked.doneness == Doneness.PERFECT && missed == 0
    val streak = if (clean) state.streak + 1 else 0
    return state.copy(
        bench = state.bench.filterIndexed { i, _ -> i != index },
        queue = state.queue.drop(1),
        drops = state.drops + CoinDrop(state.nextDropId, paid, params.coinLife),
        nextDropId = state.nextDropId + 1,
        earned = state.earned + paid,
        served = state.served + 1,
        streak = streak,
        bestStreak = maxOf(state.bestStreak, streak),
        note = when {
            missed == 1 -> "+$paid — they wanted khodra on that"
            missed > 1 -> "+$paid — $missed things missing"
            else -> "+$paid on the counter — pick it up"
        },
    )
}

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    mapIndexed { i, existing -> if (i == index) value else existing }

private fun GameState.nag(message: String): GameState = copy(note = message)
