package com.example.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShoppingCloudStore(
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) {
    fun sync() {}
}
