package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.neonrunner.audio.SoundManager
import com.example.neonrunner.data.GamePreferences
import com.example.neonrunner.game.GameEngine
import com.example.neonrunner.model.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Neon Runner", appName)
    }

    @Test
    fun `game preferences persists high score and coins`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = GamePreferences(context)
        prefs.resetStats()

        val isNewHigh = prefs.recordRun(score = 1500, distance = 300, coinsEarned = 25)
        assertTrue(isNewHigh)
        assertEquals(1500, prefs.highScore)
        assertEquals(300, prefs.bestDistance)
        assertEquals(25, prefs.totalCoins)
    }

    @Test
    fun `game engine status transitions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundManager = SoundManager(isSoundEnabled = { false })
        val prefs = GamePreferences(context)
        val engine = GameEngine(soundManager, prefs)

        assertEquals(GameStatus.MENU, engine.status)

        // START RUN action
        engine.startGame()
        assertEquals(GameStatus.PLAYING, engine.status)

        // PAUSE action
        engine.pauseGame()
        assertEquals(GameStatus.PAUSED, engine.status)

        // RESUME action
        engine.resumeGame()
        assertEquals(GameStatus.PLAYING, engine.status)

        // OPEN INSTRUCTIONS action
        engine.openInstructions()
        assertEquals(GameStatus.INSTRUCTIONS, engine.status)

        // BACK TO MENU action
        engine.openMenu()
        assertEquals(GameStatus.MENU, engine.status)
    }
}
