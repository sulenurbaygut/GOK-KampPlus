package com.kampplus.hava

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File

/** GÖK: Kotlin/Compose Android kabuğu, aynı HTML tasarımını çevrimdışı yerel dosyadan açar. */
class MainActivity : ComponentActivity() {
    private val mainThread = Handler(Looper.getMainLooper())

    private fun toast(message: String) {
        mainThread.post { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    private fun safePngName(original: String): String {
        val base = original.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^a-zA-Z0-9_.-]"), "_")
            .take(75)
            .ifBlank { "GOK_kartpostal.png" }
        return if (base.lowercase().endsWith(".png")) base else "$base.png"
    }

    private fun decodePng(base64: String): ByteArray {
        require(base64.length in 50..20_000_000) { "Invalid image size" }
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val pngHeader = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        require(bytes.size >= 8 && pngHeader.indices.all { bytes[it] == pngHeader[it] }) {
            "Only PNG image data is accepted"
        }
        return bytes
    }

    inner class GokBridge {
        @JavascriptInterface
        fun savePng(base64: String, fileName: String) {
            try {
                val bytes = decodePng(base64)
                val name = safePngName(fileName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GOK")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("MediaStore insert failed")
                    try {
                        contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("MediaStore output stream unavailable")
                        contentResolver.update(uri, ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }, null, null)
                        toast("Kartpostal Galeri / Resimler / GOK klasörüne kaydedildi")
                    } catch (e: Exception) {
                        contentResolver.delete(uri, null, null)
                        throw e
                    }
                } else {
                    val folder = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "GOK")
                    check(folder.exists() || folder.mkdirs())
                    File(folder, name).writeBytes(bytes)
                    toast("Kartpostal uygulamanın Resimler/GOK klasörüne kaydedildi")
                }
            } catch (e: Exception) {
                toast("Kartpostal kaydedilemedi: " + (e.message ?: "bilinmeyen hata"))
            }
        }

        @JavascriptInterface
        fun sharePng(base64: String, fileName: String) {
            try {
                val bytes = decodePng(base64)
                val dir = File(cacheDir, "shared")
                check(dir.exists() || dir.mkdirs())
                val file = File(dir, safePngName(fileName))
                file.writeBytes(bytes)
                val uri: Uri = FileProvider.getUriForFile(
                    this@MainActivity, "${packageName}.fileprovider", file
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(contentResolver, "GÖK kartpostal", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                mainThread.post {
                    try {
                        startActivity(Intent.createChooser(send, "GÖK kartpostalını paylaş"))
                    } catch (e: Exception) {
                        toast("Paylaşım ekranı açılamadı")
                    }
                }
            } catch (e: Exception) {
                toast("Kartpostal paylaşılamadı: " + (e.message ?: "bilinmeyen hata"))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webChromeClient = WebChromeClient()
                        addJavascriptInterface(GokBridge(), "GokAndroid")
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val url = request.url
                                if (url.host == "appassets.androidplatform.net") return false
                                if (url.scheme == "https" || url.scheme == "http") {
                                    try { startActivity(Intent(Intent.ACTION_VIEW, url)) }
                                    catch (_: Exception) { toast("Bağlantı açılamadı") }
                                }
                                return true
                            }
                        }
                        val html = context.assets.open("gok.html").bufferedReader().use {
                            it.readText()
                        }
                        loadDataWithBaseURL(
                            "https://appassets.androidplatform.net/",
                            html, "text/html", "UTF-8", null
                        )
                    }
                }
            )
        }
    }
}
