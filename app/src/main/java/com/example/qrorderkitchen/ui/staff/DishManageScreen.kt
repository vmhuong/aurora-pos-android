package com.example.qrorderkitchen.ui.staff

import androidx.compose.ui.text.TextStyle

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.LightBgWarm
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightSurface
import com.example.qrorderkitchen.ui.theme.TextMuted
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import com.example.qrorderkitchen.utils.CurrencyUtils
import com.google.firebase.Timestamp
import java.util.Date

@Composable
fun DishManageScreen(viewModel: AuroraViewModel) {
    val dishes by viewModel.dishes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("TẤT CẢ") }
    var selectedDishForStatus by remember { mutableStateOf<Dish?>(null) }

    val categories = remember {
        listOf("TẤT CẢ", "COMBO", "KHAI VỊ", "MÓN CHÍNH", "CÀ PHÊ & TRÀ", "SINH TỐ & NƯỚC ÉP", "TRÁNG MIỆNG & BÁNH", "LOUNGE & SHISHA")
    }

    val filteredDishes = remember(dishes, selectedCategory, searchQuery) {
        dishes.filter { dish ->
            val isComboDish = dish.is_combo || dish.category.equals("COMBO", ignoreCase = true)
            val matchesCat = when (selectedCategory) {
                "TẤT CẢ" -> true
                "COMBO" -> isComboDish
                else -> dish.category.equals(selectedCategory, ignoreCase = true) && !isComboDish
            }
            val matchesSearch = dish.name.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgWarm)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightSurface)
                    .border(0.5.dp, LightBorder, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "QUẢN LÝ KHÓA/MỞ MÓN ĂN TRÊN MENU WEB",
                    color = AuroraGoldHover,
                    fontSize = if (isLandscape) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Search Bar & Filter Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm tên món cần khóa/mở...", color = TextMuted, fontSize = if (isLandscape) 12.sp else 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AuroraGold, modifier = Modifier.size(if (isLandscape) 18.dp else 20.dp)) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = if (isLandscape) 12.sp else 14.sp, color = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 42.dp else 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraGold,
                        unfocusedBorderColor = LightBorder,
                        focusedContainerColor = LightSurface,
                        unfocusedContainerColor = LightSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Category Filter Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) TextPrimary else LightSurface)
                            .border(1.dp, if (isSelected) AuroraGold else LightBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (cat == "COMBO") "COMBO / SET MENU" else cat,
                            color = if (isSelected) AuroraGold else TextPrimary,
                            fontSize = if (isLandscape) 11.sp else 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Dishes List: Render 2-Column Grid in Landscape Mode for maximum content visibility
            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredDishes, key = { it.id }) { dish ->
                        DishManageCard(
                            dish = dish,
                            onStatusClick = { selectedDishForStatus = dish }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(10.dp),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredDishes, key = { it.id }) { dish ->
                        DishManageCard(
                            dish = dish,
                            onStatusClick = { selectedDishForStatus = dish }
                        )
                    }
                }
            }
        }

        // List Status Selection Dialog
        selectedDishForStatus?.let { dish ->
            DishStatusListDialog(
                dishName = dish.name,
                isAvailableNow = dish.is_available,
                onSelectOption = { option ->
                    when (option) {
                        DishStatusOption.ENABLE -> {
                            viewModel.toggleDishTimed(dish.id, null)
                        }
                        DishStatusOption.DISABLE_MANUAL -> {
                            viewModel.toggleDish(dish.id, false)
                        }
                        DishStatusOption.LOCK_30M -> {
                            val ts = Timestamp(Date(com.example.qrorderkitchen.utils.NetworkTimeProvider.currentTimeMillis() + 30 * 60 * 1000))
                            viewModel.toggleDishTimed(dish.id, ts)
                        }
                        DishStatusOption.LOCK_1H -> {
                            val ts = Timestamp(Date(com.example.qrorderkitchen.utils.NetworkTimeProvider.currentTimeMillis() + 60 * 60 * 1000))
                            viewModel.toggleDishTimed(dish.id, ts)
                        }
                        DishStatusOption.LOCK_2H -> {
                            val ts = Timestamp(Date(com.example.qrorderkitchen.utils.NetworkTimeProvider.currentTimeMillis() + 120 * 60 * 1000))
                            viewModel.toggleDishTimed(dish.id, ts)
                        }
                    }
                    selectedDishForStatus = null
                },
                onDismiss = { selectedDishForStatus = null }
            )
        }
    }
}

