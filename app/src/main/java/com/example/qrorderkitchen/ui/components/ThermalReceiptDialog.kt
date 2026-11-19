package com.example.qrorderkitchen.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qrorderkitchen.models.Customer
import com.example.qrorderkitchen.models.Order
import com.example.qrorderkitchen.models.Voucher
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.utils.CurrencyUtils
import com.example.qrorderkitchen.utils.TableUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFD1D5DB),
    dashWidth: Float = 6f,
    gapWidth: Float = 4f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun ThermalReceiptDialog(
    tableId: String,
    orders: List<Order>,
    isWaiterOnly: Boolean = false,
    attachedCustomer: Customer? = null,
    preAppliedVoucher: Voucher? = null,
    preUsePoints: Boolean = false,
    allowVoucherInput: Boolean = false,
    onValidateVoucher: suspend (code: String) -> Voucher?,
    onApplyVoucher: ((Voucher) -> Unit)? = null,
    onCancelVoucher: (() -> Unit)? = null,
    onPrintOnly: () -> Unit,
    onSettlePayment: (paymentMethod: String, voucherCode: String?, customer: Customer?, pointsUsed: Double, netPaid: Double, billTotal: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Group items across rounds (distinct by dish + options)
    val groupedItems = remember(orders) {
        val allItems = orders.flatMap { it.items }
        allItems.groupBy { "${it.dish_name}__${it.options_summary}__${it.price}" }.map { (_, items) ->
            val first = items.first()
            val totalQty = items.sumOf { it.quantity }
            Triple(first, totalQty, first.price)
        }
    }

    val totalAmount = remember(orders) { orders.sumOf { it.total_amount } }
    val timeFormatted = remember { SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val dateCode = remember { SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date()) }
    val cleanTable = remember(tableId) { TableUtils.cleanTableCode(tableId) }

    // Voucher State
    var voucherInput by remember { mutableStateOf("") }
    var appliedVoucher by remember(preAppliedVoucher) { mutableStateOf<Voucher?>(preAppliedVoucher) }
    var voucherError by remember { mutableStateOf<String?>(null) }
    var isValidatingVoucher by remember { mutableStateOf(false) }

    // Points State
    var usePoints by remember(preUsePoints) { mutableStateOf(preUsePoints) }

    // Voucher Discount calculation
    val voucherDiscount = remember(appliedVoucher, totalAmount, orders) {
        val v = appliedVoucher ?: return@remember 0.0
        if (totalAmount < v.min_bill) return@remember 0.0
        val vVal = if (v.value > 0.0) v.value else v.discount_amount
        when (v.type.lowercase()) {
            "fixed" -> minOf(totalAmount, vVal)
            "percent" -> {
                val raw = totalAmount * (vVal / 100.0)
                if (v.max_discount > 0.0) minOf(raw, v.max_discount) else raw
            }
            "dish" -> {
                val allItems = orders.flatMap { it.items }
                val matched = allItems.firstOrNull {
                    (!v.specific_dish_id.isNullOrBlank() && it.dish_id == v.specific_dish_id) ||
                    (!v.specific_dish_name.isNullOrBlank() && it.dish_name.equals(v.specific_dish_name, ignoreCase = true))
                }
                if (matched != null) matched.price else 0.0
            }
            else -> minOf(totalAmount, vVal)
        }
    }

    val billAfterVoucher = (totalAmount - voucherDiscount).coerceAtLeast(0.0)

    // Loyalty Points calculation: 1 diem = 1 vnd
    val customerPoints = attachedCustomer?.points ?: 0.0
    val maxUsablePoints = minOf(customerPoints, billAfterVoucher)
    val pointsDeducted = if (usePoints) maxUsablePoints else 0.0
    val netTotal = (billAfterVoucher - pointsDeducted).coerceAtLeast(0.0)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.52f else 0.94f)
                .fillMaxHeight(if (isLandscape) 0.96f else 0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. HEADER ROW (Dong bo voi ban Web)
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
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Hóa đơn",
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Hóa Đơn Thanh Toán - Bàn $cleanTable",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1F2937)
                        )
                        Text(
                            text = "Xem trước hóa đơn & hoàn tất phiên",
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

            // 2. KHUNG TỔNG HỢP VOUCHER & HỘI VIÊN (Chỉ hiện khi có dữ liệu hoặc được phép nhập)
            if (allowVoucherInput || appliedVoucher != null || attachedCustomer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFCFAF7))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Dong A: Hoi vien & Tich diem
                    if (attachedCustomer != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Hội viên",
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = attachedCustomer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1F2937)
                                    )
                                    Text(
                                        text = "Điểm khả dụng: ${CurrencyUtils.formatPlain(customerPoints)} đ",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }

                            if (customerPoints > 0 && billAfterVoucher > 0) {
                                if (!usePoints) {
                                    Button(
                                        onClick = { usePoints = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraGoldHover),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = "Dùng ${CurrencyUtils.formatPlain(maxUsablePoints)}đ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { usePoints = false },
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = "Bỏ dùng điểm",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        if (appliedVoucher != null || allowVoucherInput) {
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.6.dp)
                        }
                    }

                    // Dong B: Nhap ma Voucher
                    if (appliedVoucher == null && allowVoucherInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ConfirmationNumber,
                                    contentDescription = "Voucher",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            OutlinedTextField(
                                value = voucherInput,
                                onValueChange = {
                                    voucherInput = it.uppercase()
                                    voucherError = null
                                },
                                placeholder = { Text("Nhập mã Voucher...", fontSize = 11.5.sp, color = Color(0xFF9CA3AF)) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
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
                                        voucherError = "Vui lòng nhập mã voucher!"
                                        return@Button
                                    }
                                    isValidatingVoucher = true
                                    voucherError = null
                                    coroutineScope.launch {
                                        val v = onValidateVoucher(code)
                                        isValidatingVoucher = false
                                        val cleanThisTable = TableUtils.cleanTableCode(tableId)
                                        if (v == null) {
                                            voucherError = "Mã voucher \"$code\" không tồn tại trong hệ thống!"
                                            return@launch
                                        }
                                        val allItems = orders.flatMap { it.items }
                                        val evalResult = v.validate(totalAmount, allItems, cleanThisTable)
                                        if (!evalResult.isValid) {
                                            voucherError = evalResult.message
                                            return@launch
                                        }
                                        appliedVoucher = v
                                        voucherError = null
                                        onApplyVoucher?.invoke(v)
                                    }
                                },
                                enabled = !isValidatingVoucher && voucherInput.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraGold),
                                modifier = Modifier.height(40.dp)
                            ) {
                                if (isValidatingVoucher) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Áp dụng", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                }
                            }
                        }
                    } else if (appliedVoucher != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val vCodeDisplay = if (appliedVoucher!!.code.isNotBlank()) appliedVoucher!!.code else appliedVoucher!!.id
                                Text(
                                    text = "Voucher: $vCodeDisplay",
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

                            if (allowVoucherInput) {
                                Text(
                                    text = "Hủy",
                                    color = Color(0xFFDC2626),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            appliedVoucher = null
                                            voucherInput = ""
                                            voucherError = null
                                            onCancelVoucher?.invoke()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (voucherError != null) {
                        Text(
                            text = voucherError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3. KHUNG HÓA ĐƠN NHIỆT (Thermal Paper Look & Feel như bản Web)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFAF8F5))
                    .border(1.dp, Color(0xFFE8E4DD), RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header Nha Hang
                Text(
                    text = "AURORA RESTAURANT & LOUNGE",
                    color = Color(0xFF111827),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Đ/c: Số 123 Đường Ven Hồ, Hà Nội",
                    color = Color(0xFF6B7280),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Hotline: 0987.654.321",
                    color = Color(0xFF6B7280),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                DashedDivider(color = Color(0xFFD1D5DB))

                Text(
                    text = "PHIẾU THANH TOÁN",
                    color = Color(0xFF1F2937),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "BÀN: $cleanTable",
                    color = Color(0xFF7E22CE),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Metadata Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Mã HĐ: #AU$dateCode-$cleanTable", color = Color(0xFF374151), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = timeFormatted, color = Color(0xFF6B7280), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (attachedCustomer != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Khách hàng: ${attachedCustomer.name}", color = Color(0xFF1F2937), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(text = "Lượt gọi: ${orders.size}", color = Color(0xFF374151), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(text = "Lượt gọi: ${orders.size}", color = Color(0xFF374151), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                DashedDivider(color = Color(0xFFD1D5DB))

                // Table Header: Mon | SL | D.Gia | T.Tien
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Món", color = Color(0xFF4B5563), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                    Text(text = "SL", color = Color(0xFF4B5563), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                    Text(text = "Đ.Giá", color = Color(0xFF4B5563), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
                    Text(text = "T.Tiền", color = Color(0xFF4B5563), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(76.dp))
                }

                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.8.dp)

                // Danh sach mon an
                groupedItems.forEach { (item, qty, unitPrice) ->
                    val (paidToppings, freeOptions) = run {
                        if (item.selected_options.isNotEmpty()) {
                            val paid = item.selected_options.filter { it.price > 0 }.map {
                                it.item_name.removePrefix("•").removePrefix("+").trim() to it.price
                            }
                            val free = item.selected_options.filter { it.price <= 0 }.map {
                                it.item_name.removePrefix("•").removePrefix("+").trim()
                            }
                            Pair(paid, free)
                        } else if (item.options_summary.isNotBlank()) {
                            val lines = if (item.options_summary.contains("\n")) {
                                item.options_summary.split("\n")
                            } else {
                                item.options_summary.split(", ")
                            }
                            val paid = mutableListOf<Pair<String, Double>>()
                            val free = mutableListOf<String>()
                            val priceRegex = Regex("""^(.*?)\s*\(\+([0-9.,]+)\s*(?:VNĐ|đ|d)?\)""", RegexOption.IGNORE_CASE)
                            for (rawLine in lines) {
                                val clean = rawLine.trim().removePrefix("+").removePrefix("•").trim()
                                if (clean.isBlank()) continue
                                val match = priceRegex.find(clean)
                                if (match != null) {
                                    val optName = match.groupValues[1].trim()
                                    val priceNum = match.groupValues[2].replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
                                    if (priceNum > 0) {
                                        paid.add(optName to priceNum)
                                    } else {
                                        free.add(optName)
                                    }
                                } else {
                                    free.add(clean)
                                }
                            }
                            Pair(paid, free)
                        } else {
                            Pair(emptyList<Pair<String, Double>>(), emptyList<String>())
                        }
                    }

                    val paidToppingsSum = paidToppings.sumOf { it.second }
                    val dishBasePrice = if (item.base_price > 0.0) item.base_price else (unitPrice - paidToppingsSum).coerceAtLeast(0.0)
                    val dishTotal = dishBasePrice * qty

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.5.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1.8f)) {
                            Text(
                                text = item.dish_name,
                                color = Color(0xFF111827),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            freeOptions.forEach { fo ->
                                Text(
                                    text = "  - $fo",
                                    color = Color(0xFF6B7280),
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                        Text(text = qty.toString(), color = Color(0xFF111827), fontSize = 10.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                        Text(text = CurrencyUtils.formatPlain(dishBasePrice), color = Color(0xFF4B5563), fontSize = 10.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
                        Text(text = CurrencyUtils.formatPlain(dishTotal), color = Color(0xFF111827), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(76.dp))
                    }

                    paidToppings.forEach { (toppingName, toppingPrice) ->
                        val ptTotal = toppingPrice * qty
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "  + $toppingName",
                                color = Color(0xFF4B5563),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1.8f)
                            )
                            Text(text = qty.toString(), color = Color(0xFF6B7280), fontSize = 9.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                            Text(text = CurrencyUtils.formatPlain(toppingPrice), color = Color(0xFF6B7280), fontSize = 9.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
                            Text(text = CurrencyUtils.formatPlain(ptTotal), color = Color(0xFF111827), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(76.dp))
                        }
                    }
                }

                DashedDivider(color = Color(0xFFD1D5DB))

                // Totals Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tiền hàng:", color = Color(0xFF4B5563), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(text = CurrencyUtils.formatVnd(totalAmount), color = Color(0xFF111827), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                if (voucherDiscount > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Voucher giảm giá:", color = Color(0xFF047857), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "-${CurrencyUtils.formatVnd(voucherDiscount)}", color = Color(0xFF047857), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                if (pointsDeducted > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Trừ điểm tích lũy:", color = Color(0xFFB45309), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "-${CurrencyUtils.formatVnd(pointsDeducted)}", color = Color(0xFFB45309), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                DashedDivider(color = Color(0xFFD1D5DB))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "TỔNG CỘNG:", color = Color(0xFF111827), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    Text(text = CurrencyUtils.formatVnd(netTotal), color = AuroraGoldHover, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                }

                if (netTotal == 0.0 && pointsDeducted > 0.0) {
                    Text(
                        text = "(Thanh toán 100% bằng Điểm tích lũy)",
                        color = Color(0xFF047857),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                DashedDivider(color = Color(0xFFD1D5DB))

                // Footer Loi cam on & Wifi
                Text(text = "Cảm ơn Quý khách & Hẹn gặp lại!", color = Color(0xFF374151), fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "Wifi: Aurora_Guest / Pass: auroracoffee", color = Color(0xFF9CA3AF), fontSize = 9.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
            }

            // 4. ACTION BUTTONS (Xếp trên 1 hàng ngang chuẩn POS Web)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: In bill (Purple/Pink gradient style hoac Gold)
                Button(
                    onClick = onPrintOnly,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraGoldHover)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "In bill",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isWaiterOnly) "In bill tạm tính" else "In bill",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                // Button 2: Ket & Tra ban (Emerald Green chuan #10B981)
                if (!isWaiterOnly) {
                    Button(
                        onClick = {
                            val method = if (netTotal == 0.0 && pointsDeducted > 0.0) "points" else "cash"
                            val vCode = appliedVoucher?.let { if (it.code.isNotBlank()) it.code else it.id }
                            onSettlePayment(
                                method,
                                vCode,
                                attachedCustomer,
                                pointsDeducted,
                                netTotal,
                                totalAmount
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Kết bàn",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Kết & Trả bàn",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                // Button 3: Dong (Nhe nhang, khong chiem cho)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF4B5563)
                    )
                ) {
                    Text(
                        text = "Đóng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}
