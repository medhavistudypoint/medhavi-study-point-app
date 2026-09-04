package com.medhavistudypoint.app

import android.app.Activity
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val navyBlue = Color(0xFF0F1E4A)
    val goldYellow = Color(0xFFFACC15)

    var showPhoneLogin by remember { mutableStateOf(false) }

    var studentNameInput by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    val auth = remember { FirebaseAuth.getInstance() }

    // 🔒 डिवाइस लॉक चेक करने का फिक्स किया गया फंक्शन
    val checkAndLockDevice: (() -> Unit) -> Unit = { onSuccessAction ->
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onSuccessAction()
        } else {
            val thisDeviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val dbRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUser.uid)
                .child("registeredDeviceId")

            dbRef.get().addOnSuccessListener { snapshot ->
                val registeredId = snapshot.getValue(String::class.java)

                if (registeredId == null) {
                    dbRef.setValue(thisDeviceId).addOnSuccessListener {
                        onSuccessAction()
                    }.addOnFailureListener {
                        isLoading = false
                        Toast.makeText(context, "डेटाबेस एरर, पुनः प्रयास करें", Toast.LENGTH_SHORT).show()
                    }
                } else if (registeredId == thisDeviceId) {
                    onSuccessAction()
                } else {
                    auth.signOut()
                    isLoading = false
                    Toast.makeText(
                        context,
                        "⚠️ सुरक्षा चेतावनी: यह अकाउंट किसी अन्य डिवाइस पर रजिस्टर्ड है! यहाँ लॉगिन वर्जित है।",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "नेटवर्क त्रुटि, कृपया इंटरनेट जांचें", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    isLoading = true
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            checkAndLockDevice {
                                isLoading = false
                                val user = auth.currentUser
                                val name = user?.displayName ?: account.displayName ?: "विद्यार्थी"
                                val email = user?.email ?: account.email ?: ""
                                AuthSessionManager.saveStudentDetails(name, email)
                                onLoginSuccess(name, email)
                            }
                        } else {
                            isLoading = false
                            Toast.makeText(context, "Google लॉगिन विफल रहा", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MEDHAVI STUDY POINT",
                    color = navyBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ज्ञान, अनुशासन और सफलता का डिजिटल सारथी",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (!showPhoneLogin) {
                    Text(
                        text = "सीधे अपने गूगल अकाउंट से जुड़ें",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable(enabled = !isLoading) {
                                isLoading = true
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = navyBlue, modifier = Modifier.size(24.dp))
                            } else {
                                Text("🌐", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google (ईमेल चुनें)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = "  अथवा  ",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = { showPhoneLogin = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, navyBlue)
                    ) {
                        Text("📱 मोबाइल नंबर OTP से लॉगिन करें", color = navyBlue, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "मोबाइल नंबर द्वारा लॉगिन",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = navyBlue
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isOtpSent) {
                        OutlinedTextField(
                            value = studentNameInput,
                            onValueChange = { studentNameInput = it },
                            label = { Text("आपका पूरा नाम") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 10) phoneNumber = it },
                            label = { Text("10 अंकों का मोबाइल नंबर") },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (studentNameInput.isBlank()) {
                                    Toast.makeText(context, "कृपया अपना नाम लिखें", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (phoneNumber.length != 10) {
                                    Toast.makeText(context, "कृपया 10 अंकों का सही मोबाइल नंबर दर्ज करें", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (activity == null) return@Button

                                isLoading = true
                                val options = PhoneAuthOptions.newBuilder(auth)
                                    .setPhoneNumber("+91$phoneNumber")
                                    .setTimeout(60L, TimeUnit.SECONDS)
                                    .setActivity(activity)
                                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    checkAndLockDevice {
                                                        isLoading = false
                                                        AuthSessionManager.saveStudentDetails(studentNameInput, phoneNumber)
                                                        onLoginSuccess(studentNameInput, phoneNumber)
                                                    }
                                                } else {
                                                    isLoading = false
                                                }
                                            }
                                        }

                                        override fun onVerificationFailed(e: FirebaseException) {
                                            isLoading = false
                                            Toast.makeText(context, "SMS त्रुटि: ${e.message}", Toast.LENGTH_LONG).show()
                                        }

                                        override fun onCodeSent(
                                            vId: String,
                                            token: PhoneAuthProvider.ForceResendingToken
                                        ) {
                                            isLoading = false
                                            verificationId = vId
                                            isOtpSent = true
                                            Toast.makeText(context, "OTP भेज दिया गया है", Toast.LENGTH_SHORT).show()
                                        }
                                    }).build()

                                PhoneAuthProvider.verifyPhoneNumber(options)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = goldYellow, modifier = Modifier.size(22.dp))
                            } else {
                                Text("OTP प्राप्त करें", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "+91 $phoneNumber पर भेजा गया OTP दर्ज करें:",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = otp,
                            onValueChange = { if (it.length <= 6) otp = it },
                            label = { Text("6-Digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (otp.length != 6 || verificationId == null) {
                                    Toast.makeText(context, "कृपया सही OTP दर्ज करें", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLoading = true
                                val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        checkAndLockDevice {
                                            isLoading = false
                                            AuthSessionManager.saveStudentDetails(studentNameInput, phoneNumber)
                                            onLoginSuccess(studentNameInput, phoneNumber)
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "अमान्य OTP", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text("सत्यापित करें ✔", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(onClick = { showPhoneLogin = false; isOtpSent = false }) {
                        Text("← वापस Google से लॉगिन करें", color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}