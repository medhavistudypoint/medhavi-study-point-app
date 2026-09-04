package com.medhavistudypoint.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class QuestionItem(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correct: Int,
    val explanation: String = ""
)

// स्थायी स्टोरेज (SharedPreferences) मैनेजर — स्विच ऑफ होने पर भी डेटा सुरक्षित रखता है
object QuizPersistentManager {
    private const val PREF_NAME = "medhavi_quiz_prefs"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isSubmitted(testId: String): Boolean {
        return prefs?.getBoolean("${testId}_submitted", false) ?: false
    }

    fun hasUnfinishedProgress(testId: String): Boolean {
        val submitted = isSubmitted(testId)
        val hasAnswers = prefs?.contains("${testId}_live_answers") ?: false
        return !submitted && hasAnswers
    }

    fun saveLiveProgress(
        testId: String,
        answers: Map<Int, Int>,
        visited: Set<Int>,
        marked: Set<Int>,
        currentIndex: Int,
        remainingSeconds: Int
    ) {
        prefs?.edit()?.apply {
            val ansJson = JSONObject()
            answers.forEach { (k, v) -> ansJson.put(k.toString(), v) }
            putString("${testId}_live_answers", ansJson.toString())

            val visitedArr = JSONArray()
            visited.forEach { visitedArr.put(it) }
            putString("${testId}_live_visited", visitedArr.toString())

            val markedArr = JSONArray()
            marked.forEach { markedArr.put(it) }
            putString("${testId}_live_marked", markedArr.toString())

            putInt("${testId}_live_index", currentIndex)
            putInt("${testId}_live_time", remainingSeconds)
            putBoolean("${testId}_submitted", false)
            apply()
        }
    }

    fun loadLiveAnswers(testId: String): MutableMap<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        val str = prefs?.getString("${testId}_live_answers", null) ?: return map
        val json = JSONObject(str)
        json.keys().forEach { k -> map[k.toInt()] = json.getInt(k) }
        return map
    }

    fun loadLiveVisited(testId: String): MutableSet<Int> {
        val set = mutableSetOf<Int>()
        val str = prefs?.getString("${testId}_live_visited", null) ?: return set
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) set.add(arr.getInt(i))
        return set
    }

    fun loadLiveMarked(testId: String): MutableSet<Int> {
        val set = mutableSetOf<Int>()
        val str = prefs?.getString("${testId}_live_marked", null) ?: return set
        val arr = JSONArray(str)
        for (i in 0 until arr.length()) set.add(arr.getInt(i))
        return set
    }

    fun loadLiveIndex(testId: String): Int = prefs?.getInt("${testId}_live_index", 0) ?: 0
    fun loadLiveTime(testId: String, defaultTime: Int): Int = prefs?.getInt("${testId}_live_time", defaultTime) ?: defaultTime

    fun markSubmitted(
        testId: String,
        answers: Map<Int, Int>,
        durationTaken: Int
    ) {
        prefs?.edit()?.apply {
            putBoolean("${testId}_submitted", true)
            putInt("${testId}_duration_taken", durationTaken)

            val ansJson = JSONObject()
            answers.forEach { (k, v) -> ansJson.put(k.toString(), v) }
            putString("${testId}_submitted_answers", ansJson.toString())

            // अधूरा सत्र साफ़ करें
            remove("${testId}_live_answers")
            remove("${testId}_live_visited")
            remove("${testId}_live_marked")
            remove("${testId}_live_index")
            remove("${testId}_live_time")
            apply()
        }
    }

    fun loadSubmittedAnswers(testId: String): MutableMap<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        val str = prefs?.getString("${testId}_submitted_answers", null) ?: return map
        val json = JSONObject(str)
        json.keys().forEach { k -> map[k.toInt()] = json.getInt(k) }
        return map
    }

    fun loadSubmittedDuration(testId: String): Int = prefs?.getInt("${testId}_duration_taken", 0) ?: 0

    fun getAttemptCount(testId: String): Int = prefs?.getInt("${testId}_attempt_count", 1) ?: 1

    fun incrementAttemptCount(testId: String) {
        val count = getAttemptCount(testId)
        prefs?.edit()?.putInt("${testId}_attempt_count", count + 1)?.apply()
    }

    fun resetSessionForReattempt(testId: String) {
        prefs?.edit()?.apply {
            remove("${testId}_submitted")
            remove("${testId}_submitted_answers")
            remove("${testId}_duration_taken")
            remove("${testId}_live_answers")
            remove("${testId}_live_visited")
            remove("${testId}_live_marked")
            remove("${testId}_live_index")
            remove("${testId}_live_time")
            apply()
        }
    }
}

