package com.example.neonrunner.ui.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.neonrunner.game.GameEngine
import com.example.neonrunner.model.Collectible
import com.example.neonrunner.model.CollectibleType
import com.example.neonrunner.model.FloatingText
import com.example.neonrunner.model.Obstacle
import com.example.neonrunner.model.ObstacleType
import com.example.neonrunner.model.Particle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun NeonRunnerCanvas(
    gameEngine: GameEngine,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {}
) {
    val isPlaying = gameEngine.status == com.example.neonrunner.model.GameStatus.PLAYING
    val gestureModifier = if (isPlaying) {
        Modifier.pointerInput(isPlaying) {
            var totalDragX = 0f
            var totalDragY = 0f
            val dragThreshold = 35f

            detectDragGestures(
                onDragStart = {
                    totalDragX = 0f
                    totalDragY = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    totalDragX += dragAmount.x
                    totalDragY += dragAmount.y
                },
                onDragEnd = {
                    val absX = abs(totalDragX)
                    val absY = abs(totalDragY)
                    if (absX < dragThreshold && absY < dragThreshold) {
                        onTap()
                    } else if (absX > absY) {
                        if (totalDragX < -dragThreshold) {
                            onSwipeLeft()
                        } else if (totalDragX > dragThreshold) {
                            onSwipeRight()
                        }
                    } else {
                        if (totalDragY < -dragThreshold) {
                            onSwipeUp()
                        } else if (totalDragY > dragThreshold) {
                            onSwipeDown()
                        }
                    }
                },
                onDragCancel = {
                    if (abs(totalDragX) < dragThreshold && abs(totalDragY) < dragThreshold) {
                        onTap()
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(gestureModifier)
    ) {
        val width = size.width
        val height = size.height

        // Apply camera screen shake
        translate(left = gameEngine.shakeX, top = gameEngine.shakeY) {
            val horizonY = height * 0.35f
            val roadBottomY = height * 0.92f
            val vpX = width * 0.5f
            val roadBottomWidth = width * 0.88f
            val roadTopWidth = width * 0.12f

            // 1. Synthwave Sky Background & Retro Sun
            drawSkyAndSun(width, horizonY, vpX)

            // 2. Parallax Cyberpunk City Skyline
            drawCitySkyline(width, horizonY, gameEngine.player.visualLane)

            // 3. Perspective Neon Road & Animated Grid
            drawRoadAndGrid(
                width = width,
                horizonY = horizonY,
                roadBottomY = roadBottomY,
                vpX = vpX,
                roadTopWidth = roadTopWidth,
                roadBottomWidth = roadBottomWidth,
                distanceTraveled = gameEngine.distanceTraveled
            )

            // Helper projection lambda: world (lane, z, height) -> Screen (x, y, scale)
            val project: (Float, Float, Float) -> ProjectResult = { lane, z, h ->
                val depth = 1.0f / (1.0f + max(0f, z) * 0.042f)
                val screenY = horizonY + (roadBottomY - horizonY) * depth - (height * 0.14f * h) * depth
                val laneSpacing = (roadBottomWidth / 3.0f) * depth
                val screenX = vpX + (lane * laneSpacing)
                ProjectResult(screenX, screenY, depth)
            }

            // 4. Draw distant obstacles & collectibles (sorted by distance descending)
            drawWorldEntities(
                gameEngine = gameEngine,
                project = project,
                width = width,
                height = height
            )

            // 5. Draw Player Character
            drawPlayer(
                gameEngine = gameEngine,
                project = project,
                width = width,
                height = height
            )

            // 6. Draw speed motion lines when running fast
            if (gameEngine.currentSpeed > 24f) {
                drawSpeedLines(width, height, horizonY, vpX, gameEngine.currentSpeed)
            }

            // 7. Draw Visual Particles
            drawParticles(gameEngine.particles, project)

            // 8. Draw Floating Popup Texts
            drawFloatingTexts(gameEngine.floatingTexts, project)
        }
    }
}

private data class ProjectResult(val x: Float, val y: Float, val scale: Float)

// --- Background Graphics ---

private fun DrawScope.drawSkyAndSun(width: Float, horizonY: Float, vpX: Float) {
    // Synthwave sky gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF060212),
                Color(0xFF130728),
                Color(0xFF2C0B4D),
                Color(0xFF4A0E4E)
            ),
            startY = 0f,
            endY = horizonY
        ),
        topLeft = Offset(0f, 0f),
        size = Size(width, horizonY)
    )

    // Retro segmented synthwave sun
    val sunRadius = width * 0.22f
    val sunCenterY = horizonY - (sunRadius * 0.15f)

    // Sun outer aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x80FF007F), Color(0x30FF5500), Color(0x00000000)),
            center = Offset(vpX, sunCenterY),
            radius = sunRadius * 1.5f
        ),
        radius = sunRadius * 1.5f,
        center = Offset(vpX, sunCenterY)
    )

    // Sun core
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFEE55), Color(0xFFFF007F)),
            startY = sunCenterY - sunRadius,
            endY = sunCenterY + sunRadius
        ),
        radius = sunRadius,
        center = Offset(vpX, sunCenterY)
    )

    // Sun horizontal cutout bars (giving the iconic 80s sliced look)
    val barCount = 7
    for (i in 0 until barCount) {
        val t = (i + 1).toFloat() / (barCount + 1)
        val barY = sunCenterY + (sunRadius * (t * 0.95f))
        val barThickness = (2.5f + (i * 2.2f))
        drawLine(
            color = Color(0xFF180838),
            start = Offset(vpX - sunRadius, barY),
            end = Offset(vpX + sunRadius, barY),
            strokeWidth = barThickness
        )
    }
}

