package com.medhavistudypoint.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.net.URLEncoder

@Composable
fun PaymentRequestDialog(
    batchName: String,
    batchFee: String,
    upiId: String = "medhavistudypoint@okaxis",
    whatsappNumber: String = "+919415597889",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // false = QR कोड दिखेगा | true = QR कोड हटकर WhatsApp स्क्रीन दिखेगी
    var isPaymentDoneStep by remember { mutableStateOf(false) }

    val navyBlue = Color(0xFF0F1E4A)
    val whatsappGreen = Color(0xFF25D366)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // बैच का नाम
                Text(
                    text = "🔒 $batchName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = navyBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (!isPaymentDoneStep) {
                    // --- स्टेप 1: QR कोड और पेमेंट विवरण ---
                    Text(
                        text = "QR कोड स्कैन करके फीस ट्रांसफर करें",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // असली QR कोड इमेज
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.my_qr_code),
                            contentDescription = "Payment QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // फीस और UPI कॉपी कार्ड
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "कुल फीस: $batchFee",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "UPI: $upiId",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(upiId))
                                        Toast.makeText(context, "UPI ID कॉपी हो गई!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = navyBlue)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // पेमेंट होने पर आगे बढ़ने का बटन
                    Button(
                        onClick = {
                            isPaymentDoneStep = true // QR छुप जाएगा
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("मैंने पेमेंट कर दिया है ➔ आगे बढ़ें", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                } else {
                    // --- स्टेप 2: QR हट गया, WhatsApp स्क्रीन ---
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✔", fontSize = 32.sp, color = Color(0xFF16A34A))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "अंतिम चरण: स्क्रीनशॉट भेजें",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "नीचे दिए गए बटन पर क्लिक करके अपने पेमेंट का स्क्रीनशॉट WhatsApp पर भेजें। पुष्टि होते ही आपका बैच अनलॉक कर दिया जाएगा।",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // डायरेक्ट WhatsApp खोलने वाला बटन
                    Button(
                        onClick = {
                            val studentName = AuthSessionManager.getStudentName()
                            val studentContact = AuthSessionManager.getStudentContact()

                            val message = """
                                नमस्ते सर! 
                                मैंने *$batchName* के लिए फीस जमा कर दी है।
                                
                                👤 नाम: $studentName
                                📞 संपर्क: $studentContact
                                💰 फीस: $batchFee
                                
                                (स्क्रीनशॉट नीचे संलग्न है, कृपया मेरा बैच अनलॉक कर दें)
                            """.trimIndent()

                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(
                                        "https://api.whatsapp.com/send?phone=$whatsappNumber&text=${
                                            URLEncoder.encode(message, "UTF-8")
                                        }"
                                    )
                                }
                                context.startActivity(intent)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp नहीं खुला: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("💬 WhatsApp पर स्क्रीनशॉट भेजें", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { isPaymentDoneStep = false }) {
                        Text("← वापस QR कोड देखें", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(onClick = onDismiss) {
                    Text("रद्द करें", color = Color.Red, fontSize = 12.sp)
                }
            }
        }
    }
}