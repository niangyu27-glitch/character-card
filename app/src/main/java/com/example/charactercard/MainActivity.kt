package com.example.charactercard

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true

        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(
            AndroidBridge(this),
            "Android"
        )

        webView.loadUrl("file:///android_asset/preview.html")

        setContentView(webView)
    }
}

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
