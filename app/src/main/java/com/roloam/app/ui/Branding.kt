package com.roloam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.model.TransportMode

@Composable
fun RoloamMark(modifier: Modifier = Modifier.size(34.dp)) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background

    Canvas(modifier) {
        val u = size.minDimension / 34f

        // Dice: compact rounded square with four punched dots.
        drawRoundRect(
            color = ink,
            topLeft = Offset(2f * u, 2f * u),
            size = Size(12f * u, 12f * u),
            cornerRadius = CornerRadius(2.1f * u)
        )
        listOf(
            Offset(5.2f * u, 5.2f * u),
            Offset(10.8f * u, 5.2f * u),
            Offset(5.2f * u, 10.8f * u),
            Offset(10.8f * u, 10.8f * u)
        ).forEach { drawCircle(paper, 1.35f * u, it) }

        // Start ring.
        val start = Offset(5.5f * u, 28f * u)
        drawCircle(ink, 4.2f * u, start)
        drawCircle(paper, 2.1f * u, start)

        // Destination dot.
        val destination = Offset(29f * u, 8f * u)
        drawCircle(ink, 4.0f * u, destination)

        // Dotted route, matching the approved dice → path → destination identity.
        val route = Path().apply {
            moveTo(9.5f * u, 26.8f * u)
            cubicTo(16f * u, 27.2f * u, 18f * u, 25f * u, 18.6f * u, 20.6f * u)
            cubicTo(19.2f * u, 15.2f * u, 22.2f * u, 12.2f * u, 25.2f * u, 10.4f * u)
        }
        drawPath(
            route,
            color = ink,
            style = Stroke(
                width = 2.35f * u,
                cap = StrokeCap.Butt,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.2f * u, 3.1f * u))
            )
        )
    }
}

@Composable
private fun PixelWordmark(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground

    val glyphs = listOf(
        listOf("11110","10001","10001","11110","10100","10010","10001"), // R
        listOf("00000","01110","10001","10001","10001","10001","01110"), // o
        listOf("01100","00100","00100","00100","00100","00100","01110"), // l
        listOf("00000","01110","10001","10001","10001","10001","01110"), // o
        listOf("00000","01110","00001","01111","10001","10011","01101"), // a
        listOf("00000","11011","10101","10101","10101","10101","10101"), // m
        listOf("0","0","0","0","0","1","1") // .
    )

    Canvas(modifier.width(112.dp).height(28.dp)) {
        val pixel = 2.75.dp.toPx()
        val gap = 0.72.dp.toPx()
        val step = pixel + gap
        var cursor = 0f

        glyphs.forEachIndexed { glyphIndex, glyph ->
            val columns = glyph.maxOf { it.length }
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { col, bit ->
                    if (bit == '1') {
                        drawRect(
                            color = ink,
                            topLeft = Offset(cursor + col * step, row * step + 1.dp.toPx()),
                            size = Size(pixel, pixel)
                        )
                    }
                }
            }
            cursor += columns * step + if (glyphIndex == glyphs.lastIndex) 0f else 1.6.dp.toPx()
        }
    }
}

@Composable
fun RoloamWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoloamMark()
        Spacer(Modifier.width(10.dp))
        PixelWordmark()
    }
}

@Composable
fun RoloamSectionBar(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        RoloamWordmark()
        androidx.compose.material3.Text(
            label.uppercase(),
            color = RoloamMuted,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp
        )
    }
}

