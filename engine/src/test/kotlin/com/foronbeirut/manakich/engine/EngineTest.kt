package com.foronbeirut.manakich.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val P = GameParams()
private val ZAATAR = P.recipe(Topping.ZAATAR)
private val JIBNEH = P.recipe(Topping.JIBNEH)

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

/** One manousheh, made and put on the peel. */
private fun GameState.make(topping: Topping): GameState =
    act(Action.TakeDough, Action.Flatten, Action.Spread(topping), Action.LoadPeel)

/** A customer who wants exactly this, so the tests are not at the mercy of arrivals. */
private fun GameState.waiting(wants: Topping, khodra: Set<Khodra> = emptySet()): GameState =
    copy(queue = listOf(Customer(id = 900, wants = wants, khodra = khodra, left = P.patienceSeconds, max = P.patienceSeconds)))

class DonenessTest {

    @Test
    fun `each topping has its own window`() {
        // zaatar 6.0 +- 0.7 perfect, 0.8 grace -> done to 7.5
        assertEquals(Doneness.RAW, P.donenessAt(Topping.ZAATAR, 5.29))
        assertEquals(Doneness.PERFECT, P.donenessAt(Topping.ZAATAR, 6.0))
        assertEquals(Doneness.DONE, P.donenessAt(Topping.ZAATAR, 7.4))
        assertEquals(Doneness.BURNT, P.donenessAt(Topping.ZAATAR, 7.6))

        // jibneh 7.5 +- 0.75 perfect, 1.1 grace -> done to 9.35
        assertEquals(Doneness.RAW, P.donenessAt(Topping.JIBNEH, 6.7))
        assertEquals(Doneness.PERFECT, P.donenessAt(Topping.JIBNEH, 7.5))
        assertEquals(Doneness.DONE, P.donenessAt(Topping.JIBNEH, 9.0))
        assertEquals(Doneness.BURNT, P.donenessAt(Topping.JIBNEH, 9.4))
    }

    @Test
    fun `there is no moment that suits a mixed load`() {
        // This is the whole design of the peel, so it is worth asserting rather
        // than trusting: the two windows do not overlap at any elapsed time.
        val overlap = generateSequence(0.0) { it + 0.05 }.takeWhile { it < 12.0 }.filter {
            P.donenessAt(Topping.ZAATAR, it) == Doneness.PERFECT &&
                P.donenessAt(Topping.JIBNEH, it) == Doneness.PERFECT
        }.toList()
        assertTrue(overlap.isEmpty(), "zaatar and jibneh must never be perfect together")

        // Pulling for the zaatar leaves the jibneh raw; waiting for the jibneh burns
        // nothing but does cost the zaatar its window.
        assertEquals(Doneness.RAW, P.donenessAt(Topping.JIBNEH, ZAATAR.bakeSeconds))
        assertEquals(Doneness.DONE, P.donenessAt(Topping.ZAATAR, JIBNEH.bakeSeconds))
    }

    @Test
    fun `only sellable ones pay, and dark ones pay less`() {
        assertEquals(8, P.payoutFor(Topping.ZAATAR, Doneness.PERFECT))
        assertEquals(5, P.payoutFor(Topping.ZAATAR, Doneness.DONE))
        assertEquals(14, P.payoutFor(Topping.JIBNEH, Doneness.PERFECT))
        assertEquals(8, P.payoutFor(Topping.JIBNEH, Doneness.DONE))
        assertEquals(0, P.payoutFor(Topping.JIBNEH, Doneness.BURNT))
    }

    @Test
    fun `the bake puts a ceiling on the tip`() {
        // Nobody can be served faster than their manousheh bakes, so the top coin of
        // the tip is unreachable by construction — and jibneh, wanting longer, can
        // never reach as high as zaatar. Speed is a bonus on a fair price, never
        // the price itself.
        val bestZaatar = P.tipFor((P.patienceSeconds - ZAATAR.bakeSeconds) / P.patienceSeconds)
        val bestJibneh = P.tipFor((P.patienceSeconds - JIBNEH.bakeSeconds) / P.patienceSeconds)
        assertEquals(3, bestZaatar)
        assertEquals(3, bestJibneh)
        assertTrue(bestZaatar < P.maxTip, "the full tip cannot be earned")
    }

    @Test
    fun `speed is paid as a tip, not as the wage`() {
        assertEquals(4, P.tipFor(1.0))
        assertEquals(2, P.tipFor(0.5))
        assertEquals(0, P.tipFor(0.0))
    }
}

class KhodraTest {

