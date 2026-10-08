package com.roloam.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.roloam.app.model.TransportMode
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay

private enum class NaturePhase { MORNING, DAY, EVENING, NIGHT }

private fun phaseFor(hour: Int): NaturePhase = when (hour) {
    in 5..8 -> NaturePhase.MORNING
    in 9..16 -> NaturePhase.DAY
    in 17..20 -> NaturePhase.EVENING
    else -> NaturePhase.NIGHT
}

@Composable
fun HomeAsciiHero(
    mode: TransportMode,
    modifier: Modifier = Modifier
) {
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hour = LocalTime.now().hour
        }
    }

    val phase = phaseFor(hour)
    val ink = androidx.compose.material3.MaterialTheme.colorScheme.onBackground

    val transition = rememberInfiniteTransition(label = "roloam_ambient")
    val breathe by transition.animateFloat(
        initialValue = .82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val twinkle by transition.animateFloat(
        initialValue = .28f,
        targetValue = .92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )
    val trailDrift by transition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trail_drift"
    )

    // Transport continues to drive JourneyHeader; the landscape itself stays calm.
    when (mode) {
        TransportMode.CAR,
        TransportMode.TRAIN,
        TransportMode.BIKE,
        TransportMode.WALK -> Unit
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(244.dp)
    ) {
        val far = ink.copy(alpha = .20f * breathe)
        val mid = ink.copy(alpha = .42f * breathe)
        val near = ink.copy(alpha = .68f * breathe)
        val trail = ink.copy(alpha = .78f * breathe)

        drawSky(
            phase = phase,
            ink = ink,
            accent = RoloamAccent,
            twinkle = twinkle
        )

        // Far, soft skyline.
        drawMountainRange(
            baseY = size.height * .48f,
            points = listOf(
                Offset(size.width * .00f, size.height * .47f),
                Offset(size.width * .10f, size.height * .33f),
                Offset(size.width * .20f, size.height * .45f),
                Offset(size.width * .30f, size.height * .38f),
                Offset(size.width * .40f, size.height * .48f),
                Offset(size.width * .53f, size.height * .31f),
                Offset(size.width * .66f, size.height * .46f),
                Offset(size.width * .79f, size.height * .35f),
                Offset(size.width * .91f, size.height * .45f),
                Offset(size.width, size.height * .40f)
            ),
            color = far,
            spacing = 8.5f,
            radius = 1.15f
        )

        // Main ridge.
        drawMountainRange(
            baseY = size.height * .62f,
            points = listOf(
                Offset(size.width * .00f, size.height * .60f),
                Offset(size.width * .14f, size.height * .44f),
                Offset(size.width * .27f, size.height * .56f),
                Offset(size.width * .43f, size.height * .40f),
                Offset(size.width * .58f, size.height * .57f),
                Offset(size.width * .75f, size.height * .45f),
                Offset(size.width * .89f, size.height * .58f),
                Offset(size.width, size.height * .51f)
            ),
            color = mid,
            spacing = 6.4f,
            radius = 1.35f
        )

        drawValleyTexture(
            color = far.copy(alpha = far.alpha * 1.15f)
        )

        // Asymmetric foreground forest.
        drawPineCluster(
            startX = size.width * .035f,
            groundY = size.height * .70f,
            count = 5,
            stepX = size.width * .045f,
            baseScale = 1.05f,
            color = near
        )
        drawPineCluster(
            startX = size.width * .75f,
            groundY = size.height * .70f,
            count = 4,
            stepX = size.width * .052f,
            baseScale = .92f,
            color = near
        )
        drawPineCluster(
            startX = size.width * .13f,
            groundY = size.height * .82f,
            count = 3,
            stepX = size.width * .055f,
            baseScale = .78f,
            color = mid
        )

        drawRoamingTrail(
            color = trail,
            drift = trailDrift
        )

        drawCampGlyph(
            center = Offset(size.width * .70f, size.height * .78f),
            color = mid
        )
    }
}

