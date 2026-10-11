package com.example.neonrunner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import com.example.neonrunner.audio.SoundManager
import com.example.neonrunner.data.GamePreferences
import com.example.neonrunner.game.GameEngine
import com.example.neonrunner.model.GameStatus
import com.example.neonrunner.ui.game.NeonRunnerCanvas
import com.example.neonrunner.ui.screens.GameHud
import com.example.neonrunner.ui.screens.GameOverScreen
import com.example.neonrunner.ui.screens.InstructionsDialog
import com.example.neonrunner.ui.screens.MenuScreen
import com.example.neonrunner.ui.screens.PauseDialog
import kotlinx.coroutines.isActive

@Composable
fun NeonRunnerMainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember { GamePreferences(context) }
    var isSoundEnabled by remember { mutableStateOf(preferences.isSoundEnabled) }
    var showTouchButtons by remember { mutableStateOf(preferences.showTouchButtons) }

    val soundManager = remember {
        SoundManager(isSoundEnabled = { isSoundEnabled })
    }

    val gameEngine = remember {
        GameEngine(soundManager = soundManager, preferences = preferences)
    }

    // Trigger state recomposition on game frame updates
    var frameTick by remember { mutableStateOf(0L) }

    val focusRequester = remember { FocusRequester() }

    // Game Loop driven by Choreographer withFrameNanos
    LaunchedEffect(gameEngine.status) {
        if (gameEngine.status == GameStatus.PLAYING) {
            var lastTime = System.nanoTime()
            while (isActive && gameEngine.status == GameStatus.PLAYING) {
                withFrameNanos { now ->
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastTime = now
                    gameEngine.update(dt)
                    frameTick = now
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Handle Android system back gesture
    BackHandler {
        when (gameEngine.status) {
            GameStatus.PLAYING -> gameEngine.pauseGame()
            GameStatus.PAUSED, GameStatus.INSTRUCTIONS, GameStatus.GAME_OVER -> gameEngine.openMenu()
            GameStatus.MENU -> {
                // Exit app handled by system
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionLeft, Key.A -> {
                            gameEngine.moveLeft()
                            true
                        }
                        Key.DirectionRight, Key.D -> {
                            gameEngine.moveRight()
                            true
                        }
                        Key.DirectionUp, Key.W, Key.Spacebar -> {
                            gameEngine.jump()
                            true
                        }
                        Key.DirectionDown, Key.S -> {
                            gameEngine.slide()
                            true
                        }
                        Key.P, Key.Escape -> {
                            if (gameEngine.status == GameStatus.PLAYING) {
                                gameEngine.pauseGame()
                            } else if (gameEngine.status == GameStatus.PAUSED) {
                                gameEngine.resumeGame()
                            }
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Render 2D / 3D Canvas Game World (always active or in background)
        // referencing frameTick ensures recomposition each frame
        if (frameTick != -1L) {
            NeonRunnerCanvas(
                gameEngine = gameEngine,
                onTap = {
                    if (gameEngine.status == GameStatus.PLAYING) {
                        gameEngine.jump()
                    }
                },
                onSwipeLeft = { gameEngine.moveLeft() },
                onSwipeRight = { gameEngine.moveRight() },
                onSwipeUp = { gameEngine.jump() },
                onSwipeDown = { gameEngine.slide() }
            )
        }

        // Overlay Game HUD while playing or paused
        if (gameEngine.status == GameStatus.PLAYING || gameEngine.status == GameStatus.PAUSED) {
            GameHud(
                gameEngine = gameEngine,
                showTouchButtons = showTouchButtons,
                onPauseClick = { gameEngine.pauseGame() },
                onLeftClick = { gameEngine.moveLeft() },
                onRightClick = { gameEngine.moveRight() },
                onJumpClick = { gameEngine.jump() },
                onSlideClick = { gameEngine.slide() },
                modifier = Modifier.safeDrawingPadding()
            )
        }

        // Menu Screen
        if (gameEngine.status == GameStatus.MENU) {
            MenuScreen(
                preferences = preferences,
                onStartGame = { gameEngine.startGame() },
                onHowToPlayClick = { gameEngine.openInstructions() },
                onToggleSound = { enabled ->
                    isSoundEnabled = enabled
                    preferences.isSoundEnabled = enabled
                },
                onToggleTouchButtons = { enabled ->
                    showTouchButtons = enabled
                    preferences.showTouchButtons = enabled
                },
                modifier = Modifier.safeDrawingPadding()
            )
        }

        // Instructions Dialog
        if (gameEngine.status == GameStatus.INSTRUCTIONS) {
            InstructionsDialog(
                onDismiss = { gameEngine.openMenu() }
            )
        }

        // Pause Dialog
        if (gameEngine.status == GameStatus.PAUSED) {
            PauseDialog(
                isSoundEnabled = isSoundEnabled,
                onResume = { gameEngine.resumeGame() },
                onRestart = { gameEngine.startGame() },
                onToggleSound = {
                    val toggled = !isSoundEnabled
                    isSoundEnabled = toggled
                    preferences.isSoundEnabled = toggled
                },
                onQuitToMenu = { gameEngine.openMenu() }
            )
        }

        // Game Over Screen
        if (gameEngine.status == GameStatus.GAME_OVER) {
            GameOverScreen(
                gameEngine = gameEngine,
                preferences = preferences,
                onRestart = { gameEngine.startGame() },
                onQuitToMenu = { gameEngine.openMenu() }
            )
        }
    }
}