    private fun settle(wanted: Set<Khodra>, given: Set<Khodra>) =
        P.settle(Topping.ZAATAR, Doneness.PERFECT, wanted, given, patience = 0.0)

    @Test
    fun `asked-for khodra pays two each`() {
        assertEquals(8, settle(emptySet(), emptySet()))
        assertEquals(10, settle(setOf(Khodra.TOMATO), setOf(Khodra.TOMATO)))
        assertEquals(12, settle(setOf(Khodra.TOMATO, Khodra.MINT), setOf(Khodra.TOMATO, Khodra.MINT)))
    }

    @Test
    fun `khodra nobody asked for is free and harmless`() {
        assertEquals(8, settle(emptySet(), setOf(Khodra.OLIVES, Khodra.PICKLES)))
    }

    @Test
    fun `each miss costs twenty-two per cent of the lot`() {
        assertEquals(6, settle(setOf(Khodra.TOMATO), emptySet()), "8 * 0.78")
        assertEquals(5, settle(setOf(Khodra.TOMATO, Khodra.MINT), emptySet()), "8 * 0.78 * 0.78")
        // Half-right is better than nothing, but still worse than not being asked.
        assertEquals(8, settle(setOf(Khodra.TOMATO, Khodra.MINT), setOf(Khodra.TOMATO)))
    }

    @Test
    fun `a serve never pays nothing`() {
        val bare = P.settle(Topping.ZAATAR, Doneness.DONE, Khodra.entries.toSet(), emptySet(), 0.0)
        assertTrue(bare >= 1, "was $bare")
    }
}

class PeelTest {

    @Test
    fun `the peel holds three and the furn takes the load`() {
        val loaded = open().make(Topping.ZAATAR).make(Topping.ZAATAR).make(Topping.JIBNEH)
        assertEquals(3, loaded.peel.size)

        val overfull = loaded.make(Topping.ZAATAR)
        assertEquals(3, overfull.peel.size, "the fourth stays on the board")
        assertTrue(overfull.board is Board.Topped)

        val inside = loaded.act(Action.IntoFurn)
        assertTrue(inside.peel.isEmpty())
        assertEquals(3, assertNotNull(inside.furn).items.size)
    }

    @Test
    fun `one clock for the whole load`() {
        val out = open()
            .make(Topping.ZAATAR).make(Topping.JIBNEH).act(Action.IntoFurn)
            .wait(ZAATAR.bakeSeconds)
            .act(Action.OutOfFurn)

        assertEquals(2, out.bench.size)
        assertEquals(Doneness.PERFECT, out.bench.first { it.topping == Topping.ZAATAR }.doneness)
        assertEquals(Doneness.RAW, out.bench.first { it.topping == Topping.JIBNEH }.doneness)
    }

    @Test
    fun `waiting for the jibneh costs the zaatar its window`() {
        val out = open()
            .make(Topping.ZAATAR).make(Topping.JIBNEH).act(Action.IntoFurn)
            .wait(JIBNEH.bakeSeconds)
            .act(Action.OutOfFurn)

        assertEquals(Doneness.PERFECT, out.bench.first { it.topping == Topping.JIBNEH }.doneness)
        assertEquals(Doneness.DONE, out.bench.first { it.topping == Topping.ZAATAR }.doneness)
    }

    @Test
    fun `a single-topping load has one right moment`() {
        val out = open()
            .make(Topping.JIBNEH).make(Topping.JIBNEH).act(Action.IntoFurn)
            .wait(JIBNEH.bakeSeconds)
            .act(Action.OutOfFurn)
        assertTrue(out.bench.all { it.doneness == Doneness.PERFECT })
    }

    @Test
    fun `the furn takes one load at a time`() {
        val busy = open().make(Topping.ZAATAR).act(Action.IntoFurn).make(Topping.ZAATAR).act(Action.IntoFurn)
        assertEquals(1, assertNotNull(busy.furn).items.size)
        assertEquals(1, busy.peel.size, "the second load waits on the peel")
    }

    @Test
    fun `a load cannot come out onto a bench that will not hold it`() {
        val stuck = open()
            .make(Topping.ZAATAR).make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0).act(Action.OutOfFurn)
            .make(Topping.ZAATAR).make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0).act(Action.OutOfFurn)

        assertEquals(2, stuck.bench.size)
        assertNotNull(stuck.furn, "the second load stays in the furn rather than overflowing")
    }
}

class LoopTest {

