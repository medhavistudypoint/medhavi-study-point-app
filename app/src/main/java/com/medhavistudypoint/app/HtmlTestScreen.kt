package com.medhavistudypoint.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isEnglish by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color(0xFFF8FAFC))
    ) {
        // 1. टॉप ब्रांड हेडर (साथ में भाषा बदलने का Translate बटन)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(navyBlue)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MEDHAVI STUDY POINT",
                    color = goldYellow,
                    fontSize = 18.sp,
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

            // 🌐 Google Translate Toggle Button
            Surface(
                modifier = Modifier
                    .clickable {
                        isEnglish = !isEnglish
                        val targetLang = if (isEnglish) "en" else "hi"
                        // WebView के अंदर Google Translate को ट्रिगर करना
                        webViewInstance?.evaluateJavascript(
                            """
                            (function() {
                                var select = document.querySelector('select.goog-te-combo');
                                if (select) {
                                    select.value = '$targetLang';
                                    select.dispatchEvent(new Event('change'));
                                }
                            })();
                            """.trimIndent(), null
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                color = goldYellow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "अ हिंदी" else "A English",
                        color = navyBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 2. टेस्ट एरिया (WebView)
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

                    webViewInstance = this

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: ""
                            return url.contains("support.google.com") || url.contains("translate.google.com/about")
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Google Translate स्क्रिप्ट और स्वच्छ CSS इंजेक्ट करना
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    // 1. गूगल ट्रांसलेटर का अनचाहा बार और फुटर छुपाना
                                    var style = document.createElement('style');
                                    style.innerHTML = 'body { top: 0px !important; } .header-left, .goog-te-banner-frame, #goog-gt-tt, .goog-te-balloon-frame, .goog-te-gadget { display: none !important; } .goog-text-highlight { background: none !important; box-shadow: none !important; }';
                                    document.head.appendChild(style);

                                    // 2. ट्रांसलेटर स्क्रिप्ट लोड करना (अगर पहले से मौजूद न हो)
                                    if (!document.getElementById('google-translate-script')) {
                                        var div = document.createElement('div');
                                        div.id = 'google_translate_element';
                                        div.style.display = 'none';
                                        document.body.appendChild(div);

                                        var s1 = document.createElement('script');
                                        s1.type = 'text/javascript';
                                        s1.innerHTML = "function googleTranslateElementInit() { new google.translate.TranslateElement({pageLanguage: 'hi', includedLanguages: 'en,hi', autoDisplay: false}, 'google_translate_element'); }";
                                        document.head.appendChild(s1);

                                        var s2 = document.createElement('script');
                                        s2.id = 'google-translate-script';
                                        s2.type = 'text/javascript';
                                        s2.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
                                        document.head.appendChild(s2);
                                    }
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

        // 3. बॉटम फिक्स्ड बैक बटन
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