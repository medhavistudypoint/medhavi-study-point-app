package com.medhavistudypoint.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppPdfViewerScreen(
    title: String,
    rawDriveUrl: String,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    // Google Drive URL से File ID निकालना
    val fileId = remember(rawDriveUrl) {
        when {
            rawDriveUrl.contains("/file/d/") -> rawDriveUrl.substringAfter("/file/d/").substringBefore("/")
            rawDriveUrl.contains("id=") -> rawDriveUrl.substringAfter("id=").substringBefore("&")
            else -> ""
        }
    }

    // Google Drive Preview URL
    val webViewerUrl = remember(fileId, rawDriveUrl) {
        if (fileId.isNotBlank()) {
            "https://drive.google.com/file/d/$fileId/preview"
        } else {
            rawDriveUrl
        }
    }

    BackHandler {
        onBackClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF082A66))
    ) {
        // 🔙 टॉप हेडर बार
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Text(text = "←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // 📄 इन-ऐप PDF वेबव्यू
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            // 🔒 बाहर जाने या Sign In वाले पेज पर जाने से रोकें
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val destination = request?.url?.toString() ?: ""
                                return if (!destination.contains("/preview")) {
                                    true // क्लिक को ब्लॉक करें ताकि साइन इन पेज न खुले
                                } else {
                                    false
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false

                                // 🎯 सिर्फ कोने वाले Pop-out/External Link बॉक्स को गायब करने के लिए
                                val removePopoutScript = """
                                    javascript:(function() {
                                        var style = document.createElement('style');
                                        style.type = 'text/css';
                                        style.innerHTML = 'a[target="_blank"], [aria-label*="Pop-out"], [data-tooltip*="Pop-out"], .drive-viewer-popout-button { display: none !important; visibility: hidden !important; pointer-events: none !important; }';
                                        (document.head || document.documentElement).appendChild(style);
                                    })();
                                """.trimIndent()

                                view?.evaluateJavascript(removePopoutScript, null)
                            }
                        }

                        webChromeClient = WebChromeClient()
                        loadUrl(webViewerUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF173F8F))
                }
            }
        }
    }
}