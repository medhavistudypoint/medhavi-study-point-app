package com.medhavistudypoint.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HtmlTestScreen(
    innerPadding: PaddingValues,
    title: String,
    testUrl: String,
    onBack: () -> Unit
) {
    val navyBlue = Color(0xFF0B1E47)
    val goldYellow = Color(0xFFFACC15)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color(0xFFF8FAFC))
    ) {
        // 1. चौड़ा ब्रांड हेडर (जो हर पेज पर फिक्स TGT पोर्टल जैसा दिखता है)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(navyBlue)
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MEDHAVI STUDY POINT",
                color = goldYellow,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title.ifBlank { "TGT / PGT / NET / DSSSB" },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 2. टेस्ट एरिया (WebView - जिसमें से ऊपर का डुप्लिकेट हेडर और ट्रांसलेट पॉपअप हाइड रहेंगे)
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    webViewClient = object : WebViewClient() {
                        // बाहरी सहायता लिंक ब्लॉक करना
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: ""
                            return if (url.contains("support.google.com") || url.contains("translate.google.com/about")) {
                                true
                            } else {
                                false
                            }
                        }

                        // पेज लोड होते ही HTML का अंदरूनी हेडर और गूगल पॉपअप छुपाना
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var style = document.createElement('style');
                                    style.innerHTML = 'body { top: 0px !important; } .header-left, .goog-te-banner-frame, #goog-gt-tt, .goog-te-balloon-frame { display: none !important; }';
                                    document.head.appendChild(style);
                                })();
                                """.trimIndent(), null
                            )
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER

                    loadUrl(testUrl)
                }
            }
        )

        // 3. बॉटम फिक्स्ड बैक बटन (बिल्कुल पोर्टल के अन्य पेजों की तरह)
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = navyBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "← Back",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}