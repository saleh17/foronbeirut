package com.foronbeirut.manakich

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * The noises the shop makes.
 *
 * Every clip is generated rather than sampled — see tools/make-sounds.py — because
 * the music this game actually wants is Fairuz, that catalogue is administered,
 * and a licence problem is not something to ship by accident. These are the
 * mechanical sounds only: wood, dough, fire, coins, paper, a bell. Music stays a
 * commission.
 */
enum class SfxId { TAP, DOUGH, SIZZLE, COIN, SERVE, PAPER, BELL, NOPE }

class Sfx(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<SfxId, Int> = mapOf(
        SfxId.TAP to R.raw.tap,
        SfxId.DOUGH to R.raw.dough,
        SfxId.SIZZLE to R.raw.sizzle,
        SfxId.COIN to R.raw.coin,
        SfxId.SERVE to R.raw.serve,
        SfxId.PAPER to R.raw.paper,
        SfxId.BELL to R.raw.bell,
        SfxId.NOPE to R.raw.nope,
    ).mapValues { (_, res) -> pool.load(context, res, 1) }

    var muted: Boolean = false

    fun play(id: SfxId, volume: Float = 1f) {
        if (muted) return
        val sound = ids[id] ?: return
        // A little pitch variation so a run of taps does not sound like a machine.
        val rate = 0.94f + (0..12).random() / 100f
        pool.play(sound, volume, volume, 1, 0, rate)
    }

    fun release() = pool.release()
}