@Composable
fun NativeQuizScreen(
    innerPadding: PaddingValues,
    testId: String = "test41",
    testTitle: String,
    studentName: String = "Ajit",
    negativeMarkingPerWrong: Double = 0.33,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        QuizPersistentManager.init(context)
    }

    val navyBlue = Color(0xFF0F1E4A)
    val goldYellow = Color(0xFFFACC15)

    var dataset by remember(testId) { mutableStateOf<List<QuestionItem>>(emptyList()) }
    var isLoading by remember(testId) { mutableStateOf(true) }
    var loadFailed by remember(testId) { mutableStateOf(false) }

    val totalDurationSeconds = remember(testId) { 50 * 60 }

    // Firebase से सवाल लोड करना
    LaunchedEffect(testId) {
        QuizPersistentManager.init(context)
        isLoading = true
        loadFailed = false
        val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("quizzes")
            .child(testId)

        dbRef.get().addOnSuccessListener { snapshot ->
            val qList = mutableListOf<QuestionItem>()
            val questionsSnapshot = snapshot.child("questions")

            for (child in questionsSnapshot.children) {
                val id = child.child("id").getValue(Int::class.java) ?: (qList.size + 1)
                val question = child.child("question").getValue(String::class.java) ?: ""
                val correct = child.child("correct").getValue(Int::class.java) ?: 0
                val explanation = child.child("explanation").getValue(String::class.java) ?: ""

                val opts = mutableListOf<String>()
                for (optChild in child.child("options").children) {
                    opts.add(optChild.getValue(String::class.java) ?: "")
                }

                if (question.isNotBlank()) {
                    qList.add(QuestionItem(id, question, opts, correct, explanation))
                }
            }
            if (qList.isNotEmpty()) {
                dataset = qList
                loadFailed = false
            } else {
                loadFailed = true
            }
            isLoading = false
        }.addOnFailureListener {
            loadFailed = true
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = navyBlue)
                Spacer(modifier = Modifier.height(12.dp))
                Text("प्रश्न पत्र लोड हो रहा है ($testId)...", color = navyBlue, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    if (loadFailed || dataset.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                Text("⚠️ टेस्ट आईडी '$testId' Firebase में नहीं मिला!", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("कृपया Firebase के 'quizzes' नोड में '$testId' अपलोड करें।", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = navyBlue)) {
                    Text("वापस जाएं")
                }
            }
        }
        return
    }

    // टेस्ट की स्थिति लोड करना
    val isAlreadySubmitted = remember(testId) { QuizPersistentManager.isSubmitted(testId) }
    val hasUnfinished = remember(testId) { QuizPersistentManager.hasUnfinishedProgress(testId) }

    var showResumeRestartDialog by remember(testId) { mutableStateOf(hasUnfinished && !isAlreadySubmitted) }
    var showCompletedGate by remember(testId) { mutableStateOf(isAlreadySubmitted) }
    var isSubmitted by remember(testId) { mutableStateOf(isAlreadySubmitted) }

    var currentIndex by remember(testId) { mutableIntStateOf(0) }
    val userAnswers = remember(testId) { mutableStateMapOf<Int, Int>() }
    val visitedQuestions = remember(testId) { mutableStateSetOf<Int>() }
    val markedQuestions = remember(testId) { mutableStateSetOf<Int>() }
    var remainingSeconds by remember(testId) { mutableIntStateOf(totalDurationSeconds) }
    var attemptCount by remember(testId) { mutableIntStateOf(QuizPersistentManager.getAttemptCount(testId)) }

    var showConfirmDialog by remember(testId) { mutableStateOf(false) }

    // अधूरा टेस्ट होने पर पूछने वाला डायलॉग (Resume vs Restart)
    if (showResumeRestartDialog) {
        AlertDialog(
            onDismissRequest = { /* डिसमिस बंद */ },
            title = { Text("अधूरा टेस्ट उपलब्ध है", fontWeight = FontWeight.Bold, color = navyBlue) },
            text = { Text("आपने यह टेस्ट पहले बीच में छोड़ दिया था। क्या आप वहीं से शुरू करना चाहते हैं या नए सिरे से?") },
            confirmButton = {
                Button(
                    onClick = {
                        userAnswers.clear()
                        userAnswers.putAll(QuizPersistentManager.loadLiveAnswers(testId))
                        visitedQuestions.clear()
                        visitedQuestions.addAll(QuizPersistentManager.loadLiveVisited(testId))
                        markedQuestions.clear()
                        markedQuestions.addAll(QuizPersistentManager.loadLiveMarked(testId))
                        currentIndex = QuizPersistentManager.loadLiveIndex(testId)
                        remainingSeconds = QuizPersistentManager.loadLiveTime(testId, totalDurationSeconds)
                        showResumeRestartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Resume (वहीं से शुरू करें)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        QuizPersistentManager.resetSessionForReattempt(testId)
                        userAnswers.clear()
                        visitedQuestions.clear()
                        markedQuestions.clear()
                        currentIndex = 0
                        remainingSeconds = totalDurationSeconds
                        showResumeRestartDialog = false
                    }
                ) {
                    Text("Restart (नए सिरे से)")
                }
            }
        )
    }

    // पहला सवाल विजिट मार्क करें
    LaunchedEffect(currentIndex, isSubmitted, showCompletedGate, showResumeRestartDialog) {
        if (!isSubmitted && !showCompletedGate && !showResumeRestartDialog && dataset.isNotEmpty()) {
            visitedQuestions.add(dataset[currentIndex].id)
            QuizPersistentManager.saveLiveProgress(
                testId = testId,
                answers = userAnswers,
                visited = visitedQuestions,
                marked = markedQuestions,
                currentIndex = currentIndex,
                remainingSeconds = remainingSeconds
            )
        }
    }

    // टाइमर
    LaunchedEffect(isSubmitted, showCompletedGate, showResumeRestartDialog, testId) {
        while (!isSubmitted && !showCompletedGate && !showResumeRestartDialog && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            // हर 5 सेकंड में या सवाल बदलने पर टाइमर सुरक्षित करें
            if (remainingSeconds % 5 == 0) {
                QuizPersistentManager.saveLiveProgress(
                    testId = testId,
                    answers = userAnswers,
                    visited = visitedQuestions,
                    marked = markedQuestions,
                    currentIndex = currentIndex,
                    remainingSeconds = remainingSeconds
                )
            }
        }
        if (remainingSeconds == 0 && !isSubmitted && !showCompletedGate && !showResumeRestartDialog) {
            isSubmitted = true
            showCompletedGate = false
            val durationTaken = totalDurationSeconds - remainingSeconds
            QuizPersistentManager.markSubmitted(testId, userAnswers, durationTaken)
        }
    }

    val currentQ = dataset.getOrElse(currentIndex) { dataset[0] }

    // सबमिट कन्फर्मेशन डायलॉग
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("टेस्ट सबमिट करें?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("कुल प्रश्न: ${dataset.size}")
                    Text("हल किए गए: ${userAnswers.size}", color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    Text("छोड़े गए: ${dataset.size - userAnswers.size}", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("क्या आप वाकई टेस्ट समाप्त करना चाहते हैं?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isSubmitted = true
                        showCompletedGate = false
                        val durationTaken = totalDurationSeconds - remainingSeconds
                        QuizPersistentManager.markSubmitted(testId, userAnswers, durationTaken)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("हाँ, सबमिट करें")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("नहीं")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color(0xFFF1F5F9))
    ) {
        // हेडर
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(navyBlue)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MEDHAVI STUDY POINT", color = goldYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(testTitle, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        if (attemptCount > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(Re-attempt #$attemptCount)",
                                color = goldYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E3A8A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val m = remainingSeconds / 60
                    val s = remainingSeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", m, s),
                        color = goldYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 1. परीक्षा पूर्ण हो चुकी है वाला गेट
        if (showCompletedGate) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📊 परीक्षा पूर्ण हो चुकी है!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = navyBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$studentName, आप यह परीक्षा पहले ही दे चुके हैं।",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                userAnswers.clear()
                                userAnswers.putAll(QuizPersistentManager.loadSubmittedAnswers(testId))
                                showCompletedGate = false
                                isSubmitted = true
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📊 See Previous Result (परिणाम देखें)", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                QuizPersistentManager.resetSessionForReattempt(testId)
                                QuizPersistentManager.incrementAttemptCount(testId)
                                attemptCount = QuizPersistentManager.getAttemptCount(testId)
                                userAnswers.clear()
                                visitedQuestions.clear()
                                markedQuestions.clear()
                                currentIndex = 0
                                remainingSeconds = totalDurationSeconds
                                showCompletedGate = false
                                isSubmitted = false
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔄 Re-Attempt Test (पुनः प्रयास करें)", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "🔙 विषय चयन पर वापस जाएं",
                            fontSize = 13.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onBack() }
                        )
                    }
                }
            }
        } else if (isSubmitted) {
            // 2. रिज़ल्ट स्क्रीन
            val correctCount = dataset.count { userAnswers[it.id] == it.correct }
            val incorrectCount = userAnswers.count { (id, ans) -> ans != (dataset.find { it.id == id }?.correct ?: -1) }
            val skippedCount = dataset.size - userAnswers.size
            val savedDuration = QuizPersistentManager.loadSubmittedDuration(testId)
            val timeTakenSeconds = if (savedDuration > 0) savedDuration else (totalDurationSeconds - remainingSeconds)
            val finalScore = (correctCount * 1.0) - (incorrectCount * negativeMarkingPerWrong)
            val scoreFormatted = String.format(Locale.US, "%.2f", if (finalScore < 0) 0.0 else finalScore)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Medhavi Study Point — Result Dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = navyBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "परीक्षार्थी: $studentName", fontSize = 14.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    if (attemptCount > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF08A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("RE-ATTEMPTED #$attemptCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF854D0E))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResultStatCard(title = "$scoreFormatted / ${dataset.size}", subtitle = "SCORE", bg = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                    ResultStatCard(title = "$correctCount", subtitle = "CORRECT", bg = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResultStatCard(title = "$incorrectCount", subtitle = "INCORRECT", bg = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                    ResultStatCard(title = "$skippedCount", subtitle = "SKIPPED", bg = Color(0xFFEA580C), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                ResultStatCard(
                    title = String.format(Locale.US, "%02d:%02d", timeTakenSeconds / 60, timeTakenSeconds % 60),
                    subtitle = "TIME",
                    bg = Color(0xFF7C3AED),
                    modifier = Modifier.fillMaxWidth(0.5f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        QuizPersistentManager.resetSessionForReattempt(testId)
                        QuizPersistentManager.incrementAttemptCount(testId)
                        attemptCount = QuizPersistentManager.getAttemptCount(testId)
                        userAnswers.clear()
                        visitedQuestions.clear()
                        markedQuestions.clear()
                        currentIndex = 0
                        remainingSeconds = totalDurationSeconds
                        isSubmitted = false
                        showCompletedGate = false
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🔄 Re-attempt Test (पुनः प्रयास करें)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "उत्तर कुंजी एवं विस्तृत हल (All Solutions)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = navyBlue,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                dataset.forEachIndexed { index, q ->
                    val userAns = userAnswers[q.id]
                    val isCorrect = userAns == q.correct
                    val isSkipped = userAns == null

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Q.${index + 1}", fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 15.sp)
                                Text(
                                    text = when {
                                        isCorrect -> "✔ सही (+1)"
                                        isSkipped -> "⚪ छोड़ा गया (0)"
                                        else -> "✖ गलत (-${String.format(Locale.US, "%.2f", negativeMarkingPerWrong)})"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isCorrect -> Color(0xFF16A34A)
                                        isSkipped -> Color(0xFF64748B)
                                        else -> Color(0xFFDC2626)
                                    },
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = q.question, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!isCorrect && !isSkipped) {
                                Text("आपका उत्तर: ${q.options.getOrElse(userAns!!) { "" }}", color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("सही उत्तर: ${q.options.getOrElse(q.correct) { "" }}", color = Color(0xFF16A34A), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            if (q.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF9C3), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color(0xFFFE0000).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("💡 व्याख्या (Explanation):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF854D0E))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = q.explanation, fontSize = 12.sp, color = Color(0xFF713F12), lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("मुख्य डैशबोर्ड पर वापस जाएं", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // 3. लाइव टेस्ट प्रश्न स्क्रीन
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("प्रश्न: ${currentIndex + 1} / ${dataset.size}", fontWeight = FontWeight.Bold, color = navyBlue)
                    Text("ऑनलाइन टेस्ट", color = Color(0xFF0284C7), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = currentQ.question, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), lineHeight = 22.sp)

                Spacer(modifier = Modifier.height(14.dp))

                currentQ.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = userAnswers[currentQ.id] == optIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable {
                                userAnswers[currentQ.id] = optIndex
                                QuizPersistentManager.saveLiveProgress(
                                    testId = testId,
                                    answers = userAnswers,
                                    visited = visitedQuestions,
                                    marked = markedQuestions,
                                    currentIndex = currentIndex,
                                    remainingSeconds = remainingSeconds
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = ('A' + optIndex).toString(), color = if (isSelected) Color.White else navyBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = optionText, fontSize = 14.sp, color = Color(0xFF1E293B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                QuizPersistentManager.saveLiveProgress(
                                    testId = testId,
                                    answers = userAnswers,
                                    visited = visitedQuestions,
                                    marked = markedQuestions,
                                    currentIndex = currentIndex,
                                    remainingSeconds = remainingSeconds
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = currentIndex > 0
                    ) {
                        Text("◀ पिछला")
                    }
                    OutlinedButton(
                        onClick = {
                            userAnswers.remove(currentQ.id)
                            QuizPersistentManager.saveLiveProgress(
                                testId = testId,
                                answers = userAnswers,
                                visited = visitedQuestions,
                                marked = markedQuestions,
                                currentIndex = currentIndex,
                                remainingSeconds = remainingSeconds
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear")
                    }
                    OutlinedButton(
                        onClick = {
                            if (markedQuestions.contains(currentQ.id)) markedQuestions.remove(currentQ.id)
                            else markedQuestions.add(currentQ.id)

                            QuizPersistentManager.saveLiveProgress(
                                testId = testId,
                                answers = userAnswers,
                                visited = visitedQuestions,
                                marked = markedQuestions,
                                currentIndex = currentIndex,
                                remainingSeconds = remainingSeconds
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (markedQuestions.contains(currentQ.id)) "★ Marked" else "☆ Mark")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (currentIndex < dataset.size - 1) {
                            currentIndex++
                            QuizPersistentManager.saveLiveProgress(
                                testId = testId,
                                answers = userAnswers,
                                visited = visitedQuestions,
                                marked = markedQuestions,
                                currentIndex = currentIndex,
                                remainingSeconds = remainingSeconds
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (currentIndex == dataset.size - 1) "अंतिम प्रश्न" else "अगला ▶", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text("प्रश्न पैलेट ग्रिड", fontWeight = FontWeight.Bold, color = navyBlue)
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(dataset.size) { idx ->
                        val qId = dataset[idx].id
                        val isAns = userAnswers.containsKey(qId)
                        val isMark = markedQuestions.contains(qId)
                        val isVisited = visitedQuestions.contains(qId)
                        val isCur = idx == currentIndex

                        val bg = when {
                            isCur && !isAns && !isMark -> Color.White
                            isMark -> Color(0xFF8B5CF6)
                            isAns -> Color(0xFF16A34A)
                            isVisited -> Color(0xFFDC2626)
                            else -> Color(0xFFFFFFFF)
                        }

                        val borderColor = if (isCur) Color(0xFFDC2626) else Color(0xFFCBD5E1)
                        val borderWidth = if (isCur) 2.5.dp else 1.dp
                        val textColor = when {
                            isCur && !isAns && !isMark -> Color(0xFFDC2626)
                            isMark || isAns || isVisited -> Color.White
                            else -> Color(0xFF1E293B)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(bg, RoundedCornerShape(6.dp))
                                .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(6.dp))
                                .clickable {
                                    currentIndex = idx
                                    QuizPersistentManager.saveLiveProgress(
                                        testId = testId,
                                        answers = userAnswers,
                                        visited = visitedQuestions,
                                        marked = markedQuestions,
                                        currentIndex = currentIndex,
                                        remainingSeconds = remainingSeconds
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${idx + 1}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("✔ सबमिट करें (Submit Test)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(42.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1E4A)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("← Back", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResultStatCard(title: String, subtitle: String, bg: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(76.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
        }
    }
}