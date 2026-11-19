package com.example.qrorderkitchen.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class ComboItem(
    @get:PropertyName("dish_id") @set:PropertyName("dish_id") var dish_id: String = "",
    var name: String = "",
    @get:PropertyName("name_en") @set:PropertyName("name_en") var name_en: String = "",
    var price: Double = 0.0,
    var extra: Double = 0.0
)

@IgnoreExtraProperties
data class ComboGroup(
    @get:PropertyName("group_title") @set:PropertyName("group_title") var group_title: String = "",
    @get:PropertyName("group_title_en") @set:PropertyName("group_title_en") var group_title_en: String = "",
    var min: Int = 1,
    var max: Int = 1,
    var items: List<ComboItem> = emptyList()
)

@IgnoreExtraProperties
data class Dish(
    var id: String = "",
    val name: String = "",
    @get:PropertyName("name_en") @set:PropertyName("name_en") var name_en: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val desc: String = "",
    @get:PropertyName("desc_en") @set:PropertyName("desc_en") var desc_en: String = "",
    val img: String = "",
    var options: List<OptionGroup> = emptyList(),
    @get:PropertyName("is_combo") @set:PropertyName("is_combo") var is_combo: Boolean = false,
    @get:PropertyName("combo_groups") @set:PropertyName("combo_groups") var combo_groups: List<ComboGroup> = emptyList(),
    @get:PropertyName("is_available") @set:PropertyName("is_available") var is_available: Boolean = true,
    @get:PropertyName("locked_until") @set:PropertyName("locked_until") var locked_until: Timestamp? = null
) {
    @get:Exclude
    val isCurrentlyLocked: Boolean
        get() {
            val locked = locked_until
            if (locked != null) {
                val nowSecs = com.example.qrorderkitchen.utils.NetworkTimeProvider.currentTimeMillis() / 1000
                return locked.seconds > nowSecs
            }
            return !is_available
        }

    @get:Exclude
    val isCurrentlyAvailable: Boolean
        get() = !isCurrentlyLocked
}
