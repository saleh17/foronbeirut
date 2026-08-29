package com.foronbeirut.manakich.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopTest {

    private fun rich(coins: Int) = GameState(phase = DayPhase.OVER, purse = coins)

    @Test
    fun `buying spends the purse and moves the level`() {
        val after = step(rich(400), GameParams(), 0.0, listOf(Action.Buy(Upgrade.OVEN)))
        assertEquals(1, after.upgrades.level(Upgrade.OVEN))
        assertEquals(50, after.purse, "350 for the furn")
    }

    @Test
    fun `you cannot buy what you cannot afford`() {
        val after = step(rich(349), GameParams(), 0.0, listOf(Action.Buy(Upgrade.OVEN)))
        assertEquals(0, after.upgrades.level(Upgrade.OVEN))
        assertEquals(349, after.purse)
        assertTrue(after.note.contains("1 short"), "was '${after.note}'")
    }

    @Test
    fun `the shop is shut while the queue is waiting`() {
        val open = step(GameState(purse = 5000), GameParams(), 0.0, listOf(Action.OpenShop))
        val after = step(open, GameParams(), 0.0, listOf(Action.Buy(Upgrade.OVEN)))
        assertEquals(0, after.upgrades.level(Upgrade.OVEN))
        assertEquals(5000, after.purse)
    }

    @Test
    fun `a maxed upgrade has nothing left to sell`() {
        var s = rich(99_999)
        repeat(Upgrade.PEEL.maxLevel + 2) { s = step(s, GameParams(), 0.0, listOf(Action.Buy(Upgrade.PEEL))) }
        assertEquals(Upgrade.PEEL.maxLevel, s.upgrades.level(Upgrade.PEEL))
        assertNull(s.upgrades.priceOf(Upgrade.PEEL))
    }
}

class DayTest {

    @Test
    fun `the day rolls over and the shop keeps what it earned`() {
        val one = step(GameState(), GameParams(), 0.0, listOf(Action.OpenShop))
        assertEquals(1, one.day)

        val closed = one.copy(phase = DayPhase.OVER, purse = 500, report = DayReport(1, 0, 0, 500, 500, 1))
        val bought = step(closed, GameParams(), 0.0, listOf(Action.Buy(Upgrade.OVEN)))
        val two = step(bought, bought.params(), 0.0, listOf(Action.OpenShop))

        assertEquals(2, two.day)
        assertEquals(150, two.purse, "what is left after the furn")
        assertEquals(1, two.upgrades.level(Upgrade.OVEN))
        assertNull(two.report, "yesterday's board is cleared")
        assertEquals(0, two.served)
    }

    @Test
    fun `opening a fresh day one does not skip to day two`() {
        val one = step(GameState(), GameParams(), 0.0, listOf(Action.OpenShop))
        assertEquals(1, step(one, GameParams(), 0.0, listOf(Action.OpenShop)).day, "already open")
    }

    @Test
    fun `day one teaches one clock, day two adds the second`() {
        assertEquals(listOf(Topping.ZAATAR), Upgrades.menuOn(1))
        assertEquals(listOf(Topping.ZAATAR, Topping.JIBNEH), Upgrades.menuOn(2))

        var s = step(GameState(), Upgrades().compile(1), 0.0, listOf(Action.OpenShop))
        repeat(400) { s = step(s, Upgrades().compile(1), 0.25) }
        assertTrue(s.nextCustomerId > 5, "the shop was busy")
    }

    @Test
    fun `nobody asks for khodra on the first day`() {
        val day1 = Upgrades().compile(1)
        assertEquals(0.0, day1.khodraOne)
        assertTrue(Upgrades().compile(6).khodraOne > 0.2, "the tickets lengthen")
        assertTrue(Upgrades().compile(30).khodraOne <= 0.55, "but not forever")
    }
}

class CompileTest {

    @Test
    fun `upgrades compile into params and the engine never sees them`() {
        val base = Upgrades().compile(2)
        val kitted = Upgrades()
            .bought(Upgrade.OVEN).bought(Upgrade.PEEL)
            .bought(Upgrade.INGREDIENTS).bought(Upgrade.AWNING)
            .compile(2)

        assertTrue(kitted.recipe(Topping.ZAATAR).bakeSeconds < base.recipe(Topping.ZAATAR).bakeSeconds)
        assertTrue(kitted.recipe(Topping.ZAATAR).price > base.recipe(Topping.ZAATAR).price)
        assertEquals(base.peelSlots + 1, kitted.peelSlots)
        assertTrue(kitted.patienceSeconds > base.patienceSeconds)
        assertTrue(kitted.spawnEvery < base.spawnEvery, "a better-known shop is busier")
    }

