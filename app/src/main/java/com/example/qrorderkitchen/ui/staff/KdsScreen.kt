package com.example.qrorderkitchen.ui.staff

import android.content.Context
import android.content.res.Configuration
import com.example.qrorderkitchen.utils.SoundManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.example.qrorderkitchen.models.OrderItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.qrorderkitchen.data.models.StaffRole
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.models.Order
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightBgWarm
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightBorderStrong
import com.example.qrorderkitchen.ui.theme.LightSurface
import com.example.qrorderkitchen.ui.theme.LightSurfaceSubtle
import com.example.qrorderkitchen.ui.theme.StatusAlertBg
import com.example.qrorderkitchen.ui.theme.StatusAlertText
import com.example.qrorderkitchen.ui.theme.StatusCookingBg
import com.example.qrorderkitchen.ui.theme.StatusCookingBorder
import com.example.qrorderkitchen.ui.theme.StatusCookingText
import com.example.qrorderkitchen.ui.theme.StatusPendingBg
import com.example.qrorderkitchen.ui.theme.StatusPendingBorder
import com.example.qrorderkitchen.ui.theme.StatusPendingText
import com.example.qrorderkitchen.ui.theme.TextMuted
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AuthState
import com.example.qrorderkitchen.ui.viewmodel.AuthViewModel
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiện ích phát âm thanh chuông báo KDS & rung cảnh báo khi có đơn mới
 */
object KitchenSoundUtil {
    fun playChime(context: Context) {
        SoundManager.playOrderChime(context)
    }
}

/**
 * Tách chuỗi tùy chọn thành từng token riêng biệt, bảo toàn dấu ngoặc đơn (giá tiền, ghi chú)
 */
fun splitOptionTokens(str: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var parenDepth = 0
    for (char in str) {
        if (char == '(') parenDepth++
        else if (char == ')') parenDepth = (parenDepth - 1).coerceAtLeast(0)

        if (char == ',' && parenDepth == 0) {
            val t = current.toString().trim()
            if (t.isNotBlank()) tokens.add(t)
            current.clear()
        } else {
            current.append(char)
        }
    }
    val last = current.toString().trim()
    if (last.isNotBlank()) tokens.add(last)
    return tokens
}

