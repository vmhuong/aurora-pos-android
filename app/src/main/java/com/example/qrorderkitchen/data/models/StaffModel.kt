package com.example.qrorderkitchen.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

enum class StaffRole(val roleKey: String, val displayName: String) {
    MANAGER("manager", "Quản Lý"),
    KITCHEN("kitchen", "Nhà Bếp / Pha Chế"),
    WAITER("waiter", "Phục Vụ"),
    CUSTOMER_KIOSK("customer", "Khách Hàng (Kiosk)");

    companion object {
        fun fromKey(key: String): StaffRole {
            val normalized = key.trim().lowercase()
            if (normalized == "admin" || normalized == "manager") return MANAGER
            if (normalized == "kitchen") return KITCHEN
            return WAITER
        }
    }
}

data class StaffModel(
    var id: String = "",
    val name: String = "",
    val username: String = "",
    val pin: String = "",
    @get:PropertyName("pin_hash") @set:PropertyName("pin_hash") var pin_hash: String = "",
    val role: String = "waiter", // "manager" | "cashier" | "waiter" | "kitchen"
    val platform: String = "app", // "app" | "web"
    val status: String = "active", // "active" | "locked"
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: Timestamp? = null
) {
    val staffRole: StaffRole
        get() = StaffRole.fromKey(role)
}