    @Test
    fun `the burn guard only ever adds forgiveness`() {
        val plain = Upgrades().compile(2).recipe(Topping.JIBNEH)
        val guarded = Upgrades().bought(Upgrade.BURN_GUARD).compile(2).recipe(Topping.JIBNEH)
        assertEquals(plain.bakeSeconds, guarded.bakeSeconds)
        assertEquals(plain.perfectWindow, guarded.perfectWindow)
        assertTrue(guarded.graceWindow > plain.graceWindow)
    }

    @Test
    fun `the starting shop is exactly what phase three shipped`() {
        val p = Upgrades().compile(2)
        assertEquals(6.0, p.recipe(Topping.ZAATAR).bakeSeconds)
        assertEquals(1.4, p.recipe(Topping.ZAATAR).perfectWindow)
        assertEquals(8, p.recipe(Topping.ZAATAR).price)
        assertEquals(7.5, p.recipe(Topping.JIBNEH).bakeSeconds)
        assertEquals(14, p.recipe(Topping.JIBNEH).price)
    }
}

class TipJarTest {

    private fun paidState(params: GameParams): GameState {
        var s = step(GameState(), params, 0.0, listOf(Action.OpenShop))
        s = step(s, params, 1.2)
        s = s.copy(queue = listOf(Customer(1, Topping.ZAATAR, emptySet(), 30.0, 30.0)))
        s = step(s, params, 0.0, listOf(Action.TakeDough, Action.Flatten, Action.Spread(Topping.ZAATAR), Action.LoadPeel, Action.IntoFurn))
        s = step(s, params, params.recipe(Topping.ZAATAR).bakeSeconds)
        return step(s, params, 0.0, listOf(Action.OutOfFurn, Action.Serve(0)))
    }

    @Test
    fun `without it, coins have to be picked up`() {
        val params = Upgrades().compile(2)
        var s = paidState(params)
        assertEquals(1, s.drops.size)
        repeat(30) { s = step(s, params, 0.1) }
        assertEquals(0, s.purse, "three seconds later it is still sitting there")
    }

    @Test
    fun `with it, they find their own way in`() {
        val params = Upgrades().bought(Upgrade.TIP_JAR).compile(2)
        var s = paidState(params)
        val earned = s.earned
        repeat(30) { s = step(s, params, 0.1) }
        assertEquals(earned, s.purse, "collected without a tap")
        assertTrue(s.drops.isEmpty())
    }
}

class SaveTest {

    @Test
    fun `a profile survives the round trip`() {
        val saved = Profile(
            day = 7,
            purse = 1234,
            upgrades = Upgrades().bought(Upgrade.OVEN).bought(Upgrade.OVEN).bought(Upgrade.PEEL),
            bestDay = 210,
        )
        val back = Profile.decode(saved.encode())
        assertEquals(saved.day, back.day)
        assertEquals(saved.purse, back.purse)
        assertEquals(2, back.upgrades.level(Upgrade.OVEN))
        assertEquals(1, back.upgrades.level(Upgrade.PEEL))
        assertEquals(0, back.upgrades.level(Upgrade.TIP_JAR))
        assertEquals(saved.bestDay, back.bestDay)
    }

    @Test
    fun `rubbish reads as a fresh start rather than a crash`() {
        for (junk in listOf(null, "", "   ", "garbage", "v=9;day=4", "day=;purse=", "v=1;day=abc;purse=-5")) {
            val p = Profile.decode(junk)
            assertTrue(p.day >= 1, "day was ${p.day} for '$junk'")
            assertTrue(p.purse >= 0)
        }
        assertEquals(1, Profile.decode("v=1;day=abc;purse=-5").day)
        assertEquals(0, Profile.decode("v=1;day=abc;purse=-5").purse)
    }

    @Test
    fun `a save picks the morning back up`() {
        val profile = Profile(day = 4, purse = 900, upgrades = Upgrades().bought(Upgrade.PEEL))
        val state = profile.toState()
        assertEquals(4, state.day)
        assertEquals(900, state.purse)
        assertEquals(DayPhase.READY, state.phase)
        assertEquals(3, state.params().peelSlots)
        assertNotNull(state.params().menu.firstOrNull())
    }

    @Test
    fun `a level beyond the maximum is clamped rather than trusted`() {
        val p = Profile.decode("v=1;day=2;purse=0;OVEN=99")
        assertEquals(Upgrade.OVEN.maxLevel, p.upgrades.level(Upgrade.OVEN))
    }
}
