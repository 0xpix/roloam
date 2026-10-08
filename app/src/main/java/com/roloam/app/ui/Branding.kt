package com.roloam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
fun HomeTravelArt(
    mode: TransportMode,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = RoloamMuted
    val accent = RoloamAccent
    val bg = MaterialTheme.colorScheme.background

    Canvas(modifier.height(190.dp)) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = accent.copy(alpha = .16f),
            radius = 34.dp.toPx(),
            center = Offset(w * .79f, h * .23f)
        )
        drawCircle(
            color = accent,
            radius = 4.dp.toPx(),
            center = Offset(w * .79f, h * .23f)
        )

        val far = Path().apply {
            moveTo(0f, h * .58f)
            cubicTo(w * .16f, h * .42f, w * .26f, h * .48f, w * .38f, h * .55f)
            cubicTo(w * .52f, h * .65f, w * .62f, h * .38f, w * .76f, h * .51f)
            cubicTo(w * .86f, h * .59f, w * .92f, h * .45f, w, h * .49f)
        }
        drawPath(
            far,
            color = muted.copy(alpha=.45f),
            style = Stroke(width=1.2.dp.toPx(), cap=StrokeCap.Round)
        )

        val near = Path().apply {
            moveTo(0f, h * .70f)
            cubicTo(w * .13f, h * .59f, w * .25f, h * .66f, w * .35f, h * .70f)
            cubicTo(w * .50f, h * .78f, w * .60f, h * .56f, w * .74f, h * .65f)
            cubicTo(w * .84f, h * .72f, w * .93f, h * .66f, w, h * .68f)
        }
        drawPath(
            near,
            color = ink.copy(alpha=.72f),
            style = Stroke(width=1.7.dp.toPx(), cap=StrokeCap.Round)
        )

        val road = Path().apply {
            moveTo(w * .10f, h * .94f)
            cubicTo(w * .26f, h * .82f, w * .39f, h * .90f, w * .50f, h * .77f)
            cubicTo(w * .62f, h * .63f, w * .66f, h * .54f, w * .76f, h * .47f)
        }
        drawPath(road, color=ink, style=Stroke(width=2.2.dp.toPx(), cap=StrokeCap.Round))

        listOf(.10f to .94f, .50f to .77f, .76f to .47f).forEachIndexed { index, p ->
            drawCircle(
                color = if (index == 2) accent else ink,
                radius = if (index == 2) 4.dp.toPx() else 2.8.dp.toPx(),
                center = Offset(w * p.first, h * p.second)
            )
        }

        val tentX = w * .22f
        val tentY = h * .73f
        drawLine(ink.copy(alpha=.78f), Offset(tentX-12.dp.toPx(),tentY+12.dp.toPx()), Offset(tentX,tentY-8.dp.toPx()), 1.5.dp.toPx())
        drawLine(ink.copy(alpha=.78f), Offset(tentX,tentY-8.dp.toPx()), Offset(tentX+13.dp.toPx(),tentY+12.dp.toPx()), 1.5.dp.toPx())
        drawLine(ink.copy(alpha=.78f), Offset(tentX-12.dp.toPx(),tentY+12.dp.toPx()), Offset(tentX+13.dp.toPx(),tentY+12.dp.toPx()), 1.5.dp.toPx())

        val x = w * .41f
        val y = h * .82f
        when(mode) {
            TransportMode.CAR -> {
                drawRoundRect(ink, Offset(x-8.dp.toPx(),y-5.dp.toPx()), Size(16.dp.toPx(),8.dp.toPx()), CornerRadius(2.dp.toPx()))
                drawCircle(bg,2.dp.toPx(),Offset(x-5.dp.toPx(),y+4.dp.toPx()))
                drawCircle(bg,2.dp.toPx(),Offset(x+5.dp.toPx(),y+4.dp.toPx()))
            }
            TransportMode.TRAIN -> {
                drawRoundRect(ink, Offset(x-6.dp.toPx(),y-8.dp.toPx()), Size(12.dp.toPx(),16.dp.toPx()), CornerRadius(2.dp.toPx()))
                drawLine(bg,Offset(x-3.dp.toPx(),y-3.dp.toPx()),Offset(x+3.dp.toPx(),y-3.dp.toPx()),1.5.dp.toPx())
            }
            TransportMode.BIKE -> {
                drawCircle(ink,4.dp.toPx(),Offset(x-6.dp.toPx(),y+3.dp.toPx()),style=Stroke(1.4.dp.toPx()))
                drawCircle(ink,4.dp.toPx(),Offset(x+6.dp.toPx(),y+3.dp.toPx()),style=Stroke(1.4.dp.toPx()))
                drawLine(ink,Offset(x-6.dp.toPx(),y+3.dp.toPx()),Offset(x,y-3.dp.toPx()),1.4.dp.toPx())
                drawLine(ink,Offset(x,y-3.dp.toPx()),Offset(x+6.dp.toPx(),y+3.dp.toPx()),1.4.dp.toPx())
            }
            TransportMode.WALK -> {
                drawCircle(ink,2.4.dp.toPx(),Offset(x,y-7.dp.toPx()))
                drawLine(ink,Offset(x,y-4.dp.toPx()),Offset(x,y+3.dp.toPx()),1.7.dp.toPx(),StrokeCap.Round)
                drawLine(ink,Offset(x,y),Offset(x-5.dp.toPx(),y+5.dp.toPx()),1.7.dp.toPx(),StrokeCap.Round)
                drawLine(ink,Offset(x,y+3.dp.toPx()),Offset(x+5.dp.toPx(),y+8.dp.toPx()),1.7.dp.toPx(),StrokeCap.Round)
            }
        }
    }
}
