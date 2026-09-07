package com.medhavistudypoint.app

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

// 💬 चैट मैसेज डेटा मॉडल
data class LiveChatMessage(
    val id: String = "",
    val senderName: String = "",
    val senderId: String = "",
    val message: String = "",
    val isTeacher: Boolean = false,
    val timestamp: Long = 0L
)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoTitle: String,
    videoUrl: String = "",
    pdfUrl: String = "", // 👈 नया पैरामीटर: क्लास की PDF का लिंक
    onBackClick: () -> Unit,
    onNextVideoClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val navyBlue = Color(0xFF082A66)
    val royalBlue = Color(0xFF173F8F)
    val golden = Color(0xFFFFC400)
    val pageBg = Color(0xFFF6F7FB)

    // 🔙 बैक बटन हैंडलर
    BackHandler {
        onBackClick()
    }

    // 🔐 यूज़र पहचान
    val currentFirebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val userEmail = currentFirebaseUser?.email ?: "student@gmail.com"
    val userId = currentFirebaseUser?.uid ?: "unknown_user"

    val sharedPref = remember { context.getSharedPreferences("MedhaviUserProfile", android.content.Context.MODE_PRIVATE) }
    val studentDisplayName = remember {
        sharedPref.getString("user_name", null)?.takeIf { it.isNotBlank() }
            ?: currentFirebaseUser?.displayName?.takeIf { it.isNotBlank() }
            ?: "विद्यार्थी"
    }

    val adminEmails = listOf(
        "medhavistudypoint@gmail.com",
        "premkr9648@gmail.com"
    )
    val isAdmin = adminEmails.any { it.equals(userEmail, ignoreCase = true) }

    // 🔑 क्लास ID
    val cleanClassId = remember(videoTitle) {
        videoTitle.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
            .replace(" ", "_")
            .ifBlank { "default_class" }
    }

    val pollRef = remember(cleanClassId) {
        FirebaseDatabase.getInstance().getReference("polls").child(cleanClassId)
    }
    val pollResultsRef = remember(cleanClassId) {
        FirebaseDatabase.getInstance().getReference("poll_results").child(cleanClassId)
    }
    val studentScoresRef = remember(cleanClassId) {
        FirebaseDatabase.getInstance().getReference("class_leaderboard").child(cleanClassId)
    }
    val chatRef = remember(cleanClassId) {
        FirebaseDatabase.getInstance().getReference("live_chats").child(cleanClassId)
    }

    var showPollToStudents by remember { mutableStateOf(false) }
    var customTimeInput by remember { mutableStateOf("20") }
    var remainingTime by remember { mutableIntStateOf(0) }
    var pollEndTime by remember { mutableLongStateOf(0L) }
    var teacherSelectedCorrectOption by remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }

    var isLeaderboardActiveOnScreen by remember { mutableStateOf(false) }
    var studentScoreList = remember { mutableStateOf(listOf<StudentScoreModel>()) }

    var voteCounts by remember { mutableStateOf(mapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)) }
    var totalPollVotes by remember { mutableIntStateOf(0) }

    var isLiveClassMode by remember { mutableStateOf(false) }
    var isPrivateChatMode by remember { mutableStateOf(false) }

    var chatMessages by remember { mutableStateOf(listOf<LiveChatMessage>()) }
    var messageInputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // 🆔 YouTube Video ID निकालने का तरीका
    fun extractYouTubeId(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            trimmed.contains("v=") -> trimmed.substringAfter("v=").substringBefore("&")
            trimmed.contains("live/") -> trimmed.substringAfter("live/").substringBefore("?").substringBefore("&")
            trimmed.length == 11 && !trimmed.contains("/") -> trimmed
            else -> ""
        }
    }

    val ytVideoId = remember(videoUrl) { extractYouTubeId(videoUrl) }
    val isYouTubeVideo = remember(videoUrl, ytVideoId) {
        val trimmed = videoUrl.trim()
        !trimmed.contains("#exo") && (trimmed.contains("youtu.be") || trimmed.contains("youtube.com") || ytVideoId.length == 11)
    }

    var ytPlayerInstance by remember { mutableStateOf<YouTubePlayer?>(null) }

    // 📱 स्क्रीन ओरिएंटेशन और फुल स्क्रीन कंट्रोलर
    LaunchedEffect(isFullScreen) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun formatDrivePlayableUrl(url: String): String {
        if (url.contains("drive.google.com")) {
            val fileId = when {
                url.contains("/file/d/") -> url.substringAfter("/file/d/").substringBefore("/")
                url.contains("id=") -> url.substringAfter("id=").substringBefore("&")
                else -> ""
            }
            if (fileId.isNotBlank()) {
                return "https://docs.google.com/uc?export=download&id=$fileId"
            }
        }
        return url
    }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(2000, 15000, 1500, 2000)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }

    // जब Google Drive या ExoPlayer का वीडियो बदले
    LaunchedEffect(videoUrl, isYouTubeVideo) {
        if (!isYouTubeVideo) {
            val targetUrl = formatDrivePlayableUrl(videoUrl.trim().replace("#exo", ""))
            if (targetUrl.isNotBlank()) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(MediaItem.fromUri(targetUrl))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    // जब YouTube का वीडियो बदले
    LaunchedEffect(ytVideoId, ytPlayerInstance) {
        if (isYouTubeVideo && ytVideoId.isNotBlank() && ytPlayerInstance != null) {
            ytPlayerInstance?.loadVideo(ytVideoId, 0f)
        }
    }

    // 📡 क्लास पोल और सेटिंग्स लिसनर
    DisposableEffect(cleanClassId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isActive = snapshot.child("active").getValue(Boolean::class.java) ?: false
                val endMs = snapshot.child("endTime").getValue(Long::class.java) ?: 0L
                val correctOpt = snapshot.child("correctOption").getValue(Long::class.java)?.toInt() ?: 0
                val showLeaderboard = snapshot.child("showLeaderboard").getValue(Boolean::class.java) ?: false

                isLiveClassMode = snapshot.child("isLive").getValue(Boolean::class.java) ?: false
                isPrivateChatMode = snapshot.child("isPrivateChat").getValue(Boolean::class.java) ?: false
                val syncTime = snapshot.child("currentPosition").getValue(Long::class.java) ?: 0L

                showPollToStudents = isActive
                pollEndTime = endMs
                teacherSelectedCorrectOption = correctOpt
                isLeaderboardActiveOnScreen = showLeaderboard

                if (!isAdmin && isLiveClassMode && syncTime > 0L && !isYouTubeVideo) {
                    val currentPos = exoPlayer.currentPosition
                    if (kotlin.math.abs(currentPos - syncTime) > 5000L) {
                        exoPlayer.seekTo(syncTime)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        pollRef.addValueEventListener(listener)
        onDispose { pollRef.removeEventListener(listener) }
    }

    // 💬 लाइव चैट लिसनर
    DisposableEffect(cleanClassId, isAdmin, isPrivateChatMode, userId) {
        val chatListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<LiveChatMessage>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val sName = child.child("senderName").getValue(String::class.java) ?: ""
                    val sId = child.child("senderId").getValue(String::class.java) ?: ""
                    val msg = child.child("message").getValue(String::class.java) ?: ""
                    val isT = child.child("isTeacher").getValue(Boolean::class.java) ?: false
                    val time = child.child("timestamp").getValue(Long::class.java) ?: 0L

                    if (isAdmin || isT || !isPrivateChatMode || sId == userId) {
                        list.add(LiveChatMessage(id, sName, sId, msg, isT, time))
                    }
                }
                chatMessages = list.takeLast(50)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        chatRef.addValueEventListener(chatListener)
        onDispose { chatRef.removeEventListener(chatListener) }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    LaunchedEffect(isAdmin, isLiveClassMode) {
        if (isAdmin && isLiveClassMode) {
            while (true) {
                if (!isYouTubeVideo && exoPlayer.isPlaying) {
                    pollRef.child("currentPosition").setValue(exoPlayer.currentPosition)
                }
                delay(3000L)
            }
        }
    }

    // ⏱️ टाइमर और ऑटो-क्लोज़
    LaunchedEffect(showPollToStudents, pollEndTime) {
        if (showPollToStudents && pollEndTime > 0L) {
            while (showPollToStudents) {
                val now = System.currentTimeMillis()
                val diffSec = ((pollEndTime - now) / 1000L).toInt()

                if (diffSec > 0) {
                    remainingTime = diffSec
                } else {
                    remainingTime = 0
                    delay(3000L)
                    showPollToStudents = false
                    if (isAdmin) {
                        pollRef.child("active").setValue(false)
                    }
                    break
                }
                delay(500L)
            }
        }
    }

    DisposableEffect(cleanClassId) {
        val resultsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var c0 = 0; var c1 = 0; var c2 = 0; var c3 = 0; var total = 0
                for (child in snapshot.children) {
                    val opt = child.child("selectedIndex").getValue(Long::class.java)?.toInt() ?: -1
                    total++
                    when (opt) { 0 -> c0++; 1 -> c1++; 2 -> c2++; 3 -> c3++ }
                }
                voteCounts = mapOf(0 to c0, 1 to c1, 2 to c2, 3 to c3)
                totalPollVotes = total
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        pollResultsRef.addValueEventListener(resultsListener)
        onDispose { pollResultsRef.removeEventListener(resultsListener) }
    }

    DisposableEffect(cleanClassId) {
        val scoreListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StudentScoreModel>()
                for (child in snapshot.children) {
                    val name = child.child("name").getValue(String::class.java)
                        ?: child.child("email").getValue(String::class.java)?.substringBefore("@")
                        ?: "विद्यार्थी"
                    val correct = child.child("correctCount").getValue(Long::class.java)?.toInt() ?: 0
                    val incorrect = child.child("incorrectCount").getValue(Long::class.java)?.toInt() ?: 0
                    list.add(StudentScoreModel(name, correct, incorrect))
                }
                studentScoreList.value = list.sortedByDescending { it.correctCount }.take(20)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        studentScoresRef.addValueEventListener(scoreListener)
        onDispose { studentScoresRef.removeEventListener(scoreListener) }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(if (isFullScreen) Color.Black else pageBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullScreen) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLeaderboardActiveOnScreen) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = navyBlue
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🏆 MEDHAVI LIVE LEADERBOARD (Top 20)",
                            color = golden,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (studentScoreList.value.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("अभी तक कोई डेटा उपलब्ध नहीं है।", color = Color.White, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(studentScoreList.value) { score ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = score.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = royalBlue)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("✅ ${score.correctCount} सही", fontSize = 11.sp, color = Color(0xFF0F5132), fontWeight = FontWeight.Bold)
                                                Text("❌ ${score.incorrectCount} गलत", fontSize = 11.sp, color = Color(0xFF842029), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 🔄 प्लेयर इंजन स्विच: YouTube (FrameLayout Fix) या ExoPlayer
                if (isYouTubeVideo && ytVideoId.isNotBlank()) {
                    key(ytVideoId) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                FrameLayout(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )

                                    val ytView = YouTubePlayerView(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                        )
                                        lifecycleOwner.lifecycle.addObserver(this)
                                        enableAutomaticInitialization = false

                                        initialize(object : AbstractYouTubePlayerListener() {
                                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                                ytPlayerInstance = youTubePlayer
                                                youTubePlayer.loadVideo(ytVideoId, 0f)
                                            }

                                            override fun onError(
                                                youTubePlayer: YouTubePlayer,
                                                error: PlayerConstants.PlayerError
                                            ) {
                                                Log.e("YT_ERROR", "YouTube Error Code: $error")
                                            }
                                        })
                                    }
                                    addView(ytView)
                                }
                            }
                        )
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                setShowFastForwardButton(!isLiveClassMode || isAdmin)
                                setShowRewindButton(!isLiveClassMode || isAdmin)
                                setFullscreenButtonClickListener { isFull ->
                                    isFullScreen = isFull
                                }
                            }
                        },
                        update = { view ->
                            view.setShowFastForwardButton(!isLiveClassMode || isAdmin)
                            view.setShowRewindButton(!isLiveClassMode || isAdmin)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 🌟 फुल स्क्रीन में तैरता हुआ (Floating Overlay) पोल कार्ड
            if (isFullScreen && showPollToStudents) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.75f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        StudentLockedPollView(
                            correctAnswerIndex = teacherSelectedCorrectOption,
                            remainingTime = remainingTime,
                            userId = userId,
                            userEmail = userEmail,
                            userName = studentDisplayName,
                            pollResultsRef = pollResultsRef,
                            studentScoresRef = studentScoresRef,
                            voteCounts = voteCounts,
                            totalVotes = totalPollVotes,
                            isPollActive = showPollToStudents
                        )
                    }
                }
            }
        }

        if (!isFullScreen) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = videoTitle, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = navyBlue)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isLiveClassMode) "🔴 लाइव क्लास चालू है" else "📁 रिकॉर्डेड क्लास",
                            fontSize = 12.sp,
                            color = if (isLiveClassMode) Color(0xFFDC2626) else Color.DarkGray,
                            fontWeight = if (isLiveClassMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // 📄 नया: क्लास नोट्स (PDF) बटन
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (pdfUrl.isNotBlank()) {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "PDF खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "इस क्लास की PDF जल्द उपलब्ध होगी!", Toast.LENGTH_SHORT).show()
                            }
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (pdfUrl.isNotBlank()) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (pdfUrl.isNotBlank()) Color(0xFF93C5FD) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📄", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "क्लास नोट्स (PDF)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (pdfUrl.isNotBlank()) Color(0xFF1D4ED8) else Color(0xFF64748B)
                                )
                                Text(
                                    text = if (pdfUrl.isNotBlank()) "नोट्स पढ़ने या डाउनलोड करने के लिए क्लिक करें" else "जल्द अपलोड किए जाएँगे",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        if (pdfUrl.isNotBlank()) {
                            Text(
                                text = "डाउनलोड 📥",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 👑 टीचर कंट्रोल पैनल
                if (isAdmin) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, golden)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("लाइव मोड", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = royalBlue)
                                Button(
                                    onClick = {
                                        val nextMode = !isLiveClassMode
                                        pollRef.child("isLive").setValue(nextMode)
                                        if (nextMode) {
                                            if (!isYouTubeVideo) {
                                                pollRef.child("currentPosition").setValue(exoPlayer.currentPosition)
                                            }
                                        } else {
                                            chatRef.removeValue()
                                            pollResultsRef.removeValue()
                                            studentScoresRef.removeValue()
                                            pollRef.child("active").setValue(false)
                                            pollRef.child("showLeaderboard").setValue(false)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLiveClassMode) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isLiveClassMode) "🔴 लाइव खत्म करें" else "🟢 लाइव शुरू करें",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (isLiveClassMode) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isPrivateChatMode) "चैट: 🔒 केवल शिक्षक" else "चैट: 📢 सार्वजनिक (सबको)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPrivateChatMode) Color(0xFFB91C1C) else Color(0xFF15803D)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val nextMode = !isPrivateChatMode
                                            pollRef.child("isPrivateChat").setValue(nextMode)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (isPrivateChatMode) "सार्वजनिक करें" else "प्राइवेट करें 🔒", fontSize = 11.sp)
                                    }
                                }
                            }

                            Divider(color = Color(0xFFE5E7EB), modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("लाइव पोल (${remainingTime}s)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                Button(
                                    onClick = {
                                        val newState = !showPollToStudents
                                        if (newState) {
                                            pollResultsRef.removeValue()
                                            val timeVal = customTimeInput.toIntOrNull() ?: 20
                                            val currentTime = System.currentTimeMillis()
                                            val calculatedEndTime = currentTime + (timeVal * 1000L)

                                            pollRef.updateChildren(
                                                mapOf(
                                                    "active" to true,
                                                    "duration" to timeVal,
                                                    "endTime" to calculatedEndTime,
                                                    "correctOption" to teacherSelectedCorrectOption,
                                                    "showLeaderboard" to false
                                                )
                                            )
                                        } else {
                                            pollRef.child("active").setValue(false)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = golden),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = if (showPollToStudents) "बंद करें" else "पोल भेजें 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (!showPollToStudents) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("सही उत्तर:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    for (i in 0..3) {
                                        val isChosen = teacherSelectedCorrectOption == i
                                        val optLetter = when(i) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                                        Surface(
                                            modifier = Modifier.size(30.dp).clickable { teacherSelectedCorrectOption = i },
                                            shape = CircleShape,
                                            color = if (isChosen) royalBlue else Color.White,
                                            border = BorderStroke(1.dp, royalBlue)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = optLetter, color = if (isChosen) Color.White else royalBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("समय (सेकंड):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    OutlinedTextField(
                                        value = customTimeInput,
                                        onValueChange = { customTimeInput = it },
                                        modifier = Modifier.width(75.dp).height(46.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val newLeaderboardState = !isLeaderboardActiveOnScreen
                                    pollRef.child("showLeaderboard").setValue(newLeaderboardState)
                                },
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isLeaderboardActiveOnScreen) Color(0xFFDC2626) else royalBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isLeaderboardActiveOnScreen) "❌ लीडरबोर्ड बंद करें" else "🏆 वीडियो पर लीडरबोर्ड दिखाएं",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 🎓 पोर्ट्रेट मोड में पोल कार्ड
                if (showPollToStudents) {
                    StudentLockedPollView(
                        correctAnswerIndex = teacherSelectedCorrectOption,
                        remainingTime = remainingTime,
                        userId = userId,
                        userEmail = userEmail,
                        userName = studentDisplayName,
                        pollResultsRef = pollResultsRef,
                        studentScoresRef = studentScoresRef,
                        voteCounts = voteCounts,
                        totalVotes = totalPollVotes,
                        isPollActive = showPollToStudents
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 💬 लाइव चैट सेक्शन
                if (isLiveClassMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💬 लाइव प्रश्न / चैट", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = navyBlue)
                                if (isPrivateChatMode) {
                                    Text("🔒 केवल शिक्षक को दिखेगा", fontSize = 11.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                } else {
                                    Text("📢 सार्वजनिक चैट", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                }
                            }
                            Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))

                            LazyColumn(
                                state = chatListState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (chatMessages.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Text("अभी कोई प्रश्न नहीं है। अपना सवाल पूछें...", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                } else {
                                    items(chatMessages) { msg ->
                                        val isMe = msg.senderId == userId
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                        ) {
                                            Surface(
                                                color = when {
                                                    msg.isTeacher -> Color(0xFFFEF3C7)
                                                    isMe -> Color(0xFFE0E7FF)
                                                    else -> Color(0xFFF3F4F6)
                                                },
                                                border = if (msg.isTeacher) BorderStroke(1.dp, golden) else null,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.clickable(enabled = isAdmin && !isMe) {
                                                    messageInputText = "@${msg.senderName} "
                                                }
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = when {
                                                                msg.isTeacher -> "👑 शिक्षक (Teacher)"
                                                                isMe -> "आप"
                                                                else -> msg.senderName
                                                            },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (msg.isTeacher) Color(0xFF92400E) else royalBlue
                                                        )
                                                        if (isAdmin && !isMe) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("(उत्तर दें ↩)", fontSize = 9.sp, color = Color.Gray)
                                                        }
                                                    }
                                                    Text(
                                                        text = msg.message,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF1F2937)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = messageInputText,
                                    onValueChange = { messageInputText = it },
                                    placeholder = {
                                        Text(
                                            text = if (isAdmin) "छात्रों को उत्तर दें..." else if (isPrivateChatMode) "शिक्षक से प्रश्न पूछें..." else "प्रश्न पूछें...",
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val trimmedMsg = messageInputText.trim()
                                        if (trimmedMsg.isNotBlank()) {
                                            val newMsgMap = mapOf(
                                                "senderName" to (if (isAdmin) "शिक्षक" else studentDisplayName),
                                                "senderId" to userId,
                                                "message" to trimmedMsg,
                                                "isTeacher" to isAdmin,
                                                "timestamp" to System.currentTimeMillis()
                                            )
                                            chatRef.push().setValue(newMsgMap)
                                            messageInputText = ""
                                        }
                                    },
                                    modifier = Modifier.height(46.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isAdmin) golden else navyBlue),
                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = "➔",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdmin) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("← वापस जाएँ")
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Button(
                        onClick = onNextVideoClick,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("अगली क्लास ➔", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class StudentScoreModel(
    val name: String,
    val correctCount: Int,
    val incorrectCount: Int
)

@Composable
fun StudentLockedPollView(
    correctAnswerIndex: Int,
    remainingTime: Int,
    userId: String,
    userEmail: String,
    userName: String,
    pollResultsRef: com.google.firebase.database.DatabaseReference,
    studentScoresRef: com.google.firebase.database.DatabaseReference,
    voteCounts: Map<Int, Int>,
    totalVotes: Int,
    isPollActive: Boolean
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(isPollActive) {
        if (isPollActive) {
            selectedIndex = null
            isSubmitted = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚡ लाइव पोल (उत्तर दें)", fontWeight = FontWeight.Bold, color = Color(0xFF082A66), fontSize = 14.sp)
                Surface(color = if (remainingTime > 0) Color(0xFFFEE2E2) else Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = if (remainingTime > 0) "⏱️ ${remainingTime}s remaining" else "⏰ समय समाप्त",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = if (remainingTime > 0) Color(0xFFB91C1C) else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = if (remainingTime > 0) "सही विकल्प चुनें:" else "📊 पोल परिणाम (% वोट):", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0..3) {
                    val isSelected = selectedIndex == i
                    val isCorrect = i == correctAnswerIndex

                    val count = voteCounts[i] ?: 0
                    val percentage = if (totalVotes > 0) (count * 100) / totalVotes else 0

                    val bg = when {
                        remainingTime == 0 && isCorrect -> Color(0xFFD1E7DD)
                        remainingTime == 0 && isSelected && !isCorrect -> Color(0xFFF8D7DA)
                        isSelected -> Color(0xFF173F8F)
                        else -> Color(0xFFF1F5F9)
                    }
                    val textColor = if (isSelected && remainingTime > 0) Color.White else Color.Black

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable(enabled = !isSubmitted && remainingTime > 0) { selectedIndex = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when(i) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" },
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (remainingTime == 0) {
                                Text(
                                    text = "$percentage%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) Color(0xFF0F5132) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isSubmitted && remainingTime > 0) {
                Button(
                    onClick = {
                        if (selectedIndex != null) {
                            isSubmitted = true
                            val isCorrect = selectedIndex == correctAnswerIndex

                            pollResultsRef.child(userId.replace(".", "_")).setValue(
                                mapOf("selectedIndex" to selectedIndex, "isCorrect" to isCorrect)
                            )

                            val userScorePath = studentScoresRef.child(userId.replace(".", "_"))
                            userScorePath.child("name").setValue(userName)
                            userScorePath.child("email").setValue(userEmail)
                            userScorePath.get().addOnSuccessListener { snapshot ->
                                val currentCorrect = snapshot.child("correctCount").getValue(Long::class.java)?.toInt() ?: 0
                                val currentIncorrect = snapshot.child("incorrectCount").getValue(Long::class.java)?.toInt() ?: 0

                                if (isCorrect) {
                                    userScorePath.child("correctCount").setValue(currentCorrect + 1)
                                } else {
                                    userScorePath.child("incorrectCount").setValue(currentIncorrect + 1)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF082A66)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedIndex != null
                ) {
                    Text("उत्तर लॉक करें (Submit)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else if (remainingTime == 0) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedIndex == correctAnswerIndex) "✅ गजब! आपका उत्तर सही था।" else "❌ समय समाप्त! सही उत्तर हाईलाइट है।",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F5132)
                    )
                }
            }
        }
    }
}