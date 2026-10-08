package com.roloam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.R
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
    // Transport still controls JourneyHeader; the approved artwork itself stays fixed.
    when (mode) {
        TransportMode.CAR,
        TransportMode.TRAIN,
        TransportMode.BIKE,
        TransportMode.WALK -> Unit
    }

    Image(
        painter = painterResource(R.drawable.home_hero),
        contentDescription = "Pixel landscape with mountains, pine trees and a winding road",
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(706f / 519f),
        contentScale = ContentScale.Fit
    )
}
