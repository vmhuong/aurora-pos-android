package com.example.qrorderkitchen.ui.customer

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrorderkitchen.models.Customer
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
import com.example.qrorderkitchen.ui.theme.WebInputBg
import com.example.qrorderkitchen.ui.theme.WebSurfacePill
import com.example.qrorderkitchen.ui.theme.WebTextMuted
import com.example.qrorderkitchen.ui.theme.WebTextPrimary
import com.example.qrorderkitchen.ui.theme.WebTextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AuthViewModel
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import com.example.qrorderkitchen.utils.CurrencyUtils
import kotlinx.coroutines.launch

@Composable
fun CustomerAccountScreen(
    viewModel: AuroraViewModel,
    authViewModel: AuthViewModel? = null
) {
    val tableId by viewModel.currentTableId.collectAsState()
    val attachedCustomers by viewModel.attachedCustomers.collectAsState()
    val cleanTable = remember(tableId) {
        tableId.trim().replace("Table ", "", ignoreCase = true)
            .replace("table_", "", ignoreCase = true)
            .replace("Ban ", "", ignoreCase = true)
            .replace("Bàn ", "", ignoreCase = true).uppercase()
    }
    val attachedCustomer = attachedCustomers[cleanTable]

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()

    var phoneInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<Customer?>(null) }
    var searchNotFound by remember { mutableStateOf(false) }
    var notFoundPhone by remember { mutableStateOf("") }
    var newNameInput by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    fun executeSearch() {
        val cleanPhone = phoneInput.trim().replace(" ", "").replace(".", "")
        if (cleanPhone.length < 9) {
            viewModel.showToast("Vui lòng nhập số điện thoại hợp lệ (tối thiểu 9 số)")
            return
        }
        coroutineScope.launch {
            isSearching = true
            searchResult = null
            searchNotFound = false
            val cust = viewModel.searchCustomerByPhone(cleanPhone)
            isSearching = false
            if (cust != null) {
                searchResult = cust
                searchNotFound = false
                viewModel.showToast("Tìm thấy khách hàng: ${cust.name}")
            } else {
                searchNotFound = true
                notFoundPhone = cleanPhone
                newNameInput = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WebCreamBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isLandscape) 24.dp else 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.72f else 1f)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 36.dp else 42.dp)
                        .clip(CircleShape)
                        .background(WebSurfacePill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Customer",
                        tint = WebGoldDark,
                        modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tài khoản khách hàng",
                        color = WebTextPrimary,
                        fontSize = if (isLandscape) 16.sp else 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tích lũy & khấu trừ điểm cho Bàn $tableId",
                        color = WebTextSecondary,
                        fontSize = if (isLandscape) 11.5.sp else 13.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(WebGoldPrimary.copy(alpha = 0.15f))
                    .border(1.dp, WebGoldPrimary.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "BÀN $tableId",
                    color = WebGoldDark,
                    fontSize = if (isLandscape) 11.sp else 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content Container
        Column(
            modifier = Modifier.fillMaxWidth(if (isLandscape) 0.72f else 1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Case 1: Đã gắn khách hàng vào bàn
            if (attachedCustomer != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, AuroraGold, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuroraGoldSoft.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(AuroraGold),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = attachedCustomer.name.ifBlank { "Khách hàng" },
                                        color = TextPrimary,
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "SĐT: ${attachedCustomer.phone}",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AuroraGold)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = attachedCustomer.getTierDisplayName(),
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = LightBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, LightBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Điểm khả dụng",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${CurrencyUtils.formatVnd(attachedCustomer.points).replace("VNĐ", "").trim()} điểm",
                                        color = AuroraGoldHover,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, LightBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Tổng chi tiêu",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyUtils.formatVnd(attachedCustomer.total_spend),
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tích lũy trọn đời",
                                        color = TextSecondary,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Đã liên kết với Bàn $tableId. Điểm thưởng sẽ được tích tự động hoặc có thể dùng trừ tiền khi in hóa đơn thanh toán.",
                            color = Color(0xFF78350F),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedButton(
                            onClick = {
                                viewModel.detachCustomerFromTable(tableId)
                                viewModel.showToast("Đã đăng xuất tài khoản khỏi Bàn $tableId")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Detach",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Đăng xuất tài khoản khỏi Bàn $tableId",
                                color = Color(0xFFDC2626),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Case 2: Chưa gắn tài khoản - Tra cứu bằng SĐT
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WebBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = WebCardBg),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tra cứu tài khoản hội viên",
                            color = WebTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Nhập số điện thoại khách hàng để kiểm tra điểm thưởng và tích lũy vào hóa đơn bàn này.",
                            color = WebTextMuted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = {
                                    if (it.length <= 11) {
                                        phoneInput = it.filter { char -> char.isDigit() }
                                    }
                                },
                                placeholder = { Text("Nhập SĐT khách (VD: 0987...)", fontSize = 13.5.sp, color = WebTextMuted) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WebGoldDark, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (phoneInput.isNotBlank()) {
                                        IconButton(onClick = { phoneInput = ""; searchResult = null; searchNotFound = false }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = WebTextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WebGoldPrimary,
                                    unfocusedBorderColor = WebBorder,
                                    focusedContainerColor = WebInputBg,
                                    unfocusedContainerColor = WebInputBg
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { executeSearch() }),
                                singleLine = true
                            )

                            Button(
                                onClick = { executeSearch() },
                                enabled = !isSearching && phoneInput.isNotBlank(),
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WebGoldPrimary)
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tìm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Search Result Found
                        searchResult?.let { found ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AuroraGold, RoundedCornerShape(14.dp)),
                                colors = CardDefaults.cardColors(containerColor = AuroraGoldSoft.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = found.name.ifBlank { "Khách hàng" }, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "SĐT: ${found.phone}", color = TextSecondary, fontSize = 12.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AuroraGold)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(text = found.getTierDisplayName(), color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = "Điểm tích lũy: ${CurrencyUtils.formatVnd(found.points).replace("VNĐ", "").trim()} điểm",
                                        color = AuroraGoldHover,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.attachCustomerToTable(tableId, found)
                                            searchResult = null
                                            phoneInput = ""
                                            viewModel.showToast("Đã gắn khách hàng ${found.name} vào Bàn $tableId!")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraGold)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Gắn vào Bàn $tableId để tích điểm", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Search Result Not Found -> Form Đăng Ký Mới Nhanh
                        if (searchNotFound) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Chưa có tài khoản với SĐT $notFoundPhone",
                                        color = Color(0xFF92400E),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Nhập tên khách hàng dưới đây để tạo thẻ hội viên mới và tích lũy điểm ngay:",
                                        color = Color(0xFFB45309),
                                        fontSize = 11.5.sp
                                    )

                                    OutlinedTextField(
                                        value = newNameInput,
                                        onValueChange = { newNameInput = it },
                                        placeholder = { Text("Họ và tên khách hàng...", fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AuroraGold,
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        ),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            val name = newNameInput.trim()
                                            if (name.isBlank()) {
                                                viewModel.showToast("Vui lòng nhập họ và tên khách hàng!")
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                isRegistering = true
                                                val created = viewModel.registerCustomer(notFoundPhone, name)
                                                isRegistering = false
                                                if (created != null) {
                                                    viewModel.attachCustomerToTable(tableId, created)
                                                    searchNotFound = false
                                                    phoneInput = ""
                                                    newNameInput = ""
                                                    viewModel.showToast("Đã tạo hội viên mới và gắn vào Bàn $tableId!")
                                                } else {
                                                    viewModel.showToast("Không thể đăng ký khách hàng, vui lòng thử lại!")
                                                }
                                            }
                                        },
                                        enabled = !isRegistering && newNameInput.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AuroraGold)
                                    ) {
                                        if (isRegistering) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Đăng ký mới & Gắn vào Bàn $tableId", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Loyalty Perks Guide
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.8.dp, WebBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = WebCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WebGoldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quy chế tích điểm & Hạng thành viên",
                            color = WebTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "• Thành viên (Member): Tích 3% giá trị thanh toán\n" +
                               "• Hạng Bạc (Silver): Tích 5% giá trị thanh toán\n" +
                               "• Hạng Vàng (Gold): Tích 8% giá trị thanh toán\n" +
                               "• Bạch Kim / VIP: Tích 10% - 13% giá trị thanh toán\n" +
                               "• Tỷ lệ quy đổi: 1 điểm = 1 VNĐ, có thể dùng khấu trừ 100% khi in bill thanh toán.",
                        color = WebTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
