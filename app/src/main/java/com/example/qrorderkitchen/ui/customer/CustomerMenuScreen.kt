package com.example.qrorderkitchen.ui.customer

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import com.example.qrorderkitchen.ui.theme.WebInputBg
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.ui.components.DishDetailDialog
import com.example.qrorderkitchen.ui.components.MenuCard
import com.example.qrorderkitchen.utils.CurrencyUtils
import com.example.qrorderkitchen.ui.theme.WebBorder
import com.example.qrorderkitchen.ui.theme.WebCardBg
import com.example.qrorderkitchen.ui.theme.WebCreamBg
import com.example.qrorderkitchen.ui.theme.WebGoldDark
import com.example.qrorderkitchen.ui.theme.WebGoldPrimary
import com.example.qrorderkitchen.ui.theme.WebSurfacePill
import com.example.qrorderkitchen.ui.theme.WebTextMuted
import com.example.qrorderkitchen.ui.theme.WebTextPrimary
import com.example.qrorderkitchen.ui.theme.WebTextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel

@Composable
fun CustomerMenuScreen(viewModel: AuroraViewModel) {
    val context = LocalContext.current
    val dishes by viewModel.dishes.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val isSubmitting by viewModel.isSubmittingOrder.collectAsState()
    val currentTableId by viewModel.currentTableId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var selectedDishForDetail by remember { mutableStateOf<Dish?>(null) }

    val categories = remember {
        listOf("TẤT CẢ", "COMBO", "KHAI VỊ", "MÓN CHÍNH", "CÀ PHÊ & TRÀ", "SINH TỐ & NƯỚC ÉP", "TRÁNG MIỆNG & BÁNH", "LOUNGE & SHISHA")
    }
    val categoriesOrder = remember {
        listOf("COMBO", "KHAI VỊ", "MÓN CHÍNH", "CÀ PHÊ & TRÀ", "SINH TỐ & NƯỚC ÉP", "TRÁNG MIỆNG & BÁNH", "LOUNGE & SHISHA")
    }

    val filteredDishes = remember(dishes, selectedCategory, searchQuery) {
        dishes.filter { dish ->
            val isComboDish = dish.is_combo || dish.category.equals("COMBO", ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "TẤT CẢ" -> true
                "COMBO" -> isComboDish
                else -> dish.category.equals(selectedCategory, ignoreCase = true) && !isComboDish
            }
            val matchesSearch = dish.name.contains(searchQuery, ignoreCase = true) || dish.desc.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // ⚡ TỐI ƯU HIỆU NĂNG 1: Bảng tra cứu số lượng món trong giỏ O(1), loại bỏ hoàn toàn việc quét mảng khi cuộn menu
    val dishQuantities = remember(cartItems) {
        val map = HashMap<String, Int>(cartItems.size)
        for (item in cartItems.values) {
            val dId = item.dish.id
            map[dId] = (map[dId] ?: 0) + item.quantity
        }
        map
    }

    // ⚡ TỐI ƯU HIỆU NĂNG 2: Nhóm và sắp xếp danh mục 1 lần duy nhất trong remember, không lặp lại mỗi frame
    val sortedCategoryGroups = remember(filteredDishes, categoriesOrder) {
        filteredDishes.groupBy { dish ->
            if (dish.is_combo || dish.category.equals("COMBO", ignoreCase = true)) {
                "COMBO"
            } else {
                dish.category.uppercase()
            }
        }
            .toList()
            .sortedBy { (catName, _) ->
                val idx = categoriesOrder.indexOf(catName)
                if (idx != -1) idx else 999
            }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WebCreamBg)
    ) {
        if (isLandscape) {
            // LANDSCAPE 2-COLUMN SPLIT SCREEN: LEFT MENU (60%), RIGHT CART ORDER PANEL (40%)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // LEFT SIDE: MENU DISHES LIST
                Column(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                ) {
                    // Search Bar Compact matching Web
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Tìm kiếm món ăn, đồ uống...", color = WebTextMuted, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = WebTextMuted, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = WebTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WebGoldPrimary.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = WebSurfacePill,
                            unfocusedContainerColor = WebSurfacePill,
                            focusedTextColor = WebTextPrimary,
                            unfocusedTextColor = WebTextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category Filter Pills
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            CategoryFilterChip(
                                categoryName = if (cat == "COMBO") "COMBO / SET MENU" else cat,
                                isSelected = isSelected,
                                onClick = { viewModel.setSelectedCategory(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dish List
                    if (filteredDishes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Không tìm thấy món ăn phù hợp", color = WebTextMuted, fontSize = 13.sp)
                        }
                    } else if (selectedCategory == "TẤT CẢ" && searchQuery.isBlank()) {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            sortedCategoryGroups.forEach { (catName, dishGroup) ->
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (catName == "COMBO") "COMBO / SET MENU" else catName,
                                            color = WebGoldDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        HorizontalDivider(
                                            color = Color(0xFFE2DACB),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                        )
                                        Text(
                                            text = if (catName == "COMBO") "${dishGroup.size} combo" else "${dishGroup.size} món",
                                            color = WebTextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                items(dishGroup, key = { it.id }) { dish ->
                                    val qty = dishQuantities[dish.id] ?: 0
                                    MenuCard(
                                        dish = dish,
                                        cartQuantity = qty,
                                        onAddToCart = {
                                            val isCombo = it.is_combo || it.combo_groups.isNotEmpty() || it.category.equals("COMBO", ignoreCase = true)
                                            if (!it.isCurrentlyAvailable) {
                                                viewModel.showToast("Món ${it.name} hiện đã hết hàng hoặc tạm khóa!")
                                            } else if (isCombo || it.options.isNotEmpty()) {
                                                selectedDishForDetail = it
                                            } else {
                                                viewModel.addToCart(it, 1)
                                            }
                                        },
                                        onRemoveFromCart = { viewModel.addToCart(it, -1) },
                                        onCardClick = {
                                            if (it.isCurrentlyAvailable) {
                                                selectedDishForDetail = it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredDishes, key = { it.id }) { dish ->
                                val qty = dishQuantities[dish.id] ?: 0
                                MenuCard(
                                    dish = dish,
                                    cartQuantity = qty,
                                    onAddToCart = {
                                        val isCombo = it.is_combo || it.combo_groups.isNotEmpty() || it.category.equals("COMBO", ignoreCase = true)
                                        if (!it.isCurrentlyAvailable) {
                                            viewModel.showToast("Món ${it.name} hiện đã hết hàng hoặc tạm khóa!")
                                        } else if (isCombo || it.options.isNotEmpty()) {
                                            selectedDishForDetail = it
                                        } else {
                                            viewModel.addToCart(it, 1)
                                        }
                                    },
                                    onRemoveFromCart = { viewModel.addToCart(it, -1) },
                                    onCardClick = {
                                        if (it.isCurrentlyAvailable) {
                                            selectedDishForDetail = it
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT SIDE: QUICK CART & ORDER PANEL FOR TABLE (Matching Web Luxury Card)
                val totalCartAmount = cartItems.values.sumOf { it.quantity * (if (it.unitPrice > 0.0) it.unitPrice else (it.dish.price + it.optionsPrice)) }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, WebBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = WebCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ĐƠN ĐẶT MÓN",
                                color = WebTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WebGoldPrimary)
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "BÀN $currentTableId",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = WebBorder, modifier = Modifier.padding(vertical = 10.dp))

                        // Cart Items List
                        if (cartItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Giỏ hàng đang trống.\nChạm (+) bên trái để chọn món.",
                                    color = WebTextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(cartItems.values.toList(), key = { it.cartItemKey.ifBlank { it.dish.id } }) { item ->
                                    val itemUnitPrice = if (item.unitPrice > 0.0) item.unitPrice else (item.dish.price + item.optionsPrice)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(WebCreamBg.copy(alpha = 0.5f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.dish.name,
                                                color = WebTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            if (item.optionsSummary.isNotBlank()) {
                                                val optLines = if (item.optionsSummary.contains("\n")) item.optionsSummary.split("\n") else item.optionsSummary.split(", ")
                                                optLines.filter { it.isNotBlank() }.forEach { optLine ->
                                                    val cleanLine = optLine.trim().removePrefix("+").removePrefix("•").trim()
                                                    Text(
                                                        text = "• $cleanLine",
                                                        color = Color(0xFFB45309),
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, end = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = CurrencyUtils.formatVnd(itemUnitPrice * item.quantity),
                                                    color = WebGoldDark,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                // Ghi chú nhỏ cạnh giá ở bảng đơn đặt món
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(WebInputBg)
                                                        .border(0.5.dp, Color(0xFFE2DACB), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    if (item.note.isBlank()) {
                                                        Text(
                                                            text = "Ghi chú...",
                                                            color = Color(0xFFB5ADA0),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    BasicTextField(
                                                        value = item.note,
                                                        onValueChange = { newNote ->
                                                            viewModel.updateCartItemNote(item.cartItemKey, newNote)
                                                        },
                                                        textStyle = TextStyle(
                                                            color = WebTextPrimary,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.updateCartItemQuantity(item.cartItemKey, -1) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Minus", tint = WebTextPrimary, modifier = Modifier.size(12.dp))
                                            }
                                            Text(
                                                text = "${item.quantity}",
                                                color = WebTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.updateCartItemQuantity(item.cartItemKey, 1) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Plus", tint = WebGoldPrimary, modifier = Modifier.size(13.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = WebBorder, modifier = Modifier.padding(vertical = 8.dp))

                        // Total & Submit Order
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "TỔNG TIỀN:", color = WebTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = CurrencyUtils.formatVnd(totalCartAmount),
                                color = WebGoldDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (!isSubmitting) {
                                    viewModel.submitCustomerOrder(context)
                                }
                            },
                            enabled = !isSubmitting && cartItems.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WebGoldPrimary,
                                disabledContainerColor = WebGoldPrimary.copy(alpha = 0.4f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ĐANG GỬI ĐƠN...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "XÁC NHẬN ĐẶT MÓN BÀN $currentTableId",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // PORTRAIT SINGLE-COLUMN LAYOUT (TỐI ƯU CỠ CHỮ TO RÕ TRÊN ĐIỆN THOẠI)
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar với cỡ chữ to rõ 14.sp
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Tìm kiếm món ăn, đồ uống...", color = WebTextMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = WebTextMuted, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = WebTextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WebGoldPrimary.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = WebSurfacePill,
                        unfocusedContainerColor = WebSurfacePill,
                        focusedTextColor = WebTextPrimary,
                        unfocusedTextColor = WebTextPrimary
                    ),
                    textStyle = TextStyle(fontSize = 14.sp, color = WebTextPrimary),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category Filter Pills với cỡ chữ 13.5.sp to đẹp
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        CategoryFilterChip(
                            categoryName = if (cat == "COMBO") "COMBO / SET MENU" else cat,
                            isSelected = isSelected,
                            onClick = { viewModel.setSelectedCategory(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dish List
                if (filteredDishes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy món phù hợp",
                            color = WebTextMuted,
                            fontSize = 15.sp
                        )
                    }
                } else if (selectedCategory == "TẤT CẢ" && searchQuery.isBlank()) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 75.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sortedCategoryGroups.forEach { (catName, dishGroup) ->
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (catName == "COMBO") "COMBO / SET MENU" else catName,
                                        color = WebGoldDark,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    HorizontalDivider(
                                        color = Color(0xFFE2DACB),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 10.dp)
                                        )
                                    Text(
                                        text = if (catName == "COMBO") "${dishGroup.size} combo" else "${dishGroup.size} món",
                                        color = WebTextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            items(dishGroup, key = { it.id }) { dish ->
                                val qty = dishQuantities[dish.id] ?: 0
                                MenuCard(
                                    dish = dish,
                                    cartQuantity = qty,
                                    onAddToCart = {
                                        val isCombo = dish.is_combo || dish.combo_groups.isNotEmpty() || dish.category.equals("COMBO", ignoreCase = true)
                                        if (!it.isCurrentlyAvailable) {
                                            viewModel.showToast("Món ${it.name} hiện đã hết hàng hoặc tạm khóa!")
                                        } else if (isCombo || it.options.isNotEmpty()) {
                                            selectedDishForDetail = it
                                        } else {
                                            viewModel.addToCart(it, 1)
                                        }
                                    },
                                    onRemoveFromCart = { viewModel.addToCart(it, -1) },
                                    onCardClick = {
                                        if (it.isCurrentlyAvailable) {
                                            selectedDishForDetail = it
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 75.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredDishes, key = { it.id }) { dish ->
                            val qty = dishQuantities[dish.id] ?: 0
                            MenuCard(
                                dish = dish,
                                cartQuantity = qty,
                                onAddToCart = {
                                    val isCombo = dish.is_combo || dish.combo_groups.isNotEmpty() || dish.category.equals("COMBO", ignoreCase = true)
                                    if (!it.isCurrentlyAvailable) {
                                        viewModel.showToast("Món ${it.name} hiện đã hết hàng hoặc tạm khóa!")
                                    } else if (isCombo || it.options.isNotEmpty()) {
                                        selectedDishForDetail = it
                                    } else {
                                        viewModel.addToCart(it, 1)
                                    }
                                },
                                onRemoveFromCart = { viewModel.addToCart(it, -1) },
                                onCardClick = {
                                    if (it.isCurrentlyAvailable) {
                                        selectedDishForDetail = it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Dialog chi tiết món & Tùy chọn topping
        selectedDishForDetail?.let { dish ->
            DishDetailDialog(
                dish = dish,
                allDishes = dishes,
                onAddToCart = { d, q, note, opts, sumVi, sumEn, extraPrice ->
                    viewModel.addToCart(
                        dish = d,
                        quantityDelta = q,
                        note = note,
                        selectedOptions = opts,
                        optionsSummary = sumVi,
                        optionsSummaryEn = sumEn,
                        optionsPrice = extraPrice
                    )
                },
                onDismiss = { selectedDishForDetail = null }
            )
        }
    }
}

@Composable
fun CategoryFilterChip(
    categoryName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val chipHeight = if (isLandscape) 29.dp else 36.dp
    val fontSize = if (isLandscape) 11.sp else 13.5.sp
    val horizPadding = if (isLandscape) 11.dp else 14.dp

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) WebGoldPrimary else WebSurfacePill,
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier
            .height(chipHeight)
            .padding(horizontal = 1.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = horizPadding, vertical = 4.dp)
        ) {
            Text(
                text = categoryName,
                fontSize = fontSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else WebTextSecondary,
                maxLines = 1
            )
        }
    }
}
