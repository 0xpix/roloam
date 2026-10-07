package com.roloam.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roloam.app.BuildConfig
import com.roloam.app.Screen
import com.roloam.app.UiState

@Composable
fun SettingsScreen(
    state: UiState,
    open: (Screen) -> Unit,
    back: () -> Unit,
    checkUpdate: () -> Unit,
    downloadUpdate: () -> Unit,
    installUpdate: () -> Unit
) = Page {
    JourneyHeader(state.preferences.transport, false)

    Text(
        "‹  Settings.",
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clickable { back() }
    )
    Text(
        "Keep it small.",
        color = RoloamMuted,
        modifier = Modifier.padding(top = 4.dp)
    )

    Spacer(Modifier.height(28.dp))

    Text(
        "TRIP",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = RoloamMuted
    )
    Spacer(Modifier.height(8.dp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open(Screen.PREFERENCES) },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Trip preferences", fontWeight = FontWeight.Bold)
                Text(
                    "Duration · transport · stay · style",
                    fontSize = 11.sp,
                    color = RoloamMuted
                )
            }
            Text("→", color = RoloamMuted)
        }
    }

    Spacer(Modifier.height(28.dp))

    Text(
        "APP",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = RoloamMuted
    )
    Spacer(Modifier.height(8.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Roloam.", fontWeight = FontWeight.Black)
                Text(
                    BuildConfig.VERSION_NAME,
                    fontSize = 12.sp,
                    color = RoloamMuted
                )
            }

            if (BuildConfig.BETA_CHANNEL) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = RoloamMuted.copy(alpha = .18f))
                Spacer(Modifier.height(20.dp))

                Text("GitHub beta updates", fontWeight = FontWeight.Bold)
                Text(
                    "Checks only prerelease APKs published by 0xpix/roloam.",
                    fontSize = 11.sp,
                    color = RoloamMuted,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                state.updateMessage?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }

                val progress = state.updateDownloadProgress
                if (progress != null && progress in 0..99) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = RoloamAccent
                    )
                    Text(
                        "$progress%",
                        fontSize = 10.sp,
                        color = RoloamMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                when {
                    state.downloadedUpdate != null -> {
                        Button(
                            onClick = installUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoloamInk)
                        ) {
                            Text("INSTALL UPDATE →")
                        }
                    }

                    state.updateAvailable != null -> {
                        Button(
                            onClick = downloadUpdate,
                            enabled = progress == null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoloamInk)
                        ) {
                            Text("DOWNLOAD " + state.updateAvailable.version.uppercase() + " →")
                        }
                    }

                    else -> {
                        OutlinedButton(
                            onClick = checkUpdate,
                            enabled = !state.updateChecking,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (state.updateChecking) "CHECKING…" else "CHECK FOR UPDATE")
                        }
                    }
                }

                Text(
                    "Beta channel only · APK signature is verified before Android opens the installer.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = RoloamMuted,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                Text(
                    "Production builds do not use the GitHub beta updater.",
                    fontSize = 11.sp,
                    color = RoloamMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    Spacer(Modifier.weight(1f))

    Text(
        "No account · no feed · no background tracking.",
        fontSize = 10.sp,
        color = RoloamMuted,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}
