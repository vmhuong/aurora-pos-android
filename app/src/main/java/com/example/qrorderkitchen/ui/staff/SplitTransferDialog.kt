package com.example.qrorderkitchen.ui.staff

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qrorderkitchen.models.Order
import com.example.qrorderkitchen.models.TableModel
import com.example.qrorderkitchen.models.TransferDishItem
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightBgWarm
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightSurface
import com.example.qrorderkitchen.ui.theme.LightSurfaceSubtle
import com.example.qrorderkitchen.ui.theme.StatusCookingBg
import com.example.qrorderkitchen.ui.theme.StatusCookingBorder
import com.example.qrorderkitchen.ui.theme.StatusCookingText
import com.example.qrorderkitchen.ui.theme.TableEmptyBg
import com.example.qrorderkitchen.ui.theme.TableEmptyBorder
import com.example.qrorderkitchen.ui.theme.TableEmptyText
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.utils.CurrencyUtils

@Composable
fun SplitTransferDialog(
    sourceTable: TableModel,
    targetTable: TableModel,
    sourceOrders: List<Order>,
    targetOrders: List<Order>,
    onCommit: (remainingSourceItems: List<TransferDishItem>, movedItems: List<TransferDishItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val tableGridCols = 1

    // In-memory list for Source Table items
    val sourceItems = remember {
        val list = mutableStateListOf<TransferDishItem>()
        sourceOrders.forEach { ord ->
            ord.items.forEach { itm ->
                val uniqueId = if (itm.id.isNotBlank()) itm.id else "${ord.id}_${itm.dish_name}_${System.currentTimeMillis()}"
                list.add(
                    TransferDishItem(
                        id = uniqueId,
                        originalDocId = itm.id,
                        orderId = ord.id,
                        dishId = itm.dish_id,
                        dishName = itm.dish_name,
                        dishNameEn = itm.dish_name_en,
                        price = itm.price,
                        basePrice = itm.base_price,
                        optionsPrice = itm.options_price,
                        quantity = itm.quantity,
                        maxAvailable = itm.quantity,
                        note = itm.note,
                        isCompleted = itm.is_completed,
                        isFromSource = true,
                        isCombo = itm.is_combo,
                        comboSummary = itm.combo_summary,
                        optionsSummary = itm.options_summary,
                        optionsSummaryEn = itm.options_summary_en,
                        selectedOptions = itm.selected_options
                    )
                )
            }
        }
        list
    }

    // In-memory list for Target Table items (includes original target items + moved items)
    val targetItems = remember {
        val list = mutableStateListOf<TransferDishItem>()
        targetOrders.forEach { ord ->
            ord.items.forEach { itm ->
                val uniqueId = if (itm.id.isNotBlank()) itm.id else "${ord.id}_${itm.dish_name}_${System.currentTimeMillis()}"
                list.add(
                    TransferDishItem(
                        id = uniqueId,
                        originalDocId = itm.id,
                        orderId = ord.id,
                        dishId = itm.dish_id,
                        dishName = itm.dish_name,
                        dishNameEn = itm.dish_name_en,
                        price = itm.price,
                        basePrice = itm.base_price,
                        optionsPrice = itm.options_price,
                        quantity = itm.quantity,
                        maxAvailable = itm.quantity,
                        note = itm.note,
                        isCompleted = itm.is_completed,
                        isFromSource = false,
                        isCombo = itm.is_combo,
                        comboSummary = itm.combo_summary,
                        optionsSummary = itm.options_summary,
                        optionsSummaryEn = itm.options_summary_en,
                        selectedOptions = itm.selected_options
                    )
                )
            }
        }
        list
    }

    // Pending item for Quantity Picker Modal
    var itemForQuantityDialog by remember { mutableStateOf<Pair<TransferDishItem, Boolean>?>(null) } // Pair(item, isReturning)

    val sourceTotal = sourceItems.sumOf { it.price * it.quantity }
    val targetTotal = targetItems.sumOf { it.price * it.quantity }
    val movedItemsCount = targetItems.count { it.isFromSource }
    val hasChanges = movedItemsCount > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = LightBgWarm,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, LightBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // TOP HEADER BAR (COMPACT & HIGH DENSITY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuroraGoldSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = AuroraGoldHover,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CHUYỂN MÓN: BÀN ${sourceTable.tableId.uppercase()} ➜ BÀN ${targetTable.tableId.uppercase()}",
                                color = AuroraGoldHover,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chạm vào món để chuyển | Nhấn [Lưu và thoát bàn] khi hoàn tất",
                                color = TextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = LightBorder)
                Spacer(modifier = Modifier.height(4.dp))

                // 3-COLUMN SPLIT TRANSFER WORKSPACE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LEFT COLUMN: SOURCE TABLE
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // Sleek Single-Line Column Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .border(0.5.dp, AuroraGold, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "BÀN NGUỒN",
                                            color = AuroraGoldHover,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BÀN ${sourceTable.tableId.uppercase()}",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${sourceItems.size} món)",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = CurrencyUtils.formatVnd(sourceTotal),
                                    color = AuroraGoldHover,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = LightBorder)
                            Spacer(modifier = Modifier.height(4.dp))

                            // 2-Column Responsive High-Density Grid for dishes
                            if (sourceItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Đã chuyển hết món sang Bàn ${targetTable.tableId}! Bàn ${sourceTable.tableId} sẽ được giải phóng khi lưu.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(tableGridCols),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(sourceItems, key = { it.id }) { item ->
                                        SourceDishCard(
                                            item = item,
                                            onClick = {
                                                if (item.quantity == 1) {
                                                    sourceItems.remove(item)
                                                    val existing = targetItems.find { it.id == item.id }
                                                    if (existing != null) {
                                                        val idx = targetItems.indexOf(existing)
                                                        targetItems[idx] = existing.copy(quantity = existing.quantity + 1)
                                                    } else {
                                                        targetItems.add(item.copy(isFromSource = true))
                                                    }
                                                } else {
                                                    itemForQuantityDialog = Pair(item, false)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // MIDDLE COLUMN: ACTION CONTROLS (COMPACT)
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Button: Transfer ALL from Source to Target
                        OutlinedButton(
                            onClick = {
                                if (sourceItems.isNotEmpty()) {
                                    sourceItems.forEach { srcItem ->
                                        val existing = targetItems.find { it.id == srcItem.id }
                                        if (existing != null) {
                                            val idx = targetItems.indexOf(existing)
                                            targetItems[idx] = existing.copy(quantity = existing.quantity + srcItem.quantity)
                                        } else {
                                            targetItems.add(srcItem.copy(isFromSource = true))
                                        }
                                    }
                                    sourceItems.clear()
                                }
                            },
                            enabled = sourceItems.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentPadding = PaddingValues(2.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (sourceItems.isNotEmpty()) AuroraGold else LightBorder
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Chuyển tất cả",
                                    tint = if (sourceItems.isNotEmpty()) AuroraGoldHover else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Tất cả",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sourceItems.isNotEmpty()) AuroraGoldHover else TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button: Return ALL transferred items back to Source
                        OutlinedButton(
                            onClick = {
                                val movedBack = targetItems.filter { it.isFromSource }
                                movedBack.forEach { trItem ->
                                    val existing = sourceItems.find { it.id == trItem.id }
                                    if (existing != null) {
                                        val idx = sourceItems.indexOf(existing)
                                        sourceItems[idx] = existing.copy(quantity = existing.quantity + trItem.quantity)
                                    } else {
                                        sourceItems.add(trItem)
                                    }
                                }
                                targetItems.removeAll(movedBack)
                            },
                            enabled = targetItems.any { it.isFromSource },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentPadding = PaddingValues(2.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (targetItems.any { it.isFromSource }) Color(0xFF6B7280) else LightBorder
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Hoàn lại",
                                    tint = if (targetItems.any { it.isFromSource }) Color(0xFF374151) else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Hoàn lại",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetItems.any { it.isFromSource }) Color(0xFF374151) else TextSecondary
                                )
                            }
                        }
                    }

                    // RIGHT COLUMN: TARGET TABLE
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            // Sleek Single-Line Column Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (targetOrders.isNotEmpty()) StatusCookingBg else TableEmptyBg)
                                            .border(
                                                0.5.dp,
                                                if (targetOrders.isNotEmpty()) StatusCookingBorder else TableEmptyBorder,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (targetOrders.isNotEmpty()) "BÀN CÓ KHÁCH" else "BÀN TRỐNG",
                                            color = if (targetOrders.isNotEmpty()) StatusCookingText else TableEmptyText,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BÀN ${targetTable.tableId.uppercase()}",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${targetItems.size} món)",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = CurrencyUtils.formatVnd(targetTotal),
                                    color = AuroraGoldHover,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = LightBorder)
                            Spacer(modifier = Modifier.height(4.dp))

                            // 2-Column Responsive High-Density Grid for target dishes
                            if (targetItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Chưa có món nào ở bàn đích. Chạm vào món ở bàn nguồn để chuyển sang đây.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(tableGridCols),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(targetItems, key = { it.id }) { item ->
                                        TargetDishCard(
                                            item = item,
                                            sourceTableId = sourceTable.tableId,
                                            onClick = {
                                                if (item.isFromSource) {
                                                    if (item.quantity == 1) {
                                                        targetItems.remove(item)
                                                        val existing = sourceItems.find { it.id == item.id }
                                                        if (existing != null) {
                                                            val idx = sourceItems.indexOf(existing)
                                                            sourceItems[idx] = existing.copy(quantity = existing.quantity + 1)
                                                        } else {
                                                            sourceItems.add(item)
                                                        }
                                                    } else {
                                                        itemForQuantityDialog = Pair(item, true)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = LightBorder)
                Spacer(modifier = Modifier.height(4.dp))

                // BOTTOM ACTION BAR: [THOÁT (KHÔNG LƯU)] and [LƯU VÀ THOÁT BÀN] (COMPACT 40DP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Thoát (Không lưu)",
                            color = Color(0xFF6B7280),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val movedItems = targetItems.filter { it.isFromSource }
                            onCommit(sourceItems.toList(), movedItems)
                        },
                        enabled = hasChanges,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraGold,
                            disabledContainerColor = Color(0xFFE5E7EB)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (hasChanges) LightSurface else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lưu và thoát bàn",
                                color = if (hasChanges) LightSurface else TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // QUANTITY PICKER MODAL DIALOG
    itemForQuantityDialog?.let { (item, isReturning) ->
        TransferQuantityDialog(
            item = item,
            isReturning = isReturning,
            sourceTableId = sourceTable.tableId,
            targetTableId = targetTable.tableId,
            onConfirm = { qtyToMove ->
                if (!isReturning) {
                    // Move from Source to Target
                    if (qtyToMove >= item.quantity) {
                        sourceItems.remove(item)
                    } else {
                        val idx = sourceItems.indexOf(item)
                        if (idx >= 0) {
                            sourceItems[idx] = item.copy(quantity = item.quantity - qtyToMove)
                        }
                    }

                    val existing = targetItems.find { it.id == item.id }
                    if (existing != null) {
                        val idx = targetItems.indexOf(existing)
                        targetItems[idx] = existing.copy(quantity = existing.quantity + qtyToMove)
                    } else {
                        targetItems.add(item.copy(quantity = qtyToMove, isFromSource = true))
                    }
                } else {
                    // Return from Target to Source
                    if (qtyToMove >= item.quantity) {
                        targetItems.remove(item)
                    } else {
                        val idx = targetItems.indexOf(item)
                        if (idx >= 0) {
                            targetItems[idx] = item.copy(quantity = item.quantity - qtyToMove)
                        }
                    }

                    val existing = sourceItems.find { it.id == item.id }
                    if (existing != null) {
                        val idx = sourceItems.indexOf(existing)
                        sourceItems[idx] = existing.copy(quantity = existing.quantity + qtyToMove)
                    } else {
                        sourceItems.add(item.copy(quantity = qtyToMove))
                    }
                }
                itemForQuantityDialog = null
            },
            onDismiss = { itemForQuantityDialog = null }
        )
    }
}

@Composable
private fun SourceDishCard(
    item: TransferDishItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(0.8.dp, LightBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = LightSurfaceSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [x1 badge]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "x${item.quantity}",
                    color = AuroraGoldHover,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // [Tên món kèm Combo / Tùy chọn]
            val noteSuffix = if (item.note.isNotBlank()) " (${item.note})" else ""
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.dishName}$noteSuffix",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isCombo) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(0.5.dp, Color(0xFFD97706), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        ) {
                            Text(text = "COMBO", color = Color(0xFFB45309), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val summary = if (item.comboSummary.isNotBlank()) item.comboSummary else item.optionsSummary
                if (summary.isNotBlank()) {
                    val previewLines = summary.split("\n").filter { it.isNotBlank() }.take(2)
                    previewLines.forEach { l ->
                        Text(
                            text = l.trim(),
                            color = Color(0xFFB45309),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // [Giá]
            Text(
                text = CurrencyUtils.formatVnd(item.price * item.quantity),
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(8.dp))

            // [Nút =>]
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Chuyển sang",
                    tint = AuroraGoldHover,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun TargetDishCard(
    item: TransferDishItem,
    sourceTableId: String,
    onClick: () -> Unit
) {
    val isMoved = item.isFromSource
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isMoved) 1.2.dp else 0.8.dp,
                color = if (isMoved) AuroraGold else LightBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = isMoved) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isMoved) Color(0xFFFFFBEB) else LightSurfaceSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // [x1 badge]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isMoved) Color(0xFFFEF3C7) else Color(0xFFE5E7EB))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "x${item.quantity}",
                    color = if (isMoved) AuroraGoldHover else TextPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // [Tên món kèm Combo / Tùy chọn]
            val noteSuffix = if (item.note.isNotBlank()) " (${item.note})" else ""
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.dishName}$noteSuffix",
                        color = TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isCombo) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(0.5.dp, Color(0xFFD97706), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        ) {
                            Text(text = "COMBO", color = Color(0xFFB45309), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val summary = if (item.comboSummary.isNotBlank()) item.comboSummary else item.optionsSummary
                if (summary.isNotBlank()) {
                    val previewLines = summary.split("\n").filter { it.isNotBlank() }.take(2)
                    previewLines.forEach { l ->
                        Text(
                            text = l.trim(),
                            color = Color(0xFFB45309),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // [Badge Bàn X hoặc Sẵn có]
            if (isMoved) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(AuroraGoldSoft)
                        .border(0.5.dp, AuroraGold, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Bàn $sourceTableId",
                        color = AuroraGoldHover,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE5E7EB))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "Sẵn có",
                        color = Color(0xFF6B7280),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // [Giá]
            Text(
                text = CurrencyUtils.formatVnd(item.price * item.quantity),
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )

            if (isMoved) {
                Spacer(modifier = Modifier.width(8.dp))
                // [Nút hoàn lại <=]
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Hoàn lại về bàn nguồn",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferQuantityDialog(
    item: TransferDishItem,
    isReturning: Boolean,
    sourceTableId: String,
    targetTableId: String,
    onConfirm: (quantity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf("1") }
    val currentQty = textValue.toIntOrNull() ?: 1

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.5.dp, LightBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isReturning) "HOÀN MÓN VỀ BÀN $sourceTableId" else "CHUYỂN MÓN SANG BÀN $targetTableId",
                color = AuroraGoldHover,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.dishName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Số lượng hiện có: ${item.quantity} phần",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // STEPPER CONTROLS [-] [ INPUT ] [+]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        val q = (textValue.toIntOrNull() ?: 1) - 1
                        if (q >= 1) textValue = q.toString()
                    },
                    enabled = currentQty > 1,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Giảm", tint = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(44.dp)
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightSurfaceSubtle)
                        .border(1.dp, AuroraGold, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.isBlank()) {
                                textValue = ""
                            } else {
                                val num = filtered.toIntOrNull() ?: 1
                                val clamped = num.coerceIn(1, item.quantity)
                                textValue = clamped.toString()
                            }
                        },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                IconButton(
                    onClick = {
                        val q = (textValue.toIntOrNull() ?: 1) + 1
                        if (q <= item.quantity) textValue = q.toString()
                    },
                    enabled = currentQty < item.quantity,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tăng", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // QUICK CHIPS: [1 phần] [Tất cả]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { textValue = "1" },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = "1 phần", fontSize = 10.5.sp, color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { textValue = item.quantity.toString() },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = "Tất cả (${item.quantity})", fontSize = 10.5.sp, color = AuroraGoldHover)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DIALOG BUTTONS: [HỦY] and [XÁC NHẬN]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Hủy", color = TextSecondary, fontWeight = FontWeight.Bold)
                }

                val finalQty = (textValue.toIntOrNull() ?: 1).coerceIn(1, item.quantity)
                Button(
                    onClick = { onConfirm(finalQty) },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraGold)
                ) {
                    Text(
                        text = if (isReturning) "Hoàn $finalQty" else "Chuyển $finalQty",
                        color = LightSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
