package com.foronbeirut.manakich.engine

/**
 * A bot that plays whole days headlessly, so the upgrade tree can be priced
 * against what the game actually pays rather than against arithmetic.
 *
 * It is deliberately imperfect and tunable, because a bot that plays flawlessly
 * prices the tree for a player who does not exist.
 */
data class Skill(
    /** How far off the middle of the window it pulls, in seconds. */
    val timingSlop: Double = 0.35,
    /** Chance it forgets a vegetable that was asked for. */
    val forgetsKhodra: Double = 0.12,
    /** Chance it leaves a coin on the counter. */
    val missesCoins: Double = 0.15,
    /** Seconds of dithering between actions. */
    val dither: Double = 0.25,
) {
    companion object {
        val CLUMSY = Skill(timingSlop = 0.9, forgetsKhodra = 0.3, missesCoins = 0.35, dither = 0.6)
        val STEADY = Skill()
        val SHARP = Skill(timingSlop = 0.1, forgetsKhodra = 0.02, missesCoins = 0.02, dither = 0.1)
    }
}

class Bot(private val skill: Skill, seed: Long = 7L) {
    private var s = seed
    private fun roll(): Double {
        s = s * 6364136223846793005L + 1442695040888963407L
        return ((s ushr 11).toDouble() / (1L shl 53).toDouble()).let { if (it < 0) it + 1.0 else it }
    }

    private fun spread() = (roll() * 2 - 1) * skill.timingSlop

    /** Plays one day from an opened state and returns it at the closing bell. */
    fun playDay(start: GameState): GameState {
        val params = start.params()
        var st = step(start, params, 0.0, listOf(Action.OpenShop))

        fun tick(seconds: Double) {
            var left = seconds
            while (left > 0 && st.phase == DayPhase.RUNNING) {
                val dt = minOf(0.1, left)
                st = step(st, params, dt)
                left -= dt
                // pick coins up, most of the time
                st.drops.forEach { d ->
                    if (roll() > skill.missesCoins) st = step(st, params, 0.0, listOf(Action.Collect(d.id)))
                }
            }
        }
        fun act(vararg a: Action) {
            st = step(st, params, 0.0, a.toList())
        }

        tick(params.firstCustomerAfter + 0.2)
        while (st.phase == DayPhase.RUNNING) {
            // Load the peel against the front of the queue, not just the first order.
            val wants = st.queue.take(params.peelSlots).map { it.wants }
                .ifEmpty { listOf(params.menu.first()) }
            wants.forEach { t -> act(Action.TakeDough, Action.Flatten, Action.Spread(t), Action.LoadPeel) }
            act(Action.IntoFurn)
            tick(skill.dither)

            val load = st.furn?.items.orEmpty()
            if (load.isEmpty()) { tick(0.5); continue }
            // Aim at the middle of the busiest topping's window, then miss by a bit.
            val aim = load.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
            val target = params.recipe(aim).bakeSeconds + spread()
            tick((target - (st.furn?.elapsed ?: 0.0)).coerceAtLeast(0.0))
            act(Action.OutOfFurn)

            var guard = 0
            while (st.bench.isNotEmpty() && st.phase == DayPhase.RUNNING && guard++ < 8) {
                val want = st.front
                val i = st.bench.indexOfFirst { want != null && it.topping == want.wants }
                if (want == null || i < 0) { act(Action.BinBaked(0)); continue }
                want.khodra.forEach { k ->
                    if (roll() > skill.forgetsKhodra) act(Action.AddKhodra(i, k))
                }
                val before = st.served
                act(Action.Serve(i))
                if (st.served == before) act(Action.BinBaked(i))
                tick(skill.dither)
            }
            tick(skill.dither)
        }
        return st
    }
}