@Composable
fun KdsScreen(
    viewModel: AuroraViewModel,
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val activeOrders by viewModel.activeOrders.collectAsState()
    val struckItemKeys by viewModel.struckItemKeys.collectAsState()
    val allDishes by viewModel.dishes.collectAsState()
    val authState by authViewModel?.authState?.collectAsState() ?: remember { androidx.compose.runtime.mutableStateOf(AuthState.Unauthenticated) }

    val currentStaffRole = (authState as? AuthState.Authenticated)?.staff?.staffRole ?: StaffRole.KITCHEN
    val canKitchenOperate = currentStaffRole == StaffRole.KITCHEN || currentStaffRole == StaffRole.MANAGER

    // 🔔 TRẠNG THÁI ÂM BÁO CHUÔNG KDS ĐƯỢC ĐỒNG BỘ TOÀN APP QUA SOUNDMANAGER
    val isSoundEnabled by SoundManager.isSoundEnabled.collectAsState()

    // FIFO: ĐƠN VÀO TRƯỚC XẾP TRƯỚC (SẮP XẾP THEO THỜI GIAN ĐẶT TĂNG DẦN)
    val sortedOrders = remember(activeOrders) {
        activeOrders.filter { it.status == "pending" || it.status == "cooking" }
            .sortedWith(compareBy(
                { it.created_at?.toDate()?.time ?: Long.MAX_VALUE },
                { it.id }
            ))
    }

    val pendingCount = remember(sortedOrders) { sortedOrders.count { it.status == "pending" } }
    val cookingCount = remember(sortedOrders) { sortedOrders.count { it.status == "cooking" } }
    val totalOrdersCount = sortedOrders.size

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = screenWidthDp >= 720 || (isLandscape && screenWidthDp >= 600)

    // 📱/💻 TỐI ƯU: MẶC ĐỊNH 3 CỘT Ở MÀN NGANG, 2 CỘT Ở MÀN DỌC
    val defaultCols = when {
        isLandscape -> 3
        else -> 2
    }
    var selectedColumns by remember(screenWidthDp, isLandscape) { mutableStateOf(if (isLandscape) 3 else 2) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgWarm)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BANNER: Total active KDS bills + Controls (Sound Bell + Grid Columns)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightSurface)
                    .border(0.5.dp, LightBorder, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isTablet) {
                    // TABLET HOẶC MÀN HÌNH XOAY NGANG: HIỂN THỊ TRÊN 1 HÀNG DUY NHẤT RỘNG RÃI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Tổng số đơn & Huy hiệu trạng thái
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "TỔNG BILL CẦN THỰC HIỆN: $totalOrdersCount ĐƠN",
                                color = AuroraGoldHover,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StatusPendingBg)
                                    .border(0.5.dp, StatusPendingBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Chờ: $pendingCount",
                                    color = StatusPendingText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StatusCookingBg)
                                    .border(0.5.dp, StatusCookingBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Đang nấu: $cookingCount",
                                    color = StatusCookingText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right: Lựa chọn 2, 3, 4 Cột & Nút Chuông Báo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Bộ chuyển số cột (Grid Column Selector)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Lưới:",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                listOf(2, 3, 4).forEach { cols ->
                                    val isSelected = selectedColumns == cols
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AuroraGold else LightSurfaceSubtle)
                                            .border(0.5.dp, if (isSelected) AuroraGoldHover else LightBorder, RoundedCornerShape(8.dp))
                                            .clickable { selectedColumns = cols }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "$cols cột",
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Nút bật/tắt chuông báo KDS
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSoundEnabled) AuroraGoldSoft else LightSurfaceSubtle)
                                    .border(0.5.dp, if (isSoundEnabled) AuroraGold else LightBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val next = SoundManager.toggleSound(context)
                                        if (next) {
                                            viewModel.showToast("🔔 Đã bật âm báo chuông KDS")
                                        } else {
                                            viewModel.showToast("🔕 Đã tắt âm báo KDS")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSoundEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = "Chime Bell",
                                        tint = if (isSoundEnabled) AuroraGoldHover else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isSoundEnabled) "Chuông: BẬT" else "Chuông: TẮT",
                                        color = if (isSoundEnabled) AuroraGoldHover else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ĐIỆN THOẠI DỌC: CHIA LÀM 2 HÀNG GỌN GÀNG, KHÔNG BỊ TRÀN CHỮ
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TỔNG BILL: $totalOrdersCount ĐƠN",
                                color = AuroraGoldHover,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                    .background(StatusPendingBg)
                                    .border(0.5.dp, StatusPendingBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Chờ: $pendingCount",
                                        color = StatusPendingText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusCookingBg)
                                        .border(0.5.dp, StatusCookingBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Đang nấu: $cookingCount",
                                        color = StatusCookingText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bộ chọn 1 hoặc 2 cột trên điện thoại
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Lưới:",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                listOf(1, 2).forEach { cols ->
                                    val isSelected = selectedColumns == cols
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AuroraGold else LightSurfaceSubtle)
                                            .border(0.5.dp, if (isSelected) AuroraGoldHover else LightBorder, RoundedCornerShape(8.dp))
                                            .clickable { selectedColumns = cols }
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$cols cột",
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Nút chuông báo KDS trên điện thoại
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSoundEnabled) AuroraGoldSoft else LightSurfaceSubtle)
                                    .border(0.5.dp, if (isSoundEnabled) AuroraGold else LightBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val next = SoundManager.toggleSound(context)
                                        if (next) {
                                            viewModel.showToast("🔔 Đã bật chuông báo KDS")
                                        } else {
                                            viewModel.showToast("🔕 Đã tắt chuông báo KDS")
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSoundEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = "Chime Bell",
                                        tint = if (isSoundEnabled) AuroraGoldHover else TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isSoundEnabled) "Chuông: BẬT" else "Chuông: TẮT",
                                        color = if (isSoundEnabled) AuroraGoldHover else TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (sortedOrders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bếp hiện không có đơn hàng nào chờ nấu",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // LƯỚI KDS ĐÁP ỨNG TOÀN DIỆN CHO DI ĐỘNG & MÁY TÍNH BẢNG (1, 2, 3 HOẶC 4 CỘT)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(selectedColumns),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedOrders, key = { it.id }) { order ->
                        KdsOrderCard(
                            order = order,
                            canKitchenOperate = canKitchenOperate,
                            struckItemKeys = struckItemKeys,
                            allDishes = allDishes,
                            onToggleItem = { item -> viewModel.toggleItemCompleted(order.id, item) },
                            onUpdateStatus = { ordId, status -> viewModel.updateOrderStatus(ordId, status) },
                            onShowToast = { msg -> viewModel.showToast(msg) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KdsOrderCard(
    order: Order,
    canKitchenOperate: Boolean,
    struckItemKeys: Set<String>,
    allDishes: List<Dish> = emptyList(),
    onToggleItem: (OrderItem) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val orderDate = order.created_at?.toDate() ?: Date()
    val calOrder = java.util.Calendar.getInstance().apply { time = orderDate }
    val calNow = java.util.Calendar.getInstance()
    val isToday = calOrder.get(java.util.Calendar.YEAR) == calNow.get(java.util.Calendar.YEAR) &&
                  calOrder.get(java.util.Calendar.DAY_OF_YEAR) == calNow.get(java.util.Calendar.DAY_OF_YEAR)

    val orderTimeStr = if (isToday) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }.format(orderDate)
    } else {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }.format(orderDate)
    }

    val nowMs = System.currentTimeMillis()
    val orderMs = orderDate.time
    val rawDiffMillis = nowMs - orderMs
    val diffMins = (rawDiffMillis / (1000 * 60)).coerceAtLeast(0L)
    val isLateWarning = diffMins >= 15
    val timeAgoText = when {
        rawDiffMillis < 60_000 -> "Vừa xong"
        diffMins < 60 -> "${diffMins}m trước"
        diffMins < 24 * 60 -> "${diffMins / 60}h ${diffMins % 60}m"
        else -> "${diffMins / (24 * 60)}d ${ (diffMins % (24 * 60)) / 60 }h"
    }

    // TÍNH TỔNG SỐ LƯỢNG MÓN CÓ TRONG ĐƠN
    val totalItemCount = order.items.sumOf { it.quantity }
    val rawTable = order.table_id.uppercase()
    val baseTable = if (rawTable.startsWith("BÀN")) rawTable else "BÀN $rawTable"
    val struckCount = order.items.count { item ->
        val k = if (item.id.isNotBlank()) item.id else "${order.id}_${item.dish_name}"
        struckItemKeys.contains(k) || item.is_completed
    }
    val tableDisplayName = if (struckCount > 0) {
        "$baseTable ($struckCount/${order.items.size} món xong)"
    } else {
        "$baseTable ($totalItemCount món)"
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, LightBorderStrong, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header: Table Display Name (kèm tổng số lượng món) & Status & Elapsed Time
            val (statusBg, statusBorder, statusText, statusTextColor) = when (order.status) {
                "pending" -> Quadruple(StatusPendingBg, StatusPendingBorder, "CHỜ BẾP DUYỆT", StatusPendingText)
                else -> Quadruple(StatusCookingBg, StatusCookingBorder, "ĐANG NẤU", StatusCookingText)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusBg)
                    .border(0.5.dp, statusBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = tableDisplayName,
                        color = TextPrimary,
                        fontSize = if (isLandscape) 12.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Giờ đặt: $orderTimeStr",
                        color = TextSecondary,
                        fontSize = if (isLandscape) 8.5.sp else 10.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LightSurface)
                            .border(0.8.dp, statusBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = if (order.status == "pending") "CHỜ BẾP" else "ĐANG NẤU",
                            color = statusTextColor,
                            fontSize = if (isLandscape) 8.5.sp else 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Time Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLateWarning) StatusAlertBg else LightSurface)
                            .border(0.8.dp, if (isLateWarning) StatusAlertText else LightBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = timeAgoText,
                            color = if (isLateWarning) StatusAlertText else TextPrimary,
                            fontSize = if (isLandscape) 8.5.sp else 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            // Items Breakdown (Ấn giữ để gạch món đã nấu xong - hiển thị gọn trên 1 dòng)
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                order.items.forEach { item ->
                    val itemKey = if (item.id.isNotBlank()) item.id else "${order.id}_${item.dish_name}"
                    val isStruck = struckItemKeys.contains(itemKey) || item.is_completed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .combinedClickable(
                                onClick = {
                                    if (canKitchenOperate) {
                                        onShowToast("💡 Ấn giữ để gạch hoàn thành: ${item.dish_name}")
                                    }
                                },
                                onLongClick = {
                                    if (canKitchenOperate) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleItem(item)
                                    }
                                }
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Tên món và số lượng rõ ràng, không bị cắt
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "• ${item.quantity}x ${item.dish_name}",
                                    color = if (isStruck) TextMuted else TextPrimary,
                                    fontSize = if (isLandscape) 11.5.sp else 13.5.sp,
                                    fontWeight = if (isStruck) FontWeight.Normal else FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (isStruck) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            // Topping & Các món thành phần của Combo (Chuẩn giao diện như Web: dạng dòng text có bullet, thụt lề, màu amber-700)
                            val summaryText = when {
                                item.combo_summary.isNotBlank() -> item.combo_summary
                                item.options_summary.isNotBlank() -> item.options_summary
                                else -> {
                                    val dish = allDishes.find { it.id == item.dish_id || it.name.equals(item.dish_name, ignoreCase = true) }
                                    if (dish?.is_combo == true && dish.combo_groups.isNotEmpty()) {
                                        dish.combo_groups.flatMap { grp ->
                                            grp.items.take(grp.min.coerceAtLeast(1)).map { ci -> "• ${ci.name}" }
                                        }.joinToString("\n")
                                    } else ""
                                }
                            }
                            if (summaryText.isNotBlank()) {
                                val lines = if (summaryText.contains("\n")) summaryText.split("\n") else summaryText.split(", ")
                                lines.filter { it.isNotBlank() }.forEach { line ->
                                    val cleanLine = line.trim().removePrefix("+").removePrefix("•").trim()
                                    Text(
                                        text = "    • $cleanLine",
                                        color = if (isStruck) TextMuted.copy(alpha = 0.6f) else Color(0xFFB45309),
                                        fontSize = if (isLandscape) 10.sp else 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (isStruck) TextDecoration.LineThrough else null,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                            // Ghi chú của món ăn (Note) nổi bật để Bếp không bao giờ bỏ sót
                            if (item.note.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .padding(start = 6.dp, top = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isStruck) LightSurfaceSubtle else Color(0xFFFEF3C7))
                                        .border(0.5.dp, if (isStruck) LightBorder else Color(0xFFFCD34D), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ghi chú: ${item.note}",
                                        color = if (isStruck) TextMuted else Color(0xFF92400E),
                                        fontSize = if (isLandscape) 10.sp else 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        textDecoration = if (isStruck) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }
                }

                if (canKitchenOperate) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = LightBorder
                    )

                    if (order.status == "pending") {
                        // NÚT "BẮT ĐẦU NẤU" GỌN NHẸ KHÔNG ICON
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 32.dp else 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB))
                                .clickable { onUpdateStatus(order.id, "cooking") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bắt đầu nấu",
                                color = Color.White,
                                fontSize = if (isLandscape) 12.sp else 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (order.status == "cooking") {
                        // NÚT "RA MÓN XONG" GỌN NHẸ KHÔNG ICON
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 32.dp else 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981))
                                .clickable { onUpdateStatus(order.id, "done") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ra món xong",
                                color = Color.White,
                                fontSize = if (isLandscape) 12.sp else 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
