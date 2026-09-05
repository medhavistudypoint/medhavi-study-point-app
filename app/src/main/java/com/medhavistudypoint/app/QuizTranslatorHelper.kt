package com.medhavistudypoint.app

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

object QuizTranslatorHelper {
    // हिंदी से इंग्लिश ट्रांसलेशन कॉन्फ़िगरेशन
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.HINDI)
        .setTargetLanguage(TranslateLanguage.ENGLISH)
        .build()

    private val translator = Translation.getClient(options)
    private var isModelDownloaded = false

    // टेक्स्ट को ट्रांसलेट करने वाला मुख्य फंक्शन
    suspend fun translate(text: String): String {
        if (text.isBlank()) return text
        return try {
            // पहली बार में मॉडल डाउनलोड होगा
            if (!isModelDownloaded) {
                translator.downloadModelIfNeeded().await()
                isModelDownloaded = true
            }
            translator.translate(text).await()
        } catch (e: Exception) {
            text // किसी कारणवश एरर आने पर ओरिजिनल टेक्स्ट ही वापस करेगा
        }
    }
}