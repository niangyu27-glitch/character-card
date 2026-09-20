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

    // =====================================================
    // WebView 文件选择回调
    // =====================================================

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Android 系统文件选择器
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =================================================
        // 注册 Android 文件选择器
        // =================================================

        fileChooserLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->

                val callback = filePathCallback
                filePathCallback = null

                if (callback == null) {
                    return@registerForActivityResult
                }

                if (result.resultCode == RESULT_OK) {

                    val results =
                        WebChromeClient.FileChooserParams.parseResult(
                            result.resultCode,
                            result.data
                        )

                    callback.onReceiveValue(results)

                } else {

                    // 用户取消文件选择
                    callback.onReceiveValue(null)
                }
            }

        // =================================================
        // 创建 WebView
        // =================================================

        val webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // 允许 WebView 读取 Android 文件选择器返回的 URI
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        // =================================================
        // Assets 加载器
        // =================================================

        val assetLoader =
            WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    WebViewAssetLoader.AssetsPathHandler(this)
                )
                .build()

        // =================================================
        // WebViewClient
        // =================================================

        webView.webViewClient =
            object : WebViewClientCompat() {

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {

                    return assetLoader.shouldInterceptRequest(
                        request.url
                    )
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

        // =================================================
        // WebChromeClient
        //
        // 负责处理 HTML：
        //
        // <input type="file">
        //
        // 包括你现在导入器里面的：
        //
        // .json
        // .png
        // =================================================

        webView.webChromeClient =
            object : WebChromeClient() {

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePath: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {

                    // 如果之前还有一个未完成的选择
                    // 先取消它
                    this@MainActivity
                        .filePathCallback
                        ?.onReceiveValue(null)

                    // 保存当前回调
                    this@MainActivity.filePathCallback =
                        filePath

                    // =================================================
                    // Android 系统文件选择器
                    // =================================================

                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                            addCategory(
                                Intent.CATEGORY_OPENABLE
                            )

                            // 允许 JSON / PNG
                            type = "*/*"

                            putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                arrayOf(
                                    "application/json",
                                    "image/png"
                                )
                            )

                            // 允许 WebView 读取选中的 URI
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                    return try {

                        fileChooserLauncher.launch(
                            intent
                        )

                        true

                    } catch (e: Exception) {

                        this@MainActivity
                            .filePathCallback
                            ?.onReceiveValue(null)

                        this@MainActivity
                            .filePathCallback = null

                        Toast.makeText(
                            this@MainActivity,
                            "无法打开文件选择器",
                            Toast.LENGTH_SHORT
                        ).show()

                        false
                    }
                }
            }

        // =================================================
        // JavaScript ↔ Android
        // =================================================

        webView.addJavascriptInterface(
            AndroidBridge(this),
            "Android"
        )

        // =================================================
        // 打开主页面
        // =================================================

        webView.loadUrl(
            "https://appassets.androidplatform.net/assets/preview.html"
        )

        setContentView(webView)
    }

    // =====================================================
    // Activity 销毁
    // =====================================================

    override fun onDestroy() {

        // 如果此时还有文件选择没有完成
        // 通知 WebView 选择已经取消
        filePathCallback?.onReceiveValue(null)

        filePathCallback = null

        super.onDestroy()
    }
}

// =========================================================
// Android JS Bridge
// =========================================================

class AndroidBridge(
    private val context: Context
) {

    // =====================================================
    // 角色卡复制
    // =====================================================

    @JavascriptInterface
    fun copyText(text: String) {

        val clipboard =
            context.getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        val clip =
            ClipData.newPlainText(
                "角色卡",
                text
            )

        clipboard.setPrimaryClip(
            clip
        )

        Toast.makeText(
            context,
            "角色卡已复制",
            Toast.LENGTH_SHORT
        ).show()
    }
}
