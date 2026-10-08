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
import androidx.compose.ui.text.style.TextAlign
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

private val morningLandscape = """
              .       \  |  /       .
        .              .-*-.
                 .   .'     '.    .
           /\       /         \       /\
      /\  /  \____/           \____/  \  /\
     /  \/                              \/  \
 ___/                                        \___
       Y       Y Y              Y        Y
      /|\     /|\              /|\      /|\
     /|||\   /|||\      .     /|||\    /|||\
 ___/_|||_\_/_|||_\__________/_|||_\__/_|||_\___
             .        .    .
                 __..---..__
            _..-'          '-.._
        _.-'                    '-._
 ____.-'____________________________'-.___
""".trimIndent()

private val dayLandscape = """
                     \  |  /
                  --  ☼  --
                     / | \
        /\                           /\
   /\  /  \____      /\       _____/  \  /\
  /  \/       \_____/  \_____/         \/  \
_/                                             \_
      Y     Y Y                      Y      Y
     /|\   /|\       .       .      /|\    /|\
    /|||\ /|||\                   /|||\  /|||\
___/_|||_V_|||_\_________________/_|||_\/_|||_\___
          .       .      .
              ___....___
          _.-'          '-._
      _.-'                  '-._
  _.-'                         '-._
_'_________________________________'_
""".trimIndent()

private val eveningLandscape = """
                .              .
                         .-.
              .        .'   '.
                    .-'       '-.
        /\                         /\
   /\  /  \____      /\      ____/  \  /\
  /  \/       \_____/  \____/        \/  \
_/                                             \_
     Y Y       Y                    Y      Y Y
    /|\       /|\      .          /|\    /|\
   /|||\     /|||\         .     /|||\  /|||\
__/_|||_\___/_|||_\_____________/_|||_\/_|||_\___
           .        .       .
               ___.....___
          __.-'         '-.__
      _.-'                   '-._
  _.-'                          '-._
_'__________________________________'_
""".trimIndent()

private val nightLandscape = """
      *        .          *           .
            .        .          *
                       ☾
   .                *         .              *
        /\                           /\
   /\  /  \____      /\       _____/  \  /\
  /  \/       \_____/  \_____/         \/  \
_/                                             \_
       Y       Y Y                 Y       Y
      /|\     /|\       *         /|\     /|\
     /|||\   /|||\          .    /|||\   /|||\
 ___/_|||_\_/_|||_\____________/_|||_\_/_|||_\___
         .        *       .
               ___.....___
          __.-'         '-.__
      _.-'                   '-._
  _.-'                          '-._
_'__________________________________'_
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
    val pulse by infinite.animateFloat(
        initialValue = .78f,
        targetValue = .94f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ascii_pulse"
    )
    val floatY by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ascii_float"
    )

    when (mode) {
        TransportMode.CAR,
        TransportMode.TRAIN,
        TransportMode.BIKE,
        TransportMode.WALK -> Unit
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(226.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = art,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = pulse
                    translationY = floatY
                },
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 10.6.sp,
            lineHeight = 11.6.sp,
            letterSpacing = (-0.9).sp,
            textAlign = TextAlign.Center,
            softWrap = false
        )

        // Tiny ambient marker only; no detached phase label.
        val marker = when (phase) {
            NaturePhase.MORNING -> "05—09"
            NaturePhase.DAY -> "09—17"
            NaturePhase.EVENING -> "17—21"
            NaturePhase.NIGHT -> "21—05"
        }
        Text(
            text = marker,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-2).dp),
            color = RoloamMuted.copy(alpha = .65f),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 7.sp,
            letterSpacing = 1.0.sp
        )
    }
}