    @Test
    fun `make it, bake it, hand it over, get paid`() {
        val end = open().waiting(Topping.ZAATAR)
            .make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0)
            .act(Action.OutOfFurn, Action.Serve(0))

        assertEquals(1, end.served)
        assertEquals(1, end.streak)
        assertTrue(end.bench.isEmpty())

        // The coins are on the counter, not in the purse — picking them up is a move.
        assertEquals(0, end.purse)
        val drop = assertNotNull(end.drops.singleOrNull())
        assertEquals(11, drop.amount, "8 for a perfect zaatar plus 3 of the 4-coin tip")

        assertEquals(11, end.act(Action.Collect(drop.id)).purse)
    }

    @Test
    fun `jibneh is worth the longer wait`() {
        val end = open().waiting(Topping.JIBNEH)
            .make(Topping.JIBNEH).act(Action.IntoFurn).wait(JIBNEH.bakeSeconds)
            .act(Action.OutOfFurn, Action.Serve(0))

        assertEquals(17, assertNotNull(end.drops.singleOrNull()).amount, "14 plus the tip")
    }

    @Test
    fun `a full order pays for reading the ticket`() {
        val wants = setOf(Khodra.TOMATO, Khodra.MINT)
        val ready = open().waiting(Topping.ZAATAR, wants)
            .make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0).act(Action.OutOfFurn)

        val complete = ready
            .act(Action.AddKhodra(0, Khodra.TOMATO), Action.AddKhodra(0, Khodra.MINT), Action.Serve(0))
        val forgotten = ready.act(Action.Serve(0))

        assertEquals(15, assertNotNull(complete.drops.singleOrNull()).amount, "8 + 2 + 2 + 3 tip")
        assertEquals(7, assertNotNull(forgotten.drops.singleOrNull()).amount, "11 knocked down twice")
        assertEquals(1, complete.streak)
        assertEquals(0, forgotten.streak, "a miss is not a clean one")
    }

    @Test
    fun `they will not take what they did not order`() {
        val end = open().waiting(Topping.JIBNEH)
            .make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0)
            .act(Action.OutOfFurn, Action.Serve(0))

        assertEquals(0, end.served)
        assertEquals(1, end.bench.size, "it stays on the bench")
    }

    @Test
    fun `a burnt one is refused, not discounted`() {
        val burnt = open().waiting(Topping.ZAATAR)
            .make(Topping.ZAATAR).act(Action.IntoFurn).wait(20.0).act(Action.OutOfFurn)
        assertEquals(Doneness.BURNT, burnt.bench.single().doneness)

        val refused = burnt.act(Action.Serve(0))
        assertEquals(0, refused.served)
        assertTrue(refused.drops.isEmpty())

        val cleared = refused.act(Action.BinBaked(0))
        assertTrue(cleared.bench.isEmpty())
        assertEquals(1, cleared.binned)
    }

    @Test
    fun `serving takes the one you pointed at`() {
        val ready = open().waiting(Topping.JIBNEH)
            .make(Topping.ZAATAR).make(Topping.JIBNEH).act(Action.IntoFurn)
            .wait(JIBNEH.bakeSeconds).act(Action.OutOfFurn)

        assertEquals(Topping.ZAATAR, ready.bench[0].topping)
        val end = ready.act(Action.Serve(1))
        assertEquals(1, end.served)
        assertEquals(Topping.ZAATAR, end.bench.single().topping, "the other one is untouched")
    }
}

class RulesTest {

    @Test
    fun `the chain cannot be skipped`() {
        assertEquals(Board.Empty, open().act(Action.Flatten).board)
        assertEquals(Board.Ball, open().act(Action.TakeDough, Action.Spread(Topping.ZAATAR)).board)
        assertTrue(open().act(Action.TakeDough, Action.Flatten, Action.LoadPeel).peel.isEmpty())
        assertNull(open().act(Action.IntoFurn).furn)
    }