private fun DrawScope.drawCitySkyline(width: Float, horizonY: Float, playerLane: Float) {
    // Skyline base shift with subtle parallax
    val parallaxOffset = -playerLane * (width * 0.02f)

    // Distant dark skyscrapers
    val buildingCount = 14
    val buildingWidth = width / 10f

    for (i in -2..buildingCount + 2) {
        val bx = i * buildingWidth + parallaxOffset
        // Deterministic heights based on index
        val pseudoHash = ((i * 37 + 101) % 17) / 17f
        val bHeight = horizonY * (0.25f + pseudoHash * 0.45f)
        val by = horizonY - bHeight

        // Dark skyscraper body
        drawRect(
            color = Color(0xFF0C0720),
            topLeft = Offset(bx, by),
            size = Size(buildingWidth * 0.92f, bHeight)
        )

        // Skyscraper roof neon outline
        val outlineColor = if (i % 2 == 0) Color(0x7000E5FF) else Color(0x70FF007F)
        drawLine(
            color = outlineColor,
            start = Offset(bx, by),
            end = Offset(bx + buildingWidth * 0.92f, by),
            strokeWidth = 2f
        )

        // Antenna with blinking beacon
        if (i % 3 == 0) {
            val antennaX = bx + buildingWidth * 0.46f
            val antennaH = bHeight * 0.25f
            drawLine(
                color = Color(0xFF1E113A),
                start = Offset(antennaX, by),
                end = Offset(antennaX, by - antennaH),
                strokeWidth = 2f
            )
            drawCircle(
                color = if (i % 2 == 0) Color(0xFFFF2A6D) else Color(0xFF00FFCC),
                radius = 3f,
                center = Offset(antennaX, by - antennaH)
            )
        }
    }
}

// --- Road & Perspective Grid ---

