package com.example.qrorderkitchen.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class TableModel(
    val tableId: String = "",
    var tableName: String = "",
    var status: String = "available", // "available" (trống) | "waiting" (chờ đồ) | "eating" (đang ăn) | "checkout" (thanh toán)
    var activeOrderCount: Int = 0,
    var serviceRequestType: String? = null,
    var assignedStaffUsername: String? = null,
    var assignedStaffName: String? = null,
    var transferringByUsername: String? = null,
    var transferringByName: String? = null,
    var attachedCustomer: Customer? = null,
    var appliedVoucher: Voucher? = null,
    var usePoints: Boolean = false,
    @get:PropertyName("qr_token")
    @set:PropertyName("qr_token")
    var qrToken: String? = null
)
