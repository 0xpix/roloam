package com.roloam.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Pure frame generator; spaces, hyphens and punctuation never move. */
internal object DestinationScrambleFrames {
    private val glyphs = "@#£&€¥%$*?!ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"

    fun letterCount(destination: String): Int = destination.count { it.isLetterOrDigit() }

    fun frame(destination: String, settledLetters: Int, random: Random): String = buildString {
        var index = 0
        destination.forEach { actual ->
            if (!actual.isLetterOrDigit()) {
                append(actual)
            } else {
                if (index < settledLetters) {
                    append(actual)
                } else {
                    var symbol: Char
                    do { symbol = glyphs[random.nextInt(glyphs.length)] }
                    while (symbol.equals(actual, ignoreCase = true))
                    append(symbol)
                }
                index++
            }
        }
    }
}

/** A slot-machine-like reveal with no vertical movement: glyphs mutate in place. */
@Composable
fun ScramblingDestinationName(destination: String, rollSequence: Long, onFinished: () -> Unit) {
    var display by remember(destination, rollSequence) {
        mutableStateOf(DestinationScrambleFrames.frame(destination, 0, Random.Default))
    }
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(destination, rollSequence) {
        val count = DestinationScrambleFrames.letterCount(destination)
        val random = Random(rollSequence xor destination.hashCode().toLong())
        // Keep even short names animated; long names settle at a faster cadence.
        val totalFrames = ((1_000 + count * 45).coerceAtMost(2_200) / 45)
        val initialFrames = 4
        repeat(totalFrames) { tick ->
            val settled = if (count == 0) 0 else
                ((tick - initialFrames).coerceAtLeast(0) * count /
                    (totalFrames - initialFrames).coerceAtLeast(1)).coerceIn(0, count)
            display = DestinationScrambleFrames.frame(destination, settled, random)
            delay(45)
        }
        display = destination
        latestOnFinished()
    }

    // Screen readers receive the actual destination once, not dozens of random frames.
    Text(
        display,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "Random destination: $destination" },
        fontFamily = FontFamily.Monospace,
        fontSize = 36.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.3).sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}
