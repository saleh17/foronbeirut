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
class Game(private val save: Save?, start: GameState) {
    var state: GameState by mutableStateOf(start)
        private set

    val params: GameParams get() = state.params()

    private var best = save?.load()?.bestDay ?: 0

    fun advance(dtSeconds: Double) {
        apply(step(state, params, dtSeconds))
    }

    fun send(action: Action) {
        apply(step(state, params, dt = 0.0, actions = listOf(action)))
    }

    /** Write only when something that outlives the day has actually moved. */
    private fun apply(next: GameState) {
        val was = state
        state = next
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

@Composable
fun rememberGame(save: Save?): Game {
    val game = remember(save) { Game(save, (save?.load() ?: Profile()).toState()) }
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
