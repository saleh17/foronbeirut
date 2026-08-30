package com.foronbeirut.manakich

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val save = remember { Save(applicationContext) }
            val sfx = remember { Sfx(applicationContext) }
            DisposableEffect(sfx) { onDispose { sfx.release() } }
            val game = rememberGame(save, sfx)
            var muted by remember { mutableStateOf(false) }
            StationScreen(
                state = game.state,
                params = game.params,
                fx = game.fx,
                muted = muted,
                onToggleSound = { muted = !muted; sfx.muted = muted },
                onAction = game::send,
            )
        }
    }
}
