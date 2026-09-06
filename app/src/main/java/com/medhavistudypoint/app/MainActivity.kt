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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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

// 🎨 ऐप के कलर पैलेट (App Color Palette)
private val NavyBlue = Color(0xFF082A66)
private val RoyalBlue = Color(0xFF173F8F)
private val Golden = Color(0xFFFFC400)
private val PageBackground = Color(0xFFF6F7FB)
private val DarkText = Color(0xFF172033)
private val GreyText = Color(0xFF747C89)
private val LightGold = Color(0xFFFFFBEB)
private val LightGreen = Color(0xFFEAF7EF)
private val GreyBorder = Color(0xFFE0E0E0)
private val AccentYellow = Color(0xFFE9B200)

// 📄 ऐप के सभी पेजेस के आईडी (Page Navigation Constants)
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
private const val PAGE_HTML_TEST = 10
private const val PAGE_VIDEO_PLAYER = 11
private const val PAGE_DYNAMIC_VIDEO_LIST = 12

// 📊 डेटा मॉडल्स (Data Models)
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
    val status: String = "OPEN",
    val testUrl: String = ""
)

data class VideoClassItem(
    val id: String = "",
    val title: String = "",
    val videoUrl: String = "",
    val status: String = "COMING SOON",
    val order: Int = 0
)

data class GsSubject(
    val icon: String,
    val title: String,
    val subtitle: String,
    val unit: Int,
    val firebaseKey: String = "polity"
)

// 🚀 मुख्य एक्टिविटी (Main Activity)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MedhaviStudyPointTheme {
                val context = androidx.compose.ui.platform.LocalContext.current

                LaunchedEffect(Unit) {
                    AuthSessionManager.init(context)
                }

                var isLoggedIn by remember {
                    mutableStateOf(AuthSessionManager.isLoggedIn())
                }

                if (!isLoggedIn) {
                    LoginScreen(
                        onLoginSuccess = { _, _ ->
                            isLoggedIn = true
                        }
                    )
                } else {
                    MedhaviHomeScreen()
                }
            }
        }
    }
}

