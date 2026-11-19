package com.example.qrorderkitchen.models

data class TransferDishItem(
    val id: String,                 // Unique ID for Compose list key
    val originalDocId: String,      // Firestore order_items document ID
    val orderId: String,            // Firestore orders document ID
    val dishId: String,
    val dishName: String,
    val price: Double,
    var quantity: Int,              // Current quantity in the respective column
    val maxAvailable: Int,          // Total initial quantity
    val note: String = "",
    val isCompleted: Boolean = false,
    val isFromSource: Boolean = true, // true = originally belonged to source table, false = target table
    val isCombo: Boolean = false,
    val comboSummary: String = "",
    val optionsSummary: String = "",
    val optionsSummaryEn: String = "",
    val dishNameEn: String = "",
    val basePrice: Double = 0.0,
    val optionsPrice: Double = 0.0,
    val selectedOptions: List<SelectedOption> = emptyList()
)

