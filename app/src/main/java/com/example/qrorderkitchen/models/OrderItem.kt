package com.example.qrorderkitchen.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class OrderItem(
    var id: String = "",
    @get:PropertyName("order_id") @set:PropertyName("order_id") var order_id: String = "",
    @get:PropertyName("dish_id") @set:PropertyName("dish_id") var dish_id: String = "",
    @get:PropertyName("dish_name") @set:PropertyName("dish_name") var dish_name: String = "",
    @get:PropertyName("dish_name_en") @set:PropertyName("dish_name_en") var dish_name_en: String = "",
    var quantity: Int = 1,
    var price: Double = 0.0,
    @get:PropertyName("base_price") @set:PropertyName("base_price") var base_price: Double = 0.0,
    @get:PropertyName("options_price") @set:PropertyName("options_price") var options_price: Double = 0.0,
    @get:PropertyName("options_summary") @set:PropertyName("options_summary") var options_summary: String = "",
    @get:PropertyName("options_summary_en") @set:PropertyName("options_summary_en") var options_summary_en: String = "",
    @get:PropertyName("is_combo") @set:PropertyName("is_combo") var is_combo: Boolean = false,
    @get:PropertyName("combo_summary") @set:PropertyName("combo_summary") var combo_summary: String = "",
    @get:PropertyName("selected_options") @set:PropertyName("selected_options") var selected_options: List<SelectedOption> = emptyList(),
    var note: String = "",
    @get:PropertyName("is_completed") @set:PropertyName("is_completed") var is_completed: Boolean = false,
    @get:PropertyName("created_at") @set:PropertyName("created_at") var created_at: Timestamp? = null
)
