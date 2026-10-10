package com.roloam.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.model.TransportMode

/** Every section header's compact identity is also a functional Home button. */
val LocalGoHome = compositionLocalOf<() -> Unit> { {} }

@Composable
fun RoloamMark(modifier: Modifier = Modifier.size(32.dp)) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = RoloamAccent
    Canvas(modifier) {
        val scale = size.minDimension / 40f
        fun o(x: Float, y: Float) = Offset(x * scale, y * scale)
        drawCircle(accent, 4.7f * scale, center = o(29f, 9f))
        val hills = Path().apply {
            moveTo(2f * scale, 27f * scale)
            lineTo(12f * scale, 14f * scale)
            lineTo(21f * scale, 25f * scale)
            lineTo(27f * scale, 18f * scale)
            lineTo(38f * scale, 30f * scale)
        }
        drawPath(hills, ink, style = Stroke(width=2.35f * scale,cap=StrokeCap.Round))
        val path = Path().apply {
            moveTo(18f * scale, 38f * scale)
            cubicTo(33f * scale,32f * scale,10f * scale,28f * scale,23f * scale,23f * scale)
        }
        drawPath(path, accent, style = Stroke(width=2.3f * scale, cap=StrokeCap.Round))
        drawCircle(ink, 2.5f * scale, center=o(18f, 38f))
    }
}

@Composable
fun RoloamWordmark(modifier: Modifier = Modifier) {
    val home = LocalGoHome.current
    Row(
        modifier = modifier
            .clickable(onClick = home)
            .semantics { contentDescription = "Roloam · go to Home" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoloamMark()
        Text(
            "roloam",
            fontWeight=FontWeight.Bold,
            letterSpacing=(-0.7).sp,
            fontSize=20.sp,
            color=MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun RoloamSectionBar(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RoloamWordmark()
        Text(label.uppercase(), color=RoloamMuted, fontSize=10.sp, letterSpacing=1.1.sp)
    }
}

@Composable
fun HomeTravelArt(mode: TransportMode, modifier: Modifier = Modifier) {
    HomeAsciiHero(mode = mode, modifier = modifier)
}