private fun DrawScope.drawRoadAndGrid(
    width: Float,
    horizonY: Float,
    roadBottomY: Float,
    vpX: Float,
    roadTopWidth: Float,
    roadBottomWidth: Float,
    distanceTraveled: Float
) {
    // 1. Off-road terrain (dark neon grid ground)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF15082E), Color(0xFF050110)),
            startY = horizonY,
            endY = size.height
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width, size.height - horizonY)
    )

    // 2. Road surface trapezoid
    val roadPath = Path().apply {
        moveTo(vpX - roadTopWidth / 2f, horizonY)
        lineTo(vpX + roadTopWidth / 2f, horizonY)
        lineTo(vpX + roadBottomWidth / 2f, roadBottomY + (size.height - roadBottomY))
        lineTo(vpX - roadBottomWidth / 2f, roadBottomY + (size.height - roadBottomY))
        close()
    }

    drawPath(
        path = roadPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1B0A33), Color(0xFF0D061A), Color(0xFF070310)),
            startY = horizonY,
            endY = roadBottomY
        )
    )

    // 3. Glowing Outer Guardrails (Electric Cyan)
    val guardrailGlow = 8f
    drawLine(
        color = Color(0x4000E5FF),
        start = Offset(vpX - roadTopWidth / 2f, horizonY),
        end = Offset(vpX - roadBottomWidth / 2f, roadBottomY),
        strokeWidth = guardrailGlow
    )
    drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(vpX - roadTopWidth / 2f, horizonY),
        end = Offset(vpX - roadBottomWidth / 2f, roadBottomY),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0x4000E5FF),
        start = Offset(vpX + roadTopWidth / 2f, horizonY),
        end = Offset(vpX + roadBottomWidth / 2f, roadBottomY),
        strokeWidth = guardrailGlow
    )
    drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(vpX + roadTopWidth / 2f, horizonY),
        end = Offset(vpX + roadBottomWidth / 2f, roadBottomY),
        strokeWidth = 3f
    )

    // 4. Lane Divider Lines (Neon Magenta dashes)
    val laneOffsets = listOf(-0.5f, 0.5f)
    for (offset in laneOffsets) {
        val topX = vpX + (offset * (roadTopWidth / 1.5f))
        val botX = vpX + (offset * (roadBottomWidth / 3.0f) * 2f)

        // Draw segmented glowing lines
        val segments = 16
        for (s in 0 until segments) {
            val t1 = s.toFloat() / segments
            val t2 = (s + 0.55f) / segments
            val segY1 = horizonY + (roadBottomY - horizonY) * (t1 * t1)
            val segY2 = horizonY + (roadBottomY - horizonY) * (t2 * t2)
            val segX1 = topX + (botX - topX) * (t1 * t1)
            val segX2 = topX + (botX - topX) * (t2 * t2)

            drawLine(
                color = Color(0x90FF007F),
                start = Offset(segX1, segY1),
                end = Offset(segX2, segY2),
                strokeWidth = 2.5f * (1f + t2 * 2f)
            )
        }
    }

    // 5. Perspective Animated Horizontal Speed Cross-Lines
    val gridSpacing = 4f
    val offset = (distanceTraveled % gridSpacing)
    for (i in 0..25) {
        val worldZ = (i * gridSpacing - offset).coerceAtLeast(0.1f)
        val depth = 1.0f / (1.0f + worldZ * 0.042f)
        val lineY = horizonY + (roadBottomY - horizonY) * depth
        val currentRoadW = roadTopWidth + (roadBottomWidth - roadTopWidth) * depth

        val alpha = ((depth - 0.2f) / 0.8f).coerceIn(0f, 0.65f)
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = alpha * 0.5f),
            start = Offset(vpX - currentRoadW / 2f, lineY),
            end = Offset(vpX + currentRoadW / 2f, lineY),
            strokeWidth = 2f * depth
        )

        // Roadside neon light beacons
        if (i % 2 == 0) {
            val beaconH = 18f * depth
            val leftBeaconX = vpX - currentRoadW / 2f
            val rightBeaconX = vpX + currentRoadW / 2f

            drawLine(
                color = Color(0xFFFF007F).copy(alpha = alpha),
                start = Offset(leftBeaconX, lineY),
                end = Offset(leftBeaconX, lineY - beaconH),
                strokeWidth = 3f * depth
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = alpha),
                radius = 3.5f * depth,
                center = Offset(leftBeaconX, lineY - beaconH)
            )

            drawLine(
                color = Color(0xFFFF007F).copy(alpha = alpha),
                start = Offset(rightBeaconX, lineY),
                end = Offset(rightBeaconX, lineY - beaconH),
                strokeWidth = 3f * depth
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = alpha),
                radius = 3.5f * depth,
                center = Offset(rightBeaconX, lineY - beaconH)
            )
        }
    }
}

// --- Entities Drawing ---

