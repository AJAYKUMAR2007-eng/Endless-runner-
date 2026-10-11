package com.example.neonrunner.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("neon_runner_prefs", Context.MODE_PRIVATE)

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) = prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()

    var bestDistance: Int
        get() = prefs.getInt(KEY_BEST_DISTANCE, 0)
        set(value) = prefs.edit().putInt(KEY_BEST_DISTANCE, value).apply()

    var totalCoins: Int
        get() = prefs.getInt(KEY_TOTAL_COINS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_COINS, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var showTouchButtons: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TOUCH_BUTTONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TOUCH_BUTTONS, value).apply()

    fun recordRun(score: Int, distance: Int, coinsEarned: Int): Boolean {
        var isNewHigh = false
        if (score > highScore) {
            highScore = score
            isNewHigh = true
        }
        if (distance > bestDistance) {
            bestDistance = distance
        }
        totalCoins += coinsEarned
        return isNewHigh
    }

    fun resetStats() {
        prefs.edit()
            .putInt(KEY_HIGH_SCORE, 0)
            .putInt(KEY_BEST_DISTANCE, 0)
            .putInt(KEY_TOTAL_COINS, 0)
            .apply()
    }

    companion object {
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_BEST_DISTANCE = "best_distance"
        private const val KEY_TOTAL_COINS = "total_coins"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_SHOW_TOUCH_BUTTONS = "show_touch_buttons"
    }
}
