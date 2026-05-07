package com.example.fit.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FirebaseSyncManager {

    // Lazy so unit tests can construct this class without Firebase being initialized.
    private val db by lazy { FirebaseDatabase.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val uid: String?
        get() = auth.currentUser?.uid

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    fun exportProgramme(
        programmeName: String,
        identifier: String,
        jsonData: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isSignedIn) {
            onComplete(false)
            return
        }
        val key = "${programmeName}_${identifier}"
        db.reference
            .child("users")
            .child(uid!!)
            .child("exports")
            .child(key)
            .setValue(jsonData)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
