package com.example.qrorderkitchen.ui.staff

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrorderkitchen.models.TableModel
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightBgWarm
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightSurface
import com.example.qrorderkitchen.ui.theme.StatusAlertBg
import com.example.qrorderkitchen.ui.theme.StatusAlertBorder
import com.example.qrorderkitchen.ui.theme.StatusAlertText
import com.example.qrorderkitchen.ui.theme.StatusCheckoutBg
import com.example.qrorderkitchen.ui.theme.StatusCheckoutBorder
import com.example.qrorderkitchen.ui.theme.StatusCheckoutText
import com.example.qrorderkitchen.ui.theme.StatusCookingBg
import com.example.qrorderkitchen.ui.theme.StatusCookingBorder
import com.example.qrorderkitchen.ui.theme.StatusCookingText
import com.example.qrorderkitchen.ui.theme.StatusDoneBg
import com.example.qrorderkitchen.ui.theme.StatusDoneBorder
import com.example.qrorderkitchen.ui.theme.StatusDoneText
import com.example.qrorderkitchen.ui.theme.StatusPendingBg
import com.example.qrorderkitchen.ui.theme.StatusPendingBorder
import com.example.qrorderkitchen.ui.theme.StatusPendingText
import com.example.qrorderkitchen.ui.theme.TableEmptyBg
import com.example.qrorderkitchen.ui.theme.TableEmptyBorder
import com.example.qrorderkitchen.ui.theme.TableEmptyText
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AuthViewModel
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import com.example.qrorderkitchen.utils.TableUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun cleanTableCode(raw: String?): String = TableUtils.cleanTableCode(raw)

