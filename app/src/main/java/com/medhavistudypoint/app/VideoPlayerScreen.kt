package com.medhavistudypoint.app

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoTitle: String,
    videoUrl: String = "", // 👈 Firebase से आ रहा वीडियो लिंक
    onBackClick: () -> Unit,
    onNextVideoClick: () -> Unit
) {
    val context = LocalContext.current
    val navyBlue = Color(0xFF082A66)
    val royalBlue = Color(0xFF173F8F)
    val golden = Color(0xFFFFC400)
    val pageBg = Color(0xFFF6F7FB)

    // 🔐 एडमिन और यूजर पहचान
    val currentFirebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val userEmail = currentFirebaseUser?.email ?: "student@gmail.com"
    val userId = currentFirebaseUser?.uid ?: "unknown_user"

    val adminEmails = listOf(
        "medhavistudypoint@gmail.com",
        "premkr9648@gmail.com"
    )
    val isAdmin = adminEmails.any { it.equals(userEmail, ignoreCase = true) }

    // 🔥 Firebase References
    val pollRef = remember { FirebaseDatabase.getInstance().getReference("polls") }
    val pollResultsRef = remember { FirebaseDatabase.getInstance().getReference("poll_results") }
    val studentScoresRef = remember { FirebaseDatabase.getInstance().getReference("class_leaderboard") }

    var showPollToStudents by remember { mutableStateOf(false) }
    var customTimeInput by remember { mutableStateOf("20") }
    var remainingTime by remember { mutableStateOf(0) }
    var teacherSelectedCorrectOption by remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }

    var isLeaderboardActiveOnScreen by remember { mutableStateOf(false) }
    var studentScoreList = remember { mutableStateOf(listOf<StudentScoreModel>()) }

    var voteCounts by remember { mutableStateOf(mapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)) }
    var totalPollVotes by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isActive = snapshot.child("active").getValue(Boolean::class.java) ?: false
                val duration = snapshot.child("duration").getValue(Long::class.java)?.toInt() ?: 20
                val correctOpt = snapshot.child("correctOption").getValue(Long::class.java)?.toInt() ?: 0
                val showLeaderboard = snapshot.child("showLeaderboard").getValue(Boolean::class.java) ?: false

                showPollToStudents = isActive
                isLeaderboardActiveOnScreen = showLeaderboard

                if (isActive) {
                    remainingTime = duration
                    teacherSelectedCorrectOption = correctOpt
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        pollRef.addValueEventListener(listener)
        onDispose { pollRef.removeEventListener(listener) }
    }

    DisposableEffect(Unit) {
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

    DisposableEffect(Unit) {
        val scoreListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StudentScoreModel>()
                for (child in snapshot.children) {
                    val email = child.child("email").getValue(String::class.java) ?: "Student"
                    val correct = child.child("correctCount").getValue(Long::class.java)?.toInt() ?: 0
                    val incorrect = child.child("incorrectCount").getValue(Long::class.java)?.toInt() ?: 0
                    list.add(StudentScoreModel(email, correct, incorrect))
                }
                studentScoreList.value = list.sortedByDescending { it.correctCount }.take(20)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        studentScoresRef.addValueEventListener(scoreListener)
        onDispose { studentScoresRef.removeEventListener(scoreListener) }
    }

    LaunchedEffect(showPollToStudents, remainingTime) {
        if (showPollToStudents && remainingTime > 0) {
            kotlinx.coroutines.delay(1000L)
            remainingTime -= 1
        } else if (remainingTime == 0 && showPollToStudents && isAdmin) {
            pollRef.child("active").setValue(false)
        }
    }

    // 🎬 प्लेयर इनिशियलाइज़ेशन
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // 🔄 जब भी नया videoUrl आए, प्लेयर तुरंत नया वीडियो चलाएगा
    LaunchedEffect(videoUrl) {
        val defaultBackup = "https://docs.google.com/uc?export=download&id=1Tvh65EbENOTsKFYBLnWhekNpi15ob8X6"
        val playLink = if (videoUrl.isNotBlank()) videoUrl else defaultBackup

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(MediaItem.fromUri(playLink))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(if (isFullScreen) Color.Black else pageBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isFullScreen) it.fillMaxHeight() else it.aspectRatio(16f / 9f) }
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
                                            Text(text = score.email, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = royalBlue)
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
                AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } }, modifier = Modifier.fillMaxSize())
            }
        }

        if (!isFullScreen) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(text = videoTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = navyBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "कोर्स: TGT / गृह विज्ञान लाइव क्लास", fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(10.dp))

                if (isAdmin) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, golden)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Teacher Control Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = royalBlue)
                                    Text("लाइव पोल व वीडियो लीडरबोर्ड", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                }
                                Button(
                                    onClick = {
                                        val newState = !showPollToStudents
                                        if (newState) {
                                            pollResultsRef.removeValue()
                                            val timeVal = customTimeInput.toIntOrNull() ?: 20
                                            pollRef.updateChildren(mapOf("active" to true, "duration" to timeVal, "correctOption" to teacherSelectedCorrectOption, "showLeaderboard" to false))
                                        } else {
                                            pollRef.child("active").setValue(false)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = golden)
                                ) {
                                    Text(text = if (showPollToStudents) "पोल बंद करें (${remainingTime}s)" else "पोल भेजें 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!showPollToStudents) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("सही उत्तर चुनें:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    for (i in 0..3) {
                                        val isChosen = teacherSelectedCorrectOption == i
                                        val optLetter = when(i) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" }
                                        Surface(
                                            modifier = Modifier.size(36.dp).clickable { teacherSelectedCorrectOption = i },
                                            shape = CircleShape,
                                            color = if (isChosen) royalBlue else Color.White,
                                            border = BorderStroke(1.dp, royalBlue)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = optLetter, color = if (isChosen) Color.White else royalBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("समय (सेकंड):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    OutlinedTextField(
                                        value = customTimeInput,
                                        onValueChange = { customTimeInput = it },
                                        modifier = Modifier.width(75.dp).height(48.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val newLeaderboardState = !isLeaderboardActiveOnScreen
                                    pollRef.child("showLeaderboard").setValue(newLeaderboardState)
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isLeaderboardActiveOnScreen) Color(0xFFDC2626) else royalBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isLeaderboardActiveOnScreen) "❌ वीडियो स्क्रीन से लीडरबोर्ड बंद करें" else "🏆 वीडियो स्क्रीन पर लीडरबोर्ड लाइव दिखाएं",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (showPollToStudents || remainingTime == 0) {
                    StudentLockedPollView(
                        correctAnswerIndex = teacherSelectedCorrectOption,
                        remainingTime = remainingTime,
                        userId = userId,
                        userEmail = userEmail,
                        pollResultsRef = pollResultsRef,
                        studentScoresRef = studentScoresRef,
                        voteCounts = voteCounts,
                        totalVotes = totalPollVotes,
                        isPollActive = showPollToStudents
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { /* पिछली क्लास */ }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(10.dp)) {
                        Text("पिछली क्लास")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onNextVideoClick, modifier = Modifier.weight(1f).height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = navyBlue), shape = RoundedCornerShape(10.dp)) {
                        Text("अगली क्लास ➔", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

data class StudentScoreModel(
    val email: String,
    val correctCount: Int,
    val incorrectCount: Int
)

@Composable
fun StudentLockedPollView(
    correctAnswerIndex: Int,
    remainingTime: Int,
    userId: String,
    userEmail: String,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚡ लाइव पोल (उत्तर दें)", fontWeight = FontWeight.Bold, color = Color(0xFF082A66), fontSize = 15.sp)
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
            Text(text = if (remainingTime > 0) "सही विकल्प चुनें:" else "📊 पोल परिणाम (% वोट):", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

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
                            .height(55.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable(enabled = !isSubmitted && remainingTime > 0) { selectedIndex = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when(i) { 0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D" },
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (remainingTime == 0) {
                                Text(
                                    text = "$percentage%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) Color(0xFF0F5132) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF082A66)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedIndex != null
                ) {
                    Text("उत्तर लॉक करें (Submit)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else if (remainingTime == 0) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedIndex == correctAnswerIndex) "✅ गजब! आपका उत्तर सही था।" else "❌ समय समाप्त! सही उत्तर हाईलाइट है।",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F5132)
                    )
                }
            }
        }
    }
}