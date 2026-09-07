package com.medhavistudypoint.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// 📄 PDF डेटा मॉडल
data class PdfItem(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val pdfUrl: String = "",
    val order: Int = 0,
    val isLocked: Boolean = false
)

@Composable
fun PdfNotesScreen(
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var pdfList by remember { mutableStateOf<List<PdfItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val navyBlue = Color(0xFF082A66)
    val golden = Color(0xFFFFC400)

    // Firebase से PDF नोट्स लोड करना
    LaunchedEffect(Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("pdf_notes")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<PdfItem>()
                for (child in snapshot.children) {
                    val id = child.key ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                    val pdfUrl = child.child("pdfUrl").getValue(String::class.java) ?: ""
                    val isLocked = child.child("isLocked").getValue(Boolean::class.java) ?: false
                    val order = when (val o = child.child("order").value) {
                        is Long -> o.toInt()
                        is Int -> o
                        else -> 0
                    }
                    if (title.isNotBlank()) {
                        list.add(PdfItem(id, title, subtitle, pdfUrl, order, isLocked))
                    }
                }
                pdfList = list.sortedBy { it.order }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
            .padding(innerPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 🔔 हेडर सेक्शन
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = navyBlue,
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 26.dp)
            ) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(text = "🔔", fontSize = 22.sp)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "MEDHAVI STUDY POINT",
                        color = golden,
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
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, golden.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Study Material & Notes",
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

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "📚 क्लास नोट्स एवं ई-बुक्स (${pdfList.size})",
                    color = navyBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "पढ़ने और डाउनलोड करने के लिए उपलब्ध PDF सामग्री",
                    color = Color(0xFF747C89),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 2.dp, color = navyBlue)
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
                    CircularProgressIndicator(color = Color(0xFF173F8F))
                }
            }
        } else if (pdfList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "अभी कोई PDF नोट्स उपलब्ध नहीं है।",
                        color = Color(0xFF747C89),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(pdfList) { item ->
                PdfCardRow(item = item) {
                    if (item.pdfUrl.isNotBlank()) {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.pdfUrl))
                            context.startActivity(browserIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "PDF खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "यह PDF जल्द ही अपलोड होगी!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 🔙 बैक बटन
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .clickable(onClick = onBackClick),
                color = navyBlue,
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
    }
}

// 📋 प्रत्येक PDF आइटम का कार्ड
@Composable
private fun PdfCardRow(
    item: PdfItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
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
                    text = "📄",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    if (item.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFF93C5FD))
            ) {
                Text(
                    text = "डाउनलोड 📥",
                    color = Color(0xFF1D4ED8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}