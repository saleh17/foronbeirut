package com.foronbeirut.manakich

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.foronbeirut.manakich.engine.Action
import com.foronbeirut.manakich.engine.DayPhase
import com.foronbeirut.manakich.engine.GameParams
import com.foronbeirut.manakich.engine.GameState
import com.foronbeirut.manakich.engine.Profile
import com.foronbeirut.manakich.engine.step
import com.foronbeirut.manakich.engine.toState

/** The whole save file is one string, so the engine never learns Android exists. */
class Save(context: Context) {
    private val prefs = context.getSharedPreferences("manakich", Context.MODE_PRIVATE)

    fun load(): Profile = Profile.decode(prefs.getString(KEY, null))

    fun write(profile: Profile) {
        prefs.edit().putString(KEY, profile.encode()).apply()
    }

    private companion object {
        const val KEY = "profile.v1"
    }
}

/**
 * The only bridge between Compose and the engine. Compose owns the frame clock and
 * says how much time passed; the engine decides what that means.
 *
 * Params are not held here: they are recompiled from the state's own upgrades and
 * day, so buying something takes effect without anything having to remember to
 * rebuild it.
 */
class Game(private val save: Save?, private val sfx: Sfx?, start: GameState) {
    var state: GameState by mutableStateOf(start)
        private set

    val params: GameParams get() = state.params()

    /** Feedback lives here, not in the engine, and never blocks an action. */
    val fx = Effects().apply { onSound = { id -> sfx?.play(id) } }

    private var best = save?.load()?.bestDay ?: 0

    fun advance(dtSeconds: Double) {
        fx.tick(dtSeconds)
        apply(step(state, params, dtSeconds))
    }

    fun send(action: Action) {
        val before = state
        apply(step(state, params, dt = 0.0, actions = listOf(action)))
        // Only the hint moved, so the engine turned the action down. Say so.
        if (state.copy(note = before.note) == before && state.note != before.note) fx.refused()
    }

    /** Write only when something that outlives the day has actually moved. */
    private fun apply(next: GameState) {
        val was = state
        state = next
        fx.observe(was, next, dropAt = ::dropPosition, slotAt = ::slotPosition)
        val worth = next.day != was.day ||
            next.purse != was.purse ||
            next.upgrades != was.upgrades ||
            (next.phase == DayPhase.OVER && was.phase != DayPhase.OVER)
        if (worth) {
            best = maxOf(best, next.report?.collected ?: 0)
            save?.write(next.toProfile(best))
        }
    }
}

/** Where the coin was sitting when it was picked up — kept in step with Station.kt. */
private fun dropPosition(id: Int): Pair<Float, Float> = (214f + (id % 4) * 58f) to 196f

/** Where a customer in that queue slot is standing. */
private fun slotPosition(index: Int): Float = listOf(218f, 336f, 454f)[index.coerceIn(0, 2)]

@Composable
fun rememberGame(save: Save?, sfx: Sfx? = null): Game {
    val game = remember(save, sfx) { Game(save, sfx, (save?.load() ?: Profile()).toState()) }
    LaunchedEffect(game) {
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - previous) / 1_000_000_000.0
            previous = now
            // A backgrounded app can hand back a huge delta; never burn a bake on it.
            game.advance(dt.coerceIn(0.0, 0.05))
        }
    }
    return game
}
