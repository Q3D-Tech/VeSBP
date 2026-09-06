package com.example.vesbp

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

private const val RELEASES_API_URL = "https://api.github.com/repos/Q3D-Tech/VeSBP/releases/latest"
private const val UPDATE_SETTINGS_NAME = "vesbp_updates"

data class AppUpdate(
    val version: String,
    val publishedAt: String,
    val notes: String,
    val downloadUrl: String,
    val assetName: String,
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
)

sealed class UpdateState {
    data object Checking : UpdateState()
    data class UpToDate(val latest: AppUpdate?) : UpdateState()
    data class Available(val update: AppUpdate) : UpdateState()
    data class Downloading(val update: AppUpdate, val progress: DownloadProgress) : UpdateState()
    data class Downloaded(val update: AppUpdate, val file: File) : UpdateState()
    data class Installing(val update: AppUpdate, val file: File) : UpdateState()
    data class Failed(val message: String) : UpdateState()
}

class UpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences: SharedPreferences = appContext.getSharedPreferences(UPDATE_SETTINGS_NAME, Context.MODE_PRIVATE)
    private val updateDirectory = File(appContext.filesDir, "updates")

    suspend fun fetchLatest(): Result<AppUpdate> = withContext(Dispatchers.IO) {
        runCatching {
            val response = openConnection(RELEASES_API_URL).useConnection { connection ->
                if (connection.responseCode !in 200..299) error("Сервер обновлений вернул код ${connection.responseCode}")
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            parseRelease(response)
        }
    }

    suspend fun download(update: AppUpdate, onProgress: (DownloadProgress) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            updateDirectory.mkdirs()
            val temporaryFile = File(updateDirectory, "download.tmp")
            val finalFile = File(updateDirectory, "vesbp-${safeFilePart(update.version)}.apk")
            temporaryFile.delete()
            openConnection(update.downloadUrl).useConnection { connection ->
                if (connection.responseCode !in 200..299) error("Не удалось скачать обновление: ${connection.responseCode}")
                val totalBytes = connection.contentLengthLong
                var downloadedBytes = 0L
                var bytesAtLastSample = 0L
                var sampleTime = System.nanoTime()
                connection.inputStream.buffered().use { input ->
                    temporaryFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloadedBytes += count
                            val now = System.nanoTime()
                            val elapsed = (now - sampleTime) / 1_000_000_000.0
                            if (elapsed >= 0.25 || (totalBytes > 0 && downloadedBytes == totalBytes)) {
                                val speed = ((downloadedBytes - bytesAtLastSample) / elapsed).toLong().coerceAtLeast(0L)
                                onProgress(DownloadProgress(downloadedBytes, totalBytes, speed))
                                bytesAtLastSample = downloadedBytes
                                sampleTime = now
                            }
                        }
                    }
                }
                if (totalBytes > 0 && downloadedBytes != totalBytes) error("Файл обновления загружен не полностью")
                if (finalFile.exists()) finalFile.delete()
                if (!temporaryFile.renameTo(finalFile)) error("Не удалось подготовить файл обновления")
                saveDownloadedUpdate(update, finalFile)
                finalFile
            }
        }.also { result -> if (result.isFailure) File(updateDirectory, "download.tmp").delete() }
    }

    fun downloadedUpdate(): Pair<AppUpdate, File>? {
        val filePath = preferences.getString(KEY_FILE_PATH, null) ?: return null
        val file = File(filePath)
        if (!file.isFile || file.length() == 0L) {
            clearDownloadedUpdate()
            return null
        }
        val version = preferences.getString(KEY_VERSION, null) ?: return null
        return AppUpdate(
            version = version,
            publishedAt = preferences.getString(KEY_PUBLISHED_AT, "") ?: "",
            notes = preferences.getString(KEY_NOTES, "") ?: "",
            downloadUrl = preferences.getString(KEY_DOWNLOAD_URL, "") ?: "",
            assetName = preferences.getString(KEY_ASSET_NAME, file.name) ?: file.name,
        ) to file
    }

    fun isNewerThanInstalled(version: String, installedVersion: String): Boolean = compareVersions(version, installedVersion) > 0

    private fun saveDownloadedUpdate(update: AppUpdate, file: File) {
        preferences.edit()
            .putString(KEY_VERSION, update.version)
            .putString(KEY_PUBLISHED_AT, update.publishedAt)
            .putString(KEY_NOTES, update.notes)
            .putString(KEY_DOWNLOAD_URL, update.downloadUrl)
            .putString(KEY_ASSET_NAME, update.assetName)
            .putString(KEY_FILE_PATH, file.absolutePath)
            .apply()
    }

    private fun clearDownloadedUpdate() {
        preferences.edit().clear().apply()
    }

    private fun parseRelease(response: String): AppUpdate {
        val release = JSONObject(response)
        val assets = release.getJSONArray("assets")
        val apkAssets = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
        val asset = apkAssets.firstOrNull { it.optString("name").contains("universal", ignoreCase = true) }
            ?: apkAssets.firstOrNull()
            ?: error("В последнем релизе не найден APK")
        return AppUpdate(
            version = release.optString("tag_name").ifBlank { error("В релизе не указана версия") },
            publishedAt = release.optString("published_at"),
            notes = release.optString("body"),
            downloadUrl = asset.getString("browser_download_url"),
            assetName = asset.getString("name"),
        )
    }

    private fun openConnection(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "VeSBP-Android-Updater")
        instanceFollowRedirects = true
    }

    private inline fun <T> HttpURLConnection.useConnection(block: (HttpURLConnection) -> T): T = try {
        connect()
        block(this)
    } finally {
        disconnect()
    }

    private fun safeFilePart(version: String): String = version.filter { it.isLetterOrDigit() || it == '.' || it == '-' }.ifBlank { "update" }

    private fun compareVersions(first: String, second: String): Int {
        val firstParts = first.filter { it.isDigit() || it == '.' }.split('.').filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
        val secondParts = second.filter { it.isDigit() || it == '.' }.split('.').filter { it.isNotBlank() }.map { it.toIntOrNull() ?: 0 }
        repeat(max(firstParts.size, secondParts.size)) { index ->
            val comparison = (firstParts.getOrElse(index) { 0 }).compareTo(secondParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private companion object {
        const val KEY_VERSION = "version"
        const val KEY_PUBLISHED_AT = "published_at"
        const val KEY_NOTES = "notes"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_ASSET_NAME = "asset_name"
        const val KEY_FILE_PATH = "file_path"
    }
}
