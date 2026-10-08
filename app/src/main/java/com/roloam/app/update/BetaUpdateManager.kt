package com.roloam.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class BetaUpdate(
    val tag: String,
    val version: String,
    val notes: String?,
    val apkUrl: String,
    val apkName: String
)

sealed interface UpdateCheck {
    data class Available(val update: BetaUpdate) : UpdateCheck
    data class Current(val version: String) : UpdateCheck
    data class Failed(val message: String) : UpdateCheck
}

private data class GithubAsset(
    val name: String,
    @Json(name = "browser_download_url") val downloadUrl: String
)

private data class GithubRelease(
    @Json(name = "tag_name") val tag: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList()
)

class BetaUpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val releaseListAdapter = moshi.adapter<List<GithubRelease>>(
        Types.newParameterizedType(List::class.java, GithubRelease::class.java)
    )

    suspend fun check(): UpdateCheck = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/0xpix/roloam/releases?per_page=20")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Roloam-Beta-Updater")
                .build()

            val releases = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("GitHub returned HTTP ${response.code}")
                releaseListAdapter.fromJson(response.body?.string().orEmpty()).orEmpty()
            }

            val latest = releases.firstOrNull { release ->
                release.prerelease &&
                    !release.draft &&
                    release.tag.contains("beta", ignoreCase = true) &&
                    release.assets.any { it.name.endsWith(".apk", ignoreCase = true) }
            } ?: return@withContext UpdateCheck.Current(currentVersion())

            val asset = latest.assets.first { it.name.endsWith(".apk", ignoreCase = true) }
            val remote = latest.tag.removePrefix("v")
            val current = currentVersion()

            if (isNewerBetaVersion(remote, current)) {
                UpdateCheck.Available(
                    BetaUpdate(
                        tag = latest.tag,
                        version = remote,
                        notes = latest.body,
                        apkUrl = asset.downloadUrl,
                        apkName = asset.name
                    )
                )
            } else {
                UpdateCheck.Current(current)
            }
        }.getOrElse { UpdateCheck.Failed(it.message ?: "Could not check GitHub") }
    }

    suspend fun download(
        update: BetaUpdate,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(update.apkUrl)
            .header("User-Agent", "Roloam-Beta-Updater")
            .build()

        val file = File(context.cacheDir, "updates/roloam-${update.version}.apk")
        file.parentFile?.mkdirs()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed: HTTP ${response.code}")
            val body = response.body ?: error("GitHub returned an empty APK")
            val total = body.contentLength()
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) {
                            onProgress(((copied * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
        }

        verifyDownloadedApk(file)
        file
    }

    fun install(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private fun verifyDownloadedApk(file: File) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        @Suppress("DEPRECATION")
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: error("Downloaded file is not a valid APK")

        if (archive.packageName != context.packageName) {
            error("Downloaded APK belongs to a different app")
        }

        val current = context.packageManager.getPackageInfo(context.packageName, flags)
        val currentCerts = signingDigests(current)
        val archiveCerts = signingDigests(archive)

        if (currentCerts.isEmpty() || archiveCerts.isEmpty() || currentCerts.intersect(archiveCerts).isEmpty()) {
            error("APK signature does not match this beta installation")
        }
    }

    private fun signingDigests(info: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }

        return signatures.orEmpty().map { signature ->
            val bytes = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun currentVersion(): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return info.versionName ?: "0.0.0"
    }


}


internal fun isNewerBetaVersion(remote: String, local: String): Boolean {
    data class Version(val major: Int, val minor: Int, val patch: Int, val betaRevision: Int)

    fun parse(value: String): Version {
        val normalized = value.trim().removePrefix("v").lowercase()
        val base = normalized.substringBefore("-")
        val numbers = base.split(".").map { it.toIntOrNull() ?: 0 }

        // "0.2.0-beta" is the normal Roloam beta name. A numbered suffix is still
        // understood for compatibility with older installs such as 0.1.0-beta.2.
        val betaRevision = Regex("""(?:^|[-.])beta(?:[.-]?(\d+))?""")
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0

        return Version(
            major = numbers.getOrElse(0) { 0 },
            minor = numbers.getOrElse(1) { 0 },
            patch = numbers.getOrElse(2) { 0 },
            betaRevision = betaRevision
        )
    }

    val a = parse(remote)
    val b = parse(local)

    return when {
        a.major != b.major -> a.major > b.major
        a.minor != b.minor -> a.minor > b.minor
        a.patch != b.patch -> a.patch > b.patch
        else -> a.betaRevision > b.betaRevision
    }
}
