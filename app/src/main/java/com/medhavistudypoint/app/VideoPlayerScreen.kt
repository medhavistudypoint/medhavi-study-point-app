package com.medhavistudypoint.app

import android.content.Intent
import android.net.Uri
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

    // आपकी वीडियो का लिंक
    val videoUrl = "https://youtu.be/a52lhiNMfVw?si=ZQ0-DxM6yfDRXYs0"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(16.dp)
    ) {
        // टॉप बार
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBackClick) {
                Text("← वापस", color = navyBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(
                text = "Medhavi Study Point",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 🔥 सुरक्षित और हमेशा काम करने वाला वीडियो प्लेयर बैनर (Direct YouTube App Opener)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(navyBlue)
                .clickable {
                    // जैसे ही छात्र इस बॉक्स पर क्लिक करेगा, वीडियो सीधे YouTube ऐप में खुल जाएगी
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                    context.startActivity(intent)
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Red,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "▶", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "क्लास देखने के लिए यहाँ क्लिक करें",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "(Medhavi Study Point Secure Player)",
                    color = golden,
                    fontSize = 11.sp
                )
            }

            // 👑 प्रीमियम वॉटरमार्क
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

        Spacer(modifier = Modifier.height(16.dp))

        // वीडियो का टाइटल
        Text(
            text = videoTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = navyBlue
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "कोर्स: TGT / गृह विज्ञान लाइव क्लास",
            fontSize = 14.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 👨‍🏫 टीचर कंट्रोल पैनल (पोल भेजने के लिए)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFFFBEB),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, golden)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Teacher Control Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = royalBlue)
                    Text("लाइव पोल ट्रिगर करें", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
                Button(
                    onClick = { showPollToStudents = !showPollToStudents },
                    colors = ButtonDefaults.buttonColors(containerColor = golden)
                ) {
                    Text(
                        text = if (showPollToStudents) "पोल बंद करें" else "पोल भेजें 🚀",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // छात्र के लिए लाइव पोल व्यू
        if (showPollToStudents) {
            Spacer(modifier = Modifier.height(12.dp))
            StudentLockedPollView(
                correctAnswerIndex = 0,
                onSubmitted = {}
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // नीचे नेविगेशन बटन
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { /* पिछली क्लास */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("पिछली क्लास")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onNextVideoClick,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("अगली क्लास ➔", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 🔒 छात्र का पोल व्यू: सिर्फ A, B, C, D विकल्प
@Composable
fun StudentLockedPollView(
    correctAnswerIndex: Int,
    onSubmitted: (Boolean) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ लाइव पोल (उत्तर दें)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF082A66),
                    fontSize = 16.sp
                )
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color(0xFF15803D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "कृपया बोर्ड या टीचर की आवाज़ सुनकर सही विकल्प चुनें:",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable(enabled = !isSubmitted) {
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isSubmitted) {
                Button(
                    onClick = {
                        if (selectedIndex != null) {
                            isSubmitted = true
                            onSubmitted(selectedIndex == correctAnswerIndex)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF082A66)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = selectedIndex != null
                ) {
                    Text("उत्तर लॉक करें (Submit)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedIndex == correctAnswerIndex) "✅ बहुत बढ़िया! आपका उत्तर बिल्कुल सही है।" else "❌ गलत उत्तर! सही विकल्प हाइलाइट कर दिया गया है।",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == correctAnswerIndex) Color(0xFF0F5132) else Color(0xFF842029)
                    )
                }
            }
        }
    }
}