private fun DrawScope.drawWorldEntities(
    gameEngine: GameEngine,
    project: (Float, Float, Float) -> ProjectResult,
    width: Float,
    height: Float
) {
    // Sort all visible obstacles and collectibles by distance descending so distant objects draw first
    val visibleObstacles = gameEngine.obstacles.filter { it.distance in -2f..90f }
    val visibleCollectibles = gameEngine.collectibles.filter { !it.collected && it.distance in -2f..90f }

    // Merge & sort by distance descending
    val renderList = mutableListOf<RenderItem>()
    visibleObstacles.forEach { renderList.add(RenderItem.ObstacleItem(it)) }
    visibleCollectibles.forEach { renderList.add(RenderItem.CollectibleItem(it)) }
    renderList.sortByDescending { it.distance }

    for (item in renderList) {
        when (item) {
            is RenderItem.ObstacleItem -> drawObstacle(item.obstacle, project, width, height)
            is RenderItem.CollectibleItem -> drawCollectible(item.collectible, project, width)
        }
    }
}

private sealed class RenderItem(val distance: Float) {
    class ObstacleItem(val obstacle: Obstacle) : RenderItem(obstacle.distance)
    class CollectibleItem(val collectible: Collectible) : RenderItem(collectible.distance)
}

private fun DrawScope.drawObstacle(
    obstacle: Obstacle,
    project: (Float, Float, Float) -> ProjectResult,
    width: Float,
    height: Float
) {
    val proj = project(obstacle.lane.toFloat(), obstacle.distance, 0f)
    val scale = proj.scale
    if (scale <= 0.05f) return

    val laneWidth = (width * 0.26f) * scale

    when (obstacle.type) {
        ObstacleType.HURDLE -> {
            // Low barrier: Jump over!
            val hurdleH = height * 0.05f * scale
            val hurdleW = laneWidth * 0.9f
            val topY = proj.y - hurdleH

            // Shadow
            drawOval(
                color = Color(0x60000000),
                topLeft = Offset(proj.x - hurdleW * 0.5f, proj.y - 4f * scale),
                size = Size(hurdleW, 10f * scale)
            )

            // Pylons on left and right
            val pylonW = 8f * scale
            drawRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(proj.x - hurdleW / 2f, topY),
                size = Size(pylonW, hurdleH)
            )
            drawRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(proj.x + hurdleW / 2f - pylonW, topY),
                size = Size(pylonW, hurdleH)
            )

            // Crossbar with neon hazard stripes
            drawRect(
                color = Color(0xFFFF2A6D),
                topLeft = Offset(proj.x - hurdleW / 2f, topY),
                size = Size(hurdleW, 8f * scale)
            )
            // Glowing neon laser beam across top
            drawLine(
                color = Color(0xFFFFF070),
                start = Offset(proj.x - hurdleW / 2f, topY),
                end = Offset(proj.x + hurdleW / 2f, topY),
                strokeWidth = 3f * scale
            )

            // "JUMP" indicator icon on obstacle when close
            if (obstacle.distance in 5f..35f) {
                val iconY = topY - (16f * scale)
                drawCircle(
                    color = Color(0xCC00E5FF),
                    radius = 8f * scale,
                    center = Offset(proj.x, iconY)
                )
                // Up arrow
                val arrowPath = Path().apply {
                    moveTo(proj.x, iconY - 4f * scale)
                    lineTo(proj.x - 3.5f * scale, iconY + 3f * scale)
                    lineTo(proj.x + 3.5f * scale, iconY + 3f * scale)
                    close()
                }
                drawPath(path = arrowPath, color = Color.White)
            }
        }

        ObstacleType.OVERHEAD_BEAM -> {
            // High cyber gate: Slide under!
            val gateTotalH = height * 0.16f * scale
            val gateW = laneWidth * 0.95f
            val beamThickness = 12f * scale
            val beamTopY = proj.y - gateTotalH

            // High side posts
            val postW = 9f * scale
            drawRect(
                color = Color(0xFF7928CA),
                topLeft = Offset(proj.x - gateW / 2f, beamTopY),
                size = Size(postW, gateTotalH)
            )
            drawRect(
                color = Color(0xFF7928CA),
                topLeft = Offset(proj.x + gateW / 2f - postW, beamTopY),
                size = Size(postW, gateTotalH)
            )

            // Glowing High Laser Beam
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF0055), Color(0xFFFF5500)),
                    startY = beamTopY,
                    endY = beamTopY + beamThickness
                ),
                topLeft = Offset(proj.x - gateW / 2f, beamTopY),
                size = Size(gateW, beamThickness)
            )

            // Intense laser beam glow
            drawLine(
                color = Color(0x60FF0055),
                start = Offset(proj.x - gateW / 2f, beamTopY + beamThickness / 2f),
                end = Offset(proj.x + gateW / 2f, beamTopY + beamThickness / 2f),
                strokeWidth = beamThickness * 2f
            )

            // Slide warning indicator
            if (obstacle.distance in 5f..35f) {
                val iconY = beamTopY + beamThickness + 14f * scale
                drawCircle(
                    color = Color(0xCCFF2A6D),
                    radius = 8f * scale,
                    center = Offset(proj.x, iconY)
                )
                // Down arrow
                val arrowPath = Path().apply {
                    moveTo(proj.x, iconY + 4f * scale)
                    lineTo(proj.x - 3.5f * scale, iconY - 3f * scale)
                    lineTo(proj.x + 3.5f * scale, iconY - 3f * scale)
                    close()
                }
                drawPath(path = arrowPath, color = Color.White)
            }
        }

        ObstacleType.BLOCKADE -> {
            // Massive solid cyber drone / pillar: Must switch lane!
            val blockH = height * 0.15f * scale
            val blockW = laneWidth * 0.88f
            val blockTopY = proj.y - blockH

            // Ground shadow
            drawOval(
                color = Color(0x80000000),
                topLeft = Offset(proj.x - blockW * 0.5f, proj.y - 6f * scale),
                size = Size(blockW, 14f * scale)
            )

            // Block body
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E0854), Color(0xFF0F0024)),
                    startY = blockTopY,
                    endY = proj.y
                ),
                topLeft = Offset(proj.x - blockW / 2f, blockTopY),
                size = Size(blockW, blockH)
            )

            // Block neon border
            drawRect(
                color = Color(0xFFFF007F),
                topLeft = Offset(proj.x - blockW / 2f, blockTopY),
                size = Size(blockW, blockH),
                style = Stroke(width = 3f * scale)
            )

            // Warning X symbol in center
            val cx = proj.x
            val cy = blockTopY + blockH / 2f
            val xSpan = blockW * 0.3f
            val ySpan = blockH * 0.3f
            drawLine(
                color = Color(0xFFFF2A6D),
                start = Offset(cx - xSpan, cy - ySpan),
                end = Offset(cx + xSpan, cy + ySpan),
                strokeWidth = 3f * scale
            )
            drawLine(
                color = Color(0xFFFF2A6D),
                start = Offset(cx + xSpan, cy - ySpan),
                end = Offset(cx - xSpan, cy + ySpan),
                strokeWidth = 3f * scale
            )
        }
    }
}

