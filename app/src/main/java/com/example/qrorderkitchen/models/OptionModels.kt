package com.example.qrorderkitchen.models

import com.google.firebase.firestore.PropertyName

data class OptionItem(
    var name: String = "",
    @get:PropertyName("name_en") @set:PropertyName("name_en") var name_en: String = "",
    var price: Double = 0.0,
    @get:PropertyName("is_default") @set:PropertyName("is_default") var is_default: Boolean = false
)

data class OptionGroup(
    var name: String = "",
    @get:PropertyName("name_en") @set:PropertyName("name_en") var name_en: String = "",
    var type: String = "single", // "single" or "multiple"
    var required: Boolean = false,
    var items: List<OptionItem> = emptyList()
)

data class SelectedOption(
    @get:PropertyName("group_name") @set:PropertyName("group_name") var group_name: String = "",
    @get:PropertyName("group_name_en") @set:PropertyName("group_name_en") var group_name_en: String = "",
    @get:PropertyName("item_name") @set:PropertyName("item_name") var item_name: String = "",
    @get:PropertyName("item_name_en") @set:PropertyName("item_name_en") var item_name_en: String = "",
    var price: Double = 0.0
)
