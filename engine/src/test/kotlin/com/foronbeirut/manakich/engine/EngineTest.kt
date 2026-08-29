package com.foronbeirut.manakich.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val P = GameParams()

/** Runs the whole chain the way a player would, with no time passing. */
private fun GameState.act(vararg actions: Action): GameState =
    step(this, P, dt = 0.0, actions = actions.toList())

private fun GameState.wait(seconds: Double): GameState = step(this, P, dt = seconds)

class DonenessTest {

    @Test
    fun `the window straddles the bake time`() {
        // bake 6.0, perfect 1.4 -> perfect from 5.3 to 6.7, grace 0.8 -> done to 7.5
        assertEquals(Doneness.RAW, P.donenessAt(0.0))
        assertEquals(Doneness.RAW, P.donenessAt(5.29))
        assertEquals(Doneness.PERFECT, P.donenessAt(5.3))
        assertEquals(Doneness.PERFECT, P.donenessAt(6.0))
        assertEquals(Doneness.PERFECT, P.donenessAt(6.7))
        assertEquals(Doneness.DONE, P.donenessAt(6.71))
        assertEquals(Doneness.DONE, P.donenessAt(7.5))
        assertEquals(Doneness.BURNT, P.donenessAt(7.51))
    }

    @Test
    fun `only sellable ones pay, and dark ones pay less`() {
        assertEquals(8, P.payoutFor(Doneness.PERFECT))
        assertEquals(5, P.payoutFor(Doneness.DONE))
        assertEquals(0, P.payoutFor(Doneness.RAW))
        assertEquals(0, P.payoutFor(Doneness.BURNT))
    }
}

class LoopTest {

    @Test
    fun `flatten, spread, bake, serve, paid`() {
        val end = GameState()
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)
            .wait(6.0)
            .act(Action.OutOfFurn, Action.Serve)

        assertEquals(8, end.coins)
        assertEquals(1, end.served)
        assertEquals(2, end.customer.id, "the next customer steps up")
        assertNull(end.bench)
        assertNull(end.furn)
        assertEquals(Board.Empty, end.board)
    }

    @Test
    fun `pulling it late still sells, for less`() {
        val end = GameState()
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)
            .wait(7.2)
            .act(Action.OutOfFurn, Action.Serve)

        assertEquals(5, end.coins)
        assertEquals(1, end.served)
    }

    @Test
    fun `a burnt one is refused, not discounted`() {
        val burnt = GameState()
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)
            .wait(20.0)
            .act(Action.OutOfFurn)

        assertEquals(Doneness.BURNT, assertNotNull(burnt.bench).doneness)

        val refused = burnt.act(Action.Serve)
        assertEquals(0, refused.coins)
        assertEquals(0, refused.served)
        assertNotNull(refused.bench, "it stays on the bench until it is binned")

        val cleared = refused.act(Action.Bin)
        assertNull(cleared.bench)
        assertEquals(1, cleared.binned)
    }

    @Test
    fun `a raw one is refused too`() {
        val end = GameState()
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)
            .wait(2.0)
            .act(Action.OutOfFurn, Action.Serve)

        assertEquals(0, end.coins)
        assertNotNull(end.bench)
    }
}

class RulesTest {

    @Test
    fun `the chain cannot be skipped`() {
        val noDough = GameState().act(Action.Flatten)
        assertEquals(Board.Empty, noDough.board)

        val notFlat = GameState().act(Action.TakeDough, Action.Spread(Topping.ZAATAR))
        assertEquals(Board.Ball, notFlat.board)

        val notTopped = GameState().act(Action.TakeDough, Action.Flatten, Action.IntoFurn)
        assertNull(notTopped.furn)
    }

    @Test
    fun `the furn holds one at a time`() {
        val loaded = GameState()
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)

        assertEquals(0.0, assertNotNull(loaded.furn).elapsed, "the first one keeps baking")
        assertTrue(loaded.board is Board.Topped, "the second one waits on the board")
    }

    @Test
    fun `the bench holds one at a time`() {
        val state = GameState()
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)
            .wait(6.0)
            .act(Action.OutOfFurn)
            .act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)
            .wait(6.0)
            .act(Action.OutOfFurn)

        assertNotNull(state.furn, "the second one cannot come out onto a full bench")
    }

    @Test
    fun `time only moves while something is baking`() {
        val idle = GameState().wait(30.0)
        assertNull(idle.furn)
        assertEquals(0, idle.coins)
    }

    @Test
    fun `nothing happens on an empty tap`() {
        val before = GameState()
        val after = before.act(Action.Serve, Action.OutOfFurn, Action.Bin)
        assertEquals(before.coins, after.coins)
        assertEquals(before.served, after.served)
        assertEquals(before.binned, after.binned)
    }
}