private fun DrawScope.drawSky(
    phase: NaturePhase,
    ink: Color,
    accent: Color,
    twinkle: Float
) {
    when (phase) {
        NaturePhase.MORNING -> {
            drawDotSun(
                center = Offset(size.width * .70f, size.height * .13f),
                radius = size.width * .030f,
                color = accent.copy(alpha = .78f)
            )
            drawStars(
                listOf(
                    Offset(.17f, .16f),
                    Offset(.36f, .11f),
                    Offset(.50f, .18f)
                ),
                ink.copy(alpha = .20f)
            )
        }

        NaturePhase.DAY -> {
            drawDotSun(
                center = Offset(size.width * .72f, size.height * .12f),
                radius = size.width * .031f,
                color = accent.copy(alpha = .90f)
            )
        }

        NaturePhase.EVENING -> {
            drawDotSun(
                center = Offset(size.width * .67f, size.height * .14f),
                radius = size.width * .031f,
                color = accent.copy(alpha = .68f)
            )
            drawStars(
                listOf(
                    Offset(.16f, .15f),
                    Offset(.34f, .10f),
                    Offset(.49f, .18f),
                    Offset(.86f, .09f)
                ),
                ink.copy(alpha = .22f + .12f * twinkle)
            )
        }

        NaturePhase.NIGHT -> {
            drawDotMoon(
                center = Offset(size.width * .72f, size.height * .12f),
                radius = size.width * .031f,
                color = accent.copy(alpha = .88f)
            )
            drawStars(
                listOf(
                    Offset(.08f, .13f),
                    Offset(.19f, .08f),
                    Offset(.31f, .18f),
                    Offset(.42f, .10f),
                    Offset(.56f, .17f),
                    Offset(.63f, .07f),
                    Offset(.84f, .16f),
                    Offset(.91f, .08f)
                ),
                ink.copy(alpha = .34f + .34f * twinkle)
            )
        }
    }
}

private fun DrawScope.drawMountainRange(
    baseY: Float,
    points: List<Offset>,
    color: Color,
    spacing: Float,
    radius: Float
) {
    val canvasWidth = size.width
    val full = buildList {
        add(Offset(0f, baseY))
        addAll(points)
        add(Offset(canvasWidth, baseY))
    }

    full.zipWithNext().forEach { (a, b) ->
        dottedLine(a, b, color, spacing, radius)
    }
}

private fun DrawScope.drawValleyTexture(color: Color) {
    val rows = listOf(
        Triple(.68f, .22f, .82f),
        Triple(.72f, .26f, .78f),
        Triple(.76f, .31f, .72f),
        Triple(.80f, .36f, .66f)
    )

    rows.forEachIndexed { row, (yFactor, leftFactor, rightFactor) ->
        var x = size.width * leftFactor
        val end = size.width * rightFactor
        val step = 12f + row * 1.5f
        var n = 0

        while (x < end) {
            if ((n + row) % 4 != 1) {
                drawCircle(
                    color = color,
                    radius = 1.0f,
                    center = Offset(x, size.height * yFactor)
                )
            }
            x += step
            n++
        }
    }
}

private fun DrawScope.drawPineCluster(
    startX: Float,
    groundY: Float,
    count: Int,
    stepX: Float,
    baseScale: Float,
    color: Color
) {
    repeat(count) { index ->
        val scalePattern = when (index % 4) {
            0 -> 1.00f
            1 -> .78f
            2 -> 1.14f
            else -> .88f
        }
        drawPine(
            x = startX + index * stepX,
            groundY = groundY + (index % 2) * 4f,
            scale = baseScale * scalePattern,
            color = color
        )
    }
}

private fun DrawScope.drawPine(
    x: Float,
    groundY: Float,
    scale: Float,
    color: Color
) {
    val top = groundY - 34f * scale

    dottedLine(
        start = Offset(x, top + 8f * scale),
        end = Offset(x, groundY),
        color = color.copy(alpha = color.alpha * .78f),
        spacing = 4.8f,
        radius = 1.1f
    )

    repeat(4) { level ->
        val y = top + 7f + level * 7.5f * scale
        val half = (6f + level * 4.2f) * scale
        dottedLine(
            start = Offset(x - half, y + 5f * scale),
            end = Offset(x, y - 3f * scale),
            color = color,
            spacing = 4.2f,
            radius = 1.2f
        )
        dottedLine(
            start = Offset(x, y - 3f * scale),
            end = Offset(x + half, y + 5f * scale),
            color = color,
            spacing = 4.2f,
            radius = 1.2f
        )
    }
}