// 🏠 होम स्क्रीन और नेविगेशन कंट्रोलर
@Composable
fun MedhaviHomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activePage by remember { mutableIntStateOf(PAGE_ROOT) }
    var selectedSubject by remember { mutableStateOf<GsSubject?>(null) }
    val backStack = remember { mutableStateListOf<Int>() }
    var currentTestTitle by remember { mutableStateOf("Online Test") }
    var currentTestUrl by remember { mutableStateOf("") }

    var currentVideoTitle by remember { mutableStateOf("TGT Home Science Class") }
    var currentVideoUrl by remember { mutableStateOf("") }
    var currentVideoDatabasePath by remember { mutableStateOf("batch_video/home_science") }
    var currentVideoBatchTitle by remember { mutableStateOf("Home Science Classes") }

    var hasFullBatchAccess by remember { mutableStateOf(false) }
    var hasGsAccess by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedBatchName by remember { mutableStateOf("") }
    var selectedBatchFee by remember { mutableStateOf("") }

    var isInsideFullBatchFlow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BatchAccessManager.listenBatchAccess(BatchAccessManager.BATCH_TGT_2026) { allowed ->
            hasFullBatchAccess = allowed
        }
        BatchAccessManager.listenBatchAccess(BatchAccessManager.BATCH_GS_SPECIAL) { allowed ->
            hasGsAccess = allowed
        }
    }

    var freeTestsList by remember { mutableStateOf(emptyList<PortalListItem>()) }
    var homeScienceTestsList by remember { mutableStateOf(emptyList<PortalListItem>()) }
    var fullMockTestsList by remember { mutableStateOf(emptyList<PortalListItem>()) }
    var gsTestsMap by remember { mutableStateOf(mapOf<String, List<PortalListItem>>()) }

    LaunchedEffect(Unit) {
        val database = com.google.firebase.database.FirebaseDatabase.getInstance()

        database.getReference("free_tests").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<PortalListItem>()
                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "LIVE"
                    val testUrl = child.child("testUrl").getValue(String::class.java) ?: ""
                    if (title.isNotBlank()) list.add(PortalListItem(title, subtitle, status, testUrl))
                }
                if (list.isNotEmpty()) freeTestsList = list
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        database.getReference("home_science_tests").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<PortalListItem>()
                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "TEST OVER"
                    val testUrl = child.child("testUrl").getValue(String::class.java) ?: ""
                    if (title.isNotBlank()) list.add(PortalListItem(title, subtitle, status, testUrl))
                }
                if (list.isNotEmpty()) homeScienceTestsList = list
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        database.getReference("full_mock_tests").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<PortalListItem>()
                for (child in snapshot.children) {
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                    val status = child.child("status").getValue(String::class.java) ?: "COMING SOON"
                    val testUrl = child.child("testUrl").getValue(String::class.java) ?: ""
                    if (title.isNotBlank()) list.add(PortalListItem(title, subtitle, status, testUrl))
                }
                if (list.isNotEmpty()) fullMockTestsList = list
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        database.getReference("gs_tests").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val map = mutableMapOf<String, List<PortalListItem>>()
                for (subjChild in snapshot.children) {
                    val list = mutableListOf<PortalListItem>()
                    for (testChild in subjChild.children) {
                        val title = testChild.child("title").getValue(String::class.java) ?: ""
                        val subtitle = testChild.child("subtitle").getValue(String::class.java) ?: ""
                        val status = testChild.child("status").getValue(String::class.java) ?: "COMING SOON"
                        val testUrl = testChild.child("testUrl").getValue(String::class.java) ?: ""
                        if (title.isNotBlank()) list.add(PortalListItem(title, subtitle, status, testUrl))
                    }
                    map[subjChild.key ?: ""] = list
                }
                if (map.isNotEmpty()) gsTestsMap = map
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

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
        if (activePage == PAGE_ROOT) {
            selectedSubject = null
            isInsideFullBatchFlow = false
        }
    }

    val onUniversalTestClick: (PortalListItem) -> Unit = { item ->
        val cleanStatus = item.status.trim().uppercase()
        val isLive = (cleanStatus == "LIVE" || cleanStatus == "LIVE NOW" || cleanStatus == "START")
        if (isLive) {
            val isFreeSectionTest = activePage == PAGE_FREE_TESTS ||
                    freeTestsList.any { it.title == item.title || (it.testUrl.isNotBlank() && it.testUrl == item.testUrl) }

            if (isFreeSectionTest) {
                currentTestTitle = item.title
                currentTestUrl = item.testUrl
                openPage(PAGE_HTML_TEST)
            } else {
                val hsIndex = homeScienceTestsList.indexOfFirst { it.title == item.title || (it.testUrl.isNotBlank() && it.testUrl == item.testUrl) }
                val mockIndex = fullMockTestsList.indexOfFirst { it.title == item.title || (it.testUrl.isNotBlank() && it.testUrl == item.testUrl) }

                val currentGsList = selectedSubject?.let { gsTestsMap[it.title] }
                    ?: gsTestsMap.values.firstOrNull { list -> list.any { it.title == item.title } }
                    ?: emptyList()
                val gsIndex = currentGsList.indexOfFirst { it.title == item.title || (it.testUrl.isNotBlank() && it.testUrl == item.testUrl) }

                val isFreeDemo = when {
                    mockIndex != -1 -> mockIndex in 0..1
                    hsIndex != -1 -> hsIndex in 0..1
                    gsIndex != -1 -> gsIndex in 0..1
                    else -> false
                }

                val isFullBatchItem = isInsideFullBatchFlow ||
                        backStack.contains(PAGE_COMPLETE_BATCH) ||
                        activePage == PAGE_COMPLETE_BATCH ||
                        hsIndex != -1 ||
                        mockIndex != -1

                val isUnlocked = isFreeDemo || hasFullBatchAccess || (!isFullBatchItem && hasGsAccess)

                if (isUnlocked) {
                    currentTestTitle = item.title
                    currentTestUrl = item.testUrl
                    openPage(PAGE_HTML_TEST)
                } else {
                    if (isFullBatchItem) {
                        selectedBatchName = "TGT 2026 Full Batch (Home Science + GS)"
                        selectedBatchFee = "₹299"
                    } else {
                        selectedBatchName = "TGT 2026 GS Batch"
                        selectedBatchFee = "₹99"
                    }
                    showPaymentDialog = true
                }
            }
        }
    }

    BackHandler(enabled = activePage != PAGE_ROOT || selectedTab != 0) {
        if (activePage != PAGE_ROOT) {
            goBack()
        } else {
            selectedTab = 0
            isInsideFullBatchFlow = false
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
                                isInsideFullBatchFlow = false
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
                onOpenPage = { targetPage ->
                    isInsideFullBatchFlow = true
                    if (targetPage == PAGE_VIDEO_CLASSES) {
                        currentVideoDatabasePath = "batch_video/home_science"
                        currentVideoBatchTitle = "Home Science Classes (80 Lectures)"
                        openPage(PAGE_DYNAMIC_VIDEO_LIST)
                    } else {
                        openPage(targetPage)
                    }
                }
            )
            PAGE_GS_BATCH -> BatchPortalContent(
                innerPadding = innerPadding,
                title = "🎯 TGT 2026 GS पोर्टल",
                subtitle = "General Studies की पूरी तैयारी एक ही स्थान पर",
                cards = gsBatchCards(),
                onBack = { goBack() },
                onOpenPage = { targetPage ->
                    if (!isInsideFullBatchFlow && !backStack.contains(PAGE_COMPLETE_BATCH)) {
                        isInsideFullBatchFlow = false
                    }
                    if (targetPage == PAGE_VIDEO_CLASSES) {
                        currentVideoDatabasePath = "batch_video/gs_batch"
                        currentVideoBatchTitle = "General Studies Classes"
                        openPage(PAGE_DYNAMIC_VIDEO_LIST)
                    } else {
                        openPage(targetPage)
                    }
                }
            )
            PAGE_FREE_TESTS -> PortalListContent(
                innerPadding = innerPadding,
                headerBadge = "सभी विद्यार्थियों के लिए",
                title = "Free Online Tests",
                subtitle = "निशुल्क ऑनलाइन मॉक टेस्ट सीरीज",
                entries = if (freeTestsList.isNotEmpty()) freeTestsList else defaultFreeTests(),
                hasAccess = true,
                onBack = { goBack() },
                onItemClick = onUniversalTestClick
            )
            PAGE_HOME_SCIENCE_TESTS -> PortalListContent(
                innerPadding = innerPadding,
                headerBadge = "TGT 2026 गृह विज्ञान बैच",
                title = "Home Science Daily Tests",
                subtitle = "Topic Wise Practice Tests",
                entries = if (homeScienceTestsList.isNotEmpty()) homeScienceTestsList else defaultHomeScienceTest(),
                hasAccess = hasFullBatchAccess,
                onBack = { goBack() },
                onItemClick = onUniversalTestClick
            )
            PAGE_GS_SECTIONAL -> GsSectionalContent(
                innerPadding = innerPadding,
                onBack = { goBack() },
                onSubjectClick = { subject ->
                    selectedSubject = subject
                    openPage(PAGE_SUBJECT_TESTS)
                }
            )
            PAGE_SUBJECT_TESTS -> {
                val currentSubj = selectedSubject ?: gsSubjects().first()
                val liveTests = gsTestsMap[currentSubj.firebaseKey] ?: defaultGsSubjectTest(currentSubj)
                SubjectTestsContent(
                    innerPadding = innerPadding,
                    subject = currentSubj,
                    tests = liveTests,
                    hasAccess = hasFullBatchAccess || hasGsAccess,
                    onBack = { goBack() },
                    onTestClick = onUniversalTestClick
                )
            }
            PAGE_FULL_TESTS -> PortalListContent(
                innerPadding = innerPadding,
                headerBadge = "TGT 2026 Full Mock Tests",
                title = "Full Mock Tests (125 Q)",
                subtitle = "Home Science + General Studies",
                entries = if (fullMockTestsList.isNotEmpty()) fullMockTestsList else defaultFullMockTest(),
                hasAccess = hasFullBatchAccess,
                onBack = { goBack() },
                onItemClick = onUniversalTestClick
            )
            PAGE_VIDEO_CLASSES -> PortalListContent(
                innerPadding = innerPadding,
                headerBadge = "Video Classes",
                title = "Video Classes",
                subtitle = "Batch के वीडियो लेक्चर",
                entries = videoClasses(),
                hasAccess = true,
                onBack = { goBack() },
                onItemClick = { videoItem ->
                    currentVideoDatabasePath = videoItem.testUrl.ifBlank { "batch_video/home_science" }
                    currentVideoBatchTitle = videoItem.title
                    if (currentVideoDatabasePath.contains("home_science")) {
                        isInsideFullBatchFlow = true
                    }
                    openPage(PAGE_DYNAMIC_VIDEO_LIST)
                }
            )
            PAGE_DYNAMIC_VIDEO_LIST -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val isFullBatchVideoList = isInsideFullBatchFlow ||
                        backStack.contains(PAGE_COMPLETE_BATCH) ||
                        activePage == PAGE_COMPLETE_BATCH ||
                        currentVideoDatabasePath.contains("home_science")

                val videoBatchAccess = if (isFullBatchVideoList) hasFullBatchAccess else (hasFullBatchAccess || hasGsAccess)

                FirebaseVideoClassListScreen(
                    innerPadding = innerPadding,
                    databasePath = currentVideoDatabasePath,
                    batchTitle = currentVideoBatchTitle,
                    hasAccess = videoBatchAccess,
                    onBackClick = { goBack() },
                    onClassClick = { classItem, videoIndex ->
                        val cleanStatus = classItem.status.trim().uppercase()
                        val isLive = (cleanStatus == "LIVE" || cleanStatus == "LIVE NOW" || cleanStatus == "START")

                        val isFreeDemoVideo = videoIndex in 0..1
                        val isFullBatchVideo = isFullBatchVideoList
                        val isUnlocked = isFreeDemoVideo || hasFullBatchAccess || (!isFullBatchVideo && hasGsAccess)

                        if (isLive && classItem.videoUrl.isNotBlank()) {
                            if (isUnlocked) {
                                currentVideoTitle = classItem.title
                                currentVideoUrl = classItem.videoUrl
                                openPage(PAGE_VIDEO_PLAYER)
                            } else {
                                if (isFullBatchVideo) {
                                    selectedBatchName = "TGT 2026 Full Batch (Home Science + GS)"
                                    selectedBatchFee = "₹299"
                                } else {
                                    selectedBatchName = "TGT 2026 GS Batch"
                                    selectedBatchFee = "₹99"
                                }
                                showPaymentDialog = true
                            }
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "यह क्लास अभी जल्द लाइव होगी!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            PAGE_VIDEO_PLAYER -> VideoPlayerScreen(
                videoTitle = currentVideoTitle,
                videoUrl = currentVideoUrl, // 👈 यहाँ नया वीडियो लिंक जा रहा है
                onBackClick = { goBack() },
                onNextVideoClick = { }
            )
            PAGE_PDF_NOTES -> PortalListContent(
                innerPadding = innerPadding,
                headerBadge = "Study Material",
                title = "PDF Notes",
                subtitle = "डाउनलोड करने योग्य अध्ययन सामग्री",
                entries = pdfNotes(),
                hasAccess = hasFullBatchAccess || hasGsAccess,
                onBack = { goBack() }
            )
            PAGE_HTML_TEST -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val sharedPref = remember {
                    context.getSharedPreferences("MedhaviUserProfile", android.content.Context.MODE_PRIVATE)
                }
                val firebaseUser = remember {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                }

                val profileStudentName = sharedPref.getString("user_name", null)?.takeIf { it.isNotBlank() }
                    ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: "विद्यार्थी"

                NativeQuizScreen(
                    innerPadding = innerPadding,
                    testId = if (currentTestUrl.isNotBlank()) currentTestUrl else "test41",
                    testTitle = currentTestTitle,
                    studentName = profileStudentName,
                    onBack = { goBack() }
                )
            }
            else -> when (selectedTab) {
                0 -> HomeContent(innerPadding) { openPage(it) }
                1 -> CoursesContent(innerPadding) { openPage(it) }
                2 -> TestsContent(innerPadding) { openPage(it) }
                3 -> ClassesContent(innerPadding) { openPage(PAGE_VIDEO_CLASSES) }
                else -> ProfileContent(
                    innerPadding = innerPadding,
                    hasFullBatch = hasFullBatchAccess,
                    hasGs = hasGsAccess
                )
            }
        }
    }
    if (showPaymentDialog) {
        PaymentRequestDialog(
            batchName = selectedBatchName,
            batchFee = selectedBatchFee,
            onDismiss = { showPaymentDialog = false }
        )
    }
}

