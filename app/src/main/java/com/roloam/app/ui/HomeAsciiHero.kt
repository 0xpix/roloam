package com.roloam.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.model.TransportMode
import java.time.LocalTime
import kotlinx.coroutines.delay

private enum class NaturePhase { MORNING, DAY, EVENING, NIGHT }

private fun phaseFor(hour: Int): NaturePhase = when (hour) {
    in 5..8 -> NaturePhase.MORNING
    in 9..16 -> NaturePhase.DAY
    in 17..20 -> NaturePhase.EVENING
    else -> NaturePhase.NIGHT
}

private val dayLandscape = """
                   /\                      /\
            /\    /  \        /\        /  \__
       /\  /  \__/    \  /\  /  \  /\ /      \
  /\__/  \/            \/  \/    \/  V        \
 /                                                \
        ^      ^^            .            ^^      ^
       /|\    /|\       .         .      /|\    /|\
      /|||\  /|||\          .            /|||\  /|||\
 ___ /_|||_\/_|||_\ __________________ /_|||_\/_|||_\ ___
             .          .      .   .
                .   ___---___
            __---'''       '''---__
       __--'                     '--__
    __'      .   .          .         '__
 __/_______________________________________\__
""".trimIndent()

private val morningLandscape = """
                   .-.
               .--'   '--.
             .'           '.
        /\                 /\        /\
   /\  /  \      /\      /  \  /\  /  \__
  /  \/    \____/  \____/    \/  \/      \
 /                                                \
      ^      ^^^                         ^^       ^
     /|\    /|||\        .             /|\     /|\
    /|||\  /|||||\             .      /|||\   /|||\
 __/_|||_\/_|||||_\__________________/_|||_\_/_|||_\__
               .     .       .
                  ___---___
             __---'       '---__
        __--'                 '--__
    __-'                         '-__
 __/__________________________________\__
""".trimIndent()

private val eveningLandscape = """
                       .
          .                            .
                 _.-'''''-._
              .-'           '-.
        /\                       /\
   /\  /  \      /\      /\    /  \__
  /  \/    \____/  \____/  \__/      \
 /                                              \
       ^^^                 .             ^^     ^
      /|||\       .                     /|\   /|\
     /|||||\              .            /|||\ /|||\
 ___/_|||||_\_________________________/_|||_V_|||_\__
          .        .          .
                    ___---___
               __---'       '---__
          __--'                 '--__
      __-'                         '-__
 ___-'_________________________________'-___
""".trimIndent()

private val nightLandscape = """
        .        *             .          *
   *          .        *              .
               _..._             .
             .:::::::.        *
          .  :::::::::
             ':::::::'
               '::'
        /\                 /\           /\
   /\  /  \      /\      /  \     /\ /  \__
  /  \/    \____/  \____/    \___/  V      \
 /                                               \
       ^       ^^                       ^^^      ^
      /|\     /|\         .           /|||\    /|\
     /|||\   /|||\              .    /|||||\  /|||\
 ___/_|||_\_/_|||_\_________________/_|||||_\/_|||_\__
                 .        .
                     __---__
                 __--'     '--__
             __-'             '-__
         __-'                   '-__
 ____---'___________________________'---____
""".trimIndent()

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
    val art = when (phase) {
        NaturePhase.MORNING -> morningLandscape
        NaturePhase.DAY -> dayLandscape
        NaturePhase.EVENING -> eveningLandscape
        NaturePhase.NIGHT -> nightLandscape
    }

    val infinite = rememberInfiniteTransition(label = "ascii_nature")
    val breathe by infinite.animateFloat(
        initialValue = .84f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scene_breathe"
    )
    val drift by infinite.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scene_drift"
    )

    // Transport remains represented by the animated route header above the scene.
    when (mode) {
        TransportMode.CAR,
        TransportMode.TRAIN,
        TransportMode.BIKE,
        TransportMode.WALK -> Unit
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(268.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = art,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = breathe
                    translationX = drift
                },
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .68f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 7.2.sp,
            lineHeight = 7.8.sp,
            letterSpacing = (-0.45).sp,
            maxLines = 22
        )

        val celestial = when (phase) {
            NaturePhase.MORNING -> "☼"
            NaturePhase.DAY -> "☀"
            NaturePhase.EVENING -> "◌"
            NaturePhase.NIGHT -> "☾"
        }
        val label = when (phase) {
            NaturePhase.MORNING -> "DAWN"
            NaturePhase.DAY -> "ROAM"
            NaturePhase.EVENING -> "GOLDEN HOUR"
            NaturePhase.NIGHT -> "NIGHT ROAM"
        }

        Text(
            text = celestial,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 5.dp)
                .graphicsLayer { alpha = .72f + (breathe - .84f) },
            color = RoloamAccent,
            fontFamily = FontFamily.Monospace,
            fontSize = if (phase == NaturePhase.NIGHT) 24.sp else 21.sp
        )

        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-2).dp),
            color = RoloamMuted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            letterSpacing = 1.4.sp
        )
    }
}
