package com.medhavistudypoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medhavistudypoint.app.ui.theme.MedhaviStudyPointTheme

private val NavyBlue = Color(0xFF082A66)
private val RoyalBlue = Color(0xFF173F8F)
private val Golden = Color(0xFFFFC400)
private val PageBackground = Color(0xFFF6F7FB)
private val DarkText = Color(0xFF172033)
private val GreyText = Color(0xFF747C89)
private val LightGold = Color(0xFFFFFBEB)

data class HomeService(
    val icon: String,
    val title: String,
    val subtitle: String,
    val highlighted: Boolean = false
)

data class BottomItem(
    val icon: String,
    val title: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MedhaviStudyPointTheme {
                MedhaviHomeScreen()
            }
        }
    }
}

@Composable
fun MedhaviHomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val bottomItems = listOf(
        BottomItem("⌂", "Home"),
        BottomItem("▶", "Courses"),
        BottomItem("✓", "Tests"),
        BottomItem("●", "Profile")
    )

    Scaffold(
        containerColor = PageBackground,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                bottomItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Text(
                                text = item.icon,
                                fontSize = 21.sp,
                                color = if (selectedTab == index) {
                                    RoyalBlue
                                } else {
                                    GreyText
                                }
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        HomeContent(innerPadding)
    }
}

@Composable
private fun HomeContent(innerPadding: PaddingValues) {
    val services = listOf(
        HomeService(
            icon = "📝",
            title = "Free Online Tests",
            subtitle = "मॉक टेस्ट सीरीज"
        ),
        HomeService(
            icon = "▶️",
            title = "Free Video Classes",
            subtitle = "YouTube लेक्चर्स"
        ),
        HomeService(
            icon = "📚",
            title = "Free PDF Notes",
            subtitle = "डाउनलोड नोट्स"
        ),
        HomeService(
            icon = "🎯",
            title = "TGT 2026 Batch",
            subtitle = "Home Science + GS",
            highlighted = true
        ),
        HomeService(
            icon = "🎯",
            title = "TGT 2026 GS Batch",
            subtitle = "Complete General Studies",
            highlighted = true
        ),
        HomeService(
            icon = "📢",
            title = "Telegram Group",
            subtitle = "जुड़ें हमारे साथ"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HeaderSection()
        }

        item {
            Text(
                text = "हमारी सेवाएँ",
                modifier = Modifier.padding(
                    start = 22.dp,
                    top = 22.dp,
                    end = 22.dp,
                    bottom = 14.dp
                ),
                color = NavyBlue,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ServiceGrid(
                services = services,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            InformationCard(
                title = "About Institute",
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 26.dp,
                    end = 20.dp
                )
            ) {
                Text(
                    text = "Medhavi Study Point का उद्देश्य प्रत्येक विद्यार्थी को " +
                            "गुणवत्तापूर्ण शिक्षा प्रदान करना है। हम आपकी सफलता के लिए " +
                            "प्रतिबद्ध हैं। यहाँ आपको वे सभी संसाधन मिलेंगे जो आपकी " +
                            "परीक्षा की तैयारी को एक नया आयाम दे सकें।",
                    color = DarkText,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )
            }
        }

        item {
            InformationCard(
                title = "लेटेस्ट अपडेट",
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp
                )
            ) {
                UpdateLine("गृह विज्ञान की NCERT PDF डाउनलोड करें।")
                Spacer(modifier = Modifier.height(12.dp))
                UpdateLine("प्रतिदिन Home Science और GS के टेस्ट उपलब्ध हैं।")
                Spacer(modifier = Modifier.height(12.dp))
                UpdateLine("TGT 2026 Batch के नए वीडियो लेक्चर जोड़े गए हैं।")
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RoyalBlue,
                shape = RoundedCornerShape(
                    bottomStart = 36.dp,
                    bottomEnd = 36.dp
                )
            )
            .padding(
                start = 20.dp,
                top = 25.dp,
                end = 20.dp,
                bottom = 35.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Medhavi Study Point Logo",
                modifier = Modifier
                    .size(135.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        color = Golden,
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "MEDHAVI STUDY POINT",
                color = Golden,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ज्ञान, अनुशासन और सफलता का डिजिटल सारथी",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServiceGrid(
    services: List<HomeService>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = if (maxWidth >= 600.dp) 3 else 2
        val rows = services.chunked(columnCount)

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            rows.forEach { rowServices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowServices.forEach { service ->
                        Box(modifier = Modifier.weight(1f)) {
                            ServiceCard(service)
                        }
                    }

                    repeat(columnCount - rowServices.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(service: HomeService) {
    val cardBackground = if (service.highlighted) {
        LightGold
    } else {
        Color.White
    }

    val cardBorder = if (service.highlighted) {
        Golden
    } else {
        Color(0xFFE1E4EA)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .border(
                width = if (service.highlighted) 2.dp else 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = cardBackground,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = service.icon,
                fontSize = 39.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = service.title,
                color = DarkText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = service.subtitle,
                color = if (service.highlighted) {
                    RoyalBlue
                } else {
                    GreyText
                },
                fontSize = 14.sp,
                fontWeight = if (service.highlighted) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InformationCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Text(
                text = title,
                color = NavyBlue,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.24f),
                thickness = 3.dp,
                color = Golden
            )

            Spacer(modifier = Modifier.height(20.dp))

            content()
        }
    }
}

@Composable
private fun UpdateLine(text: String) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = RoyalBlue,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = DarkText,
            fontSize = 16.sp,
            lineHeight = 23.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 1280)
@Composable
private fun MedhaviHomePreview() {
    MedhaviStudyPointTheme {
        MedhaviHomeScreen()
    }
}