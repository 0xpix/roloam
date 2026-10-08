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

    Canvas(modifier.height(232.dp)) {
        val w = size.width
        val h = size.height
        val px = 1.18.dp.toPx()

        fun dots(
            points: List<Offset>,
            alpha: Float = .58f,
            spacing: Float = 3.7f,
            radius: Float = 1.0f
        ) {
            points.zipWithNext().forEach { (a, b) ->
                val dx = b.x - a.x
                val dy = b.y - a.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val count = (distance / spacing.dp.toPx()).toInt().coerceAtLeast(1)
                repeat(count + 1) { i ->
                    val t = i / count.toFloat()
                    drawCircle(
                        ink.copy(alpha = alpha),
                        radius.dp.toPx(),
                        Offset(a.x + dx * t, a.y + dy * t)
                    )
                }
            }
        }

        fun pine(x: Float, ground: Float, scale: Float, alpha: Float) {
            drawLine(
                ink.copy(alpha = alpha),
                Offset(x, ground - 20.dp.toPx() * scale),
                Offset(x, ground),
                1.dp.toPx()
            )
            repeat(5) { level ->
                val y = ground - (6 + level * 4).dp.toPx() * scale
                val half = (4 + level * 1.9f).dp.toPx() * scale
                val count = 4 + level * 2
                repeat(count) { i ->
                    val t = i / (count - 1f)
                    drawCircle(
                        ink.copy(alpha = alpha),
                        px * scale,
                        Offset(x - half + 2f * half * t, y)
                    )
                }
            }
        }

        fun stippleMountain(
            peakX: Float,
            peakY: Float,
            leftX: Float,
            rightX: Float,
            baseY: Float,
            alpha: Float,
            rows: Int
        ) {
            val left = Offset(leftX, baseY)
            val peak = Offset(peakX, peakY)
            val right = Offset(rightX, baseY)
            dots(listOf(left, peak, right), alpha = alpha, spacing = 3.4f, radius = .95f)

            repeat(rows) { row ->
                val yT = (row + 1f) / (rows + 1f)
                val y = peakY + (baseY - peakY) * yT
                val leftAtY = peakX + (leftX - peakX) * yT
                val rightAtY = peakX + (rightX - peakX) * yT
                val step = (6 + row % 2 * 2).dp.toPx()
                var x = leftAtY + step * .4f
                while (x < rightAtY) {
                    val rhythm = (((x / step).toInt() + row) % 3)
                    if (rhythm != 1) {
                        drawCircle(
                            ink.copy(alpha = alpha * .46f),
                            .75.dp.toPx(),
                            Offset(x, y)
                        )
                    }
                    x += step
                }
            }
        }

        // Thin warm sun from the original concept.
        drawCircle(
            accent.copy(alpha = .78f),
            13.dp.toPx(),
            Offset(w * .31f, h * .24f),
            style = Stroke(1.3.dp.toPx())
        )

        // Background peaks.
        stippleMountain(w*.13f,h*.35f,w*.01f,w*.28f,h*.57f,.32f,5)
        stippleMountain(w*.76f,h*.28f,w*.57f,w*.96f,h*.58f,.34f,6)
        stippleMountain(w*.48f,h*.42f,w*.24f,w*.72f,h*.68f,.48f,7)

        // Foreground ridge: denser, like the original concept board.
        dots(
            listOf(
                Offset(w*.02f,h*.66f),
                Offset(w*.16f,h*.57f),
                Offset(w*.28f,h*.62f),
                Offset(w*.42f,h*.51f),
                Offset(w*.56f,h*.64f),
                Offset(w*.68f,h*.55f),
                Offset(w*.83f,h*.61f),
                Offset(w*.98f,h*.52f)
            ),
            alpha=.72f,
            spacing=3.2f,
            radius=1.0f
        )

        // Forest clusters on both sides.
        val trees = listOf(
            floatArrayOf(.04f,.78f,.72f), floatArrayOf(.075f,.73f,.88f),
            floatArrayOf(.11f,.79f,.64f), floatArrayOf(.145f,.75f,.80f),
            floatArrayOf(.18f,.81f,.58f), floatArrayOf(.22f,.77f,.67f),
            floatArrayOf(.76f,.78f,.62f), floatArrayOf(.80f,.73f,.85f),
            floatArrayOf(.845f,.80f,.62f), floatArrayOf(.89f,.75f,.78f),
            floatArrayOf(.94f,.80f,.58f)
        )
        trees.forEachIndexed { i, t ->
            pine(w*t[0], h*t[1], t[2], if(i%2==0) .52f else .62f)
        }

        // Valley texture / little fields.
        repeat(6) { row ->
            val y = h * (.69f + row * .036f)
            val x0 = w * (.25f + row * .014f)
            val x1 = w * (.82f - row * .02f)
            var x = x0
            val step = (5 + row % 3).dp.toPx()
            while (x < x1) {
                drawCircle(
                    muted.copy(alpha = .28f + row*.025f),
                    .7.dp.toPx(),
                    Offset(x, y)
                )
                x += step
            }
        }

        // Winding road / river from foreground into the valley.
        dots(
            listOf(
                Offset(w*.42f,h*.98f),
                Offset(w*.34f,h*.93f),
                Offset(w*.38f,h*.88f),
                Offset(w*.51f,h*.87f),
                Offset(w*.56f,h*.82f),
                Offset(w*.51f,h*.78f),
                Offset(w*.58f,h*.73f),
                Offset(w*.55f,h*.69f)
            ),
            alpha=.76f,
            spacing=2.9f,
            radius=.95f
        )

        // Tiny campsite, intentionally part of the landscape rather than a UI icon.
        val tx=w*.69f
        val ty=h*.80f
        drawLine(ink.copy(alpha=.54f),Offset(tx-8.dp.toPx(),ty+7.dp.toPx()),Offset(tx,ty-6.dp.toPx()),1.dp.toPx())
        drawLine(ink.copy(alpha=.54f),Offset(tx,ty-6.dp.toPx()),Offset(tx+8.dp.toPx(),ty+7.dp.toPx()),1.dp.toPx())
        drawLine(ink.copy(alpha=.54f),Offset(tx-8.dp.toPx(),ty+7.dp.toPx()),Offset(tx+8.dp.toPx(),ty+7.dp.toPx()),1.dp.toPx())

        // Mode stays in JourneyHeader; keep the illustration serene and consistent.
        when(mode) {
            TransportMode.CAR, TransportMode.TRAIN, TransportMode.BIKE, TransportMode.WALK -> Unit
        }
    }
}