// 🎥 फायरबेस से वीडियो लिस्ट फेच करने वाली स्क्रीन
@Composable
private fun FirebaseVideoClassListScreen(
    innerPadding: PaddingValues,
    databasePath: String,
    batchTitle: String,
    hasAccess: Boolean = false,
    onBackClick: () -> Unit,
    onClassClick: (VideoClassItem, Int) -> Unit
) {
    var classList by remember { mutableStateOf<List<VideoClassItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(databasePath) {
        val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference(databasePath)
        dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<VideoClassItem>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: ""

                    // 🎬 Firebase की किसी भी संभावित Key से लिंक फेच करने का कोड
                    val videoUrl = child.child("videoUrl").getValue(String::class.java)
                        ?: child.child("url").getValue(String::class.java)
                        ?: child.child("link").getValue(String::class.java)
                        ?: child.child("videourl").getValue(String::class.java)
                        ?: ""

                    val status = child.child("status").getValue(String::class.java) ?: "COMING SOON"

                    val order = when (val orderVal = child.child("order").value) {
                        is Long -> orderVal.toInt()
                        is Int -> orderVal
                        is String -> orderVal.toIntOrNull() ?: 0
                        else -> 0
                    }

                    list.add(VideoClassItem(id, title, videoUrl, status, order))
                }
                classList = list.sortedBy { it.order }
                isLoading = false
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                isLoading = false
            }
        })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            HomeBrandHeader(badgeText = batchTitle)
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(text = "🎥 वीडियो क्लासेज (${classList.size})", color = NavyBlue, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "डेटाबेस से लाइव फेच की गई क्लासेज", color = GreyText, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 2.dp, color = NavyBlue)
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RoyalBlue)
                }
            }
        } else if (classList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "इस बैच में अभी कोई वीडियो क्लास उपलब्ध नहीं है।", color = GreyText)
                }
            }
        } else {
            items(classList.size) { index ->
                val item = classList[index]
                val isLocked = (index >= 2) && !hasAccess
                VideoClassCardRow(
                    item = item,
                    isLocked = isLocked,
                    onClick = { onClassClick(item, index) }
                )
            }
        }

        item {
            BottomBackButton(onClick = onBackClick)
        }
    }
}