private fun DrawScope.drawCollectible(
    collectible: Collectible,
    project: (Float, Float, Float) -> ProjectResult,
    width: Float
) {
    val proj = project(collectible.lane.toFloat(), collectible.distance, collectible.height)
    val scale = proj.scale
    if (scale <= 0.05f) return

    val cx = proj.x
    val cy = proj.y

    when (collectible.type) {
        CollectibleType.COIN -> {
            // Rotating gold neon diamond
            val coinSize = 22f * scale
            val rad = Math.toRadians(collectible.visualAngle.toDouble()).toFloat()
            val xFactor = cos(rad)

            // Ground shadow
            if (collectible.height == 0f) {
                drawOval(
                    color = Color(0x40000000),
                    topLeft = Offset(cx - coinSize * 0.6f, cy + coinSize * 0.8f),
                    size = Size(coinSize * 1.2f, 6f * scale)
                )
            }

            // Outer golden glow
            drawCircle(
                color = Color(0x40FFD700),
                radius = coinSize * 1.2f,
                center = Offset(cx, cy)
            )

            // Diamond polygon with perspective width based on rotation
            val dw = abs(coinSize * xFactor).coerceAtLeast(3f * scale)
            val dh = coinSize
            val diamondPath = Path().apply {
                moveTo(cx, cy - dh)
                lineTo(cx + dw, cy)
                lineTo(cx, cy + dh)
                lineTo(cx - dw, cy)
                close()
            }

            drawPath(
                path = diamondPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF070), Color(0xFFFF9900)),
                    start = Offset(cx - dw, cy - dh),
                    end = Offset(cx + dw, cy + dh)
                )
            )
            drawPath(
                path = diamondPath,
                color = Color(0xFFFFFFFF),
                style = Stroke(width = 1.5f * scale)
            )
        }

        CollectibleType.SHIELD -> {
            // Neon cyan shield orb
            val orbR = 22f * scale

            // Pulsing aura
            drawCircle(
                color = Color(0x4000E5FF),
                radius = orbR * 1.5f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0x9000E5FF),
                radius = orbR,
                center = Offset(cx, cy),
                style = Stroke(width = 3f * scale)
            )

            // Inner shield crest
            val crestPath = Path().apply {
                moveTo(cx, cy - orbR * 0.6f)
                lineTo(cx + orbR * 0.5f, cy - orbR * 0.3f)
                lineTo(cx + orbR * 0.4f, cy + orbR * 0.4f)
                lineTo(cx, cy + orbR * 0.7f)
                lineTo(cx - orbR * 0.4f, cy + orbR * 0.4f)
                lineTo(cx - orbR * 0.5f, cy - orbR * 0.3f)
                close()
            }
            drawPath(path = crestPath, color = Color(0xFF00FFCC))
            drawPath(path = crestPath, color = Color.White, style = Stroke(width = 1.5f * scale))
        }

        CollectibleType.MAGNET -> {
            // Neon magnet orb
            val orbR = 22f * scale
            drawCircle(
                color = Color(0x40BF55EC),
                radius = orbR * 1.5f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFFBF55EC),
                radius = orbR,
                center = Offset(cx, cy),
                style = Stroke(width = 3f * scale)
            )

            // Horseshoe U-symbol
            val uPath = Path().apply {
                moveTo(cx - orbR * 0.4f, cy - orbR * 0.4f)
                lineTo(cx - orbR * 0.4f, cy + orbR * 0.2f)
                // bottom curve
                cubicTo(
                    cx - orbR * 0.4f, cy + orbR * 0.6f,
                    cx + orbR * 0.4f, cy + orbR * 0.6f,
                    cx + orbR * 0.4f, cy + orbR * 0.2f
                )
                lineTo(cx + orbR * 0.4f, cy - orbR * 0.4f)
            }
            drawPath(path = uPath, color = Color(0xFFFF2A6D), style = Stroke(width = 4f * scale))
        }
    }
}

