package com.foronbeirut.manakich

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val save = remember { Save(applicationContext) }
            val game = rememberGame(save)
            StationScreen(state = game.state, params = game.params, fx = game.fx, onAction = game::send)
        }
    }
}
