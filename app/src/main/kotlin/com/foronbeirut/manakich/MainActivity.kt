package com.foronbeirut.manakich

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.foronbeirut.manakich.engine.GameParams

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { Slice() }
    }
}

@Composable
private fun Slice() {
    val game = rememberGame(GameParams())
    StationScreen(state = game.state, params = game.params, onAction = game::send)
}
