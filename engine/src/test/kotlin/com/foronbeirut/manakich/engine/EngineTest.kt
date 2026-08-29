package com.foronbeirut.manakich.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val P = GameParams()

private fun GameState.act(vararg actions: Action): GameState =
    step(this, P, dt = 0.0, actions = actions.toList())

/** Time in one lump, the way a test wants it. */
private fun GameState.wait(seconds: Double): GameState = step(this, P, dt = seconds)

/** Time in frames, for anything that depends on things expiring along the way. */
private fun GameState.run(seconds: Double, frame: Double = 0.25): GameState {
    var s = this
    var left = seconds
    while (left > 0.0) {
        val dt = minOf(frame, left)
        s = step(s, P, dt)
        left -= dt
    }
    return s
}

/** An open shop with one customer already at the counter. */
private fun open(): GameState = GameState().act(Action.OpenShop).run(P.firstCustomerAfter + 0.1)

private fun GameState.cook(): GameState =
    act(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.IntoFurn)

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

    @Test
    fun `speed is paid as a tip, not as the wage`() {
        assertEquals(4, P.tipFor(1.0))
        assertEquals(2, P.tipFor(0.5))
        assertEquals(0, P.tipFor(0.0))
    }
}

class LoopTest {

    @Test
    fun `flatten, spread, bake, serve, paid`() {
        val end = open().cook().wait(6.0).act(Action.OutOfFurn, Action.Serve)

        assertEquals(1, end.served)
        assertEquals(1, end.streak)
        assertEquals(Board.Empty, end.board)
        assertNull(end.bench)
        assertNull(end.furn)

        // The coins are on the counter, not in the purse — picking them up is a move.
        assertEquals(0, end.purse)
        val drop = assertNotNull(end.drops.singleOrNull())
        // 8 for a perfect one, plus 3 of the 4-coin tip. The last coin is not
        // reachable: the bake alone spends six of their twenty-four seconds, so a
        // full tip would mean serving them before the manousheh exists.
        assertEquals(11, drop.amount)
        assertEquals(11, end.earned)

        val collected = end.act(Action.Collect(drop.id))
        assertEquals(11, collected.purse)
        assertTrue(collected.drops.isEmpty())
    }

    @Test
    fun `a perfect one left sitting keeps the wage and loses the tip`() {
        val ready = open().cook().wait(6.0).act(Action.OutOfFurn)
        val waiting = assertNotNull(ready.front)

        val end = ready.run(12.0).act(Action.Serve)
        assertEquals(waiting.id, assertNotNull(ready.run(12.0).front).id, "still the same customer")
        assertEquals(1, end.served)
        assertEquals(9, assertNotNull(end.drops.singleOrNull()).amount, "8 plus what is left of the tip")
    }

    @Test
    fun `pulling it late still sells, for less`() {
        val end = open().cook().wait(7.2).act(Action.OutOfFurn, Action.Serve)
        assertEquals(1, end.served)
        assertEquals(0, end.streak, "only a perfect one keeps a streak alive")
        assertEquals(8, assertNotNull(end.drops.singleOrNull()).amount, "5 plus the tip")
    }

    @Test
    fun `a burnt one is refused, not discounted`() {
        val burnt = open().cook().wait(20.0).act(Action.OutOfFurn)
        assertEquals(Doneness.BURNT, assertNotNull(burnt.bench).doneness)

        val refused = burnt.act(Action.Serve)
        assertEquals(0, refused.served)
        assertTrue(refused.drops.isEmpty())
        assertNotNull(refused.bench, "it stays on the bench until it is binned")

        val cleared = refused.act(Action.Bin)
        assertNull(cleared.bench)
        assertEquals(1, cleared.binned)
    }

    @Test
    fun `a raw one is refused too`() {
        val end = open().cook().wait(2.0).act(Action.OutOfFurn, Action.Serve)
        assertEquals(0, end.served)
        assertNotNull(end.bench)
    }
}

class RulesTest {

    @Test
    fun `the chain cannot be skipped`() {
        assertEquals(Board.Empty, open().act(Action.Flatten).board)
        assertEquals(Board.Ball, open().act(Action.TakeDough, Action.Spread(Topping.ZAATAR)).board)
        assertNull(open().act(Action.TakeDough, Action.Flatten, Action.IntoFurn).furn)
    }

    @Test
    fun `the furn holds one at a time`() {
        val loaded = open().cook().cook()
        assertEquals(0.0, assertNotNull(loaded.furn).elapsed, "the first one keeps baking")
        assertTrue(loaded.board is Board.Topped, "the second one waits on the board")
    }

