package com.example.qrorderkitchen.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.example.qrorderkitchen.utils.CurrencyUtils

@IgnoreExtraProperties
data class Voucher(
    var id: String = "",
    var code: String = "",
    @get:PropertyName("campaign_id") @set:PropertyName("campaign_id") var campaign_id: String? = null,
    @get:PropertyName("campaign_name") @set:PropertyName("campaign_name") var campaign_name: String? = null,
    var type: String = "fixed", // "fixed", "percent", "dish"
    @get:PropertyName("discount_type") @set:PropertyName("discount_type") var discount_type: String? = null,
    var value: Double = 0.0,
    @get:PropertyName("discount_value") @set:PropertyName("discount_value") var discount_value: Double = 0.0,
    @get:PropertyName("discount_amount") @set:PropertyName("discount_amount") var discount_amount: Double = 0.0,
    @get:PropertyName("max_discount") @set:PropertyName("max_discount") var max_discount: Double = 0.0,
    @get:PropertyName("min_bill") @set:PropertyName("min_bill") var min_bill: Double = 0.0,
    @get:PropertyName("specific_dish_id") @set:PropertyName("specific_dish_id") var specific_dish_id: String? = null,
    @get:PropertyName("specific_dish_name") @set:PropertyName("specific_dish_name") var specific_dish_name: String? = null,
    var status: String = "available", // "available", "applied", "used", "disabled", "expired"
    @get:PropertyName("applied_table") @set:PropertyName("applied_table") var applied_table: String? = null,
    @get:PropertyName("applied_at") @set:PropertyName("applied_at") var applied_at: Timestamp? = null,
    @get:PropertyName("valid_from") @set:PropertyName("valid_from") var valid_from: Timestamp? = null,
    @get:PropertyName("valid_to") @set:PropertyName("valid_to") var valid_to: Timestamp? = null,
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: Timestamp? = null
) {
    /**
     * Xác thực tính hợp lệ của Voucher đồng bộ 100% với Web VoucherEngine
     */
    fun validate(orderTotal: Double, orderItems: List<OrderItem>, targetTable: String = ""): VoucherValidationResult {
        val cleanCode = if (code.isNotBlank()) code else id
        val st = status.trim().lowercase()

        // 1. Kiểm tra trạng thái đã sử dụng
        if (st == "used") {
            return VoucherValidationResult(false, "Mã voucher \"$cleanCode\" đã được sử dụng trước đó!")
        }

        // 2. Kiểm tra trạng thái bị khóa / hết hạn
        if (st == "disabled" || st == "expired") {
            return VoucherValidationResult(false, "Mã voucher đã bị vô hiệu hóa hoặc hết hạn")
        }

        // 3. Kiểm tra bàn áp dụng trùng lặp
        val cleanApplied = (applied_table ?: "").trim()
            .replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "")
            .uppercase()
        val cleanTarget = targetTable.trim()
            .replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "")
            .uppercase()
        if (st == "applied" && cleanApplied.isNotBlank() && cleanTarget.isNotBlank() && cleanApplied != cleanTarget) {
            return VoucherValidationResult(
                false,
                "Mã voucher \"$cleanCode\" đang được áp dụng tại Bàn ${applied_table ?: cleanApplied}, không thể dùng cho bàn khác!"
            )
        }

        // 4. Kiểm tra thời hạn hiệu lực (valid_from & valid_to)
        val now = System.currentTimeMillis()
        if (valid_from != null && now < valid_from!!.toDate().time) {
            return VoucherValidationResult(false, "Voucher chưa đến thời gian áp dụng")
        }
        if (valid_to != null && now > valid_to!!.toDate().time) {
            return VoucherValidationResult(false, "Voucher đã hết hạn sử dụng")
        }

        // 5. Kiểm tra giá trị đơn hàng tối thiểu
        if (orderTotal < min_bill) {
            return VoucherValidationResult(
                false,
                "Hóa đơn phải từ ${CurrencyUtils.formatVnd(min_bill)} trở lên để áp dụng voucher này"
            )
        }

        // 6. Tính toán giá trị giảm giá theo loại voucher (fixed, percent, dish)
        val effectiveType = type.trim().lowercase()
        var discount = 0.0

        when (effectiveType) {
            "fixed" -> {
                val fixedVal = if (value > 0.0) value else discount_value
                discount = minOf(orderTotal, fixedVal)
            }
            "percent" -> {
                val percentVal = if (value > 0.0) value else discount_value
                val raw = (orderTotal * percentVal) / 100.0
                discount = if (max_discount > 0.0 && raw > max_discount) max_discount else raw
                discount = minOf(orderTotal, discount)
            }
            "dish" -> {
                val foundItem = orderItems.find { item ->
                    (!specific_dish_id.isNullOrBlank() && item.dish_id == specific_dish_id) ||
                    (!specific_dish_name.isNullOrBlank() && item.dish_name.equals(specific_dish_name, ignoreCase = true))
                }
                if (foundItem == null) {
                    val dishDisplay = specific_dish_name?.ifBlank { "món được chỉ định" } ?: "món được chỉ định"
                    return VoucherValidationResult(
                        false,
                        "Hóa đơn hiện chưa có món \"$dishDisplay\" để áp dụng ưu đãi tặng món này"
                    )
                }
                discount = minOf(orderTotal, foundItem.price)
            }
            else -> {
                return VoucherValidationResult(false, "Loại voucher không xác định")
            }
        }

        return VoucherValidationResult(
            isValid = true,
            message = "Áp dụng thành công: Giảm ${CurrencyUtils.formatVnd(discount)}",
            discountAmount = discount
        )
    }
}

data class VoucherValidationResult(
    val isValid: Boolean,
    val message: String,
    val discountAmount: Double = 0.0
)
