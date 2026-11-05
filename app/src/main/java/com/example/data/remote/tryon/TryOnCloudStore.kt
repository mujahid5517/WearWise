package com.example.data.remote.tryon

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.domain.model.TryOnPreview

class TryOnCloudStore(private val auth: FirebaseAuth?, private val firestore: FirebaseFirestore?) {
    suspend fun sync() {}
    suspend fun delete(id: Long) {}
    suspend fun setSavedLook(id: Long, saved: Boolean) {}
    suspend fun upsertMetadata(preview: TryOnPreview) {}
}
