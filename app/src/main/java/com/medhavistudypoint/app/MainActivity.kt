package com.medhavistudypoint.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
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
private val LightGreen = Color(0xFFEAF7EF)

private const val PAGE_ROOT = 0
private const val PAGE_COMPLETE_BATCH = 1
private const val PAGE_GS_BATCH = 2
private const val PAGE_FREE_TESTS = 3
private const val PAGE_HOME_SCIENCE_TESTS = 4
private const val PAGE_GS_SECTIONAL = 5
private const val PAGE_FULL_TESTS = 6
private const val PAGE_VIDEO_CLASSES = 7
private const val PAGE_PDF_NOTES = 8
private const val PAGE_SUBJECT_TESTS = 9

data class HomeService(
    val icon: String,
    val title: String,
    val subtitle: String,
    val highlighted: Boolean = false,
    val page: Int = PAGE_ROOT
)

data class BottomItem(
    val icon: String,
    val title: String
)

data class PortalCardData(
    val icon: String,
    val title: String,
    val subtitle: String,
    val page: Int
)

data class PortalListItem(
    val title: String,
    val subtitle: String,
    val status: String = "OPEN"
)

data class GsSubject(
    val icon: String,
    val title: String,
    val subtitle: String,
    val unit: Int
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
    var activePage by remember { mutableIntStateOf(PAGE_ROOT) }
    var selectedSubject by remember { mutableStateOf<GsSubject?>(null) }
    val backStack = remember { mutableStateListOf<Int>() }

    val bottomItems = listOf(
        BottomItem("⌂", "Home"),
        BottomItem("▶", "Courses"),
        BottomItem("✓", "Tests"),
        BottomItem("🎥", "Classes"),
        BottomItem("●", "Profile")
    )

    fun openPage(page: Int) {
        backStack.add(activePage)
        activePage = page
    }

    fun goBack() {
        if (backStack.isNotEmpty()) {
            activePage = backStack.removeAt(backStack.lastIndex)
        } else if (activePage != PAGE_ROOT) {
            activePage = PAGE_ROOT
        }
        if (activePage == PAGE_ROOT && selectedTab == 0) {
            selectedSubject = null
        }
    }

    // हर Back press पर एक ही step पीछे जाएँ। Home पर पहुँचने के बाद ही अगला Back app को बंद करेगा।
    BackHandler(enabled = activePage != PAGE_ROOT || selectedTab != 0) {
        if (activePage != PAGE_ROOT) {
            goBack()
        } else {
            selectedTab = 0
        }
    }

    Scaffold(
        containerColor = PageBackground,
        bottomBar = {
            if (activePage == PAGE_ROOT) {
                NavigationBar(containerColor = Color.White) {
                    bottomItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                activePage = PAGE_ROOT
                                backStack.clear()
                            },
                            icon = {
                                Text(
                                    text = item.icon,
                                    fontSize = 21.sp,
                                    color = if (selectedTab == index) RoyalBlue else GreyText
                                )
                            },
                            label = { Text(text = item.title, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (activePage) {
            PAGE_COMPLETE_BATCH -> BatchPortalContent(
                innerPadding = innerPadding,
                title = "🎯 TGT 2026 होम साइंस पोर्टल",
                subtitle = "अपनी सुविधानुसार नीचे दिए गए विकल्प को चुनें",
                cards = completeBatchCards(),
                onBack = { goBack() },
                onOpenPage = { openPage(it) }
            )
            PAGE_GS_BATCH -> BatchPortalContent(
                innerPadding = innerPadding,
                title = "🎯 TGT 2026 GS पोर्टल",
                subtitle = "General Studies की पूरी तैयारी एक ही स्थान पर",
                cards = gsBatchCards(),
                onBack = { goBack() },
                onOpenPage = { openPage(it) }
            )
            PAGE_FREE_TESTS -> PortalListContent(innerPadding, "Free Online Tests", "सभी विद्यार्थियों के लिए", freeTests(), { goBack() })
            PAGE_HOME_SCIENCE_TESTS -> PortalListContent(innerPadding, "Home Science Daily Tests", "Topic Wise Test Series", homeScienceTests(), { goBack() })
            PAGE_GS_SECTIONAL -> GsSectionalContent(
                innerPadding = innerPadding,
                onBack = { goBack() },
                onSubjectClick = { subject ->
                    selectedSubject = subject
                    openPage(PAGE_SUBJECT_TESTS)
                }
            )
            PAGE_SUBJECT_TESTS -> SubjectTestsContent(
                innerPadding = innerPadding,
                subject = selectedSubject ?: gsSubjects().first(),
                onBack = { goBack() }
            )
            PAGE_FULL_TESTS -> PortalListContent(innerPadding, "Full Mock Tests", "Home Science + General Studies", fullTests(), { goBack() })
            PAGE_VIDEO_CLASSES -> PortalListContent(innerPadding, "Video Classes", "Batch के वीडियो लेक्चर", videoClasses(), { goBack() })
            PAGE_PDF_NOTES -> PortalListContent(innerPadding, "PDF Notes", "डाउनलोड करने योग्य अध्ययन सामग्री", pdfNotes(), { goBack() })
            else -> when (selectedTab) {
                0 -> HomeContent(innerPadding) { openPage(it) }
                1 -> CoursesContent(innerPadding) { openPage(it) }
                2 -> TestsContent(innerPadding) { openPage(it) }
                3 -> ClassesContent(innerPadding) { openPage(PAGE_VIDEO_CLASSES) }
                else -> ProfileContent(innerPadding)
            }
        }
    }
}

/* -------------------- TEST TAB -------------------- */

@Composable
private fun TestsContent(
    innerPadding: PaddingValues,
    onOpenPage: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = RoyalBlue,
                shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎯 TEST PORTAL",
                        color = Golden,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Free और Batch Tests एक ही स्थान पर",
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 18.dp, end = 16.dp),
                color = LightGreen,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "✓ Free Tests और आपके खरीदे हुए Batch Tests",
                    modifier = Modifier.padding(15.dp),
                    color = Color(0xFF16824B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Text(
                text = "Test Categories",
                modifier = Modifier.padding(start = 18.dp, top = 24.dp, end = 18.dp, bottom = 14.dp),
                color = NavyBlue,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                TestPortalCard(
                    icon = "📝",
                    title = "Free Online Tests",
                    subtitle = "सभी विद्यार्थियों के लिए",
                    status = "FREE",
                    statusColor = Color(0xFF16824B),
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenPage(PAGE_FREE_TESTS) }
                )
                Spacer(modifier = Modifier.size(12.dp))
                TestPortalCard(
                    icon = "📋",
                    title = "Home Science Daily Tests",
                    subtitle = "Topic Wise Tests",
                    status = "BATCH",
                    statusColor = RoyalBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenPage(PAGE_HOME_SCIENCE_TESTS) }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                TestPortalCard(
                    icon = "📖",
                    title = "GS Sectional Tests",
                    subtitle = "विषयवार टेस्ट सीरीज",
                    status = "BATCH",
                    statusColor = RoyalBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenPage(PAGE_GS_SECTIONAL) }
                )
                Spacer(modifier = Modifier.size(12.dp))
                TestPortalCard(
                    icon = "📚",
                    title = "Full Mock Tests",
                    subtitle = "Home Science + GS",
                    status = "BATCH",
                    statusColor = RoyalBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenPage(PAGE_FULL_TESTS) }
                )
            }
        }
    }
}

