package com.example.qrorderkitchen.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Order(
    var id: String = "",
    @get:PropertyName("table_id") @set:PropertyName("table_id") var table_id: String = "",
    @get:PropertyName("total_amount") @set:PropertyName("total_amount") var total_amount: Double = 0.0,
    var status: String = "pending",
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: Timestamp? = null,
    var items: List<OrderItem> = emptyList(),
    var note: String = "",
    @get:PropertyName("transferred_from") @set:PropertyName("transferred_from") var transferred_from: String? = null,
    @get:PropertyName("transferred_to") @set:PropertyName("transferred_to") var transferred_to: String? = null,
    @get:PropertyName("transferred_at") @set:PropertyName("transferred_at") var transferred_at: Timestamp? = null,
    @get:PropertyName("cancel_reason") @set:PropertyName("cancel_reason") var cancel_reason: String? = null,
    @get:PropertyName("cancelled_by") @set:PropertyName("cancelled_by") var cancelled_by: String? = null,
    @get:PropertyName("cancelled_at") @set:PropertyName("cancelled_at") var cancelled_at: Timestamp? = null
)