// --- Player Drawing ---

private fun DrawScope.drawPlayer(
    gameEngine: GameEngine,
    project: (Float, Float, Float) -> ProjectResult,
    width: Float,
    height: Float
) {
    val player = gameEngine.player

    // Invulnerability blink effect
    if (player.invulnerableTimer > 0f) {
        val blink = (player.invulnerableTimer * 20f).toInt() % 2 == 0
        if (blink && !player.hasShield) {
            // Skip drawing character body on alternate frames for flashing damage effect
            return
        }
    }

    // World position of player: z = 0f, height = player.jumpHeight
    val proj = project(player.visualLane, 0f, player.jumpHeight)
    val cx = proj.x
    val cy = proj.y
    val scale = proj.scale

    val playerH = height * 0.12f * scale
    val playerW = playerH * 0.5f

    // 1. Dynamic ground shadow
    val shadowProj = project(player.visualLane, 0f, 0f)
    val shadowW = playerW * (1f - (player.jumpHeight * 0.35f)).coerceAtLeast(0.3f)
    val shadowAlpha = (0.6f - player.jumpHeight * 0.3f).coerceIn(0.15f, 0.6f)
    drawOval(
        color = Color(0xFF000000).copy(alpha = shadowAlpha),
        topLeft = Offset(shadowProj.x - shadowW * 0.6f, shadowProj.y - 4f),
        size = Size(shadowW * 1.2f, 10f * scale)
    )

    // 2. Character Body & Limbs
    if (player.isSliding) {
        // --- Sliding Pose: Low dash / crouch slide with sparks ---
        val slideH = playerH * 0.45f
        val slideW = playerW * 1.25f
        val topY = cy - slideH

        // Cyber body sliding low
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00E5FF), Color(0xFF1F0D42)),
                startY = topY,
                endY = cy
            ),
            topLeft = Offset(cx - slideW / 2f, topY),
            size = Size(slideW, slideH)
        )

        // Glowing visor band
        drawOval(
            color = Color(0xFFFFF070),
            topLeft = Offset(cx - slideW * 0.3f, topY + slideH * 0.2f),
            size = Size(slideW * 0.6f, slideH * 0.3f)
        )

        // Neon cyber suit trim
        drawOval(
            color = Color(0xFFFF007F),
            topLeft = Offset(cx - slideW / 2f, topY),
            size = Size(slideW, slideH),
            style = Stroke(width = 2.5f * scale)
        )
    } else {
        // --- Upright Running or Jumping Pose ---
        val headRadius = playerW * 0.32f
        val headCenterY = cy - playerH + headRadius
        val torsoTopY = cy - playerH + (headRadius * 1.8f)
        val torsoH = playerH * 0.42f
        val legH = playerH * 0.4f

        // Animated running leg pumping
        val runAngle = (player.runCycle * 2.0 * PI).toFloat()
        val legOffsetLeft = if (player.isJumping) -headRadius * 0.3f else sin(runAngle) * (legH * 0.35f)
        val legOffsetRight = if (player.isJumping) -headRadius * 0.3f else -sin(runAngle) * (legH * 0.35f)

        // Left Leg
        val leftLegX = cx - (playerW * 0.22f)
        drawLine(
            color = Color(0xFF00E5FF),
            start = Offset(leftLegX, torsoTopY + torsoH),
            end = Offset(leftLegX, cy + legOffsetLeft),
            strokeWidth = 5f * scale
        )
        // Left shoe thruster
        drawCircle(
            color = Color(0xFFFF007F),
            radius = 3.5f * scale,
            center = Offset(leftLegX, cy + legOffsetLeft)
        )

        // Right Leg
        val rightLegX = cx + (playerW * 0.22f)
        drawLine(
            color = Color(0xFF00E5FF),
            start = Offset(rightLegX, torsoTopY + torsoH),
            end = Offset(rightLegX, cy + legOffsetRight),
            strokeWidth = 5f * scale
        )
        // Right shoe thruster
        drawCircle(
            color = Color(0xFFFF007F),
            radius = 3.5f * scale,
            center = Offset(rightLegX, cy + legOffsetRight)
        )

        // Torso / Cyber Jacket
        val torsoPath = Path().apply {
            moveTo(cx - playerW * 0.38f, torsoTopY)
            lineTo(cx + playerW * 0.38f, torsoTopY)
            lineTo(cx + playerW * 0.25f, torsoTopY + torsoH)
            lineTo(cx - playerW * 0.25f, torsoTopY + torsoH)
            close()
        }
        drawPath(
            path = torsoPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2C145C), Color(0xFF13092A)),
                startY = torsoTopY,
                endY = torsoTopY + torsoH
            )
        )
        drawPath(
            path = torsoPath,
            color = Color(0xFFFF007F),
            style = Stroke(width = 2.5f * scale)
        )

        // Neon cyber power core in center of jacket
        drawCircle(
            color = Color(0xFF00FFCC),
            radius = 3.5f * scale,
            center = Offset(cx, torsoTopY + torsoH * 0.45f)
        )

        // Animated Arms
        val armSwing = if (player.isJumping) -0.4f else cos(runAngle) * 0.5f
        drawLine(
            color = Color(0xFF00E5FF),
            start = Offset(cx - playerW * 0.38f, torsoTopY + 4f * scale),
            end = Offset(cx - playerW * 0.55f, torsoTopY + torsoH * 0.6f + (armSwing * 12f * scale)),
            strokeWidth = 4f * scale
        )
        drawLine(
            color = Color(0xFF00E5FF),
            start = Offset(cx + playerW * 0.38f, torsoTopY + 4f * scale),
            end = Offset(cx + playerW * 0.55f, torsoTopY + torsoH * 0.6f - (armSwing * 12f * scale)),
            strokeWidth = 4f * scale
        )

        // Cyber Helmet
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF421A7F), Color(0xFF13092A)),
                startY = headCenterY - headRadius,
                endY = headCenterY + headRadius
            ),
            radius = headRadius,
            center = Offset(cx, headCenterY)
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = headRadius,
            center = Offset(cx, headCenterY),
            style = Stroke(width = 2f * scale)
        )

        // Glowing visor band on back/side of helmet
        drawOval(
            color = Color(0xFF00FFCC),
            topLeft = Offset(cx - headRadius * 0.7f, headCenterY - headRadius * 0.25f),
            size = Size(headRadius * 1.4f, headRadius * 0.5f)
        )
    }

    // 3. Cyber Shield Aura Bubble (when active)
    if (player.hasShield) {
        val shieldR = playerH * 0.75f
        val pulse = 1f + sin(System.currentTimeMillis() * 0.008f) * 0.08f

        // Translucent shield dome
        drawCircle(
            color = Color(0x3000E5FF),
            radius = shieldR * pulse,
            center = Offset(cx, cy - playerH * 0.5f)
        )
        drawCircle(
            color = Color(0xCC00E5FF),
            radius = shieldR * pulse,
            center = Offset(cx, cy - playerH * 0.5f),
            style = Stroke(width = 3.5f * scale)
        )

        // Rotating shield nodes
        val nodeCount = 3
        val angleBase = (System.currentTimeMillis() * 0.003f)
        for (n in 0 until nodeCount) {
            val nodeAngle = angleBase + (n * (2.0 * PI / nodeCount))
            val nx = cx + cos(nodeAngle).toFloat() * (shieldR * pulse)
            val ny = (cy - playerH * 0.5f) + sin(nodeAngle).toFloat() * (shieldR * pulse * 0.65f)
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = 4f * scale,
                center = Offset(nx, ny)
            )
        }
    }

    // 4. Magnet Aura (when active)
    if (player.hasMagnet) {
        val magnetPulse = sin(System.currentTimeMillis() * 0.012f) * 4f
        drawCircle(
            color = Color(0x80BF55EC),
            radius = playerH * 0.6f + magnetPulse,
            center = Offset(cx, cy - playerH * 0.5f),
            style = Stroke(width = 2.5f * scale)
        )
    }
}