    @Test
    fun `the bench holds one at a time`() {
        val state = open().cook().wait(6.0).act(Action.OutOfFurn).cook().wait(6.0).act(Action.OutOfFurn)
        assertNotNull(state.furn, "the second one cannot come out onto a full bench")
    }

    @Test
    fun `nothing happens on an empty tap`() {
        val before = open()
        val after = before.act(Action.Serve, Action.OutOfFurn, Action.Bin, Action.Collect(99))
        assertEquals(before.purse, after.purse)
        assertEquals(before.served, after.served)
        assertEquals(before.binned, after.binned)
    }

    @Test
    fun `nothing works before the shop opens`() {
        val shut = GameState().act(Action.TakeDough, Action.Flatten, Action.Serve)
        assertEquals(DayPhase.READY, shut.phase)
        assertEquals(Board.Empty, shut.board)
    }

    @Test
    fun `time only moves once the doors are open`() {
        val shut = GameState().run(30.0)
        assertEquals(DayPhase.READY, shut.phase)
        assertTrue(shut.queue.isEmpty())
    }
}

class ShiftTest {

    @Test
    fun `the day is ninety seconds and then it is over`() {
        val open = GameState().act(Action.OpenShop)
        assertEquals(DayPhase.RUNNING, open.phase)
        assertEquals(90.0, open.timeLeft)

        val nearly = open.run(89.0)
        assertEquals(DayPhase.RUNNING, nearly.phase)

        val done = nearly.run(2.0)
        assertEquals(DayPhase.OVER, done.phase)
        assertEquals(0.0, done.timeLeft)
        assertNotNull(done.report)
    }

    @Test
    fun `customers arrive and the queue is capped`() {
        val busy = GameState().act(Action.OpenShop).run(60.0)
        assertEquals(P.queueMax, busy.queue.size)
        assertTrue(busy.queue.size <= P.queueMax)
    }

    @Test
    fun `patience drains and then they walk`() {
        val waiting = open()
        val customer = assertNotNull(waiting.front)
        assertTrue(customer.patience > 0.99, "they arrive with a full heart")

        val half = waiting.run(P.patienceSeconds / 2)
        assertTrue(assertNotNull(half.queue.firstOrNull { it.id == customer.id }).patience < 0.6)

        val gone = waiting.run(P.patienceSeconds + 0.5)
        assertTrue(gone.queue.none { it.id == customer.id }, "that one left")
        assertEquals(1, gone.walkedOut)
    }

    @Test
    fun `a walk-out breaks the streak`() {
        val served = open().cook().wait(6.0).act(Action.OutOfFurn, Action.Serve)
        assertEquals(1, served.streak)

        val abandoned = served.run(P.patienceSeconds + P.spawnEvery + 1.0)
        assertTrue(abandoned.walkedOut >= 1)
        assertEquals(0, abandoned.streak)
        assertEquals(1, abandoned.bestStreak, "the best still stands")
    }

    @Test
    fun `coins left on the counter are lost`() {
        val paid = open().cook().wait(6.0).act(Action.OutOfFurn, Action.Serve)
        assertEquals(1, paid.drops.size)

        val stillThere = paid.run(P.coinLife - 1.0)
        assertEquals(1, stillThere.drops.size)

        val gone = paid.run(P.coinLife + 0.5)
        assertTrue(gone.drops.isEmpty())
        assertEquals(0, gone.purse, "earned is not collected")
        assertEquals(11, gone.earned)
    }

    @Test
    fun `the report counts what was picked up, not what was earned`() {
        val paid = open().cook().wait(6.0).act(Action.OutOfFurn, Action.Serve)
        val report = assertNotNull(paid.run(120.0).report)

        assertEquals(1, report.served)
        assertEquals(11, report.earned)
        assertEquals(0, report.collected)
        assertEquals(11, report.dropped)
        assertEquals(1, report.bestStreak)
    }

    @Test
    fun `opening again starts a clean day`() {
        val over = open().cook().wait(6.0).act(Action.OutOfFurn, Action.Serve).run(120.0)
        assertEquals(DayPhase.OVER, over.phase)

        val fresh = over.act(Action.OpenShop)
        assertEquals(DayPhase.RUNNING, fresh.phase)
        assertEquals(0, fresh.served)
        assertEquals(0, fresh.purse)
        assertNull(fresh.report)
    }

    @Test
    fun `the closing bell stops the world`() {
        val over = open().cook().run(120.0)
        assertEquals(DayPhase.OVER, over.phase)

        // The furn froze at the bell rather than running on for the extra 30s.
        val burnt = assertNotNull(over.furn)
        assertTrue(burnt.elapsed in 80.0..92.0, "it burned right up to the bell, then stopped")
        assertEquals(burnt.elapsed, assertNotNull(over.run(60.0).furn).elapsed, "and stays stopped")
    }
}