private fun DrawScope.drawRoamingTrail(
    color: Color,
    drift: Float
) {
    val points = listOf(
        Offset(size.width * .47f + drift * 2f, size.height * .99f),
        Offset(size.width * .38f + drift * 1.7f, size.height * .93f),
        Offset(size.width * .34f + drift * 1.3f, size.height * .87f),
        Offset(size.width * .40f + drift, size.height * .82f),
        Offset(size.width * .50f + drift * .7f, size.height * .79f),
        Offset(size.width * .57f + drift * .5f, size.height * .75f),
        Offset(size.width * .54f + drift * .3f, size.height * .70f),
        Offset(size.width * .58f, size.height * .65f)
    )

    points.zipWithNext().forEachIndexed { index, (a, b) ->
        dottedLine(
            start = a,
            end = b,
            color = color.copy(alpha = color.alpha * (1f - index * .055f)),
            spacing = 5.2f + index * .45f,
            radius = 1.65f - index * .06f
        )
    }
}

private fun DrawScope.drawCampGlyph(
    center: Offset,
    color: Color
) {
    val left = Offset(center.x - 8f, center.y + 6f)
    val top = Offset(center.x, center.y - 7f)
    val right = Offset(center.x + 8f, center.y + 6f)

    dottedLine(left, top, color.copy(alpha = color.alpha * .72f), 3.8f, 1.05f)
    dottedLine(top, right, color.copy(alpha = color.alpha * .72f), 3.8f, 1.05f)
    dottedLine(left, right, color.copy(alpha = color.alpha * .72f), 3.8f, 1.05f)
}

private fun DrawScope.drawDotSun(
    center: Offset,
    radius: Float,
    color: Color
) {
    repeat(20) { index ->
        val angle = 2f * PI.toFloat() * index / 20f
        drawCircle(
            color = color,
            radius = 1.55f,
            center = Offset(
                center.x + cos(angle) * radius,
                center.y + sin(angle) * radius
            )
        )
    }
}

private fun DrawScope.drawDotMoon(
    center: Offset,
    radius: Float,
    color: Color
) {
    // Two dotted arcs form a crescent; no blend-mode clearing required.
    val outerStart = 55
    val outerEnd = 305
    var angle = outerStart
    while (angle <= outerEnd) {
        val rad = angle * PI.toFloat() / 180f
        drawCircle(
            color = color,
            radius = 1.55f,
            center = Offset(
                center.x + cos(rad) * radius,
                center.y + sin(rad) * radius
            )
        )
        angle += 14
    }

    angle = 75
    while (angle <= 285) {
        val rad = angle * PI.toFloat() / 180f
        drawCircle(
            color = color.copy(alpha = color.alpha * .52f),
            radius = 1.0f,
            center = Offset(
                center.x + radius * .28f + cos(rad) * radius * .72f,
                center.y + sin(rad) * radius * .72f
            )
        )
        angle += 18
    }
}

private fun DrawScope.drawStars(
    points: List<Offset>,
    color: Color
) {
    points.forEachIndexed { index, p ->
        drawCircle(
            color = color.copy(alpha = color.alpha * if (index % 2 == 0) 1f else .68f),
            radius = if (index % 3 == 0) 1.5f else 1.15f,
            center = Offset(size.width * p.x, size.height * p.y)
        )
    }
}

private fun DrawScope.dottedLine(
    start: Offset,
    end: Offset,
    color: Color,
    spacing: Float,
    radius: Float
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val distance = sqrt(dx * dx + dy * dy)
    val steps = (distance / spacing).toInt().coerceAtLeast(1)

    repeat(steps + 1) { index ->
        val t = index / steps.toFloat()
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(
                start.x + dx * t,
                start.y + dy * t
            )
        )
    }
}
