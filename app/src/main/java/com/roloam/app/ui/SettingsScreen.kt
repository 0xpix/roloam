package com.roloam.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        RoloamSectionBar("SETTINGS")
        Spacer(Modifier.height(17.dp))
        Text(
            "‹  Settings.",
            fontSize=30.sp, fontWeight=FontWeight.Black,
            modifier=Modifier.clickable { back() }
        )
        Text("The controls that matter.", color=RoloamMuted)
        Spacer(Modifier.height(22.dp))
        Text("TRIP", fontSize=11.sp, fontWeight=FontWeight.Bold, color=RoloamMuted)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier=Modifier.fillMaxWidth().clickable { open(Screen.PREFERENCES) },
            shape=RoundedCornerShape(16.dp), tonalElevation=1.dp
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement=Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Trip preferences", fontWeight=FontWeight.Bold)
                    Text("Duration · transport · stay · style", fontSize=11.sp, color=RoloamMuted)
                }
                Text("→", color=RoloamMuted)
            }
        }

        Spacer(Modifier.height(25.dp))
        Text("APP", fontSize=11.sp, fontWeight=FontWeight.Bold, color=RoloamMuted)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(16.dp), tonalElevation=1.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                    Column {
                        Text("Roloam", fontWeight=FontWeight.Black)
                        Text("Installed version", fontSize=11.sp, color=RoloamMuted)
                    }
                    Text(BuildConfig.VERSION_NAME, fontWeight=FontWeight.Bold, fontSize=12.sp)
                }
                if (BuildConfig.BETA_CHANNEL) {
                    Spacer(Modifier.height(17.dp))
                    HorizontalDivider(color=RoloamMuted.copy(alpha=.17f))
                    Spacer(Modifier.height(17.dp))
                    Text("Beta updates", fontWeight=FontWeight.Bold)
                    Text("Signed APKs from the official Roloam GitHub releases.",
                        fontSize=11.sp, color=RoloamMuted)

                    state.updateMessage?.let {
                        Text(it, fontSize=12.sp, modifier=Modifier.padding(top=13.dp))
                    }

                    val update = state.updateAvailable
                    if (update != null) {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "WHAT'S NEW · " + BuildConfig.VERSION_NAME + " → " + update.version,
                            color=RoloamAccent, fontSize=10.sp,
                            fontWeight=FontWeight.Bold, letterSpacing=.8.sp
                        )
                        Spacer(Modifier.height(9.dp))
                        val notes = update.notes.orEmpty()
                        if(notes.isNotBlank()) {
                            Column(
                                Modifier.fillMaxWidth()
                                    .heightIn(max=250.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(notes, fontSize=12.sp, lineHeight=19.sp)
                            }
                        } else {
                            Text("No release notes available.", color=RoloamMuted, fontSize=12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    val progress = state.updateDownloadProgress
                    if (progress != null && progress in 0..99) {
                        LinearProgressIndicator(
                            progress={progress / 100f},
                            modifier=Modifier.fillMaxWidth(),
                            color=RoloamAccent
                        )
                        Text("$progress%", fontSize=11.sp, color=RoloamMuted)
                    }
                    Spacer(Modifier.height(13.dp))
                    when {
                        state.downloadedUpdate != null -> {
                            Button(
                                onClick=installUpdate, modifier=Modifier.fillMaxWidth(),
                                shape=RoundedCornerShape(14.dp)
                            ) { Text("INSTALL UPDATE →") }
                        }
                        update != null -> {
                            Button(
                                onClick=downloadUpdate, enabled=progress==null,
                                modifier=Modifier.fillMaxWidth(),
                                shape=RoundedCornerShape(14.dp)
                            ) { Text("DOWNLOAD " + update.version.uppercase() + " →") }
                        }
                        else -> {
                            OutlinedButton(
                                onClick=checkUpdate, enabled=!state.updateChecking,
                                modifier=Modifier.fillMaxWidth(),
                                shape=RoundedCornerShape(14.dp)
                            ) { Text(if(state.updateChecking) "CHECKING…" else "CHECK FOR UPDATE") }
                        }
                    }
                    Text(
                        "APK package and signing certificate are verified before installation.",
                        fontSize=10.sp, color=RoloamMuted,
                        modifier=Modifier.padding(top=12.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(23.dp))
        Text("No account · no feed · no background tracking.",
            fontSize=10.sp, color=RoloamMuted)
        Spacer(Modifier.height(20.dp))
    }
}
