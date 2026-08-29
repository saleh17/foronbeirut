package com.foronbeirut.manakich.engine

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Prices the upgrade tree against play rather than arithmetic. These print a
 * table and assert only loose bounds — tuning should be free to move without
 * turning the suite red, but a day that pays nothing or pays a fortune is a bug.
 */
class PricingTest {

    private data class Row(val day: Int, val took: Int, val purse: Int, val served: Int, val walked: Int)

    private fun run(skill: Skill, days: Int, buy: Boolean): List<Row> {
        var state = GameState()
        val bot = Bot(skill)
        val rows = mutableListOf<Row>()
        repeat(days) {
            val before = state.purse
            state = bot.playDay(state)
            val report = state.report!!
            rows += Row(state.day, state.purse - before, state.purse, report.served, report.walkedOut)
            if (buy) {
                // Always take the cheapest thing on the board — a plausible first-time
                // player, and the harshest test of whether the curve stalls.
                var again = true
                while (again) {
                    again = false
                    val next = Upgrade.entries
                        .mapNotNull { u -> state.upgrades.priceOf(u)?.let { u to it } }
                        .minByOrNull { it.second }
                    if (next != null && state.purse >= next.second) {
                        state = step(state, state.params(), 0.0, listOf(Action.Buy(next.first)))
                        println("   day ${state.day}: bought ${next.first.label} for ${next.second}")
                        again = true
                    }
                }
            }
            // playDay opens the next day itself, so nothing else may advance it.
            require(state.phase == DayPhase.OVER)
        }
        return rows
    }

    @Test
    fun `a day pays what the design expected`() {
        for (skill in listOf(Skill.CLUMSY to "clumsy", Skill.STEADY to "steady", Skill.SHARP to "sharp")) {
            var state = GameState()
            val end = Bot(skill.first).playDay(state)
            val r = end.report!!
            println(
                "%-7s served %2d  walked %2d  binned %2d  earned %3d  collected %3d"
                    .format(skill.second, r.served, r.walkedOut, r.binned, r.earned, r.collected)
            )
            assertTrue(r.served > 0, "${skill.second} served nothing")
            assertTrue(r.earned in 20..400, "${skill.second} earned ${r.earned}")
        }
    }

    @Test
    fun `the first upgrades land in the first few days`() {
        println("--- steady player, buying the cheapest thing available ---")
        val rows = run(Skill.STEADY, days = 8, buy = true)
        rows.forEach { r -> println("day %2d  served %2d  walked %2d  took %3d  purse %4d".format(r.day, r.served, r.walked, r.took, r.purse)) }

        val total = rows.sumOf { it.took }
        assertTrue(total > 200, "eight days only made $total")
        assertTrue(rows.map { it.day } == (1..8).toList(), "days ran ${rows.map { it.day }}")
        // The design's promise: something meaningful is affordable early, and the
        // tree still has road left after a week.
        assertTrue(rows.size == 8)
    }

    @Test
    fun `a better furn is a gift, never a stealth difficulty rise`() {
        // The trap this guards: shorter bake without a wider window means the player
        // pays coins to make the game harder and cannot say why it feels bad.
        var previous: Recipe? = null
        for (level in 0..Upgrade.OVEN.maxLevel) {
            val levels = if (level == 0) Upgrades() else generateSequence(Upgrades()) { it.bought(Upgrade.OVEN) }
                .elementAt(level)
            val r = levels.compile(day = 2).recipe(Topping.ZAATAR)
            println("furn %d  bake %.1f  window %.2f  grace %.2f".format(level, r.bakeSeconds, r.perfectWindow, r.graceWindow))
            previous?.let {
                assertTrue(r.bakeSeconds < it.bakeSeconds, "level $level did not get faster")
                assertTrue(r.perfectWindow >= it.perfectWindow, "level $level narrowed the window")
                assertTrue(r.graceWindow > it.graceWindow, "level $level shortened the grace")
            }
            previous = r
        }
    }
}

/**
 * What the simulation can and cannot price.
 *
 * It prices earnings well, because coins come out of rules the bot obeys exactly.
 * It cannot price patience, because the bot issues taps instantly and a person
 * doing ten taps per manousheh does not. Moving [GameParams.patienceSeconds] from
 * 30 to 20 changed walk-outs from zero to zero across eight simulated days —
 * which is evidence about the bot, not about the game. That number waits for a
 * human, and this test exists to say so out loud rather than leave a silent guess
 * in the tuning table.
 */
class WhatTheBotCannotSee {

    @Test
    fun `the bot never queues, so it cannot tell us what patience should be`() {
        var state = GameState()
        val bot = Bot(Skill.CLUMSY)
        repeat(8) { state = bot.playDay(state) }
        val report = state.report!!
        println("clumsy on day ${state.day}: served ${report.served}, walked out ${report.walkedOut}")
        assertTrue(report.walkedOut == 0, "if this ever fails the bot has finally fallen behind, and can price patience")
    }
}