// --- Speed Lines ---

private fun DrawScope.drawSpeedLines(
    width: Float,
    height: Float,
    horizonY: Float,
    vpX: Float,
    speed: Float
) {
    val count = 12
    val intensity = ((speed - 24f) / 18f).coerceIn(0f, 1f)
    val time = System.currentTimeMillis() * 0.005f

    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * 2.0 * PI + (time * 0.2)
        val dirX = cos(angle).toFloat()
        val dirY = sin(angle).toFloat()

        if (dirY > -0.2f) { // mostly forward/side
            val startDist = width * 0.35f
            val endDist = width * 0.55f

            val sx = vpX + dirX * startDist
            val sy = horizonY + dirY * (height * 0.35f)
            val ex = vpX + dirX * endDist
            val ey = horizonY + dirY * (height * 0.55f)

            drawLine(
                color = Color(0x5000E5FF).copy(alpha = 0.25f * intensity),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = 2.5f
            )
        }
    }
}

// --- Particle Effects ---

private fun DrawScope.drawParticles(
    particles: List<Particle>,
    project: (Float, Float, Float) -> ProjectResult
) {
    for (p in particles) {
        val proj = project(p.x, 0f, p.y)
        val alpha = (1.0f - (p.life / p.maxLife)).coerceIn(0f, 1f)
        val renderSize = (p.size * proj.scale).coerceAtLeast(1.5f)

        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = renderSize,
            center = Offset(proj.x, proj.y)
        )
    }
}

// --- Floating Text Popups ---

private fun DrawScope.drawFloatingTexts(
    floatingTexts: List<FloatingText>,
    project: (Float, Float, Float) -> ProjectResult
) {
    val native = drawContext.canvas.nativeCanvas

    for (ft in floatingTexts) {
        val proj = project(ft.x, 0f, ft.y)
        val alpha = (1.0f - (ft.life / ft.maxLife)).coerceIn(0f, 1f)
        val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)

        val paint = Paint().apply {
            color = android.graphics.Color.argb(
                alphaInt,
                (ft.color.red * 255).toInt(),
                (ft.color.green * 255).toInt(),
                (ft.color.blue * 255).toInt()
            )
            textSize = 38f * proj.scale.coerceAtLeast(0.6f)
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
        }

        native.drawText(ft.text, proj.x, proj.y, paint)
    }
}