@Composable
fun DishManageCard(
    dish: Dish,
    onStatusClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val nowSecs = com.example.qrorderkitchen.utils.NetworkTimeProvider.currentTimeMillis() / 1000
    val isLocked = dish.isCurrentlyLocked
    val isLockedUntilActive = dish.locked_until?.let { it.seconds > nowSecs } ?: false

    val statusDisplayText = when {
        !isLocked -> "🟢 đang bán"
        isLockedUntilActive -> {
            val remainingSecs = dish.locked_until!!.seconds - nowSecs
            val remainingMins = (remainingSecs / 60).coerceAtLeast(1)
            val timeStr = if (remainingMins >= 60) {
                val hrs = remainingMins / 60
                val mins = remainingMins % 60
                if (mins > 0) "${hrs}h${mins}m" else "${hrs}h"
            } else {
                "${remainingMins}m"
            }
            "🔴 đang khóa (Còn $timeStr)"
        }
        else -> "🔴 đang khóa"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onStatusClick() }
            .border(1.dp, LightBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dish.name,
                    color = TextPrimary,
                    fontSize = if (isLandscape) 13.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${dish.category} · ${CurrencyUtils.formatVnd(dish.price)}",
                    color = AuroraGoldHover,
                    fontSize = if (isLandscape) 11.sp else 13.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(3.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LightSurface)
                        .border(1.dp, LightBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusDisplayText,
                        color = TextPrimary,
                        fontSize = if (isLandscape) 10.sp else 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightSurface)
                    .border(1.dp, LightBorder, RoundedCornerShape(8.dp))
                    .clickable { onStatusClick() }
                    .padding(horizontal = if (isLandscape) 10.dp else 12.dp, vertical = if (isLandscape) 6.dp else 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đổi trạng thái ▾",
                    color = TextPrimary,
                    fontSize = if (isLandscape) 11.sp else 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

enum class DishStatusOption {
    ENABLE, DISABLE_MANUAL, LOCK_30M, LOCK_1H, LOCK_2H
}

@Composable
fun DishStatusListDialog(
    dishName: String,
    isAvailableNow: Boolean,
    onSelectOption: (DishStatusOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LightSurface)
                .border(1.5.dp, LightBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            Text(
                text = "CHỌN TRẠNG THÁI MÓN",
                color = AuroraGoldHover,
                fontSize = if (isLandscape) 15.sp else 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Món: $dishName",
                color = TextPrimary,
                fontSize = if (isLandscape) 12.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WhiteOptionButton(
                    text = "🟢 đang bán (Mở bán lại)",
                    onClick = { onSelectOption(DishStatusOption.ENABLE) }
                )

                WhiteOptionButton(
                    text = "🔴 đang khóa (Thủ công)",
                    onClick = { onSelectOption(DishStatusOption.DISABLE_MANUAL) }
                )

                WhiteOptionButton(
                    text = "Khóa 30 phút",
                    onClick = { onSelectOption(DishStatusOption.LOCK_30M) }
                )

                WhiteOptionButton(
                    text = "Khóa 1 giờ",
                    onClick = { onSelectOption(DishStatusOption.LOCK_1H) }
                )

                WhiteOptionButton(
                    text = "Khóa 2 giờ",
                    onClick = { onSelectOption(DishStatusOption.LOCK_2H) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Đóng", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WhiteOptionButton(
    text: String,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 42.dp else 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LightSurface)
            .border(1.dp, LightBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = if (isLandscape) 13.sp else 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
