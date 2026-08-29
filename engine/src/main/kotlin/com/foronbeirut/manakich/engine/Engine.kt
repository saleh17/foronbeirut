package com.foronbeirut.manakich.engine

/**
 * The whole game, as one pure function: `(state, params, dt, actions) -> state`.
 *
 * Nothing in here touches a clock, a framework or the screen. The caller owns the
 * frame loop and hands in how much time passed; the engine only decides what that
 * means. That is what lets the rules be tested in milliseconds and lets the same
 * code back a phone, a desktop preview or a headless balance sweep.
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
    next = expireDrops(next, dt)
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

private fun expireDrops(state: GameState, dt: Double): GameState {
    if (state.drops.isEmpty()) return state
    val aged = state.drops.map { it.copy(left = it.left - dt) }
    return state.copy(drops = aged.filter { it.left > 0.0 })
}

private fun spawn(state: GameState, params: GameParams, dt: Double): GameState {
    val due = state.spawnIn - dt
    if (due > 0.0) return state.copy(spawnIn = due)
    // A full queue holds the door rather than banking arrivals, so a bad patch
    // never turns into a wave the player could not have prevented.
    if (state.queue.size >= params.queueMax) return state.copy(spawnIn = 0.0)
    return state.copy(
        queue = state.queue + Customer(
            id = state.nextCustomerId,
            wants = Topping.ZAATAR,
            left = params.patienceSeconds,
            max = params.patienceSeconds,
        ),
        nextCustomerId = state.nextCustomerId + 1,
        spawnIn = params.spawnEvery,
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
        return if (state.phase == DayPhase.RUNNING) state else GameState(
            phase = DayPhase.RUNNING,
            timeLeft = params.dayLength,
            spawnIn = params.firstCustomerAfter,
            note = "Doors open",
        )
    }
    if (state.phase != DayPhase.RUNNING) return state

    return when (action) {

        Action.OpenShop -> state // handled above

        Action.TakeDough -> when (state.board) {
            Board.Empty -> state.copy(board = Board.Ball, note = "Press it flat")
            else -> state.nag("There is already something on the board")
        }

        Action.Flatten -> when (state.board) {
            Board.Ball -> state.copy(board = Board.Flat, note = "Spread the zaatar")
            Board.Empty -> state.nag("Nothing to flatten — take a ball of dough")
            else -> state.nag("That one is already flat")
        }

        is Action.Spread -> when (state.board) {
            Board.Flat -> state.copy(board = Board.Topped(action.topping), note = "Into the furn")
            Board.Ball -> state.nag("Press it flat first")
            Board.Empty -> state.nag("Nothing to spread it on")
            is Board.Topped -> state.nag("It already has zaatar on it")
        }

        Action.IntoFurn -> when {
            state.board !is Board.Topped -> state.nag("Nothing ready for the furn")
            state.furn != null -> state.nag("The furn is full — pull that one out first")
            else -> state.copy(
                board = Board.Empty,
                furn = Bake((state.board as Board.Topped).topping, elapsed = 0.0),
                note = "Watch it — pull it on the green",
            )
        }

        Action.OutOfFurn -> when {
            state.furn == null -> state.nag("The furn is empty")
            state.bench != null -> state.nag("Clear the bench first")
            else -> {
                val doneness = params.donenessAt(state.furn.elapsed)
                state.copy(
                    furn = null,
                    bench = Baked(state.furn.topping, doneness),
                    note = when (doneness) {
                        Doneness.RAW -> "Too early — that one is still dough"
                        Doneness.PERFECT -> "Perfect. Hand it over"
                        Doneness.DONE -> "A touch dark, but sellable"
                        Doneness.BURNT -> "Burnt. Straight in the bin"
                    },
                )
            }
        }

        Action.Serve -> serve(state, params)

        Action.Bin -> when {
            state.bench != null -> state.copy(bench = null, binned = state.binned + 1, note = "Gone. Start another")
            state.board != Board.Empty -> state.copy(board = Board.Empty, binned = state.binned + 1, note = "Gone. Start another")
            else -> state.nag("Nothing to throw away")
        }

        is Action.Collect -> {
            val drop = state.drops.firstOrNull { it.id == action.dropId }
                ?: return state
            state.copy(
                drops = state.drops.filterNot { it.id == drop.id },
                purse = state.purse + drop.amount,
            )
        }
    }
}

private fun serve(state: GameState, params: GameParams): GameState {
    val baked = state.bench ?: return state.nag("Nothing on the bench to hand over")
    val customer = state.front ?: return state.nag("Nobody at the counter")
    // Wrong item is refused outright rather than sold cheap: anything that clears
    // the queue is worth more than the coins, so a discount would be an exploit.
    if (baked.topping != customer.wants) return state.nag("They asked for ${customer.wants.label}")
    if (baked.doneness == Doneness.RAW || baked.doneness == Doneness.BURNT) {
        return state.nag("They will not take that one — bin it")
    }

    val paid = params.payoutFor(baked.doneness) + params.tipFor(customer.patience)
    val streak = if (baked.doneness == Doneness.PERFECT) state.streak + 1 else 0
    return state.copy(
        bench = null,
        queue = state.queue.drop(1),
        drops = state.drops + CoinDrop(state.nextDropId, paid, params.coinLife),
        nextDropId = state.nextDropId + 1,
        earned = state.earned + paid,
        served = state.served + 1,
        streak = streak,
        bestStreak = maxOf(state.bestStreak, streak),
        note = "+$paid on the counter — pick it up",
    )
}

private fun GameState.nag(message: String): GameState = copy(note = message)
