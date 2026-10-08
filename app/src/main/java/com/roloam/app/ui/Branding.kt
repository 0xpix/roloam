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

    Canvas(modifier.height(252.dp)) {
        val w = size.width
        val h = size.height
        val pixel = 1.55.dp.toPx()

        fun pixel(x: Float, y: Float, alpha: Float = .62f, scale: Float = 1f) {
            drawRect(
                color = ink.copy(alpha = alpha),
                topLeft = Offset(x - pixel * scale / 2f, y - pixel * scale / 2f),
                size = Size(pixel * scale, pixel * scale)
            )
        }

        fun pixelLine(
            a: Offset,
            b: Offset,
            alpha: Float = .58f,
            stepDp: Float = 4.4f,
            scale: Float = 1f
        ) {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val count = (length / stepDp.dp.toPx()).toInt().coerceAtLeast(1)
            repeat(count + 1) { i ->
                val t = i / count.toFloat()
                pixel(a.x + dx * t, a.y + dy * t, alpha, scale)
            }
        }

        fun pixelPath(
            points: List<Offset>,
            alpha: Float = .58f,
            stepDp: Float = 4.4f,
            scale: Float = 1f
        ) {
            points.zipWithNext().forEach { (a, b) ->
                pixelLine(a, b, alpha, stepDp, scale)
            }
        }

        fun pine(x: Float, base: Float, scale: Float, alpha: Float = .58f) {
            val trunkTop = base - 20.dp.toPx() * scale
            drawLine(
                color = ink.copy(alpha = alpha),
                start = Offset(x, trunkTop),
                end = Offset(x, base),
                strokeWidth = .8.dp.toPx()
            )

            repeat(5) { level ->
                val y = base - (5 + level * 4).dp.toPx() * scale
                val half = (4 + level * 2.2f).dp.toPx() * scale
                val dots = 3 + level * 2
                repeat(dots) { index ->
                    val t = index / (dots - 1f)
                    pixel(
                        x - half + half * 2f * t,
                        y,
                        alpha,
                        .82f * scale.coerceAtMost(1.05f)
                    )
                }
            }
        }

        fun mountain(
            left: Offset,
            peak: Offset,
            right: Offset,
            alpha: Float,
            fillRows: Int
        ) {
            pixelPath(listOf(left, peak, right), alpha, 3.8f, .95f)

            repeat(fillRows) { row ->
                val t = (row + 1f) / (fillRows + 1f)
                val y = peak.y + (left.y - peak.y) * t
                val lx = peak.x + (left.x - peak.x) * t
                val rx = peak.x + (right.x - peak.x) * t
                val spacing = (7 + row % 2 * 2).dp.toPx()
                var x = lx + spacing * .55f
                var n = 0
                while (x < rx) {
                    if ((n + row) % 3 != 1) {
                        pixel(x, y, alpha * .42f, .75f)
                    }
                    x += spacing
                    n++
                }
            }
        }

        // Warm ring sun centered above the valley.
        drawCircle(
            color = accent.copy(alpha = .86f),
            radius = 12.dp.toPx(),
            center = Offset(w * .48f, h * .20f),
            style = Stroke(width = 1.25.dp.toPx())
        )

        // Small far peaks, left and right.
        mountain(
            Offset(w * .02f, h * .47f),
            Offset(w * .12f, h * .34f),
            Offset(w * .24f, h * .47f),
            .40f,
            3
        )
        mountain(
            Offset(w * .67f, h * .46f),
            Offset(w * .77f, h * .30f),
            Offset(w * .89f, h * .45f),
            .42f,
            4
        )
        mountain(
            Offset(w * .80f, h * .47f),
            Offset(w * .90f, h * .37f),
            Offset(w * .98f, h * .48f),
            .34f,
            3
        )

        // Main mountain range, spanning the full scene.
        mountain(
            Offset(w * .05f, h * .67f),
            Offset(w * .33f, h * .48f),
            Offset(w * .58f, h * .67f),
            .61f,
            6
        )
        mountain(
            Offset(w * .39f, h * .67f),
            Offset(w * .66f, h * .45f),
            Offset(w * .96f, h * .66f),
            .68f,
            7
        )

        // Broken ridge detail lines like the concept drawing.
        pixelPath(
            listOf(
                Offset(w * .17f, h * .61f),
                Offset(w * .26f, h * .55f),
                Offset(w * .36f, h * .61f)
            ),
            .42f,
            3.5f,
            .78f
        )
        pixelPath(
            listOf(
                Offset(w * .55f, h * .61f),
                Offset(w * .65f, h * .52f),
                Offset(w * .76f, h * .61f)
            ),
            .46f,
            3.5f,
            .78f
        )

        // Pine forest clusters.
        val trees = listOf(
            floatArrayOf(.035f,.77f,.70f), floatArrayOf(.065f,.73f,.95f),
            floatArrayOf(.095f,.79f,.64f), floatArrayOf(.13f,.75f,.80f),
            floatArrayOf(.17f,.80f,.62f), floatArrayOf(.205f,.76f,.72f),
            floatArrayOf(.245f,.81f,.55f), floatArrayOf(.73f,.81f,.58f),
            floatArrayOf(.765f,.75f,.83f), floatArrayOf(.805f,.80f,.66f),
            floatArrayOf(.845f,.73f,.95f), floatArrayOf(.89f,.80f,.65f),
            floatArrayOf(.93f,.76f,.80f), floatArrayOf(.965f,.81f,.55f)
        )
        trees.forEachIndexed { index, t ->
            pine(
                x = w * t[0],
                base = h * t[1],
                scale = t[2],
                alpha = if (index % 2 == 0) .52f else .62f
            )
        }

        // Low valley texture.
        repeat(5) { row ->
            val y = h * (.70f + row * .038f)
            val left = w * (.28f + row * .018f)
            val right = w * (.78f - row * .01f)
            val step = (5.5f + row).dp.toPx()
            var x = left
            var n = 0
            while (x < right) {
                if ((n + row) % 4 != 2) {
                    pixel(x, y, .26f + row * .025f, .70f)
                }
                x += step
                n++
            }
        }

        // Winding trail from the foreground into the valley.
        pixelPath(
            listOf(
                Offset(w * .47f, h * .98f),
                Offset(w * .37f, h * .93f),
                Offset(w * .34f, h * .88f),
                Offset(w * .41f, h * .84f),
                Offset(w * .53f, h * .83f),
                Offset(w * .58f, h * .78f),
                Offset(w * .54f, h * .74f),
                Offset(w * .59f, h * .70f)
            ),
            .70f,
            3.1f,
            .88f
        )

        // Tiny campsite tucked into the right valley.
        val tx = w * .68f
        val ty = h * .78f
        pixelLine(Offset(tx - 8.dp.toPx(), ty + 6.dp.toPx()), Offset(tx, ty - 6.dp.toPx()), .52f, 2.6f, .78f)
        pixelLine(Offset(tx, ty - 6.dp.toPx()), Offset(tx + 8.dp.toPx(), ty + 6.dp.toPx()), .52f, 2.6f, .78f)
        pixelLine(Offset(tx - 8.dp.toPx(), ty + 6.dp.toPx()), Offset(tx + 8.dp.toPx(), ty + 6.dp.toPx()), .52f, 2.6f, .78f)

        // JourneyHeader above the scene communicates transport; hero stays calm and consistent.
        when (mode) {
            TransportMode.CAR,
            TransportMode.TRAIN,
            TransportMode.BIKE,
            TransportMode.WALK -> Unit
        }
    }
}