    @Test
    fun `khodra only goes on something baked`() {
        val nothing = open().act(Action.AddKhodra(0, Khodra.TOMATO))
        assertTrue(nothing.bench.isEmpty())

        val once = open().make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0).act(Action.OutOfFurn)
            .act(Action.AddKhodra(0, Khodra.OLIVES), Action.AddKhodra(0, Khodra.OLIVES))
        assertEquals(setOf(Khodra.OLIVES), once.bench.single().khodra, "not twice")
    }

    @Test
    fun `nothing happens on an empty tap`() {
        val before = open()
        val after = before.act(
            Action.Serve(0), Action.OutOfFurn, Action.BinBoard, Action.BinBaked(0), Action.Collect(99),
        )
        assertEquals(before.purse, after.purse)
        assertEquals(before.served, after.served)
        assertEquals(before.binned, after.binned)
    }

    @Test
    fun `nothing works before the shop opens`() {
        val shut = GameState().act(Action.TakeDough, Action.Flatten, Action.Serve(0))
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
        assertEquals(DayPhase.RUNNING, open.run(89.0).phase)

        val done = open.run(91.0)
        assertEquals(DayPhase.OVER, done.phase)
        assertNotNull(done.report)
    }

    @Test
    fun `customers arrive and the queue is capped`() {
        val busy = GameState().act(Action.OpenShop).run(60.0)
        assertEquals(P.queueMax, busy.queue.size)
    }

    @Test
    fun `arrivals are deterministic, and they are not all the same order`() {
        val a = GameState().act(Action.OpenShop).run(60.0)
        val b = GameState().act(Action.OpenShop).run(60.0)
        assertEquals(a.queue.map { it.wants to it.khodra }, b.queue.map { it.wants to it.khodra })

        // Over a full day the shop should see both toppings and some khodra asked for.
        val day = GameState().act(Action.OpenShop).run(90.0)
        val seen = mutableSetOf<Topping>()
        var withKhodra = 0
        var s = GameState().act(Action.OpenShop)
        repeat(360) {
            s = step(s, P, 0.25)
            s.queue.forEach { c -> seen += c.wants; if (c.khodra.isNotEmpty()) withKhodra++ }
        }
        assertEquals(setOf(Topping.ZAATAR, Topping.JIBNEH), seen, "both come in")
        assertTrue(withKhodra > 0, "someone asks for khodra")
        assertTrue(day.nextCustomerId > 5, "the shop is not empty")
    }

    @Test
    fun `patience drains and then they walk`() {
        val waiting = open()
        val customer = assertNotNull(waiting.front)
        assertTrue(customer.patience > 0.99, "they arrive with a full heart")

        val gone = waiting.run(P.patienceSeconds + 0.5)
        assertTrue(gone.queue.none { it.id == customer.id })
        assertEquals(1, gone.walkedOut)
    }

    @Test
    fun `a walk-out breaks the streak`() {
        val served = open().waiting(Topping.ZAATAR)
            .make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0).act(Action.OutOfFurn, Action.Serve(0))
        assertEquals(1, served.streak)

        val abandoned = served.run(P.patienceSeconds + P.spawnEvery + 1.0)
        assertTrue(abandoned.walkedOut >= 1)
        assertEquals(0, abandoned.streak)
        assertEquals(1, abandoned.bestStreak, "the best still stands")
    }

    @Test
    fun `coins left on the counter are lost`() {
        val paid = open().waiting(Topping.ZAATAR)
            .make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0).act(Action.OutOfFurn, Action.Serve(0))
        assertEquals(1, paid.drops.size)
        assertEquals(1, paid.run(P.coinLife - 1.0).drops.size)

        val gone = paid.run(P.coinLife + 0.5)
        assertTrue(gone.drops.isEmpty())
        assertEquals(0, gone.purse, "earned is not collected")
        assertEquals(11, gone.earned)
    }

    @Test
    fun `the report counts what was picked up, not what was earned`() {
        val paid = open().waiting(Topping.ZAATAR)
            .make(Topping.ZAATAR).act(Action.IntoFurn).wait(6.0).act(Action.OutOfFurn, Action.Serve(0))
        val report = assertNotNull(paid.run(120.0).report)

        assertEquals(1, report.served)
        assertEquals(11, report.earned)
        assertEquals(0, report.collected)
        assertEquals(11, report.dropped)
        assertEquals(1, report.bestStreak)
    }

    @Test
    fun `opening again starts a clean day`() {
        val over = open().run(120.0)
        assertEquals(DayPhase.OVER, over.phase)

        val fresh = over.act(Action.OpenShop)
        assertEquals(DayPhase.RUNNING, fresh.phase)
        assertEquals(0, fresh.served)
        assertNull(fresh.report)
    }

    @Test
    fun `the closing bell stops the world`() {
        val over = open().make(Topping.ZAATAR).act(Action.IntoFurn).run(120.0)
        assertEquals(DayPhase.OVER, over.phase)

        val burnt = assertNotNull(over.furn)
        assertTrue(burnt.elapsed in 80.0..92.0, "it burned right up to the bell, then stopped")
        assertEquals(burnt.elapsed, assertNotNull(over.run(60.0).furn).elapsed, "and stays stopped")
    }
}
