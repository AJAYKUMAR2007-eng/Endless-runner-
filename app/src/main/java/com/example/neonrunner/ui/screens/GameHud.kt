package com.example.neonrunner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neonrunner.game.GameEngine

@Composable
fun GameHud(
    gameEngine: GameEngine,
    showTouchButtons: Boolean,
    onPauseClick: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onJumpClick: () -> Unit,
    onSlideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- Top Status Bar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x990A061C))
                    .border(1.dp, Color(0x6000E5FF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score & Distance
                Column {
                    Text(
                        text = "SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${gameEngine.score}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Distance",
                            tint = Color(0xFFFF007F),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${gameEngine.distanceTraveled.toInt()}m",
                            fontSize = 12.sp,
                            color = Color(0xFFFF80BF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Coins Stash
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x40FFD700))
                        .border(1.dp, Color(0x80FFD700), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFFFEE55), Color(0xFFFF9900))),
                                CircleShape
                            )
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${gameEngine.coinsCollected}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Lives / Battery Cells
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..gameEngine.player.maxHealth) {
                        val isAlive = i <= gameEngine.player.health
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Health $i",
                            tint = if (isAlive) Color(0xFFFF2A6D) else Color(0x40555555),
                            modifier = Modifier
                                .size(22.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }

                // Pause Button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .testTag("pause_button")
                        .size(48.dp)
                        .background(Color(0x60FF007F), CircleShape)
                        .border(1.5.dp, Color(0xFFFF007F), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Power-up active timers bar
            if (gameEngine.player.hasShield) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x80002233))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SHIELD AURA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { (gameEngine.player.shieldTimer / 9f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0x3000E5FF)
                    )
                }
            }
        }

        // --- Bottom On-Screen Buttons (when enabled) ---
        if (showTouchButtons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steer Left & Right
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledIconButton(
                        onClick = onLeftClick,
                        modifier = Modifier
                            .testTag("steer_left_button")
                            .size(56.dp)
                            .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0x880C1433)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Steer Left",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onRightClick,
                        modifier = Modifier
                            .testTag("steer_right_button")
                            .size(56.dp)
                            .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0x880C1433)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Steer Right",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Jump & Slide
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledIconButton(
                        onClick = onJumpClick,
                        modifier = Modifier
                            .testTag("jump_action_button")
                            .size(56.dp)
                            .border(1.5.dp, Color(0xFFFF007F), CircleShape),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0x88330C28)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Jump Action",
                            tint = Color(0xFFFF007F),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onSlideClick,
                        modifier = Modifier
                            .testTag("slide_action_button")
                            .size(56.dp)
                            .border(1.5.dp, Color(0xFFFFD700), CircleShape),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0x8833280C)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Slide Action",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