@Composable
private fun TestPortalCard(
    icon: String,
    title: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(190.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE0E4EA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 34.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = DarkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = subtitle,
                color = GreyText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* -------------------- COURSES -------------------- */

@Composable
private fun CoursesContent(
    innerPadding: PaddingValues,
    onOpenPage: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            Text("Courses", color = NavyBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("अपनी तैयारी के लिए सही Batch चुनें", color = GreyText, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            InformationCard(
                title = "TGT 2026 Complete Batch",
                onClick = { onOpenPage(PAGE_COMPLETE_BATCH) }
            ) {
                Text(
                    "Home Science + General Studies\nVideo Classes • Tests",
                    color = DarkText,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("VIEW COURSE  →", color = RoyalBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            InformationCard(
                title = "TGT 2026 Only GS Batch",
                onClick = { onOpenPage(PAGE_GS_BATCH) }
            ) {
                Text(
                    "Complete General Studies\nSectional Tests • Full Tests • Video Classes",
                    color = DarkText,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("VIEW COURSE  →", color = RoyalBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* -------------------- HOME -------------------- */

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    onOpenPage: (Int) -> Unit
) {
    val services = listOf(
        HomeService("📝", "Free Online Tests", "मॉक टेस्ट सीरीज", page = PAGE_FREE_TESTS),
        HomeService("▶️", "Free Video Classes", "वीडियो लेक्चर्स", page = PAGE_VIDEO_CLASSES),
        HomeService("📚", "Free PDF Notes", "डाउनलोड नोट्स", page = PAGE_PDF_NOTES),
        HomeService("🎯", "TGT 2026 Batch", "Home Science + GS", true, PAGE_COMPLETE_BATCH),
        HomeService("🎯", "TGT 2026 GS Batch", "Complete General Studies", true, PAGE_GS_BATCH),
        HomeService("📢", "Telegram Group", "जुड़ें हमारे साथ")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { HeaderSection() }

        item {
            ServiceGrid(
                services = services,
                modifier = Modifier.padding(horizontal = 20.dp),
                onServiceClick = { service ->
                    if (service.page != PAGE_ROOT) onOpenPage(service.page)
                }
            )
        }

        item {
            InformationCard(
                title = "About Institute",
                modifier = Modifier.padding(start = 20.dp, top = 26.dp, end = 20.dp)
            ) {
                Text(
                    text = "Medhavi Study Point का उद्देश्य प्रत्येक विद्यार्थी को गुणवत्तापूर्ण शिक्षा प्रदान करना है। " +
                            "हम आपकी सफलता के लिए प्रतिबद्ध हैं।" +
                            "यहाँ आपको वो सब मिलेगा जो आपकी परीक्षा की तैयारी को एक नया आयाम दे सके",
                    color = DarkText,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )
            }
        }

        item {
            InformationCard(
                title = "लेटेस्ट अपडेट",
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)
            ) {
                UpdateLine("प्रतिदिन Home Science और GS के टेस्ट उपलब्ध हैं।")
                Spacer(modifier = Modifier.height(12.dp))
                UpdateLine("TGT 2026 Batch के नए वीडियो लेक्चर जोड़े गए हैं।")
            }
        }
    }
}

/* -------------------- BATCH DATA -------------------- */

private fun completeBatchCards() = listOf(
    PortalCardData("📚", "Home Science", "वीडियो क्लासेस एवं टेस्ट", PAGE_VIDEO_CLASSES),
    PortalCardData("📝", "20 Full Mock Tests", "ऑनलाइन टेस्ट सीरीज", PAGE_FULL_TESTS),

    // IMPORTANT: यह अलग GS portal नहीं, वही GS content खोलता है
    PortalCardData("📖", "GS (सामान्य अध्ययन)", "विषयवार टेस्ट एवं तैयारी", PAGE_GS_BATCH),

    PortalCardData("📋", "Home Science Daily Test", "Topic Wise", PAGE_HOME_SCIENCE_TESTS)
)

private fun gsBatchCards() = listOf(
    PortalCardData("▶️", "GS Video Classes", "विषयवार वीडियो लेक्चर", PAGE_VIDEO_CLASSES),
    PortalCardData("📝", "GS Sectional Tests", "8 विषयों की टेस्ट सीरीज", PAGE_GS_SECTIONAL),
    PortalCardData("📚", "GS Full Mock Tests", "संपूर्ण GS अभ्यास", PAGE_FULL_TESTS)
)

/* -------------------- GS SUBJECT GRID -------------------- */

private fun gsSubjects() = listOf(
    GsSubject(
        "📰",
        "समसामयिकी (Current Affairs)",
        "राष्ट्रीय/राज्य घटनाएँ, चर्चित व्यक्ति व खेल",
        1
    ),
    GsSubject(
        "📜",
        "भारतीय इतिहास व स्वतंत्रता आंदोलन",
        "प्राचीन, मध्यकालीन व आधुनिक इतिहास",
        2
    ),
    GsSubject(
        "🏛️",
        "संविधान एवं राज्यव्यवस्था",
        "भारतीय संविधान, धाराएँ व संवैधानिक व्यवस्था",
        2
    ),
    GsSubject(
        "📈",
        "भारतीय अर्थव्यवस्था",
        "आर्थिक व्यवस्था, बजट व योजनाएँ",
        2
    ),
    GsSubject(
        "🌍",
        "भारत का भूगोल एवं जनसंख्या",
        "भौगोलिक स्थिति, नदियाँ व जनगणना",
        2
    ),
    GsSubject(
        "🔬",
        "सामान्य विज्ञान व पर्यावरण",
        "दैनिक विज्ञान, तकनीक एवं पर्यावरण",
        3
    ),
    GsSubject(
        "🩺",
        "मानव स्वास्थ्य, पोषण एवं रोग",
        "विटामिन, खनिज, रोग, हार्मोन व एंजाइम",
        3
    ),
    GsSubject(
        "💻",
        "कंप्यूटर, ICT एवं शैक्षिक प्रौद्योगिकी",
        "कंप्यूटर अवधारणा, ICT प्रयोग व डिजिटल शिक्षा",
        3
    )
)

@Composable
private fun GsSectionalContent(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onSubjectClick: (GsSubject) -> Unit
) {
    val subjects = gsSubjects()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            PortalHeader(
                title = "📖 सामान्य ज्ञान (TGT / PGT)",
                subtitle = "उत्तर प्रदेश शिक्षा सेवा चयन आयोग पाठ्यक्रम"
            )
        }

        item {
            UnitHeading("📌 इकाई 1 : समसामयिकी (Current Affairs)")
            SubjectWideCard(subjects[0]) { onSubjectClick(subjects[0]) }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            UnitHeading("📌 इकाई 2 : इतिहास, राज्यव्यवस्था, अर्थव्यवस्था व भूगोल")
            SubjectGrid(
                subjects = subjects.filter { it.unit == 2 },
                onClick = onSubjectClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            UnitHeading("📌 इकाई 3 : सामान्य विज्ञान, ICT एवं शैक्षिक प्रौद्योगिकी")
            SubjectGrid(
                subjects = subjects.filter { it.unit == 3 },
                onClick = onSubjectClick
            )
            BottomBackButton(onBack)
        }
    }
}

@Composable
private fun UnitHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        color = DarkText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp),
        thickness = 2.dp,
        color = DarkText
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SubjectWideCard(subject: GsSubject, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(205.dp)
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE0E4EA))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(subject.icon, fontSize = 46.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                subject.title,
                color = DarkText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                subject.subtitle,
                color = GreyText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SubjectGrid(
    subjects: List<GsSubject>,
    onClick: (GsSubject) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        subjects.chunked(2).forEach { rowSubjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                rowSubjects.forEach { subject ->
                    SubjectCard(
                        subject = subject,
                        modifier = Modifier.weight(1f),
                        onClick = { onClick(subject) }
                    )
                }
                if (rowSubjects.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubjectCard(
    subject: GsSubject,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(205.dp).clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE0E4EA))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(subject.icon, fontSize = 43.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                subject.title,
                color = DarkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                subject.subtitle,
                color = GreyText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* -------------------- SUBJECT TEST LIST -------------------- */

@Composable
private fun SubjectTestsContent(
    innerPadding: PaddingValues,
    subject: GsSubject,
    onBack: () -> Unit
) {
    val tests = subjectTests(subject)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            HomeBrandHeader(
                badgeText = subject.title
            )
        }

        item {
            Text(
                text = "📝 ऑनलाइन मॉक टेस्ट (20 Tests)",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = DarkText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                thickness = 2.dp,
                color = DarkText
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        items(tests) { test ->
            AttractiveTestRow(test)
        }
        item { BottomBackButton(onBack) }
    }
}

private fun subjectTests(subject: GsSubject): List<PortalListItem> {
    val specialTitles = when (subject.title) {
        "संविधान एवं राज्यव्यवस्था" -> listOf(
            "भारतीय संविधान का ऐतिहासिक विकास",
            "संविधान सभा एवं संविधान का निर्माण",
            "संविधान की विशेषताएँ, स्रोत एवं प्रस्तावना",
            "संघ एवं उसका राज्य-क्षेत्र और नागरिकता",
            "मौलिक अधिकार—समानता एवं स्वतंत्रता का अधिकार",
            "मौलिक अधिकार—शोषण, धर्म, संस्कृति व शिक्षा",
            "राज्य के नीति-निर्देशक तत्व एवं मौलिक कर्तव्य",
            "संविधान संशोधन, मूल संरचना सिद्धांत एवं अनुसूचियाँ"
        )
        else -> emptyList()
    }

    return (1..20).map { number ->
        val title = specialTitles.getOrNull(number - 1)
            ?: "टेस्ट-$number: ${subject.title}"
        PortalListItem(
            title = title,
            subtitle = "विषयवार अभ्यास टेस्ट",
            status = if (number <= 3) "LIVE" else "SOON"
        )
    }
}

@Composable
private fun AttractiveTestRow(item: PortalListItem) {
    val statusInfo = when (item.status) {
        "LIVE" -> Triple("🟢 Live Now", Color(0xFFE1F5EA), Color(0xFF277A4C))
        "SOON" -> Triple("⌛ Coming Soon", LightGold, Color(0xFF956E1C))
        "START" -> Triple("▶ Start Test", Color(0xFFE8F0FF), RoyalBlue)
        "WATCH" -> Triple("▶ Watch Now", Color(0xFFE8F0FF), RoyalBlue)
        "OPEN" -> Triple("↗ Open", Color(0xFFF0F2F6), DarkText)
        else -> Triple(item.status, Color(0xFFF0F2F6), DarkText)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE0E4EA))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📝", fontSize = 22.sp)
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (item.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.subtitle, color = GreyText, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Surface(
                color = statusInfo.second,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, statusInfo.third.copy(alpha = 0.25f))
            ) {
                Text(
                    text = statusInfo.first,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    color = statusInfo.third,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* -------------------- LIST DATA -------------------- */

private fun freeTests() = listOf(
    PortalListItem("Free Mock Test - 1", "40 प्रश्न • 30 मिनट", "START"),
    PortalListItem("Free Mock Test - 2", "40 प्रश्न • 30 मिनट", "START"),
    PortalListItem("TGT Practice Test", "Home Science + GS", "START")
)

private fun homeScienceTests() = listOf(
    PortalListItem("Daily Mock Test - 1", "भोजन एवं पोषण", "START"),
    PortalListItem("Daily Mock Test - 2", "मानव विकास", "START"),
    PortalListItem("Daily Mock Test - 3", "वस्त्र एवं परिधान", "START"),
    PortalListItem("Daily Mock Test - 4", "गृह प्रबंधन", "START"),
    PortalListItem("Daily Mock Test - 5", "प्रसार शिक्षा", "START")
)

private fun fullTests() = listOf(
    PortalListItem("Full Mock Test - 1", "90 Home Science + 30 GS", "START"),
    PortalListItem("Full Mock Test - 2", "90 Home Science + 30 GS", "START"),
    PortalListItem("Full Mock Test - 3", "90 Home Science + 30 GS", "START"),
    PortalListItem("Full Mock Test - 4", "90 Home Science + 30 GS", "START")
)

private fun videoClasses() = listOf(
    PortalListItem("Home Science Classes", "रिकॉर्डेड वीडियो लेक्चर", "WATCH"),
    PortalListItem("General Studies Classes", "विषयवार वीडियो लेक्चर", "WATCH"),
    PortalListItem("Free YouTube Classes", "सभी विद्यार्थियों के लिए", "WATCH")
)

private fun pdfNotes() = listOf(
    PortalListItem("Home Science Notes", "अध्यायवार PDF सामग्री", "OPEN"),
    PortalListItem("General Studies Notes", "विषयवार PDF सामग्री", "OPEN"),
    PortalListItem("NCERT Books", "डाउनलोड योग्य पुस्तकें", "OPEN")
)

/* -------------------- BATCH PORTAL -------------------- */

@Composable
private fun BatchPortalContent(
    innerPadding: PaddingValues,
    title: String,
    subtitle: String,
    cards: List<PortalCardData>,
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            PortalHeader(title, subtitle)
        }

        cards.chunked(2).forEach { rowCards ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowCards.forEach { card ->
                        BatchPortalCard(
                            card = card,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenPage(card.page) }
                        )
                    }
                    if (rowCards.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
        item { BottomBackButton(onBack) }
    }
}

@Composable
private fun PortalHeader(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RoyalBlue,
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Golden,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BottomBackButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .clickable(onClick = onClick),
        color = RoyalBlue,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp
    ) {
        Text(
            text = "← Back",
            modifier = Modifier.padding(vertical = 14.dp),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BatchPortalCard(
    card: PortalCardData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(180.dp).clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE0E4EA))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = card.icon, fontSize = 38.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = card.title,
                color = DarkText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = card.subtitle,
                color = GreyText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* -------------------- NORMAL LIST PAGES -------------------- */

@Composable
private fun PortalListContent(
    innerPadding: PaddingValues,
    title: String,
    subtitle: String,
    entries: List<PortalListItem>,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavyBlue
            ) {
                Text(
                    text = "MEDHAVI STUDY POINT",
                    modifier = Modifier.padding(20.dp),
                    color = Golden,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(title, color = NavyBlue, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = GreyText, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        items(entries) { listItem ->
            AttractiveTestRow(listItem)
        }
        item { BottomBackButton(onBack) }
    }
}

/* -------------------- OTHER TABS -------------------- */

@Composable
private fun ClassesContent(
    innerPadding: PaddingValues,
    onOpenClasses: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            Text("Classes", color = NavyBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Free और Batch वीडियो क्लासेस", color = GreyText, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(20.dp))
            InformationCard(title = "मेरी वीडियो क्लासेस", onClick = onOpenClasses) {
                Text("Home Science, GS और Free YouTube lectures", color = DarkText, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text("VIEW CLASSES  →", color = RoyalBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileContent(innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(20.dp)
    ) {
        item {
            Text("Profile", color = NavyBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("आपका account और खरीदे हुए batches", color = GreyText, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(20.dp))
            InformationCard(title = "Login आवश्यक है") {
                Text(
                    "अगले चरण में Mobile OTP login, purchased batch access और एक-device सुरक्षा जोड़ी जाएगी।",
                    color = DarkText,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

/* -------------------- HEADERS -------------------- */

@Composable
private fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RoyalBlue,
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
            .padding(start = 20.dp, top = 25.dp, end = 20.dp, bottom = 35.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Medhavi Study Point Logo",
                modifier = Modifier
                    .size(135.dp)
                    .clip(CircleShape)
                    .border(3.dp, Golden, CircleShape),
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
private fun HomeBrandHeader(badgeText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = NavyBlue,
                shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
            )
            .padding(horizontal = 20.dp, vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "MEDHAVI STUDY POINT",
                color = Golden,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "ज्ञान, अनुशासन और सफलता का डिजिटल सारथी",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Golden.copy(alpha = 0.5f))
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* -------------------- HOME GRID -------------------- */

@Composable
private fun ServiceGrid(
    services: List<HomeService>,
    modifier: Modifier = Modifier,
    onServiceClick: (HomeService) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = if (maxWidth >= 600.dp) 3 else 2
        val rows = services.chunked(columnCount)

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { rowServices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowServices.forEach { service ->
                        Box(modifier = Modifier.weight(1f)) {
                            ServiceCard(
                                service = service,
                                onClick = { onServiceClick(service) }
                            )
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
private fun ServiceCard(
    service: HomeService,
    onClick: () -> Unit
) {
    val cardBackground = if (service.highlighted) LightGold else Color.White
    val cardBorder = if (service.highlighted) Golden else Color(0xFFE1E4EA)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clickable(onClick = onClick)
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
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(service.icon, fontSize = 39.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                service.title,
                color = DarkText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                service.subtitle,
                color = if (service.highlighted) RoyalBlue else GreyText,
                fontSize = 14.sp,
                fontWeight = if (service.highlighted) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* -------------------- COMMON CARD -------------------- */

@Composable
private fun InformationCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)

    Surface(
        modifier = cardModifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(title, color = NavyBlue, fontSize = 23.sp, fontWeight = FontWeight.Bold)
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
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = RoyalBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = DarkText,
            fontSize = 16.sp,
            lineHeight = 23.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 850)
@Composable
private fun MedhaviHomePreview() {
    MedhaviStudyPointTheme {
        MedhaviHomeScreen()
    }
}