@Composable
fun HomeTravelArt(
    mode: TransportMode,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = RoloamMuted
    val accent = RoloamAccent

    Canvas(modifier.height(214.dp)) {
        val w = size.width
        val h = size.height
        val dot = 1.35.dp.toPx()

        fun dottedPolyline(
            points: List<Offset>,
            colorAlpha: Float = .62f,
            spacingDp: Float = 5.4f,
            radiusDp: Float = 1.15f
        ) {
            val spacing = spacingDp.dp.toPx()
            val radius = radiusDp.dp.toPx()
            points.zipWithNext().forEach { (a, b) ->
                val dx = b.x - a.x
                val dy = b.y - a.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val steps = (distance / spacing).toInt().coerceAtLeast(1)
                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    drawCircle(
                        color = ink.copy(alpha = colorAlpha),
                        radius = radius,
                        center = Offset(a.x + dx * t, a.y + dy * t)
                    )
                }
            }
        }

        fun pixelTree(x: Float, ground: Float, scale: Float, alpha: Float = .58f) {
            val trunkTop = ground - 10.dp.toPx() * scale
            drawLine(
                color = ink.copy(alpha = alpha),
                start = Offset(x, trunkTop),
                end = Offset(x, ground),
                strokeWidth = 1.dp.toPx()
            )
            val levels = 4
            repeat(levels) { level ->
                val yy = trunkTop - level * 6.dp.toPx() * scale
                val half = (4f + level * 2.5f).dp.toPx() * scale
                val count = 3 + level * 2
                repeat(count) { index ->
                    val t = if (count == 1) .5f else index / (count - 1f)
                    drawCircle(
                        color = ink.copy(alpha = alpha),
                        radius = dot * scale,
                        center = Offset(x - half + half * 2f * t, yy)
                    )
                }
            }
        }

        // Small warm sun: ring only, like the approved mock-up.
        drawCircle(
            color = accent.copy(alpha = .82f),
            radius = 12.dp.toPx(),
            center = Offset(w * .31f, h * .27f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Distant left and right mountain groups.
        dottedPolyline(
            listOf(
                Offset(w * .03f, h * .48f),
                Offset(w * .13f, h * .37f),
                Offset(w * .21f, h * .46f),
                Offset(w * .27f, h * .40f)
            ),
            colorAlpha = .46f,
            spacingDp = 4.6f
        )
        dottedPolyline(
            listOf(
                Offset(w * .61f, h * .42f),
                Offset(w * .72f, h * .31f),
                Offset(w * .79f, h * .40f),
                Offset(w * .87f, h * .34f),
                Offset(w * .96f, h * .44f)
            ),
            colorAlpha = .50f,
            spacingDp = 4.6f
        )

        // Main mountain ridge, lower and wider.
        dottedPolyline(
            listOf(
                Offset(w * .12f, h * .62f),
                Offset(w * .25f, h * .53f),
                Offset(w * .34f, h * .58f),
                Offset(w * .48f, h * .45f),
                Offset(w * .61f, h * .57f),
                Offset(w * .73f, h * .48f),
                Offset(w * .90f, h * .61f)
            ),
            colorAlpha = .68f,
            spacingDp = 4.0f,
            radiusDp = 1.2f
        )

        // Sparse tree line.
        listOf(
            .07f to .72f to .75f,
            .11f to .75f to .58f,
            .16f to .69f to .88f,
            .21f to .73f to .64f,
            .77f to .71f to .65f,
            .82f to .68f to .90f,
            .88f to .73f to .63f,
            .93f to .70f to .78f
        ).forEach { nested ->
            val pair = nested.first
            pixelTree(
                x = w * pair.first,
                ground = h * pair.second,
                scale = nested.second,
                alpha = .52f
            )
        }

        // Pixel terrain / field texture.
        for (row in 0..3) {
            val y = h * (.72f + row * .045f)
            val startX = w * (.27f + row * .025f)
            val endX = w * (.88f - row * .015f)
            var x = startX
            while (x < endX) {
                drawCircle(
                    color = muted.copy(alpha = .36f),
                    radius = .8.dp.toPx(),
                    center = Offset(x, y)
                )
                x += (6 + (row % 2) * 2).dp.toPx()
            }
        }

        // Winding dotted path into the landscape.
        val road = listOf(
            Offset(w * .36f, h * .94f),
            Offset(w * .31f, h * .89f),
            Offset(w * .37f, h * .85f),
            Offset(w * .47f, h * .84f),
            Offset(w * .52f, h * .79f),
            Offset(w * .48f, h * .74f),
            Offset(w * .56f, h * .70f)
        )
        dottedPolyline(
            road,
            colorAlpha = .74f,
            spacingDp = 3.4f,
            radiusDp = 1.05f
        )

        // A tiny campsite glyph hidden in the scene.
        val tentX = w * .71f
        val tentY = h * .77f
        drawLine(ink.copy(alpha=.50f), Offset(tentX-7.dp.toPx(),tentY+6.dp.toPx()), Offset(tentX,tentY-5.dp.toPx()), 1.dp.toPx())
        drawLine(ink.copy(alpha=.50f), Offset(tentX,tentY-5.dp.toPx()), Offset(tentX+7.dp.toPx(),tentY+6.dp.toPx()), 1.dp.toPx())
        drawLine(ink.copy(alpha=.50f), Offset(tentX-7.dp.toPx(),tentY+6.dp.toPx()), Offset(tentX+7.dp.toPx(),tentY+6.dp.toPx()), 1.dp.toPx())

        // Transport choice is represented above by JourneyHeader. Touch the value here so
        // this illustration stays visually stable instead of changing layout per mode.
        when (mode) {
            TransportMode.CAR,
            TransportMode.TRAIN,
            TransportMode.BIKE,
            TransportMode.WALK -> Unit
        }
    }
}
