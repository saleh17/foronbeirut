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
    var next = advanceBake(state, dt)
    for (action in actions) next = apply(next, params, action)
    return next
}

private fun advanceBake(state: GameState, dt: Double): GameState {
    val furn = state.furn ?: return state
    if (dt <= 0.0) return state
    return state.copy(furn = furn.copy(elapsed = furn.elapsed + dt))
}

private fun apply(state: GameState, params: GameParams, action: Action): GameState = when (action) {

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
            furn = Bake(topping = (state.board as Board.Topped).topping, elapsed = 0.0),
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

    Action.Serve -> {
        val baked = state.bench
        when {
            baked == null -> state.nag("Nothing on the bench to hand over")
            baked.topping != state.customer.wants ->
                state.nag("They asked for ${state.customer.wants.label}")
            baked.doneness == Doneness.RAW || baked.doneness == Doneness.BURNT ->
                state.nag("They will not take that one — bin it")
            else -> {
                val paid = params.payoutFor(baked.doneness)
                state.copy(
                    bench = null,
                    coins = state.coins + paid,
                    served = state.served + 1,
                    customer = Customer(id = state.customer.id + 1, wants = Topping.ZAATAR),
                    note = "+$paid — next one is waiting",
                )
            }
        }
    }

    Action.Bin -> when {
        state.bench != null -> state.copy(
            bench = null,
            binned = state.binned + 1,
            note = "Gone. Start another",
        )
        state.board != Board.Empty -> state.copy(
            board = Board.Empty,
            binned = state.binned + 1,
            note = "Gone. Start another",
        )
        else -> state.nag("Nothing to throw away")
    }
}

private fun GameState.nag(message: String): GameState = copy(note = message)
