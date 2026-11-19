package com.example.qrorderkitchen.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.models.SelectedOption
import com.example.qrorderkitchen.ui.theme.WebBorder
import com.example.qrorderkitchen.ui.theme.WebCardBg
import com.example.qrorderkitchen.ui.theme.WebGoldDark
import com.example.qrorderkitchen.ui.theme.WebGoldPrimary
import com.example.qrorderkitchen.ui.theme.WebSurfacePill
import com.example.qrorderkitchen.ui.theme.WebTextMuted
import com.example.qrorderkitchen.ui.theme.WebTextPrimary
import com.example.qrorderkitchen.ui.theme.WebTextSecondary
import com.example.qrorderkitchen.utils.CurrencyUtils

@Composable
fun DishDetailDialog(
    dish: Dish,
    allDishes: List<Dish> = emptyList(),
    onAddToCart: (
        dish: Dish,
        quantity: Int,
        note: String,
        selectedOptions: List<SelectedOption>,
        optionsSummary: String,
        optionsSummaryEn: String,
        optionsPrice: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isComboDish = dish.is_combo || dish.combo_groups.isNotEmpty() || dish.category.equals("COMBO", ignoreCase = true)

    var quantity by remember(dish.id) { mutableIntStateOf(1) }
    var note by remember(dish.id) { mutableStateOf("") }
    var errorMessage by remember(dish.id) { mutableStateOf("") }

    // Map: "${gIdx}_${iIdx}" -> Map<OptionGroupIndex, Set<OptionItemIndex>>
    val selectedComboChildOptions = remember(dish.id) { mutableStateMapOf<String, Map<Int, Set<Int>>>() }

    fun initChildDishDefaultOptions(gIdx: Int, iIdx: Int, dishId: String) {
        val realDish = allDishes.find { it.id == dishId } ?: return
        if (realDish.options.isEmpty()) return
        val key = "${gIdx}_${iIdx}"
        if (!selectedComboChildOptions.containsKey(key)) {
            val groupMap = mutableMapOf<Int, Set<Int>>()
            realDish.options.forEachIndexed { oIdx, optGrp ->
                val defaults = optGrp.items.mapIndexedNotNull { itIdx, it ->
                    if (it.is_default) itIdx else null
                }.toSet()
                if (optGrp.type != "multiple" && optGrp.required && defaults.isEmpty() && optGrp.items.isNotEmpty()) {
                    groupMap[oIdx] = setOf(0)
                } else {
                    groupMap[oIdx] = defaults
                }
            }
            selectedComboChildOptions[key] = groupMap
        }
    }

    // COMBO GROUPS: GroupIndex -> Set of ItemIndices
    val selectedComboMap = remember(dish.id) {
        mutableStateMapOf<Int, Set<Int>>().apply {
            if (isComboDish) {
                dish.combo_groups.forEachIndexed { gIdx, group ->
                    val minCount = if (group.min > 0) group.min.coerceAtMost(group.items.size) else 0
                    if (minCount > 0) {
                        val initSet = (0 until minCount).toSet()
                        put(gIdx, initSet)
                        initSet.forEach { iIdx ->
                            if (iIdx in group.items.indices) {
                                initChildDishDefaultOptions(gIdx, iIdx, group.items[iIdx].dish_id)
                            }
                        }
                    } else {
                        put(gIdx, emptySet())
                    }
                }
            }
        }
    }

    // REGULAR OPTIONS: GroupIndex -> Set of ItemIndices
    val selectedOptionsMap = remember(dish.id) {
        mutableStateMapOf<Int, Set<Int>>().apply {
            dish.options.forEachIndexed { gIdx, group ->
                val defaults = group.items.mapIndexedNotNull { iIdx, it ->
                    if (it.is_default) iIdx else null
                }.toSet()
                if (group.type != "multiple" && group.required && defaults.isEmpty() && group.items.isNotEmpty()) {
                    put(gIdx, setOf(0))
                } else {
                    put(gIdx, defaults)
                }
            }
        }
    }

    // Tính phụ thu realtime
    var extraComboPrice = 0.0
    if (isComboDish) {
        dish.combo_groups.forEachIndexed { gIdx, group ->
            val selSet = selectedComboMap[gIdx] ?: emptySet()
            selSet.forEach { iIdx ->
                if (iIdx in group.items.indices) {
                    val it = group.items[iIdx]
                    extraComboPrice += it.extra

                    // Add child dish options price
                    val key = "${gIdx}_${iIdx}"
                    val childOpts = selectedComboChildOptions[key]
                    val realDish = allDishes.find { d -> d.id == it.dish_id }
                    if (childOpts != null && realDish != null) {
                        realDish.options.forEachIndexed { oIdx, optGrp ->
                            val selIndices = childOpts[oIdx] ?: emptySet()
                            selIndices.forEach { itmIdx ->
                                if (itmIdx in optGrp.items.indices) {
                                    extraComboPrice += optGrp.items[itmIdx].price
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var extraOptionsPrice = 0.0
    dish.options.forEachIndexed { gIdx, group ->
        val selSet = selectedOptionsMap[gIdx] ?: emptySet()
        selSet.forEach { iIdx ->
            if (iIdx in group.items.indices) {
                extraOptionsPrice += group.items[iIdx].price
            }
        }
    }

    val totalExtraPrice = extraComboPrice + extraOptionsPrice
    val unitPrice = dish.price + totalExtraPrice
    val totalPrice = unitPrice * quantity

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.75f else 0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(WebCardBg)
                .border(1.dp, WebBorder, RoundedCornerShape(24.dp))
                .padding(if (isLandscape) 14.dp else 18.dp)
        ) {
            // Header: Tiêu đề + Nút đóng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isComboDish) "Tùy biến gói Combo" else "Tùy chọn món ăn",
                        fontSize = if (isLandscape) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WebTextPrimary
                    )
                    if (isComboDish) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WebGoldPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "COMBO TIẾT KIỆM",
                                color = WebGoldDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = WebTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nội dung cuộn
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = if (isLandscape) 360.dp else 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Thẻ thông tin món (Ảnh + Tên + Giá)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WebSurfacePill)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = dish.img.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c" },
                        contentDescription = dish.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (isLandscape) 54.dp else 64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE2DACB))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dish.name,
                            color = WebTextPrimary,
                            fontSize = if (isLandscape) 13.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (dish.desc.isNotBlank()) {
                            Text(
                                text = dish.desc,
                                color = WebTextMuted,
                                fontSize = if (isLandscape) 10.5.sp else 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatVnd(dish.price),
                            color = WebGoldDark,
                            fontSize = if (isLandscape) 12.5.sp else 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==================== CÁC NHÓM MÓN TRONG COMBO ====================
                if (isComboDish && dish.combo_groups.isNotEmpty()) {
                    dish.combo_groups.forEachIndexed { gIdx, group ->
                        val min = group.min
                        val max = if (group.max > 0) group.max else 1
                        val isMultiple = max > 1
                        val currentSelections = selectedComboMap[gIdx] ?: emptySet()
                        val isEnough = currentSelections.size in min..max

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, WebBorder, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            // Header nhóm combo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.group_title.ifBlank { "Nhóm ${gIdx + 1}" },
                                    color = WebTextPrimary,
                                    fontSize = if (isLandscape) 12.5.sp else 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val badgeText = if (min == max) {
                                    if (min == 1) "Chọn 1 món" else "Chọn $min món"
                                } else {
                                    "Chọn $min - $max món"
                                }
                                val badgeBg = if (isEnough) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                val badgeColor = if (isEnough) Color(0xFF166534) else Color(0xFF92400E)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Danh sách items trong nhóm combo
                            group.items.forEachIndexed { iIdx, item ->
                                val isSelected = currentSelections.contains(iIdx)
                                val itemPriceStr = if (item.extra > 0) "+${CurrencyUtils.formatVnd(item.extra)}" else ""
                                val realDish = allDishes.find { it.id == item.dish_id }
                                val hasChildOptions = realDish != null && realDish.options.isNotEmpty()
                                val childKey = "${gIdx}_${iIdx}"

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) WebGoldPrimary.copy(alpha = 0.08f) else Color(0xFFFAFAFA))
                                        .border(
                                            1.dp,
                                            if (isSelected) WebGoldPrimary else Color(0xFFEEEEEE),
                                            RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    // Row món chính
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                errorMessage = ""
                                                if (isMultiple) {
                                                    val newSet = currentSelections.toMutableSet()
                                                    if (newSet.contains(iIdx)) {
                                                        newSet.remove(iIdx)
                                                        selectedComboChildOptions.remove(childKey)
                                                    } else {
                                                        if (newSet.size < max) {
                                                            newSet.add(iIdx)
                                                            initChildDishDefaultOptions(gIdx, iIdx, item.dish_id)
                                                        } else {
                                                            errorMessage = "Nhóm \"${group.group_title}\" chỉ được chọn tối đa $max món"
                                                        }
                                                    }
                                                    selectedComboMap[gIdx] = newSet
                                                } else {
                                                    // Max == 1
                                                    group.items.forEachIndexed { otherIdx, _ ->
                                                        if (otherIdx != iIdx) {
                                                            selectedComboChildOptions.remove("${gIdx}_${otherIdx}")
                                                        }
                                                    }
                                                    selectedComboMap[gIdx] = setOf(iIdx)
                                                    initChildDishDefaultOptions(gIdx, iIdx, item.dish_id)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (isMultiple) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null,
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = WebGoldPrimary,
                                                        checkmarkColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = null,
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = WebGoldPrimary
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = item.name,
                                                fontSize = if (isLandscape) 12.sp else 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) WebTextPrimary else WebTextSecondary
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (itemPriceStr.isNotBlank()) {
                                                Text(
                                                    text = itemPriceStr,
                                                    fontSize = if (isLandscape) 11.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WebGoldDark
                                                )
                                            }
                                            if (hasChildOptions && !isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(WebGoldPrimary.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "Có tùy chọn",
                                                        color = WebGoldDark,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Render child options when item is selected
                                    if (isSelected && hasChildOptions && realDish != null) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White)
                                                .border(0.5.dp, Color(0xFFE2DACB), RoundedCornerShape(10.dp))
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Tùy chọn cho ${item.name}:",
                                                fontSize = if (isLandscape) 10.5.sp else 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WebGoldDark
                                            )

                                            realDish.options.forEachIndexed { oIdx, optGrp ->
                                                val isOptMultiple = optGrp.type == "multiple"
                                                val isOptRequired = optGrp.required
                                                val currentChildOpts = selectedComboChildOptions[childKey]?.get(oIdx) ?: emptySet()
                                                val isOptMet = !isOptRequired || currentChildOpts.isNotEmpty()

                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = optGrp.name,
                                                            fontSize = if (isLandscape) 11.sp else 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = WebTextPrimary
                                                        )
                                                        val optBadgeText = if (isOptRequired) "Bắt buộc" else (if (isOptMultiple) "Nhiều" else "Tùy chọn")
                                                        val optBadgeBg = if (isOptMet) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                                                        val optBadgeColor = if (isOptMet) Color(0xFF92400E) else Color(0xFFB91C1C)

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(optBadgeBg)
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = optBadgeText,
                                                                color = optBadgeColor,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    optGrp.items.forEachIndexed { itmIdx, itm ->
                                                        val isOptChecked = currentChildOpts.contains(itmIdx)
                                                        val optPriceStr = if (itm.price > 0) "+${CurrencyUtils.formatVnd(itm.price)}" else ""

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isOptChecked) WebGoldPrimary.copy(alpha = 0.12f) else Color(0xFFF9F9F9))
                                                                .border(
                                                                    0.5.dp,
                                                                    if (isOptChecked) WebGoldPrimary else Color(0xFFE5E7EB),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .clickable {
                                                                    errorMessage = ""
                                                                    val currentGroupMap = (selectedComboChildOptions[childKey] ?: emptyMap()).toMutableMap()
                                                                    val currentSet = (currentGroupMap[oIdx] ?: emptySet()).toMutableSet()
                                                                    if (isOptMultiple) {
                                                                        if (currentSet.contains(itmIdx)) currentSet.remove(itmIdx) else currentSet.add(itmIdx)
                                                                    } else {
                                                                        currentSet.clear()
                                                                        currentSet.add(itmIdx)
                                                                    }
                                                                    currentGroupMap[oIdx] = currentSet
                                                                    selectedComboChildOptions[childKey] = currentGroupMap
                                                                }
                                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                if (isOptMultiple) {
                                                                    Checkbox(
                                                                        checked = isOptChecked,
                                                                        onCheckedChange = null,
                                                                        colors = CheckboxDefaults.colors(
                                                                            checkedColor = WebGoldPrimary,
                                                                            checkmarkColor = Color.White
                                                                        ),
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                } else {
                                                                    RadioButton(
                                                                        selected = isOptChecked,
                                                                        onClick = null,
                                                                        colors = RadioButtonDefaults.colors(
                                                                            selectedColor = WebGoldPrimary
                                                                        ),
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = itm.name,
                                                                    fontSize = if (isLandscape) 10.5.sp else 11.5.sp,
                                                                    fontWeight = if (isOptChecked) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isOptChecked) WebTextPrimary else WebTextSecondary
                                                                )
                                                            }
                                                            if (optPriceStr.isNotBlank()) {
                                                                Text(
                                                                    text = optPriceStr,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = WebGoldDark
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== CÁC NHÓM TÙY CHỌN (OPTIONS & TOPPINGS CHO MÓN ĐƠN) ====================
                dish.options.forEachIndexed { gIdx, group ->
                    val isMultiple = group.type == "multiple"
                    val isRequired = group.required
                    val currentSelections = selectedOptionsMap[gIdx] ?: emptySet()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, WebBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        // Header nhóm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.name,
                                color = WebTextPrimary,
                                fontSize = if (isLandscape) 12.5.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val badgeText = if (isRequired) "Bắt buộc" else (if (isMultiple) "Tùy chọn · Nhiều" else "Tùy chọn")
                            val badgeBg = if (isRequired) Color(0xFFFEF3C7) else Color(0xFFF3F4F6)
                            val badgeColor = if (isRequired) Color(0xFF92400E) else Color(0xFF4B5563)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Danh sách items trong nhóm
                        group.items.forEachIndexed { iIdx, item ->
                            val isSelected = currentSelections.contains(iIdx)
                            val itemPriceStr = if (item.price > 0) "+${CurrencyUtils.formatVnd(item.price)}" else ""

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) WebGoldPrimary.copy(alpha = 0.08f) else Color(0xFFFAFAFA))
                                    .border(
                                        1.dp,
                                        if (isSelected) WebGoldPrimary else Color(0xFFEEEEEE),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        errorMessage = ""
                                        if (isMultiple) {
                                            val newSet = currentSelections.toMutableSet()
                                            if (newSet.contains(iIdx)) newSet.remove(iIdx) else newSet.add(iIdx)
                                            selectedOptionsMap[gIdx] = newSet
                                        } else {
                                            selectedOptionsMap[gIdx] = setOf(iIdx)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isMultiple) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = WebGoldPrimary,
                                                checkmarkColor = Color.White
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = WebGoldPrimary
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = item.name,
                                        fontSize = if (isLandscape) 12.sp else 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) WebTextPrimary else WebTextSecondary
                                    )
                                }

                                if (itemPriceStr.isNotBlank()) {
                                    Text(
                                        text = itemPriceStr,
                                        fontSize = if (isLandscape) 11.sp else 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WebGoldDark
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ô ghi chú
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = {
                        Text(
                            if (isComboDish) "Thêm ghi chú cho Combo (VD: mang ra cùng lúc, ít đá)..." else "Thêm ghi chú cho món (VD: ít ngọt, nhiều đá)...",
                            color = WebTextMuted,
                            fontSize = if (isLandscape) 11.5.sp else 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WebGoldPrimary,
                        unfocusedBorderColor = WebBorder,
                        focusedContainerColor = WebSurfacePill,
                        unfocusedContainerColor = WebSurfacePill,
                        focusedTextColor = WebTextPrimary,
                        unfocusedTextColor = WebTextPrimary
                    ),
                    maxLines = 2
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = WebBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Số lượng + Nút Thêm vào giỏ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(WebSurfacePill)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 30.dp else 34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2DACB))
                            .clickable { if (quantity > 1) quantity-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "-", tint = WebTextPrimary, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = quantity.toString(),
                        color = WebTextPrimary,
                        fontSize = if (isLandscape) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 30.dp else 34.dp)
                            .clip(CircleShape)
                            .background(WebGoldPrimary)
                            .clickable { quantity++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "+", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val isDishAvailable = dish.isCurrentlyAvailable

                Button(
                    enabled = isDishAvailable,
                    onClick = {
                        if (!isDishAvailable) {
                            errorMessage = "Món hiện đã hết hàng hoặc tạm khóa!"
                            return@Button
                        }

                        // Validate Combo Groups and child dish required options
                        if (isComboDish) {
                            for ((gIdx, group) in dish.combo_groups.withIndex()) {
                                val sel = selectedComboMap[gIdx] ?: emptySet()
                                val min = group.min
                                if (sel.size < min) {
                                    val diff = min - sel.size
                                    errorMessage = "Vui lòng chọn thêm $diff món trong nhóm \"${group.group_title.ifBlank { "Nhóm ${gIdx + 1}" }}\""
                                    return@Button
                                }
                                for (iIdx in sel) {
                                    if (iIdx in group.items.indices) {
                                        val it = group.items[iIdx]
                                        val realDish = allDishes.find { d -> d.id == it.dish_id }
                                        if (realDish != null && realDish.options.isNotEmpty()) {
                                            val childKey = "${gIdx}_${iIdx}"
                                            val childOptsMap = selectedComboChildOptions[childKey] ?: emptyMap()
                                            for ((oIdx, optGrp) in realDish.options.withIndex()) {
                                                if (optGrp.required) {
                                                    val selIndices = childOptsMap[oIdx] ?: emptySet()
                                                    if (selIndices.isEmpty()) {
                                                        errorMessage = "Vui lòng chọn \"${optGrp.name}\" cho món \"${it.name}\""
                                                        return@Button
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Validate required options groups for normal dish
                        for ((gIdx, group) in dish.options.withIndex()) {
                            if (group.required) {
                                val sel = selectedOptionsMap[gIdx] ?: emptySet()
                                if (sel.isEmpty()) {
                                    errorMessage = "Vui lòng chọn: ${group.name}"
                                    return@Button
                                }
                            }
                        }

                        // Build selected options & summaries
                        val selectedList = mutableListOf<SelectedOption>()
                        val summaryViList = mutableListOf<String>()
                        val summaryEnList = mutableListOf<String>()

                        // Add combo selected items to summary
                        if (isComboDish) {
                            dish.combo_groups.forEachIndexed { gIdx, group ->
                                val sel = selectedComboMap[gIdx] ?: emptySet()
                                sel.forEach { iIdx ->
                                    if (iIdx in group.items.indices) {
                                        val it = group.items[iIdx]
                                        val childKey = "${gIdx}_${iIdx}"
                                        val childOptsMap = selectedComboChildOptions[childKey] ?: emptyMap()
                                        val realDish = allDishes.find { d -> d.id == it.dish_id }

                                        val optPartsVi = mutableListOf<String>()
                                        val optPartsEn = mutableListOf<String>()

                                        if (realDish != null && realDish.options.isNotEmpty()) {
                                            realDish.options.forEachIndexed { oIdx, optGrp ->
                                                val selIndices = childOptsMap[oIdx] ?: emptySet()
                                                selIndices.forEach { itmIdx ->
                                                    if (itmIdx in optGrp.items.indices) {
                                                        val itm = optGrp.items[itmIdx]
                                                        val sfx = if (itm.price > 0) " (+${CurrencyUtils.formatVnd(itm.price)})" else ""
                                                        optPartsVi.add("${itm.name}$sfx")
                                                        optPartsEn.add("${itm.name_en.ifBlank { itm.name }}$sfx")
                                                    }
                                                }
                                            }
                                        }

                                        val optStrVi = if (optPartsVi.isNotEmpty()) " (${optPartsVi.joinToString(", ")})" else ""
                                        val optStrEn = if (optPartsEn.isNotEmpty()) " (${optPartsEn.joinToString(", ")})" else ""
                                        val extraStr = if (it.extra > 0) " (+${CurrencyUtils.formatVnd(it.extra)})" else ""

                                        summaryViList.add("• ${it.name}$optStrVi$extraStr")
                                        summaryEnList.add("• ${it.name_en.ifBlank { it.name }}$optStrEn$extraStr")
                                    }
                                }
                            }
                        }

                        // Add normal dish options to summary
                        dish.options.forEachIndexed { gIdx, group ->
                            val sel = selectedOptionsMap[gIdx] ?: emptySet()
                            sel.forEach { iIdx ->
                                if (iIdx in group.items.indices) {
                                    val it = group.items[iIdx]
                                    selectedList.add(
                                        SelectedOption(
                                            group_name = group.name,
                                            group_name_en = group.name_en,
                                            item_name = it.name,
                                            item_name_en = it.name_en,
                                            price = it.price
                                        )
                                    )
                                    val suffix = if (it.price > 0) " (+${CurrencyUtils.formatVnd(it.price)})" else ""
                                    summaryViList.add("• ${it.name}$suffix")
                                    summaryEnList.add("• ${it.name_en.ifBlank { it.name }}$suffix")
                                }
                            }
                        }

                        val summaryVi = summaryViList.joinToString("\n")
                        val summaryEn = summaryEnList.joinToString("\n")

                        onAddToCart(dish, quantity, note.trim(), selectedList, summaryVi, summaryEn, totalExtraPrice)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isLandscape) 40.dp else 46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WebGoldPrimary)
                ) {
                    Text(
                        text = if (isDishAvailable) "Thêm · ${CurrencyUtils.formatVnd(totalPrice)}" else "Hết hàng / Tạm khóa",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isLandscape) 12.sp else 14.sp
                    )
                }
            }
        }
    }
}
