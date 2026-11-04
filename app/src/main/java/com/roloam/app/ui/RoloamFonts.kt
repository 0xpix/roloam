package com.roloam.app.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.roloam.app.R
import com.roloam.app.model.DestinationFont

/** Bundled OFL fonts; no remote font requests and no network dependency. */
object RoloamFonts {
    val doto = FontFamily(Font(R.font.doto_black, weight = FontWeight.Black))
    val geistMono = FontFamily(
        Font(R.font.geist_mono_regular, weight = FontWeight.Normal),
        Font(R.font.geist_mono_bold, weight = FontWeight.Bold)
    )

    fun forDestination(choice: DestinationFont): FontFamily = when (choice) {
        DestinationFont.DOTO -> doto
        DestinationFont.GEIST_MONO -> geistMono
    }

    fun destinationWeight(choice: DestinationFont): FontWeight = when (choice) {
        DestinationFont.DOTO -> FontWeight.Black
        DestinationFont.GEIST_MONO -> FontWeight.Bold
    }
}
