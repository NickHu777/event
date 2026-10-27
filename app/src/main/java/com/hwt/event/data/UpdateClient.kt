package com.hwt.event.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val changelog: String
)

/**
 * Gitee-hosted update feed. Set this to the public raw URL of update.json once the repository exists.
 * The feed and APK must use HTTPS. APK bytes are checked against the SHA-256 in the feed before install.
 */
object UpdateClient {
    const val MANIFEST_URL = "https://gitee.com/doggy-original-licensing/event/raw/main/updates/update.json"
    private const val MAX_APK_BYTES = 150L * 1024L * 1024L

    fun fetchUpdateInfo(): UpdateInfo {
        require(MANIFEST_URL.startsWith("https://")) { "Gitee 更新清单地址尚未配置" }
        val connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            check(connection.responseCode in 200..299) { "更新服务器返回 ${connection.responseCode}" }
            val manifest = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(manifest)
            val versionCode = json.getLong("versionCode")
            val versionName = json.getString("versionName").trim()
            val apkUrl = json.getString("apkUrl").trim()
            val sha256 = json.getString("sha256").trim().lowercase()
            require(versionCode > 0 && versionName.isNotBlank()) { "更新清单中的版本信息无效" }
            require(URL(apkUrl).protocol == "https") { "APK 下载链接必须使用 HTTPS" }
            require(sha256.matches(Regex("[0-9a-f]{64}"))) { "更新清单中的 SHA-256 无效" }
            return UpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                apkUrl = apkUrl,
                sha256 = sha256,
                changelog = json.optString("changelog").trim()
            )
        } finally {
            connection.disconnect()
        }
    }

    fun currentVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (android.os.Build.VERSION.SDK_INT >= 28) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    fun downloadAndVerify(context: Context, update: UpdateInfo): File {
        val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updateDirectory, "event-${update.versionCode}.apk")
        try {
            connection.connect()
            check(connection.responseCode in 200..299) { "APK 下载失败：HTTP ${connection.responseCode}" }
            val expectedLength = connection.contentLengthLong
            require(expectedLength <= MAX_APK_BYTES) { "APK 文件超过 150 MB" }

            val digest = MessageDigest.getInstance("SHA-256")
            var written = 0L
            connection.inputStream.buffered().use { input ->
                FileOutputStream(apkFile).buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        written += count
                        require(written <= MAX_APK_BYTES) { "APK 文件超过 150 MB" }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            val actualHash = digest.digest().joinToString("") {
                (it.toInt() and 0xff).toString(16).padStart(2, '0')
            }
            require(actualHash == update.sha256) { "APK 校验失败，文件可能已损坏" }
            return apkFile
        } catch (error: Exception) {
            apkFile.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
