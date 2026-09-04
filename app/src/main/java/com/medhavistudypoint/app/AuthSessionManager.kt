package com.medhavistudypoint.app

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

object AuthSessionManager {
    private const val PREF_AUTH = "medhavi_auth_prefs"
    private const val KEY_STUDENT_NAME = "student_name"
    private const val KEY_STUDENT_CONTACT = "student_contact"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        }
    }

    fun isLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun saveStudentDetails(name: String, contact: String) {
        prefs?.edit()?.apply {
            putString(KEY_STUDENT_NAME, name)
            putString(KEY_STUDENT_CONTACT, contact)
            apply()
        }
    }

    fun getStudentName(): String {
        val user = FirebaseAuth.getInstance().currentUser
        val savedName = prefs?.getString(KEY_STUDENT_NAME, null)
        return savedName ?: user?.displayName ?: "परीक्षार्थी"
    }

    fun getStudentContact(): String {
        val user = FirebaseAuth.getInstance().currentUser
        val savedContact = prefs?.getString(KEY_STUDENT_CONTACT, null)
        return savedContact ?: user?.email ?: user?.phoneNumber ?: ""
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        prefs?.edit()?.clear()?.apply()
    }
}