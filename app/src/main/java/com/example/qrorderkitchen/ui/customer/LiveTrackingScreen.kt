package com.example.qrorderkitchen.ui.customer

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.qrorderkitchen.data.models.StaffRole
import com.example.qrorderkitchen.models.Customer
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.models.Order
import com.example.qrorderkitchen.models.OrderItem
import com.example.qrorderkitchen.models.TableModel
import com.example.qrorderkitchen.models.Voucher
import com.example.qrorderkitchen.ui.components.ThermalReceiptDialog
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightSurface
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.ui.theme.WebBorder
import com.example.qrorderkitchen.ui.theme.WebCardBg
import com.example.qrorderkitchen.ui.theme.WebCreamBg
import com.example.qrorderkitchen.ui.theme.WebGoldDark
import com.example.qrorderkitchen.ui.theme.WebGoldPrimary
import com.example.qrorderkitchen.ui.theme.WebSurfacePill
import com.example.qrorderkitchen.ui.theme.WebTextMuted
import com.example.qrorderkitchen.ui.theme.WebTextPrimary
import com.example.qrorderkitchen.ui.theme.WebTextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AppMode
import com.example.qrorderkitchen.ui.viewmodel.AuthState
import com.example.qrorderkitchen.ui.viewmodel.AuthViewModel
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import com.example.qrorderkitchen.ui.viewmodel.CustomerTab
import com.example.qrorderkitchen.utils.CurrencyUtils
import com.example.qrorderkitchen.utils.TableUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveTrackingScreen(
    viewModel: AuroraViewModel,
    authViewModel: AuthViewModel? = null
) {
    val tableId by viewModel.currentTableId.collectAsState()
    val allOrders by viewModel.activeOrders.collectAsState()
    val allTables by viewModel.firestoreTables.collectAsState()
    val showReceipt by viewModel.showReceiptModal.collectAsState()

    val authState by authViewModel?.authState?.collectAsState() ?: remember { mutableStateOf(AuthState.Unauthenticated) }
    val currentStaff = (authState as? AuthState.Authenticated)?.staff
    val currentStaffUsername = currentStaff?.username ?: ""
    val currentStaffName = currentStaff?.name ?: ""
    val currentStaffRole = currentStaff?.staffRole ?: StaffRole.WAITER
    val canCancelDish = currentStaffRole == StaffRole.MANAGER

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val cleanTable = remember(tableId) { TableUtils.cleanTableCode(tableId) }

    // Sắp xếp đơn mới nhất lên trên đầu (giảm dần theo thời gian tạo)
    val tableOrders = remember(allOrders, cleanTable) {
        allOrders.filter {
            TableUtils.isSameTable(it.table_id, cleanTable) && it.status != "cancelled"
        }.sortedByDescending { it.created_at?.toDate()?.time ?: Long.MIN_VALUE }
    }

    var selectedFilter by remember { mutableStateOf("ALL") }

    val inProgressOrders = remember(tableOrders) { tableOrders.filter { it.status == "pending" || it.status == "cooking" } }
    val doneOrders = remember(tableOrders) { tableOrders.filter { it.status == "done" || it.status == "completed" || it.status == "served" } }

    val displayOrders = remember(selectedFilter, inProgressOrders, doneOrders, tableOrders) {
        when (selectedFilter) {
            "IN_PROGRESS" -> inProgressOrders
            "DONE" -> doneOrders
            else -> tableOrders
        }
    }

    val totalSpent = remember(tableOrders) { tableOrders.sumOf { it.total_amount } }
    val totalItemsCount = remember(tableOrders) { tableOrders.sumOf { it.items.sumOf { item -> item.quantity } } }

    val isEn = Locale.getDefault().language == "en"
    val labelAll = remember(isEn, tableOrders.size) { if (isEn) "All (${tableOrders.size})" else "Tất cả (${tableOrders.size})" }
    val labelInProgress = remember(isEn, inProgressOrders.size) { if (isEn) "In Progress (${inProgressOrders.size})" else "Đang làm (${inProgressOrders.size})" }
    val labelDone = remember(isEn, doneOrders.size) { if (isEn) "Done (${doneOrders.size})" else "Hoàn thành (${doneOrders.size})" }

    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
    }


    val coroutineScope = rememberCoroutineScope()
    val attachedCustomers by viewModel.attachedCustomers.collectAsState()
    val appliedVouchers by viewModel.appliedVouchers.collectAsState()
    val usePointsForTable by viewModel.usePointsForTable.collectAsState()
    val allDishes by viewModel.dishes.collectAsState()

    val currentCustomer = attachedCustomers[cleanTable]
    val currentVoucher = appliedVouchers[cleanTable]
    val isUsingPoints = usePointsForTable[cleanTable] ?: false

    var showCustomerDialog by remember { mutableStateOf(false) }
    var voucherInput by remember { mutableStateOf("") }
    var voucherError by remember { mutableStateOf<String?>(null) }
    var isValidatingVoucher by remember { mutableStateOf(false) }

    // Voucher Discount calculation
    val voucherDiscount = remember(currentVoucher, totalSpent, tableOrders) {
        val v = currentVoucher ?: return@remember 0.0
        if (totalSpent < v.min_bill) return@remember 0.0
        val vVal = if (v.value > 0.0) v.value else v.discount_amount
        when (v.type.lowercase()) {
            "fixed" -> minOf(totalSpent, vVal)
            "percent" -> {
                val raw = totalSpent * (vVal / 100.0)
                if (v.max_discount > 0.0) minOf(raw, v.max_discount) else raw
            }
            "dish" -> {
                val allItems = tableOrders.flatMap { it.items }
                val matched = allItems.firstOrNull {
                    (!v.specific_dish_id.isNullOrBlank() && it.dish_id == v.specific_dish_id) ||
                    (!v.specific_dish_name.isNullOrBlank() && it.dish_name.equals(v.specific_dish_name, ignoreCase = true))
                }
                if (matched != null) matched.price else 0.0
            }
            else -> minOf(totalSpent, vVal)
        }
    }

    val billAfterVoucher = (totalSpent - voucherDiscount).coerceAtLeast(0.0)
    val customerPoints = currentCustomer?.points ?: 0.0
    val maxUsablePoints = minOf(customerPoints, billAfterVoucher)
    val pointsDeducted = if (isUsingPoints) maxUsablePoints else 0.0
    val grandTotal = (billAfterVoucher - pointsDeducted).coerceAtLeast(0.0)

    // Single Dish Cancel Dialog State (Đồng bộ Web - Quản lý xóa món lẻ)
    var itemToCancel by remember { mutableStateOf<Triple<Order, Int, OrderItem>?>(null) }
    var cancelQtyInput by remember { mutableStateOf("1") }
    var cancelReasonInput by remember { mutableStateOf("Khách đổi ý / Gọi nhầm") }

    // Settle & Receipt Dialog
    if (showReceipt != null) {
        ThermalReceiptDialog(
            tableId = tableId,
            orders = showReceipt ?: emptyList(),
            isWaiterOnly = (currentStaffRole == StaffRole.WAITER),
            attachedCustomer = currentCustomer,
            preAppliedVoucher = currentVoucher,
            preUsePoints = isUsingPoints,
            allowVoucherInput = false,
            onValidateVoucher = { code -> viewModel.getVoucherByCode(code, cleanTable) },
            onApplyVoucher = { v -> viewModel.applyVoucherToTable(cleanTable, v) },
            onCancelVoucher = { viewModel.applyVoucherToTable(cleanTable, null) },
            onPrintOnly = {
                viewModel.showToast("Đã gửi lệnh in bill tạm tính cho Bàn $tableId")
                viewModel.showReceiptModal.value = null
            },
            onSettlePayment = { paymentMethod, voucherCode, customer, pointsUsed, netPaid, billTotal ->
                viewModel.settleTableWithDetails(
                    tableId = tableId,
                    paymentMethod = paymentMethod,
                    voucherCode = voucherCode,
                    customer = customer,
                    pointsUsed = pointsUsed,
                    netPaid = netPaid,
                    billTotal = billTotal
                )
            },
            onDismiss = { viewModel.showReceiptModal.value = null }
        )
    }

    if (showCustomerDialog) {
        CustomerLoyaltyPopupDialog(
            tableId = cleanTable,
            currentCustomer = currentCustomer,
            isUsingPoints = isUsingPoints,
            maxUsablePoints = maxUsablePoints,
            onUsePointsToggle = { viewModel.setUsePointsForTable(cleanTable, it) },
            onAttachCustomer = { cust -> viewModel.attachCustomerToTable(cleanTable, cust) },
            onDetachCustomer = { viewModel.detachCustomerFromTable(cleanTable) },
            viewModel = viewModel,
            onDismiss = { showCustomerDialog = false }
        )
    }

    // Modal Xác nhận Hủy Món Lẻ
    itemToCancel?.let { (order, itemIdx, item) ->
        val maxQty = item.quantity
        Dialog(onDismissRequest = { itemToCancel = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.55f else 0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFFECDD3), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "XÁC NHẬN HỦY MÓN (QUẢN LÝ)",
                            color = Color(0xFFBE123C),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF1F2))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "Bàn $tableId", color = Color(0xFFBE123C), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFFEE2E2))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Món: ${item.dish_name}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Số lượng hiện có: $maxQty phần · Đơn giá: ${CurrencyUtils.formatVnd(item.price)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (maxQty > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Số lượng muốn hủy (1 - $maxQty):", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            OutlinedTextField(
                                value = cancelQtyInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    if (filtered.isBlank()) {
                                        cancelQtyInput = ""
                                    } else {
                                        val num = filtered.toIntOrNull() ?: 1
                                        if (num in 1..maxQty) {
                                            cancelQtyInput = filtered
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Lý do hủy:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        OutlinedTextField(
                            value = cancelReasonInput,
                            onValueChange = { cancelReasonInput = it },
                            placeholder = { Text("Nhập lý do hủy món...", fontSize = 12.5.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { itemToCancel = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Hủy bỏ", color = TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val qtyToCancel = cancelQtyInput.toIntOrNull() ?: 1
                                val reason = cancelReasonInput.trim().ifBlank { "Khách đổi ý / Gọi nhầm" }
                                viewModel.cancelSingleDish(
                                    orderId = order.id,
                                    itemIndex = itemIdx,
                                    cancelQty = qtyToCancel,
                                    reason = reason,
                                    staffName = currentStaffName.ifBlank { "Quản lý" },
                                    staffRole = currentStaffRole
                                )
                                itemToCancel = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE123C))
                        ) {
                            Text(text = "Xác nhận hủy", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WebCreamBg)
    ) {
        // ================= HEADER & BỘ LỌC =================
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                        tint = WebGoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hóa đơn Bàn $tableId",
                        color = WebTextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(WebSurfacePill)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${tableOrders.size} đợt gọi",
                            color = WebTextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChipItem(
                        label = labelAll,
                        isSelected = selectedFilter == "ALL",
                        isLandscape = true,
                        onClick = { selectedFilter = "ALL" }
                    )
                    FilterChipItem(
                        label = labelInProgress,
                        isSelected = selectedFilter == "IN_PROGRESS",
                        dotColor = Color(0xFFF59E0B),
                        isLandscape = true,
                        onClick = { selectedFilter = "IN_PROGRESS" }
                    )
                    FilterChipItem(
                        label = labelDone,
                        isSelected = selectedFilter == "DONE",
                        dotColor = Color(0xFF10B981),
                        isLandscape = true,
                        onClick = { selectedFilter = "DONE" }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                        tint = WebGoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hóa đơn Bàn $tableId",
                        color = WebTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(WebSurfacePill)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${tableOrders.size} đợt gọi",
                        color = WebTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = labelAll,
                    isSelected = selectedFilter == "ALL",
                    isLandscape = false,
                    onClick = { selectedFilter = "ALL" }
                )
                FilterChipItem(
                    label = labelInProgress,
                    isSelected = selectedFilter == "IN_PROGRESS",
                    dotColor = Color(0xFFF59E0B),
                    isLandscape = false,
                    onClick = { selectedFilter = "IN_PROGRESS" }
                )
                FilterChipItem(
                    label = labelDone,
                    isSelected = selectedFilter == "DONE",
                    dotColor = Color(0xFF10B981),
                    isLandscape = false,
                    onClick = { selectedFilter = "DONE" }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ================= DANH SÁCH ĐƠN HÀNG HOẶC EMPTY STATE =================
        if (tableOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(if (isLandscape) 0.6f else 1f)
                        .border(1.dp, WebBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = WebCardBg),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = if (isLandscape) 24.dp else 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isLandscape) 56.dp else 68.dp)
                                .clip(CircleShape)
                                .background(WebSurfacePill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = WebGoldDark,
                                modifier = Modifier.size(if (isLandscape) 28.dp else 34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Bàn $tableId chưa có đơn nào",
                            color = WebTextPrimary,
                            fontSize = if (isLandscape) 15.sp else 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Các món đã gửi bếp sẽ hiển thị và cập nhật trạng thái nấu trực tiếp tại đây.",
                            color = WebTextMuted,
                            fontSize = if (isLandscape) 12.sp else 14.sp,
                            lineHeight = if (isLandscape) 16.sp else 19.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.setCustomerTab(CustomerTab.MENU) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WebGoldPrimary,
                                contentColor = Color.White
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = if (isLandscape) 8.dp else 11.dp)
                        ) {
                            Text(
                                text = "Xem Thực đơn & Gọi món",
                                fontSize = if (isLandscape) 12.sp else 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (displayOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                        tint = WebTextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Không có đơn hàng nào ở trạng thái này",
                        color = WebTextMuted,
                        fontSize = if (isLandscape) 12.sp else 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // LƯỚI HIỂN THỊ ĐƠN HÀNG: 2 CỘT KHI NẰM NGANG, 1 CỘT KHI NẰM DỌC
            val gridCols = if (isLandscape) 2 else 1
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(displayOrders, key = { _, item -> item.id }) { _, order ->
                    OrderTrackingCard(
                        order = order,
                        timeFormat = timeFormat,
                        isLandscape = isLandscape,
                        isEn = isEn,
                        canCancelDish = canCancelDish,
                        allDishes = allDishes,
                        onCancelDishItem = { itemIdx, item ->
                            itemToCancel = Triple(order, itemIdx, item)
                            cancelQtyInput = "1"
                            cancelReasonInput = "Khách đổi ý / Gọi nhầm"
                        }
                    )
                }
            }

            // ================= THANH TỔNG PHIÊN BÀN & NÚT NGHIỆP VỤ (ĐỒNG BỘ WEB) =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.8.dp, WebBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                colors = CardDefaults.cardColors(containerColor = WebCardBg),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = if (isLandscape) 8.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dòng 1: TỔNG PHIÊN & GIẢM TRỪ VOUCHER / ĐIỂM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TỔNG PHIÊN (BÀN $tableId)",
                                color = WebTextSecondary,
                                fontSize = if (isLandscape) 11.sp else 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (voucherDiscount > 0.0 || pointsDeducted > 0.0) {
                                Text(
                                    text = listOfNotNull(
                                        if (voucherDiscount > 0.0) "Voucher: -${CurrencyUtils.formatVnd(voucherDiscount)}" else null,
                                        if (pointsDeducted > 0.0) "Điểm: -${CurrencyUtils.formatVnd(pointsDeducted)}" else null
                                    ).joinToString(" · "),
                                    color = Color(0xFF047857),
                                    fontSize = if (isLandscape) 10.sp else 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = "${tableOrders.size} đợt gọi · $totalItemsCount món",
                                    color = WebTextMuted,
                                    fontSize = if (isLandscape) 10.sp else 12.sp
                                )
                            }
                        }

                        Text(
                            text = CurrencyUtils.formatVnd(grandTotal),
                            color = WebGoldDark,
                            fontSize = if (isLandscape) 16.sp else 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dòng 2: HÀNG TÁC VỤ ĐỒNG BỘ WEB (Icon Thành Viên + Ô Nhập Voucher)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Thành Viên (Ấn mở Popup tra cứu SĐT / Xem điểm / Tích điểm)
                        Box(
                            modifier = Modifier
                                .size(if (isLandscape) 40.dp else 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentCustomer != null) Color(0xFFFEF3C7) else Color.White)
                                .border(1.dp, if (currentCustomer != null) AuroraGold else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                                .clickable { showCustomerDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Tài khoản khách",
                                tint = if (currentCustomer != null) AuroraGoldHover else Color(0xFF6B7280),
                                modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                            )
                            // Chấm xanh báo hiệu đã gắn tài khoản
                            if (currentCustomer != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }

                        // Ô Nhập Voucher Rộng & Nút Áp Dụng
                        if (currentVoucher == null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = voucherInput,
                                        onValueChange = {
                                            voucherInput = it.uppercase()
                                            voucherError = null
                                        },
                                        placeholder = { Text("Nhập mã Voucher...", fontSize = 11.5.sp, color = Color(0xFF9CA3AF)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(if (isLandscape) 40.dp else 44.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = AuroraGold,
                                            unfocusedBorderColor = Color(0xFFE5E7EB),
                                            focusedTextColor = Color(0xFF1F2937),
                                            unfocusedTextColor = Color(0xFF1F2937)
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            val code = voucherInput.trim().uppercase()
                                            if (code.isBlank()) {
                                                val err = "Vui lòng nhập mã voucher!"
                                                voucherError = err
                                                viewModel.showToast(err)
                                                return@Button
                                            }
                                            isValidatingVoucher = true
                                            voucherError = null
                                            coroutineScope.launch {
                                                val v = viewModel.getVoucherByCode(code, cleanTable)
                                                isValidatingVoucher = false
                                                if (v == null) {
                                                    val err = "Mã voucher \"$code\" không tồn tại trong hệ thống!"
                                                    voucherError = err
                                                    viewModel.showToast(err)
                                                    return@launch
                                                }
                                                val allItems = tableOrders.flatMap { it.items }
                                                val evalResult = v.validate(totalSpent, allItems, cleanTable)
                                                if (!evalResult.isValid) {
                                                    voucherError = evalResult.message
                                                    viewModel.showToast(evalResult.message)
                                                    return@launch
                                                }
                                                voucherError = null
                                                viewModel.applyVoucherToTable(cleanTable, v)
                                                viewModel.showToast(evalResult.message)
                                                voucherInput = ""
                                            }
                                        },
                                        enabled = !isValidatingVoucher && voucherInput.isNotBlank(),
                                        modifier = Modifier.height(if (isLandscape) 40.dp else 44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraGold)
                                    ) {
                                        if (isValidatingVoucher) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text("Áp dụng", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                        }
                                    }
                                }
                                if (!voucherError.isNullOrBlank()) {
                                    Text(
                                        text = voucherError!!,
                                        color = Color(0xFFDC2626),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                                    )
                                }
                            }
                        } else {
                            // Đã áp dụng Voucher
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isLandscape) 40.dp else 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFECFDF5))
                                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val voucherCodeDisplay = if (currentVoucher.code.isNotBlank()) currentVoucher.code else currentVoucher.id
                                    Text(
                                        text = "Voucher: $voucherCodeDisplay",
                                        color = Color(0xFF047857),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "(-${CurrencyUtils.formatVnd(voucherDiscount)})",
                                        color = Color(0xFF065F46),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "Hủy",
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            voucherError = null
                                            viewModel.applyVoucherToTable(cleanTable, null)
                                            viewModel.showToast("Đã hủy áp dụng voucher!")
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Dòng 3: [Hóa đơn thanh toán] (Chuẩn Web)
                    Button(
                        onClick = {
                            viewModel.showReceiptModal.value = tableOrders
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) 38.dp else 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraGold)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Receipt",
                            tint = Color.White,
                            modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hóa đơn thanh toán",
                            color = Color.White,
                            fontSize = if (isLandscape) 12.sp else 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderTrackingCard(
    order: Order,
    timeFormat: SimpleDateFormat,
    isLandscape: Boolean,
    isEn: Boolean = false,
    canCancelDish: Boolean = false,
    allDishes: List<Dish> = emptyList(),
    onCancelDishItem: ((Int, OrderItem) -> Unit)? = null
) {
    val orderDate = order.created_at?.toDate() ?: Date()
    val orderTimeStr = timeFormat.format(orderDate)

    val (statusText, statusBg, statusColor) = when (order.status) {
        "pending" -> Triple(if (isEn) "Pending" else "Chờ duyệt", Color(0xFFFEF3C7), Color(0xFFB45309))
        "cooking" -> Triple(if (isEn) "Cooking" else "Đang nấu", Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        "done", "completed", "served" -> Triple(if (isEn) "Done" else "Hoàn thành", Color(0xFFDCFCE7), Color(0xFF15803D))
        else -> Triple(if (isEn) "Settled" else "Hoàn tất", Color(0xFFF3F4F6), Color(0xFF374151))
    }

    val isOrderActive = order.status !in listOf("settled", "paid", "transferred", "cancelled")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, WebBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = WebCardBg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isLandscape) 8.dp else 11.dp)) {
            // Header: Mã Đơn + Thời gian & Tag Trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "# ĐƠN ${order.id.takeLast(6).uppercase()}",
                        color = WebTextSecondary,
                        fontSize = if (isLandscape) 11.5.sp else 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " · $orderTimeStr",
                        color = WebTextMuted,
                        fontSize = if (isLandscape) 10.5.sp else 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusBg)
                        .padding(horizontal = if (isLandscape) 7.dp else 10.dp, vertical = if (isLandscape) 2.dp else 3.5.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = if (isLandscape) 10.sp else 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = if (isLandscape) 5.dp else 7.dp),
                color = WebBorder,
                thickness = 0.5.dp
            )

            // Danh sách món trong đơn (gọn gàng, rõ ràng, có nút xóa món lẻ cho Quản lý)
            Column(verticalArrangement = Arrangement.spacedBy(if (isLandscape) 4.dp else 6.dp)) {
                order.items.forEachIndexed { itemIdx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.dish_name,
                                color = WebTextPrimary,
                                fontSize = if (isLandscape) 11.5.sp else 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "  ×${item.quantity}",
                                color = WebGoldDark,
                                fontSize = if (isLandscape) 11.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = CurrencyUtils.formatVnd(item.price * item.quantity),
                                color = WebTextSecondary,
                                fontSize = if (isLandscape) 11.sp else 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Nút xóa món lẻ cho Quản lý
                            if (canCancelDish && isOrderActive && onCancelDishItem != null) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isLandscape) 22.dp else 26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFF1F2))
                                        .border(0.8.dp, Color(0xFFFECDD3), RoundedCornerShape(6.dp))
                                        .clickable { onCancelDishItem(itemIdx, item) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hủy món",
                                        tint = Color(0xFFBE123C),
                                        modifier = Modifier.size(if (isLandscape) 12.dp else 14.dp)
                                    )
                                }
                            }
                        }
                    }
                    val displaySummary = when {
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
                    if (displaySummary.isNotBlank()) {
                        val lines = if (displaySummary.contains("\n")) displaySummary.split("\n") else displaySummary.split(", ")
                        lines.filter { it.isNotBlank() }.forEach { line ->
                            val cleanLine = line.trim().removePrefix("+").removePrefix("•").trim()
                            Text(
                                text = "    • $cleanLine",
                                color = Color(0xFFB45309),
                                fontSize = if (isLandscape) 10.sp else 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = "  ↳ Ghi chú: ${item.note}",
                            color = WebTextMuted,
                            fontSize = if (isLandscape) 9.5.sp else 11.5.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = if (isLandscape) 5.dp else 7.dp, bottom = if (isLandscape) 4.dp else 6.dp),
                color = WebBorder.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // Dòng Tổng đơn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng đợt:",
                    color = WebTextSecondary,
                    fontSize = if (isLandscape) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = CurrencyUtils.formatVnd(order.total_amount),
                    color = WebGoldDark,
                    fontSize = if (isLandscape) 12.5.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    isLandscape: Boolean,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    val bg = if (isSelected) WebGoldPrimary else WebCardBg
    val textColor = if (isSelected) Color.White else WebTextSecondary
    val border = if (isSelected) WebGoldPrimary else WebBorder

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = if (isLandscape) 10.dp else 14.dp, vertical = if (isLandscape) 4.dp else 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 6.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = if (isLandscape) 10.5.sp else 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun CustomerLoyaltyPopupDialog(
    tableId: String,
    currentCustomer: Customer?,
    isUsingPoints: Boolean,
    maxUsablePoints: Double,
    onUsePointsToggle: (Boolean) -> Unit,
    onAttachCustomer: (Customer) -> Unit,
    onDetachCustomer: () -> Unit,
    viewModel: AuroraViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchPhone by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    var regName by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val doSearch: () -> Unit = {
        val phoneClean = searchPhone.trim()
        if (phoneClean.isBlank()) {
            searchError = "Vui lòng nhập số điện thoại"
        } else {
            isSearching = true
            searchError = null
            coroutineScope.launch {
                val cust = viewModel.searchCustomerByPhone(phoneClean)
                isSearching = false
                if (cust != null) {
                    onAttachCustomer(cust)
                    viewModel.showToast("Đã nhận diện hội viên: ${cust.name}")
                    searchPhone = ""
                    hasSearched = false
                } else {
                    hasSearched = true
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.55f else 0.94f)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Hội viên",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tài Khoản Hội Viên - Bàn $tableId",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "Tra cứu SĐT & tích lũy điểm thưởng",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF3F4F6))

                // If customer is already attached
                if (currentCustomer != null) {
                    val tierDisplay = when (currentCustomer.tier.lowercase()) {
                        "diamond", "kim cuong", "kim cương" -> "Kim Cương"
                        "gold", "vang", "vàng" -> "Vàng"
                        "silver", "bac", "bạc" -> "Bạc"
                        else -> "Thành viên"
                    }
                    val (tierBg, tierTextColor) = when (currentCustomer.tier.lowercase()) {
                        "diamond", "kim cuong", "kim cương" -> Color(0xFFE0E7FF) to Color(0xFF3730A3)
                        "gold", "vang", "vàng" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
                        "silver", "bac", "bạc" -> Color(0xFFF3F4F6) to Color(0xFF4B5563)
                        else -> Color(0xFFECFDF5) to Color(0xFF047857)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFCFAF7))
                            .border(1.dp, AuroraGoldSoft, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentCustomer.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1F2937)
                                )
                                Text(
                                    text = currentCustomer.phone,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(tierBg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = tierDisplay,
                                        color = tierTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Icon Đăng xuất góc trên phải cùng thẻ tên khách
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF1F2))
                                        .border(0.8.dp, Color(0xFFFECDD3), RoundedCornerShape(8.dp))
                                        .clickable {
                                            onDetachCustomer()
                                            viewModel.showToast("Đã đăng xuất tài khoản khỏi Bàn $tableId")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Đăng xuất",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE5E7EB))

                        // Khung Điểm Khả Dụng (Nổi bật, to rõ, có biểu tượng riêng biệt không nhầm với tiền chi tiêu)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFFBEB))
                                .border(1.2.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = "Điểm",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "ĐIỂM KHẢ DỤNG",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = CurrencyUtils.formatPlain(currentCustomer.points),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        color = Color(0xFFB45309)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "điểm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        // Khung Tổng chi tiêu (Thiết kế thanh ngang gọn gàng, không bị đè chữ)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF9FAFB))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = "Chi tiêu",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tổng chi tiêu:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4B5563),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = CurrencyUtils.formatVnd(currentCustomer.total_spend),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1F2937),
                                maxLines = 1
                            )
                        }

                        // Use Points Button (Nút duy nhất, không còn nút đăng xuất to bên dưới)
                        if (currentCustomer.points > 0) {
                            Button(
                                onClick = { onUsePointsToggle(!isUsingPoints) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isUsingPoints) Color(0xFFDC2626) else AuroraGoldHover
                                )
                            ) {
                                Text(
                                    text = if (isUsingPoints) "Bỏ dùng điểm tích lũy" else "Dùng ${CurrencyUtils.formatPlain(maxUsablePoints)} điểm",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                } else {
                    // Not attached: Search or Register
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchPhone,
                                onValueChange = {
                                    searchPhone = it.filter { c -> c.isDigit() }
                                    hasSearched = false
                                    searchError = null
                                },
                                placeholder = { Text("Nhập SĐT khách hàng...", fontSize = 12.sp, color = Color(0xFF9CA3AF)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuroraGold,
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )

                            Button(
                                onClick = doSearch,
                                enabled = !isSearching && searchPhone.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraGold),
                                modifier = Modifier.height(48.dp)
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Tìm kiếm", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1F2937))
                                }
                            }
                        }

                        if (searchError != null) {
                            Text(text = searchError!!, color = Color(0xFFDC2626), fontSize = 11.5.sp)
                        }

                        if (hasSearched) {
                            // Register New Customer
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Số điện thoại $searchPhone chưa có tài khoản. Đăng ký ngay:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E)
                                )
                                OutlinedTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    placeholder = { Text("Họ và tên khách hàng", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = AuroraGold
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (regName.isBlank()) {
                                            viewModel.showToast("Vui lòng nhập tên khách hàng!")
                                            return@Button
                                        }
                                        isRegistering = true
                                        coroutineScope.launch {
                                            val created = viewModel.registerCustomer(searchPhone.trim(), regName.trim())
                                            isRegistering = false
                                            if (created != null) {
                                                onAttachCustomer(created)
                                                viewModel.showToast("Đã tạo tài khoản và nhận diện: ${created.name}")
                                                searchPhone = ""
                                                regName = ""
                                                hasSearched = false
                                            } else {
                                                viewModel.showToast("Lỗi tạo tài khoản hội viên!")
                                            }
                                        }
                                    },
                                    enabled = !isRegistering && regName.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraGoldHover)
                                ) {
                                    if (isRegistering) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Đăng ký & Áp dụng", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF4B5563))
                ) {
                    Text(text = "Đóng", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
