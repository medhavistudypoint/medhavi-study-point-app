package com.medhavistudypoint.app

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoTitle: String,
    onBackClick: () -> Unit,
    onNextVideoClick: () -> Unit
) {
    val context = LocalContext.current
    val navyBlue = Color(0xFF082A66)
    val royalBlue = Color(0xFF173F8F)
    val golden = Color(0xFFFFC400)
    val pageBg = Color(0xFFF6F7FB)

    var showPollToStudents by remember { mutableStateOf(false) }

    // ⏱️ कस्टम टाइमर स्टेट (मर्जी से समय डालने और याद रखने के लिए)
    var customTimeInput by remember { mutableStateOf("20") }
    var remainingTime by remember { mutableStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }

    // ⏱️ ऑटो-टाइमर का लॉजिक (समय खत्म होने पर पोल अपने आप बंद होगा)
    LaunchedEffect(showPollToStudents, remainingTime) {
        if (showPollToStudents && remainingTime > 0) {
            kotlinx.coroutines.delay(1000L)
            remainingTime -= 1
        } else if (remainingTime == 0 && showPollToStudents) {
            showPollToStudents = false
        }
    }

    // Google Drive डायरेक्ट स्ट्रीमिंग लिंक
    val videoUrl = "https://docs.google.com/uc?export=download&id=1Tvh65EbENOTsKFYBLnWhekNpi15ob8X6"

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullScreen) Color.Black else pageBg)
    ) {
        // 🔥 टैबलेट और मोबाइल दोनों पर परफेक्ट दिखने वाला रिस्पॉन्सिव वीडियो प्लेयर बॉक्स
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (isFullScreen) it.fillMaxHeight()
                    else it.aspectRatio(16f / 9f)
                }
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 👑 प्रीमियम वॉटरमार्क
            if (!isFullScreen) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, golden, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(golden, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MEDHAVI • PREMIUM",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // बाकि का लेआउट (टाइप, पोल, बटन)
        if (!isFullScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = videoTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = navyBlue
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "कोर्स: TGT / गृह विज्ञान लाइव क्लास",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 🎛️ टीचर कंट्रोल पैनल (कस्टम टाइमर इनपुट के साथ)
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
                                Text("लाइव पोल विथ ऑटो-टाइमर", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                            Button(
                                onClick = {
                                    if (!showPollToStudents) {
                                        val timeVal = customTimeInput.toIntOrNull() ?: 20
                                        remainingTime = timeVal
                                        showPollToStudents = true
                                    } else {
                                        showPollToStudents = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = golden)
                            ) {
                                Text(
                                    text = if (showPollToStudents) "पोल बंद करें (${remainingTime}s)" else "पोल भेजें 🚀",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 🕒 अपनी मर्जी से समय डालने का इनपुट बॉक्स
                        if (!showPollToStudents) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("पोल समय (सेकंड):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                                OutlinedTextField(
                                    value = customTimeInput,
                                    onValueChange = { customTimeInput = it },
                                    modifier = Modifier
                                        .width(75.dp)
                                        .height(48.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = royalBlue
                                    ),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = royalBlue,
                                        unfocusedBorderColor = Color.Gray,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Text("सेकंड (बदलाव न करने पर यही रहेगा)", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                if (showPollToStudents) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StudentLockedPollView(
                        correctAnswerIndex = 0,
                        remainingTime = remainingTime,
                        onSubmitted = {}
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { /* पिछली क्लास */ },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("पिछली क्लास")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onNextVideoClick,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("अगली क्लास ➔", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun StudentLockedPollView(
    correctAnswerIndex: Int,
    remainingTime: Int,
    onSubmitted: (Boolean) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ लाइव पोल (उत्तर दें)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF082A66),
                    fontSize = 15.sp
                )
                // ⏱️ छात्रों की स्क्रीन पर घटता हुआ टाइमर
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "⏱️ ${remainingTime}s remaining",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color(0xFFB91C1C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "कृपया समय समाप्त होने से पहले सही विकल्प चुनें:",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0..3) {
                    val isSelected = selectedIndex == i
                    val isCorrect = i == correctAnswerIndex

                    val bg = when {
                        isSubmitted && isCorrect -> Color(0xFFD1E7DD)
                        isSubmitted && isSelected && !isCorrect -> Color(0xFFF8D7DA)
                        isSelected -> Color(0xFF173F8F)
                        else -> Color(0xFFF1F5F9)
                    }

                    val textColor = if (isSelected && !isSubmitted) Color.White else Color.Black

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable(enabled = !isSubmitted && remainingTime > 0) {
                                selectedIndex = i
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(i) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            },
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isSubmitted) {
                Button(
                    onClick = {
                        if (selectedIndex != null) {
                            isSubmitted = true
                            onSubmitted(selectedIndex == correctAnswerIndex)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF082A66)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedIndex != null && remainingTime > 0
                ) {
                    Text("उत्तर लॉक करें (Submit)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedIndex == correctAnswerIndex) "✅ बहुत बढ़िया! आपका उत्तर बिल्कुल सही है।" else "❌ गलत उत्तर! सही विकल्प हाइलाइट कर दिया गया है।",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == correctAnswerIndex) Color(0xFF0F5132) else Color(0xFF842029)
                    )
                }
            }
        }
    }
}