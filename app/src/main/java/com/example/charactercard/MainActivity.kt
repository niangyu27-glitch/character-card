package com.example.charactercard

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class MainActivity : AppCompatActivity() {

    // WebView 的文件选择回调
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Android 系统文件选择器
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册系统文件选择器
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val callback = filePathCallback
            filePathCallback = null

            if (callback == null) {
                return@registerForActivityResult
            }

            if (result.resultCode == RESULT_OK) {

                val results = WebChromeClient.FileChooserParams.parseResult(
                    result.resultCode,
                    result.data
                )

                callback.onReceiveValue(results)

            } else {

                // 用户取消选择
                callback.onReceiveValue(null)
            }
        }

        val webView = WebView(this)

        // =========================
        // WebView 基础设置
        // =========================

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // 允许访问系统选择出来的 content:// 文件
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        // =========================
        // Assets 加载器
        // =========================

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()

        webView.webViewClient = object : WebViewClientCompat() {

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(
                view: WebView,
                url: String
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(
                    Uri.parse(url)
                )
            }
        }

        // =========================
        // 文件选择器
        // =========================

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                webView: WebView?,
                filePath: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                // 如果之前还有一个未完成的选择，先取消
                this@MainActivity.filePathCallback?.onReceiveValue(null)

                this@MainActivity.filePathCallback = filePath

                /*
                 * 优先使用 WebView 根据 HTML 的
                 * accept=".json,.png,application/json,image/png"
                 * 自动生成的 Intent。
                 *
                 * 如果生成失败，再使用备用的系统文件选择器。
                 */
                val intent = try {

                    fileChooserParams?.createIntent()

                } catch (_: Exception) {

                    null
                } ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                    addCategory(Intent.CATEGORY_OPENABLE)

                    type = "*/*"

                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf(
                            "application/json",
                            "image/png"
                        )
                    )
                }.apply {

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                return try {

                    fileChooserLauncher.launch(intent)

                    true

                } catch (e: Exception) {

                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = null

                    Toast.makeText(
                        this@MainActivity,
                        "无法打开文件选择器",
                        Toast.LENGTH_SHORT
                    ).show()

                    false
                }
            }
        }

        // =========================
        // JS ↔ Android
        // =========================

        webView.addJavascriptInterface(
            AndroidBridge(this),
            "Android"
        )

        // =========================
        // 打开网页
        // =========================

        webView.loadUrl(
            "https://appassets.androidplatform.net/assets/preview.html"
        )

        setContentView(webView)
    }

    override fun onDestroy() {

        // Activity 被销毁时取消未完成的文件选择
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null

        super.onDestroy()
    }
}

// =====================================================
// Android JS Bridge
// =====================================================

class AndroidBridge(private val context: Context) {

    @JavascriptInterface
    fun copyText(text: String) {

        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager

        val clip = ClipData.newPlainText(
            "角色卡",
            text
        )

        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            "角色卡已复制",
            Toast.LENGTH_SHORT
        ).show()
    }
}
