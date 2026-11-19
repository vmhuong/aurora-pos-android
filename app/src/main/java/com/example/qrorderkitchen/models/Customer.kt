package com.example.qrorderkitchen.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Customer(
    @get:Exclude var id: String = "",
    var phone: String = "",
    var name: String = "",
    var email: String = "",
    var points: Double = 0.0,
    @get:PropertyName("total_spend") @set:PropertyName("total_spend") var total_spend: Double = 0.0,
    var tier: String = "member",
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: Timestamp? = null
) {
    @Exclude
    fun getTierDisplayName(): String = when (tier.lowercase()) {
        "vip" -> "VIP (Tích 13%)"
        "platinum", "bach_kim" -> "Bạch Kim (Tích 10%)"
        "gold", "vang" -> "Vàng (Tích 8%)"
        "silver", "bac" -> "Bạc (Tích 5%)"
        else -> "Member (Tích 3%)"
    }

    @Exclude
    fun getEarnRate(): Double = when (tier.lowercase()) {
        "vip" -> 0.13
        "platinum", "bach_kim" -> 0.10
        "gold", "vang" -> 0.08
        "silver", "bac" -> 0.05
        else -> 0.03
    }
}
