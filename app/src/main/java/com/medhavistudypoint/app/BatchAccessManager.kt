package com.medhavistudypoint.app

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object BatchAccessManager {
    // दोनों बैच की ID
    const val BATCH_TGT_2026 = "tgt_2026"
    const val BATCH_GS_SPECIAL = "gs_special"

    // Firebase की चाबी (Key) में '.', '@', '#', '$' आदि मान्य नहीं होते, इसलिए उन्हें '_' में बदलते हैं
    fun sanitizeKey(key: String): String {
        return key.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("@", "_at_")
    }

    // छात्र का रियल-टाइम एक्सेस चेक करना
    fun listenBatchAccess(batchId: String, onResult: (Boolean) -> Unit) {
        val userContact = AuthSessionManager.getStudentContact()
        if (userContact.isBlank()) {
            onResult(false)
            return
        }

        val key = sanitizeKey(userContact)
        val dbRef = FirebaseDatabase.getInstance().getReference("enrolled_students")
            .child(key)
            .child(batchId)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hasAccess = snapshot.getValue(Boolean::class.java) ?: false
                onResult(hasAccess)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(false)
            }
        })
    }
}