// 📋 वीडियो क्लास की रो डिजाइन
@Composable
private fun VideoClassCardRow(
    item: VideoClassItem,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val isLive = item.status.trim().uppercase() == "LIVE"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isLive) "▶️" else "⏳",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isLive) "क्लिक करके वीडियो देखें" else "जल्द आ रही है",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 👉 स्टेटस के बगल में 🔒 लॉक
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLocked) {
                    Text(
                        text = "🔒",
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLive) Color(0xFFDCFCE7) else Color(0xFFFEF9C3),
                    border = BorderStroke(1.dp, if (isLive) Color(0xFF86EFAC) else Color(0xFFFDE047))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF15803D), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = if (isLive) "Live Now" else "Coming Soon",
                            color = if (isLive) Color(0xFF15803D) else Color(0xFF854D0E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ✅ टेस्ट टैब स्क्रीन
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
            HomeBrandHeader(badgeText = "Online Mock Test Series")
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

@Composable
private fun CoursesContent(
    innerPadding: PaddingValues,
    onOpenPage: (Int) -> Unit
) {
    val courses = listOf(
        HomeService("🎯", "TGT 2026 Batch", "Home Science + GS", true, PAGE_COMPLETE_BATCH),
        HomeService("🎯", "TGT 2026 GS Batch", "Complete General Studies", true, PAGE_GS_BATCH)
    )

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
            ServiceGrid(
                services = courses,
                onServiceClick = { service ->
                    onOpenPage(service.page)
                }
            )
        }
    }
}

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    onOpenPage: (Int) -> Unit
) {
    val services = listOf(
        HomeService("📝", "Free Online Tests", "मॉक टेस्ट सीरीज", page = PAGE_FREE_TESTS),
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

private fun completeBatchCards() = listOf(
    PortalCardData("📚", "Home Science", "वीडियो क्लासेस एवं टेस्ट", PAGE_VIDEO_CLASSES),
    PortalCardData("📝", "20 Full Mock Tests", "ऑनलाइन टेस्ट सीरीज", PAGE_FULL_TESTS),
    PortalCardData("📖", "GS (सामान्य अध्ययन)", "विषयवार टेस्ट एवं तैयारी", PAGE_GS_BATCH),
    PortalCardData("📋", "Home Science Daily Test", "Topic Wise", PAGE_HOME_SCIENCE_TESTS)
)

private fun gsBatchCards() = listOf(
    PortalCardData("▶️", "GS Video Classes", "विषयवार वीडियो लेक्चर", PAGE_VIDEO_CLASSES),
    PortalCardData("📝", "GS Sectional Tests", "8 विषयों की टेस्ट सीरीज", PAGE_GS_SECTIONAL),
    PortalCardData("📚", "GS Full Mock Tests", "संपूर्ण GS अभ्यास", PAGE_FULL_TESTS)
)

private fun gsSubjects() = listOf(
    GsSubject("📰", "समसामयिकी (Current Affairs)", "राष्ट्रीय/राज्य घटनाएँ, चर्चित व्यक्ति व खेल", 1, "current_affairs"),
    GsSubject("📜", "भारतीय इतिहास व स्वतंत्रता आंदोलन", "प्राचीन, मध्यकालीन व आधुनिक इतिहास", 2, "history"),
    GsSubject("🏛️", "संविधान एवं राज्यव्यवस्था", "भारतीय संविधान, धाराएँ व संवैधानिक व्यवस्था", 2, "polity"),
    GsSubject("📈", "भारतीय अर्थव्यवस्था", "आर्थिक व्यवस्था, बजट व योजनाएँ", 2, "economy"),
    GsSubject("🌍", "भारत का भूगोल एवं जनसंख्या", "भौगोलिक स्थिति, नदियाँ व जनगणना", 2, "geography"),
    GsSubject("🔬", "सामान्य विज्ञान व पर्यावरण", "दैनिक विज्ञान, तकनीक एवं पर्यावरण", 3, "science_env"),
    GsSubject("🩺", "मानव स्वास्थ्य, पोषण एवं रोग", "विटामिन, खनिज, रोग, हार्मोन व एंजाइम", 3, "health_nutrition"),
    GsSubject("💻", "कंप्यूटर, ICT एवं शैक्षिक प्रौद्योगिकी", "कंप्यूटर अवधारणा, ICT प्रयोग व डिजिटल शिक्षा", 3, "computer_ict")
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
            HomeBrandHeader(badgeText = "सामान्य ज्ञान (TGT / PGT 2026)")
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

@Composable
private fun SubjectTestsContent(
    innerPadding: PaddingValues,
    subject: GsSubject,
    tests: List<PortalListItem>,
    hasAccess: Boolean = false,
    onBack: () -> Unit,
    onTestClick: (PortalListItem) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            HomeBrandHeader(badgeText = subject.title)
        }

        item {
            Text(
                text = "📝 ऑनलाइन मॉक टेस्ट (${tests.size} Tests)",
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

        items(tests.size) { index ->
            val test = tests[index]
            val isLocked = (index >= 2) && !hasAccess
            AttractiveTestRow(
                item = test,
                isLocked = isLocked,
                onClick = { onTestClick(test) }
            )
        }
        item { BottomBackButton(onBack) }
    }
}

@Composable
private fun AttractiveTestRow(
    item: PortalListItem,
    isLocked: Boolean = false,
    onClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSubmitted = remember(item.testUrl) {
        QuizPersistentManager.init(context)
        item.testUrl.isNotBlank() && QuizPersistentManager.isSubmitted(item.testUrl)
    }

    val cleanStatus = item.status.trim().uppercase()
    val isLive = (cleanStatus == "LIVE" || cleanStatus == "LIVE NOW" || cleanStatus == "START")
    val isOver = cleanStatus == "TEST OVER" || cleanStatus == "OVER"

    val (badgeText, badgeBg, badgeBorder, badgeTextColor, showDot) = when {
        isSubmitted -> StatusBadgeStyle(
            text = "Completed",
            bg = Color(0xFFE0E7FF),
            border = Color(0xFF818CF8),
            textColor = Color(0xFF4338CA),
            dot = true
        )
        isLive -> StatusBadgeStyle(
            text = "Live Now",
            bg = Color(0xFFDCFCE7),
            border = Color(0xFF86EFAC),
            textColor = Color(0xFF15803D),
            dot = true
        )
        isOver -> StatusBadgeStyle(
            text = "Test Over",
            bg = Color(0xFFFEE2E2),
            border = Color(0xFFFCA5A5),
            textColor = Color(0xFFB91C1C),
            dot = true
        )
        cleanStatus == "WATCH" -> StatusBadgeStyle(
            text = "▶ Watch",
            bg = Color(0xFFE8F5E9),
            border = Color(0xFFA5D6A7),
            textColor = Color(0xFF2E7D32),
            dot = false
        )
        else -> StatusBadgeStyle(
            text = "Coming Soon",
            bg = Color(0xFFFEF9C3),
            border = Color(0xFFFDE047),
            textColor = Color(0xFF854D0E),
            dot = false
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "📝",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLocked) {
                    Text(
                        text = "🔒",
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showDot) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(badgeTextColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = badgeText,
                            color = badgeTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class StatusBadgeStyle(
    val text: String,
    val bg: Color,
    val border: Color,
    val textColor: Color,
    val dot: Boolean
)

private fun defaultFreeTests() = listOf(
    PortalListItem("Test - 1 : Free Mock Test", "50 प्रश्न • 40 मिनट", "LIVE", "test41")
)

private fun defaultHomeScienceTest() = listOf(
    PortalListItem("Test - 1 : गृह विज्ञान का परिचय एवं क्षेत्र", "गृह विज्ञान की अवधारणा एवं शाखाएँ", "TEST OVER", "test01")
)

private fun defaultFullMockTest() = listOf(
    PortalListItem("Full Mock Test - 01", "125 प्रश्न • 120 मिनट (TGT Pattern)", "TEST OVER", "test41")
)

private fun defaultGsSubjectTest(subject: GsSubject) = listOf(
    PortalListItem("टेस्ट-1 : ${subject.title}", "विषयवार अभ्यास टेस्ट", "COMING SOON", "")
)

private fun videoClasses() = listOf(
    PortalListItem("Home Science Classes", "रिकॉर्डेड वीडियो लेक्चर", "WATCH", "batch_video/home_science"),
    PortalListItem("General Studies Classes", "विषयवार वीडियो लेक्चर", "WATCH", "batch_video/gs_batch"),
    PortalListItem("Free YouTube Classes", "सभी विद्यार्थियों के लिए", "WATCH", "youtube_free")
)

private fun pdfNotes() = listOf(
    PortalListItem("Home Science Notes", "अध्यायवार PDF सामग्री", "OPEN"),
    PortalListItem("General Studies Notes", "विषयवार PDF सामग्री", "OPEN"),
    PortalListItem("NCERT Books", "डाउनलोड योग्य पुस्तकें", "OPEN")
)

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
            HomeBrandHeader(badgeText = title)
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
private fun BottomBackButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .clickable(onClick = onClick),
        color = NavyBlue,
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

@Composable
private fun PortalListContent(
    innerPadding: PaddingValues,
    headerBadge: String,
    title: String,
    subtitle: String,
    entries: List<PortalListItem>,
    hasAccess: Boolean = false,
    onBack: () -> Unit,
    onItemClick: (PortalListItem) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground).padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            HomeBrandHeader(badgeText = headerBadge)
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(text = "📝 $title", color = NavyBlue, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = GreyText, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 2.dp, color = NavyBlue)
            }
        }

        items(entries.size) { index ->
            val listItem = entries[index]
            val isLocked = (index >= 2) && !hasAccess
            AttractiveTestRow(
                item = listItem,
                isLocked = isLocked,
                onClick = { onItemClick(listItem) }
            )
        }

        item { BottomBackButton(onClick = onBack) }
    }
}

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
private fun ProfileContent(
    innerPadding: PaddingValues,
    hasFullBatch: Boolean,
    hasGs: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("MedhaviUserProfile", android.content.Context.MODE_PRIVATE) }

    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val firebaseUser = auth.currentUser

    val initialName = sharedPref.getString("user_name", null)
        ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "विद्यार्थी"

    val initialEmail = sharedPref.getString("user_email", null)
        ?: firebaseUser?.email?.takeIf { it.isNotBlank() }
        ?: ""

    val initialImageUri = sharedPref.getString("user_image", null)?.let { android.net.Uri.parse(it) }
        ?: firebaseUser?.photoUrl

    var isEditing by remember { mutableStateOf(false) }

    var profileImageUri by remember { mutableStateOf<android.net.Uri?>(initialImageUri) }
    var userName by remember { mutableStateOf(initialName) }
    var userPhone by remember { mutableStateOf(sharedPref.getString("user_phone", "") ?: "") }
    var userEmail by remember { mutableStateOf(initialEmail) }
    var userAddress by remember { mutableStateOf(sharedPref.getString("user_city", "") ?: "") }
    var userTargetExam by remember { mutableStateOf(sharedPref.getString("user_target", "UP TGT 2026") ?: "UP TGT 2026") }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            profileImageUri = uri
            sharedPref.edit().putString("user_image", uri.toString()).apply()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(innerPadding),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                    Text("आपका account और विवरण", fontSize = 14.sp, color = GreyText)
                }
                TextButton(onClick = {
                    if (isEditing) {
                        sharedPref.edit()
                            .putString("user_name", userName)
                            .putString("user_phone", userPhone)
                            .putString("user_email", userEmail)
                            .putString("user_city", userAddress)
                            .putString("user_target", userTargetExam)
                            .apply()
                    }
                    isEditing = !isEditing
                }) {
                    Text(if (isEditing) "Save (सेव करें)" else "Edit (संशोधन)", fontWeight = FontWeight.Bold, color = NavyBlue)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (profileImageUri != null) {
                    coil.compose.AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, GreyBorder, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(NavyBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentYellow)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("सक्रिय बैच (My Batches)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        hasFullBatch -> {
                            Text("✓ TGT 2026 Full Batch (Home Science + GS)", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        hasGs -> {
                            Text("✓ TGT 2026 GS Batch", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        else -> {
                            Text("कोई पेड बैच सक्रिय नहीं है (Free Demo Active)", color = GreyText, fontSize = 14.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileFieldItemRow("पूरा नाम", userName, isEditing) { userName = it }
                    Divider(color = GreyBorder, modifier = Modifier.padding(vertical = 8.dp))

                    ProfileFieldItemRow("मोबाइल नंबर", userPhone, isEditing, "उदा. 9876543210") { userPhone = it }
                    Divider(color = GreyBorder, modifier = Modifier.padding(vertical = 8.dp))

                    ProfileFieldItemRow("ईमेल आईडी", userEmail, isEditing, "उदा. student@gmail.com") { userEmail = it }
                    Divider(color = GreyBorder, modifier = Modifier.padding(vertical = 8.dp))

                    ProfileFieldItemRow("शहर / जिला / राज्य", userAddress, isEditing, "उदा. प्रयागराज, उत्तर प्रदेश") { userAddress = it }
                    Divider(color = GreyBorder, modifier = Modifier.padding(vertical = 8.dp))

                    ProfileFieldItemRow("लक्ष्य परीक्षा", userTargetExam, isEditing, "उदा. UP TGT Home Science 2026") { userTargetExam = it }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = {
                    auth.signOut()
                    sharedPref.edit().clear().apply()

                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).build()
                    val googleClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)

                    googleClient.signOut().addOnCompleteListener {
                        android.widget.Toast.makeText(context, "सफलतापूर्वक लॉगआउट हो गया", android.widget.Toast.LENGTH_SHORT).show()

                        (context as? android.app.Activity)?.let { activity ->
                            val intent = activity.intent
                            activity.finish()
                            activity.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(
                    text = "लॉगआउट (Logout)",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ProfileFieldItemRow(
    label: String,
    value: String,
    isEditing: Boolean,
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = GreyText)
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        } else {
            Text(
                text = if (value.isBlank()) "दर्ज नहीं है" else value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (value.isBlank()) Color.LightGray else DarkText,
                modifier = Modifier.padding(top = 4.dp)
            )
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
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "MEDHAVI STUDY POINT",
                color = Golden,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "ज्ञान, अनुशासन और सफलता का डिजिटल सारथी",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (badgeText.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Golden.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

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