@Composable
fun FloorPlanScreen(
    viewModel: AuroraViewModel,
    authViewModel: AuthViewModel? = null
) {
    val activeOrders by viewModel.activeOrders.collectAsState()
    val serviceRequests by viewModel.serviceRequests.collectAsState()
    val firestoreTables by viewModel.firestoreTables.collectAsState()
    val selectedTableDetail by viewModel.selectedTableDetail.collectAsState()
    val transferSource by viewModel.transferSourceTable.collectAsState()
    val splitSession by viewModel.splitTransferSession.collectAsState()

    val authState by authViewModel?.authState?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.example.qrorderkitchen.ui.viewmodel.AuthState.Unauthenticated) }
    val currentStaff = (authState as? com.example.qrorderkitchen.ui.viewmodel.AuthState.Authenticated)?.staff
    val currentStaffUsername = currentStaff?.username ?: ""

    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
    }

    // EXCLUSIVELY GET TABLE DATA FROM REAL FIRESTORE ACTIVE ORDERS, REQUESTS, AND TABLES COLLECTION
    val tables = remember(activeOrders, serviceRequests, firestoreTables) {
        val ordersTableIds = activeOrders.map { it.table_id }
        val requestsTableIds = serviceRequests.map { it.table_id }
        val tablesCollectionIds = firestoreTables.map { it.tableId }
        val fallbackIds = listOf("B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8")

        val combinedIds = (tablesCollectionIds + ordersTableIds + requestsTableIds).ifEmpty { fallbackIds }

        val realTableIds = combinedIds
            .map { cleanTableCode(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy({
                val letterPrefix = it.replace(Regex("[0-9]"), "").uppercase()
                letterPrefix
            }, {
                val num = it.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 999
                num
            }, { it.uppercase() }))

        realTableIds.map { tableIdStr ->
            val tableOrders = activeOrders.filter {
                cleanTableCode(it.table_id) == tableIdStr
            }
            val tableRequests = serviceRequests.filter {
                cleanTableCode(it.table_id) == tableIdStr
            }

            val fsTable = firestoreTables.find {
                cleanTableCode(it.tableId) == tableIdStr
            }

            val earliestOrderDate = tableOrders.mapNotNull { it.created_at?.toDate() }.minByOrNull { it.time }

            val hasCheckout = tableRequests.any { it.type == "checkout" } || fsTable?.status == "checkout"
            val hasAssistance = tableRequests.any { it.type == "assistance" } || fsTable?.status == "assistance"
            val hasPending = tableOrders.any { it.status == "pending" }
            val hasCooking = tableOrders.any { it.status == "cooking" }
            val hasDone = tableOrders.any { it.status == "done" }

            val status = when {
                hasCheckout -> "checkout"
                hasAssistance -> "assistance"
                hasPending || hasCooking -> "waiting"
                hasDone -> "eating"
                else -> "available"
            }

            TableModel(
                tableId = tableIdStr,
                tableName = fsTable?.tableName ?: "",
                status = status,
                activeOrderCount = tableOrders.size,
                serviceRequestType = when {
                    hasCheckout -> "checkout"
                    hasAssistance -> "assistance"
                    else -> null
                },
                assignedStaffUsername = fsTable?.assignedStaffUsername,
                assignedStaffName = fsTable?.assignedStaffName,
                transferringByUsername = fsTable?.transferringByUsername,
                transferringByName = fsTable?.transferringByName
            ) to earliestOrderDate
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) 4 else 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgWarm)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TRANSFER MODE GUIDANCE BANNER
            if (transferSource != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.5.dp, AuroraGold),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AuroraGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ĐANG CHUYỂN BÀN ${transferSource!!.tableId.uppercase()}",
                                    color = AuroraGoldHover,
                                    fontSize = if (isLandscape) 13.sp else 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "➜ Chạm vào bàn đích trên sơ đồ để chuyển món",
                                    color = TextPrimary,
                                    fontSize = if (isLandscape) 11.sp else 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelTransferSelection() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(text = "Hủy bỏ", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 11.sp else 13.sp)
                        }
                    }
                }
            }

            // Legend Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LegendItem("Trống", TableEmptyBorder)
                LegendItem("Chờ món", StatusPendingBorder)
                LegendItem("Đang nấu", StatusCookingBorder)
                LegendItem("Đang ăn", StatusDoneBorder)
                LegendItem("Cần hỗ trợ", StatusAlertBorder)
                LegendItem("Thanh toán", StatusCheckoutBorder)
            }

            // Tables Grid Synced Realtime with Opening Time displayed in Top-Right Corner
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tables, key = { it.first.tableId }) { (table, earliestDate) ->
                    val isSourceTable = transferSource?.tableId?.equals(table.tableId, ignoreCase = true) == true

                    val (cardBg, cardBorder, statusText, statusTextColor) = when (table.status) {
                        "checkout" -> Quadruple(StatusCheckoutBg, StatusCheckoutBorder, "Thanh toán", StatusCheckoutText)
                        "assistance" -> {
                            val cleanId = cleanTableCode(table.tableId)
                            val assistReq = serviceRequests.find { cleanTableCode(it.table_id) == cleanId && it.type == "assistance" && it.status == "pending" }
                            val noteDisplay = if (!assistReq?.note.isNullOrBlank()) "Cần: ${assistReq!!.note}" else "Gọi phục vụ"
                            Quadruple(StatusAlertBg, StatusAlertBorder, noteDisplay, StatusAlertText)
                        }
                        "waiting" -> Quadruple(StatusPendingBg, StatusPendingBorder, "${table.activeOrderCount} đợt gọi", StatusPendingText)
                        "eating" -> Quadruple(StatusDoneBg, StatusDoneBorder, "Đang dùng bữa", StatusDoneText)
                        else -> Quadruple(TableEmptyBg, TableEmptyBorder, "Sẵn sàng", TableEmptyText)
                    }

                    val isBeingTransferred = !table.transferringByUsername.isNullOrBlank()
                    val isBeingTransferredByOther = isBeingTransferred && table.transferringByUsername != currentStaffUsername

                    val isAssigned = !table.assignedStaffUsername.isNullOrBlank()
                    val isAssignedToMe = isAssigned && table.assignedStaffUsername == currentStaffUsername
                    val isAssignedToOther = isAssigned && table.assignedStaffUsername != currentStaffUsername

                    val effectiveBorder = when {
                        isSourceTable -> AuroraGold
                        isBeingTransferred -> Color(0xFFF59E0B) // Amber
                        isAssignedToOther -> Color(0xFFFCA5A5) // Soft Red
                        else -> cardBorder
                    }
                    val borderWidth = if (isSourceTable || isBeingTransferred) 2.5.dp else 1.5.dp

                    val effectiveCardBg = when {
                        isSourceTable -> AuroraGoldSoft
                        isBeingTransferred -> Color(0xFFFFFBEB)
                        else -> cardBg
                    }

                    Card(
                        modifier = Modifier
                            .padding(6.dp)
                            .height(if (isLandscape) 118.dp else 134.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(borderWidth, effectiveBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                val src = transferSource
                                if (src != null) {
                                    if (src.tableId.equals(table.tableId, ignoreCase = true)) {
                                        viewModel.showToast("Bàn đích không được trùng Bàn nguồn (${src.tableId})!")
                                    } else if (isBeingTransferredByOther) {
                                        viewModel.showToast("⚠️ Bàn ${table.tableId} đang được ${table.transferringByName ?: "NV khác"} điều chuyển!")
                                    } else {
                                        viewModel.selectDestinationTable(table)
                                    }
                                } else {
                                    viewModel.selectedTableDetail.value = table
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = effectiveCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSourceTable || isBeingTransferred) 4.dp else 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            // TOP-LEFT BADGE: BÀN NGUỒN hoặc ĐANG CHUYỂN
                            if (isSourceTable) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AuroraGold)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "BÀN NGUỒN",
                                        color = Color.White,
                                        fontSize = if (isLandscape) 8.5.sp else 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isBeingTransferred) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF59E0B))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isLandscape) 10.dp else 12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "ĐANG CHUYỂN",
                                            color = Color.White,
                                            fontSize = if (isLandscape) 8.sp else 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // TOP RIGHT CORNER BADGE: OPENING TIME
                            if (earliestDate != null) {
                                val openTimeStr = timeFormat.format(earliestDate)
                                val rawElapsedMillis = Date().time - earliestDate.time
                                val elapsedMins = Math.max(0L, rawElapsedMillis / (1000 * 60))
                                val elapsedBadgeText = when {
                                    rawElapsedMillis < 60_000 -> "$openTimeStr (Mới vào)"
                                    elapsedMins < 60 -> "$openTimeStr (${elapsedMins}m)"
                                    else -> "$openTimeStr (${elapsedMins / 60}h ${elapsedMins % 60}m)"
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LightSurface)
                                        .border(0.5.dp, LightBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = elapsedBadgeText,
                                        color = TextSecondary,
                                        fontSize = if (isLandscape) 9.sp else 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // CENTER BODY: TABLE NAME & STATUS & STAFF LOCK
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val cleanNum = cleanTableCode(table.tableId).ifBlank { cleanTableCode(table.tableName) }
                                val displayName = if (cleanNum.isNotBlank()) cleanNum.uppercase() else table.tableId.uppercase()

                                Text(
                                    text = displayName,
                                    color = TextPrimary,
                                    fontSize = if (isLandscape) 17.5.sp else 21.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(if (isLandscape) 3.dp else 4.dp))

                                Text(
                                    text = statusText,
                                    color = statusTextColor,
                                    fontSize = if (isLandscape) 10.5.sp else 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Real-time Staff Lock / Transfer Indicator on Table Card
                                if (isBeingTransferred) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "⇄ ${table.transferringByName ?: "NV"} đang gộp",
                                        color = Color(0xFFD97706),
                                        fontSize = if (isLandscape) 8.5.sp else 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else if (isAssigned) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isAssignedToMe) AuroraGoldHover else Color(0xFFEF4444),
                                            modifier = Modifier.size(if (isLandscape) 9.dp else 12.dp)
                                        )
                                        Text(
                                            text = if (isAssignedToMe) "Bạn phụ trách" else (table.assignedStaffName ?: table.assignedStaffUsername ?: ""),
                                            color = if (isAssignedToMe) AuroraGoldHover else Color(0xFFEF4444),
                                            fontSize = if (isLandscape) 8.5.sp else 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Table Detail Dialog
        selectedTableDetail?.let { table ->
            val cleanSel = cleanTableCode(table.tableId)
            val tableOrders = activeOrders.filter {
                cleanTableCode(it.table_id) == cleanSel
            }
            TableDetailDialog(
                table = table,
                orders = tableOrders,
                viewModel = viewModel,
                authViewModel = authViewModel,
                onDismiss = { viewModel.selectedTableDetail.value = null }
            )
        }

        // Split Transfer Dialog (Visual 2-Column Dish Transfer Screen)
        splitSession?.let { (sourceTable, targetTable) ->
            val cleanSrc = cleanTableCode(sourceTable.tableId)
            val cleanTrg = cleanTableCode(targetTable.tableId)
            val sourceOrders = activeOrders.filter {
                cleanTableCode(it.table_id) == cleanSrc
            }
            val targetOrders = activeOrders.filter {
                cleanTableCode(it.table_id) == cleanTrg
            }

            SplitTransferDialog(
                sourceTable = sourceTable,
                targetTable = targetTable,
                sourceOrders = sourceOrders,
                targetOrders = targetOrders,
                onCommit = { remainingSourceItems, movedItems ->
                    viewModel.commitSplitTransfer(
                        sourceTableId = sourceTable.tableId,
                        targetTableId = targetTable.tableId,
                        originalSourceOrders = sourceOrders,
                        remainingSourceItems = remainingSourceItems,
                        movedItems = movedItems
                    )
                },
                onDismiss = {
                    viewModel.cancelSplitTransfer()
                }
            )
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(if (isLandscape) 10.dp else 12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = if (isLandscape) 11.sp else 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
