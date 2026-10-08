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

    Canvas(modifier.height(304.dp)) {
        val w = size.width
        val h = size.height
        val px = 1.7.dp.toPx()

        fun mark(x: Float, y: Float, alpha: Float = .58f, scale: Float = 1f) {
            drawRect(
                color = ink.copy(alpha = alpha),
                topLeft = Offset(x - px * scale / 2f, y - px * scale / 2f),
                size = Size(px * scale, px * scale)
            )
        }

        fun dottedSegment(
            a: Offset,
            b: Offset,
            alpha: Float,
            stepDp: Float,
            scale: Float = 1f
        ) {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val count = (length / stepDp.dp.toPx()).toInt().coerceAtLeast(1)
            repeat(count + 1) { i ->
                val t = i / count.toFloat()
                mark(a.x + dx * t, a.y + dy * t, alpha, scale)
            }
        }

        fun dottedPath(
            points: List<Offset>,
            alpha: Float,
            stepDp: Float,
            scale: Float = 1f
        ) {
            points.zipWithNext().forEach { (a, b) ->
                dottedSegment(a, b, alpha, stepDp, scale)
            }
        }

        fun mountain(
            left: Offset,
            peak: Offset,
            right: Offset,
            edgeAlpha: Float,
            fillAlpha: Float,
            rows: Int
        ) {
            dottedPath(listOf(left, peak, right), edgeAlpha, 3.5f, 1.0f)

            repeat(rows) { row ->
                val t = (row + 1f) / (rows + 1f)
                val y = peak.y + (left.y - peak.y) * t
                val lx = peak.x + (left.x - peak.x) * t
                val rx = peak.x + (right.x - peak.x) * t
                val step = (6.5f + (row % 3) * 1.4f).dp.toPx()
                var x = lx + step * .45f
                var n = 0
                while (x < rx) {
                    if ((n + row) % 4 != 1) {
                        mark(x, y, fillAlpha, .72f)
                    }
                    x += step
                    n++
                }
            }
        }

        fun pine(x: Float, base: Float, scale: Float, alpha: Float) {
            drawLine(
                color = ink.copy(alpha = alpha * .9f),
                start = Offset(x, base - 20.dp.toPx() * scale),
                end = Offset(x, base),
                strokeWidth = .8.dp.toPx()
            )
            repeat(5) { level ->
                val y = base - (5 + level * 4.2f).dp.toPx() * scale
                val half = (4.5f + level * 2.15f).dp.toPx() * scale
                val count = 3 + level * 2
                repeat(count) { i ->
                    val t = i / (count - 1f)
                    mark(
                        x - half + half * 2f * t,
                        y,
                        alpha,
                        (.78f * scale).coerceAtMost(1.0f)
                    )
                }
            }
        }

        // Sun: slightly off centre so the scene feels composed rather than mirrored.
        drawCircle(
            color = accent.copy(alpha = .88f),
            radius = 13.dp.toPx(),
            center = Offset(w * .43f, h * .18f),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Far background — deliberately unbalanced.
        mountain(
            left = Offset(w * .00f, h * .44f),
            peak = Offset(w * .11f, h * .31f),
            right = Offset(w * .24f, h * .45f),
            edgeAlpha = .30f,
            fillAlpha = .12f,
            rows = 4
        )
        mountain(
            left = Offset(w * .66f, h * .42f),
            peak = Offset(w * .80f, h * .25f),
            right = Offset(w * .96f, h * .43f),
            edgeAlpha = .34f,
            fillAlpha = .13f,
            rows = 5
        )

        // Dominant left-middle mountain mass.
        mountain(
            left = Offset(w * .03f, h * .67f),
            peak = Offset(w * .31f, h * .41f),
            right = Offset(w * .57f, h * .67f),
            edgeAlpha = .67f,
            fillAlpha = .22f,
            rows = 8
        )

        // Long right ridge; lower and wider than the main peak.
        mountain(
            left = Offset(w * .37f, h * .68f),
            peak = Offset(w * .66f, h * .46f),
            right = Offset(w * .99f, h * .64f),
            edgeAlpha = .62f,
            fillAlpha = .20f,
            rows = 7
        )

        // Secondary broken ridge details.
        dottedPath(
            listOf(
                Offset(w * .10f, h * .61f),
                Offset(w * .20f, h * .54f),
                Offset(w * .30f, h * .59f)
            ),
            .38f,
            3.2f,
            .78f
        )
        dottedPath(
            listOf(
                Offset(w * .53f, h * .61f),
                Offset(w * .64f, h * .52f),
                Offset(w * .75f, h * .58f),
                Offset(w * .86f, h * .55f)
            ),
            .40f,
            3.2f,
            .78f
        )

        // Mid-ground valley texture follows the valley opening, not a rectangular grid.
        repeat(7) { row ->
            val y = h * (.66f + row * .032f)
            val left = w * (.19f + row * .022f)
            val right = w * (.86f - row * .026f)
            val step = (5.5f + (row % 3) * 1.3f).dp.toPx()
            var x = left
            var n = 0
            while (x < right) {
                if ((n + row) % 5 != 2) {
                    mark(x, y, .18f + row * .02f, .66f)
                }
                x += step
                n++
            }
        }

        // Foreground forest — intentionally heavier on the left.
        val leftTrees = listOf(
            floatArrayOf(.02f,.78f,.78f), floatArrayOf(.055f,.73f,1.02f),
            floatArrayOf(.095f,.80f,.70f), floatArrayOf(.135f,.75f,.90f),
            floatArrayOf(.18f,.81f,.62f), floatArrayOf(.225f,.76f,.78f),
            floatArrayOf(.27f,.82f,.58f), floatArrayOf(.31f,.79f,.65f)
        )
        leftTrees.forEachIndexed { i, t ->
            pine(w*t[0], h*t[1], t[2], if (i % 2 == 0) .58f else .70f)
        }

        val rightTrees = listOf(
            floatArrayOf(.76f,.80f,.62f), floatArrayOf(.81f,.75f,.86f),
            floatArrayOf(.86f,.82f,.58f), floatArrayOf(.91f,.77f,.74f),
            floatArrayOf(.96f,.82f,.54f)
        )
        rightTrees.forEachIndexed { i, t ->
            pine(w*t[0], h*t[1], t[2], if (i % 2 == 0) .52f else .64f)
        }

        // The trail is the visual anchor. It starts bold in the foreground and
        // becomes finer as it disappears into the valley.
        val road = listOf(
            Offset(w * .48f, h * .99f),
            Offset(w * .39f, h * .95f),
            Offset(w * .34f, h * .90f),
            Offset(w * .38f, h * .86f),
            Offset(w * .49f, h * .84f),
            Offset(w * .58f, h * .81f),
            Offset(w * .61f, h * .77f),
            Offset(w * .57f, h * .73f),
            Offset(w * .60f, h * .69f)
        )
        road.zipWithNext().forEachIndexed { i, pair ->
            val fade = 1f - i / (road.size - 2f)
            dottedSegment(
                pair.first,
                pair.second,
                alpha = .56f + .22f * fade,
                stepDp = 2.5f + i * .18f,
                scale = .92f - i * .035f
            )
        }

        // Small right-side campsite detail only; not mirrored.
        val tx = w * .70f
        val ty = h * .79f
        dottedSegment(Offset(tx - 9.dp.toPx(), ty + 7.dp.toPx()), Offset(tx, ty - 7.dp.toPx()), .58f, 2.3f, .82f)
        dottedSegment(Offset(tx, ty - 7.dp.toPx()), Offset(tx + 9.dp.toPx(), ty + 7.dp.toPx()), .58f, 2.3f, .82f)
        dottedSegment(Offset(tx - 9.dp.toPx(), ty + 7.dp.toPx()), Offset(tx + 9.dp.toPx(), ty + 7.dp.toPx()), .58f, 2.3f, .82f)

        // A few foreground terrain marks to ground the trail.
        dottedPath(
            listOf(
                Offset(w * .06f, h * .86f),
                Offset(w * .18f, h * .84f),
                Offset(w * .29f, h * .86f)
            ),
            .28f,
            4.2f,
            .68f
        )
        dottedPath(
            listOf(
                Offset(w * .68f, h * .86f),
                Offset(w * .79f, h * .84f),
                Offset(w * .94f, h * .87f)
            ),
            .26f,
            4.2f,
            .68f
        )

        when (mode) {
            TransportMode.CAR,
            TransportMode.TRAIN,
            TransportMode.BIKE,
            TransportMode.WALK -> Unit
        }
    }
}
