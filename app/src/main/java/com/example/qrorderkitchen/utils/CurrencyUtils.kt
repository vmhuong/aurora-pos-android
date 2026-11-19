package com.example.qrorderkitchen.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Singleton caching currency formatters to prevent heavy NumberFormat allocations
 * and garbage collection pauses during rapid UI scrolling in Jetpack Compose LazyLists.
 */
object CurrencyUtils {
    private val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
        groupingSeparator = '.'
        monetaryDecimalSeparator = ','
    }

    private val threadLocalFormat = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat {
            return DecimalFormat("#,###", symbols).apply {
                maximumFractionDigits = 0
            }
        }
    }

    /**
     * Format number as Vietnamese Dong with 'đ' suffix (e.g. 50.000đ)
     */
    fun format(amount: Number): String {
        val formatted = threadLocalFormat.get()?.format(amount) ?: amount.toString()
        return "${formatted}đ"
    }

    /**
     * Format number with 'VNĐ' suffix (e.g. 50.000 VNĐ)
     */
    fun formatVnd(amount: Number): String {
        val formatted = threadLocalFormat.get()?.format(amount) ?: amount.toString()
        return "$formatted VNĐ"
    }

    /**
     * Format plain number without currency symbol (e.g. 50.000)
     */
    fun formatPlain(amount: Number): String {
        return threadLocalFormat.get()?.format(amount) ?: amount.toString()
    }
}
