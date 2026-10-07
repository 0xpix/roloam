package com.roloam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.R
import com.roloam.app.model.TransportMode

@Composable
fun RoloamWordmark(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.roloam_mark),
            contentDescription = "Roloam",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            "Roloam.",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            letterSpacing = (-0.4).sp,
            color = MaterialTheme.colorScheme.onBackground
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
    val bg = MaterialTheme.colorScheme.background

    Canvas(modifier.height(190.dp)) {
        val w = size.width
        val h = size.height

        // Sun / destination marker.
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

        // Far ridge.
        val far = Path().apply {
            moveTo(0f, h * .58f)
            cubicTo(w * .16f, h * .42f, w * .26f, h * .48f, w * .38f, h * .55f)
            cubicTo(w * .52f, h * .65f, w * .62f, h * .38f, w * .76f, h * .51f)
            cubicTo(w * .86f, h * .59f, w * .92f, h * .45f, w, h * .49f)
        }
        drawPath(
            far,
            color = muted.copy(alpha = .45f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Near ridge.
        val near = Path().apply {
            moveTo(0f, h * .70f)
            cubicTo(w * .13f, h * .59f, w * .25f, h * .66f, w * .35f, h * .70f)
            cubicTo(w * .50f, h * .78f, w * .60f, h * .56f, w * .74f, h * .65f)
            cubicTo(w * .84f, h * .72f, w * .93f, h * .66f, w, h * .68f)
        }
        drawPath(
            near,
            color = ink.copy(alpha = .72f),
            style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        )

        // The road: a single loose line disappearing toward the destination.
        val road = Path().apply {
            moveTo(w * .10f, h * .94f)
            cubicTo(w * .26f, h * .82f, w * .39f, h * .90f, w * .50f, h * .77f)
            cubicTo(w * .62f, h * .63f, w * .66f, h * .54f, w * .76f, h * .47f)
        }
        drawPath(
            road,
            color = ink,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Route dots.
        listOf(.10f to .94f, .50f to .77f, .76f to .47f).forEachIndexed { index, p ->
            drawCircle(
                color = if (index == 2) accent else ink,
                radius = if (index == 2) 4.dp.toPx() else 2.8.dp.toPx(),
                center = Offset(w * p.first, h * p.second)
            )
        }

        // Tiny tent, because camping is part of Roloam's identity.
        val tentX = w * .22f
        val tentY = h * .73f
        drawLine(
            ink.copy(alpha = .78f),
            Offset(tentX - 12.dp.toPx(), tentY + 12.dp.toPx()),
            Offset(tentX, tentY - 8.dp.toPx()),
            1.5.dp.toPx()
        )
        drawLine(
            ink.copy(alpha = .78f),
            Offset(tentX, tentY - 8.dp.toPx()),
            Offset(tentX + 13.dp.toPx(), tentY + 12.dp.toPx()),
            1.5.dp.toPx()
        )
        drawLine(
            ink.copy(alpha = .78f),
            Offset(tentX - 12.dp.toPx(), tentY + 12.dp.toPx()),
            Offset(tentX + 13.dp.toPx(), tentY + 12.dp.toPx()),
            1.5.dp.toPx()
        )

        // Tiny transport glyph on the road.
        val x = w * .41f
        val y = h * .82f
        when (mode) {
            TransportMode.CAR -> {
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(x - 8.dp.toPx(), y - 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawCircle(bg, 2.dp.toPx(), Offset(x - 5.dp.toPx(), y + 4.dp.toPx()))
                drawCircle(bg, 2.dp.toPx(), Offset(x + 5.dp.toPx(), y + 4.dp.toPx()))
            }
            TransportMode.TRAIN -> {
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(x - 6.dp.toPx(), y - 8.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawLine(bg, Offset(x - 3.dp.toPx(), y - 3.dp.toPx()), Offset(x + 3.dp.toPx(), y - 3.dp.toPx()), 1.5.dp.toPx())
            }
            TransportMode.BIKE -> {
                drawCircle(ink, 4.dp.toPx(), Offset(x - 6.dp.toPx(), y + 3.dp.toPx()), style = Stroke(1.4.dp.toPx()))
                drawCircle(ink, 4.dp.toPx(), Offset(x + 6.dp.toPx(), y + 3.dp.toPx()), style = Stroke(1.4.dp.toPx()))
                drawLine(ink, Offset(x - 6.dp.toPx(), y + 3.dp.toPx()), Offset(x, y - 3.dp.toPx()), 1.4.dp.toPx())
                drawLine(ink, Offset(x, y - 3.dp.toPx()), Offset(x + 6.dp.toPx(), y + 3.dp.toPx()), 1.4.dp.toPx())
            }
            TransportMode.WALK -> {
                drawCircle(ink, 2.4.dp.toPx(), Offset(x, y - 7.dp.toPx()))
                drawLine(ink, Offset(x, y - 4.dp.toPx()), Offset(x, y + 3.dp.toPx()), 1.7.dp.toPx(), StrokeCap.Round)
                drawLine(ink, Offset(x, y), Offset(x - 5.dp.toPx(), y + 5.dp.toPx()), 1.7.dp.toPx(), StrokeCap.Round)
                drawLine(ink, Offset(x, y + 3.dp.toPx()), Offset(x + 5.dp.toPx(), y + 8.dp.toPx()), 1.7.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}
