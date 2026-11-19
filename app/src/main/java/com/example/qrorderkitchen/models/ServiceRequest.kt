package com.example.qrorderkitchen.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class ServiceRequest(
    var id: String = "",
    @get:PropertyName("table_id") @set:PropertyName("table_id") var table_id: String = "",
    var type: String = "assistance",
    var status: String = "pending",
    var note: String = "",
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: Timestamp? = null
)
