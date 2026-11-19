package com.example.qrorderkitchen.ui.staff

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qrorderkitchen.data.models.StaffRole
import com.example.qrorderkitchen.models.Order
import com.example.qrorderkitchen.models.OrderItem
import com.example.qrorderkitchen.models.TableModel
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightSurface
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AppMode
import com.example.qrorderkitchen.ui.viewmodel.AuthState
import com.example.qrorderkitchen.ui.viewmodel.AuthViewModel
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import com.example.qrorderkitchen.utils.CurrencyUtils
import com.example.qrorderkitchen.utils.TableUtils

@Composable
fun TableDetailDialog(
    table: TableModel,
    orders: List<Order>,
    viewModel: AuroraViewModel,
    authViewModel: AuthViewModel? = null,
    onDismiss: () -> Unit
) {
    val authState by authViewModel?.authState?.collectAsState() ?: remember { mutableStateOf(AuthState.Unauthenticated) }
    val allDishes by viewModel.dishes.collectAsState()
    val serviceRequests by viewModel.serviceRequests.collectAsState()
    val currentStaff = (authState as? AuthState.Authenticated)?.staff
    val currentStaffUsername = currentStaff?.username ?: ""
    val currentStaffName = currentStaff?.name ?: ""
    val currentStaffRole = currentStaff?.staffRole ?: StaffRole.WAITER
    val totalSpent = orders.sumOf { it.total_amount }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.65f else 0.94f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LightSurface)
                    .border(1.5.dp, LightBorder, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                // Header Clean View (Chuẩn Web)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHI TIẾT BÀN ${table.tableId}",
                        color = AuroraGoldHover,
                        fontSize = if (isLandscape) 18.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${orders.size} ĐỢT GỌI",
                        color = TextSecondary,
                        fontSize = if (isLandscape) 12.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Banner Khách Gọi Hỗ Trợ nếu có
                val cleanThisTable = TableUtils.cleanTableCode(table.tableId)
                val pendingAssistance = serviceRequests.find {
                    TableUtils.isSameTable(it.table_id, cleanThisTable) &&
                    it.status == "pending" && it.type == "assistance"
                }
                if (pendingAssistance != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "🔔 KHÁCH GỌI HỖ TRỢ:",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = pendingAssistance.note.ifBlank { "Khách cần hỗ trợ phục vụ tại bàn" },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.resolveServiceRequest(pendingAssistance.id)
                                    viewModel.showToast("✓ Đã hoàn tất hỗ trợ Bàn ${table.tableId}!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Đã xong", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                val isBeingTransferredByOther = !table.transferringByUsername.isNullOrBlank() && table.transferringByUsername != currentStaffUsername
                val isAssignedToOther = !table.assignedStaffUsername.isNullOrBlank() && table.assignedStaffUsername != currentStaffUsername && currentStaffRole != StaffRole.MANAGER

                // Transfer in Progress Warning Banner
                if (isBeingTransferredByOther) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bàn đang được ${table.transferringByName ?: "nhân viên khác"} điều chuyển món!",
                                color = Color(0xFFB45309),
                                fontSize = if (isLandscape) 11.5.sp else 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (!table.assignedStaffUsername.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val isMine = table.assignedStaffUsername == currentStaffUsername
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMine) AuroraGoldSoft else Color(0xFFFEE2E2))
                            .border(1.dp, if (isMine) AuroraGold else Color(0xFFEF4444), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isMine) AuroraGoldHover else Color(0xFFDC2626),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMine) "Bạn đang phụ trách bàn này" else "Bàn đang do ${table.assignedStaffName ?: table.assignedStaffUsername} phụ trách",
                                color = if (isMine) AuroraGoldHover else Color(0xFFB91C1C),
                                fontSize = if (isLandscape) 11.5.sp else 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = LightBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // Danh sách đơn hàng & các món (Clean View)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (orders.isEmpty()) {
                        Text(
                            text = "Bàn trống, chưa có món ăn nào.",
                            color = TextSecondary,
                            fontSize = if (isLandscape) 13.sp else 15.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        orders.forEachIndexed { index, ord ->
                            val isOrderActive = ord.status !in listOf("settled", "paid", "transferred", "cancelled")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Đợt #${index + 1} (${ord.status.uppercase()})",
                                    color = if (ord.status == "cancelled") Color(0xFFE11D48) else AuroraGoldHover,
                                    fontSize = if (isLandscape) 13.sp else 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            ord.items.forEachIndexed { itemIdx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "• ${item.quantity}x ${item.dish_name}",
                                            color = TextPrimary,
                                            fontSize = if (isLandscape) 13.sp else 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

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
                                                    color = Color(0xFFB45309), // text-amber-700 chuẩn Web
                                                    fontSize = if (isLandscape) 11.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(top = 1.5.dp)
                                                )
                                            }
                                        }
                                        if (item.note.isNotBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFEF3C7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Ghi chú: ${item.note}",
                                                    color = Color(0xFF92400E),
                                                    fontSize = if (isLandscape) 11.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = CurrencyUtils.formatVnd(item.price * item.quantity),
                                        color = AuroraGoldHover,
                                        fontSize = if (isLandscape) 12.sp else 14.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                HorizontalDivider(color = LightBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "TỔNG PHIÊN BÀN:", color = TextSecondary, fontSize = if (isLandscape) 13.sp else 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = CurrencyUtils.formatVnd(totalSpent),
                        color = AuroraGoldHover,
                        fontSize = if (isLandscape) 16.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions (Chuẩn Web: Chuyển bàn, In hóa đơn thanh toán, Đóng, Mở bàn)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (orders.isNotEmpty()) {
                        if (currentStaffRole == StaffRole.WAITER || currentStaffRole == StaffRole.MANAGER) {
                            OutlinedButton(
                                onClick = {
                                    if (isBeingTransferredByOther) {
                                        viewModel.showToast("Bàn đang được ${table.transferringByName ?: "NV khác"} điều chuyển món!")
                                    } else {
                                        viewModel.startTableTransfer(table, currentStaffUsername, currentStaffName)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBeingTransferredByOther) Color(0xFFD1D5DB) else AuroraGold)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Chuyển / Gộp bàn",
                                        tint = if (isBeingTransferredByOther) Color(0xFF9CA3AF) else AuroraGoldHover,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBeingTransferredByOther) "Đang điều chuyển bởi ${table.transferringByName ?: "NV"}" else "Chuyển / Gộp bàn",
                                        color = if (isBeingTransferredByOther) Color(0xFF9CA3AF) else AuroraGoldHover,
                                        fontSize = if (isLandscape) 13.sp else 14.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }


                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Đóng", color = TextSecondary, fontSize = if (isLandscape) 13.sp else 14.5.sp, fontWeight = FontWeight.Bold)
                        }

                        val isBlocked = isAssignedToOther
                        Button(
                            onClick = {
                                val assignedUser = table.assignedStaffUsername
                                if (assignedUser.isNullOrEmpty() || assignedUser == currentStaffUsername || currentStaffRole == StaffRole.MANAGER) {
                                    if (currentStaffUsername.isNotBlank()) {
                                        viewModel.assignStaffToTable(table.tableId, currentStaffUsername, currentStaffName)
                                    }
                                    viewModel.setTableId(table.tableId)
                                    viewModel.selectedTableDetail.value = null
                                    viewModel.setAppMode(AppMode.CUSTOMER)
                                    viewModel.showToast("Đã mở Bàn ${table.tableId} để đặt món")
                                } else {
                                    viewModel.showToast("Bàn ${table.tableId} hiện do ${table.assignedStaffName ?: assignedUser} (@$assignedUser) phụ trách!")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBlocked) Color(0xFFE5E7EB) else AuroraGold
                            )
                        ) {
                            val buttonText = when {
                                isBlocked -> "Có NV phụ trách"
                                currentStaffRole == StaffRole.MANAGER && isAssignedToOther -> "Mở bàn ${table.tableId} (QL)"
                                else -> "Mở bàn ${table.tableId}"
                            }
                            val buttonTextColor = if (isBlocked) Color(0xFF6B7280) else LightSurface
                            Text(text = buttonText, color = buttonTextColor, fontSize = if (isLandscape) 13.sp else 14.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
