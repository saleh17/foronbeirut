package com.foronbeirut.manakich

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.foronbeirut.manakich.engine.Action
import com.foronbeirut.manakich.engine.GameParams
import com.foronbeirut.manakich.engine.GameState
import com.foronbeirut.manakich.engine.step

/**
 * The only bridge between Compose and the engine. Compose owns the frame clock and
 * says how much time passed; the engine decides what that means. Nothing about the
 * rules lives on this side of the line.
 */
class Game(val params: GameParams) {
    var state: GameState by mutableStateOf(GameState())
        private set

    fun advance(dtSeconds: Double) {
        state = step(state, params, dtSeconds)
    }

    fun send(action: Action) {
        state = step(state, params, dt = 0.0, actions = listOf(action))
    }
}

@Composable
fun rememberGame(params: GameParams = GameParams()): Game {
    val game = remember(params) { Game(params) }
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
