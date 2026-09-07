package com.medhavistudypoint.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileCompleted: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser

    val sharedPref = remember {
        context.getSharedPreferences("MedhaviUserProfile", android.content.Context.MODE_PRIVATE)
    }

    var name by remember {
        mutableStateOf(currentUser?.displayName ?: "")
    }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val isPhoneValid = phone.trim().length == 10 && phone.all { it.isDigit() }
    val isNameValid = name.trim().length >= 3

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF082A66))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📝 विद्यार्थी प्रोफ़ाइल",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF082A66)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "टेस्ट और लाइव पोल में शामिल होने के लिए अपना नाम और मोबाइल नंबर दर्ज करें।",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 👤 पूरा नाम (अनिवार्य)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("पूरा नाम (Full Name) *") },
                    placeholder = { Text("जैसे: अमित कुमार") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 📱 मोबाइल नंबर (10 अंक अनिवार्य)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 10 && it.all { ch -> ch.isDigit() }) phone = it },
                    label = { Text("मोबाइल नंबर (10 अंक) *") },
                    placeholder = { Text("9876543210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = phone.isNotBlank() && !isPhoneValid
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 📍 शहर / ज़िला (वैकल्पिक)
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("शहर / ज़िला (वैकल्पिक)") },
                    placeholder = { Text("जैसे: प्रयागराज, लखनऊ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isNameValid && isPhoneValid) {
                            isSaving = true
                            val uid = currentUser?.uid ?: "unknown"
                            val email = currentUser?.email ?: ""
                            val trimmedName = name.trim()
                            val trimmedPhone = phone.trim()
                            val trimmedCity = city.trim()

                            // 1. Firebase Auth में Display Name अपडेट
                            val profileUpdates = userProfileChangeRequest {
                                displayName = trimmedName
                            }
                            currentUser?.updateProfile(profileUpdates)

                            // 2. SharedPreferences में लोकल सेव
                            sharedPref.edit()
                                .putString("user_name", trimmedName)
                                .putString("user_phone", trimmedPhone)
                                .putString("user_city", trimmedCity)
                                .putString("user_email", email)
                                .putBoolean("is_profile_completed", true)
                                .apply()

                            // 3. Firebase Database के "users" नोड में सेव (पॉइंट 5)
                            val userMap = mapOf(
                                "name" to trimmedName,
                                "phone" to trimmedPhone,
                                "email" to email,
                                "city" to trimmedCity,
                                "uid" to uid,
                                "createdAt" to System.currentTimeMillis()
                            )

                            FirebaseDatabase.getInstance().getReference("users")
                                .child(uid)
                                .updateChildren(userMap)
                                .addOnCompleteListener {
                                    isSaving = false
                                    onProfileCompleted()
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isNameValid && isPhoneValid && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF082A66))
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = "आगे बढ़ें (Continue) ➔",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}