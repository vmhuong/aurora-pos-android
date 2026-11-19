package com.example.qrorderkitchen.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrorderkitchen.ui.components.CapsuleToast
import com.example.qrorderkitchen.ui.components.ConfirmDialog
import com.example.qrorderkitchen.ui.components.LuxuryTopBar
import com.example.qrorderkitchen.ui.customer.CustomerCartScreen
import com.example.qrorderkitchen.ui.customer.CustomerMenuScreen
import com.example.qrorderkitchen.ui.customer.LiveTrackingScreen
import com.example.qrorderkitchen.ui.customer.CustomerAccountScreen
import androidx.compose.material.icons.filled.Person
import com.example.qrorderkitchen.ui.staff.DishManageScreen
import com.example.qrorderkitchen.ui.staff.FloorPlanScreen
import com.example.qrorderkitchen.ui.staff.KdsScreen
import com.example.qrorderkitchen.ui.theme.BackgroundDark
import com.example.qrorderkitchen.ui.theme.GoldPrimary
import com.example.qrorderkitchen.ui.viewmodel.AppMode
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import com.example.qrorderkitchen.ui.viewmodel.CustomerTab
import com.example.qrorderkitchen.ui.viewmodel.StaffTab

@Composable
fun MainApp(viewModel: AuroraViewModel = viewModel()) {
    val appMode by viewModel.appMode.collectAsState()
    val customerTab by viewModel.customerTab.collectAsState()
    val staffTab by viewModel.staffTab.collectAsState()
    val tableId by viewModel.currentTableId.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val confirmDialogState by viewModel.showConfirmDialog.collectAsState()

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showTablePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            toastMessage = msg
        }
    }

    Scaffold(
        topBar = {
            LuxuryTopBar(
                appMode = appMode,
                currentTableId = tableId,
                onTableClick = { showTablePicker = true },
                onModeSwitchClick = {
                    if (appMode == AppMode.CUSTOMER) {
                        viewModel.showPinDialog.value = true
                    } else {
                        viewModel.setAppMode(AppMode.CUSTOMER)
                        viewModel.showToast("Đã chuyển sang Chế độ Khách hàng")
                    }
                }
            )
        },
        bottomBar = {
            if (appMode == AppMode.CUSTOMER) {
                CustomerBottomBar(
                    currentTab = customerTab,
                    cartCount = cartItems.values.sumOf { it.quantity },
                    onTabSelected = { viewModel.setCustomerTab(it) }
                )
            } else {
                StaffBottomBar(
                    currentTab = staffTab,
                    onTabSelected = { viewModel.setStaffTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (appMode == AppMode.CUSTOMER) {
                when (customerTab) {
                    CustomerTab.MENU -> CustomerMenuScreen(viewModel = viewModel)
                    CustomerTab.CART -> CustomerCartScreen(viewModel = viewModel)
                    CustomerTab.LIVE_TRACKING -> LiveTrackingScreen(viewModel = viewModel)
                    CustomerTab.ACCOUNT -> CustomerAccountScreen(viewModel = viewModel)
                }
            } else {
                when (staffTab) {
                    StaffTab.FLOOR_PLAN -> FloorPlanScreen(viewModel = viewModel)
                    StaffTab.KDS -> KdsScreen(viewModel = viewModel)
                    StaffTab.DISH_MANAGEMENT -> DishManageScreen(viewModel = viewModel)
                }
            }

            // Capsule Toast Message
            CapsuleToast(
                message = toastMessage,
                onDismiss = { toastMessage = null }
            )
        }
    }

    // Confirmation Popup Dialog
    confirmDialogState?.let { (title, msg, onConfirm) ->
        ConfirmDialog(
            title = title,
            message = msg,
            onConfirm = onConfirm,
            onDismiss = { viewModel.showConfirmDialog.value = null }
        )
    }

    // Staff PIN Dialog
    val showPin by viewModel.showPinDialog.collectAsState()
    if (showPin) {
        StaffPinDialog(
            onSuccess = {
                viewModel.showPinDialog.value = false
                viewModel.setAppMode(AppMode.STAFF)
                viewModel.showToast("🔓 Đã đăng nhập Chế độ Quản trị POS Staff")
            },
            onDismiss = { viewModel.showPinDialog.value = false }
        )
    }

    // Table Selector Dialog
    if (showTablePicker) {
        TablePickerDialog(
            currentTable = tableId,
            onSelectTable = {
                viewModel.setTableId(it)
                showTablePicker = false
            },
            onDismiss = { showTablePicker = false }
        )
    }
}

@Composable
fun CustomerBottomBar(
    currentTab: CustomerTab,
    cartCount: Int,
    onTabSelected: (CustomerTab) -> Unit
) {
    NavigationBar(
        containerColor = BackgroundDark,
        contentColor = GoldPrimary
    ) {
        NavigationBarItem(
            selected = currentTab == CustomerTab.MENU,
            onClick = { onTabSelected(CustomerTab.MENU) },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Menu") },
            label = { Text("Thực đơn", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BackgroundDark,
                selectedTextColor = GoldPrimary,
                indicatorColor = GoldPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == CustomerTab.CART,
            onClick = { onTabSelected(CustomerTab.CART) },
            icon = {
                if (cartCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = GoldPrimary, contentColor = BackgroundDark) {
                                Text(text = cartCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                    }
                } else {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                }
            },
            label = { Text("Giỏ hàng", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BackgroundDark,
                selectedTextColor = GoldPrimary,
                indicatorColor = GoldPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == CustomerTab.LIVE_TRACKING,
            onClick = { onTabSelected(CustomerTab.LIVE_TRACKING) },
            icon = { Icon(Icons.Default.ListAlt, contentDescription = "Live Tracking") },
            label = { Text("Hóa đơn", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BackgroundDark,
                selectedTextColor = GoldPrimary,
                indicatorColor = GoldPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

@Composable
fun StaffBottomBar(
    currentTab: StaffTab,
    onTabSelected: (StaffTab) -> Unit
) {
    NavigationBar(
        containerColor = BackgroundDark,
        contentColor = GoldPrimary
    ) {
        NavigationBarItem(
            selected = currentTab == StaffTab.FLOOR_PLAN,
            onClick = { onTabSelected(StaffTab.FLOOR_PLAN) },
            icon = { Icon(Icons.Default.TableRestaurant, contentDescription = "Floor Plan") },
            label = { Text("Sơ đồ bàn", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BackgroundDark,
                selectedTextColor = GoldPrimary,
                indicatorColor = GoldPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == StaffTab.KDS,
            onClick = { onTabSelected(StaffTab.KDS) },
            icon = { Icon(Icons.Default.Restaurant, contentDescription = "KDS") },
            label = { Text("Đơn bếp KDS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BackgroundDark,
                selectedTextColor = GoldPrimary,
                indicatorColor = GoldPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            selected = currentTab == StaffTab.DISH_MANAGEMENT,
            onClick = { onTabSelected(StaffTab.DISH_MANAGEMENT) },
            icon = { Icon(Icons.Default.Fastfood, contentDescription = "Dishes") },
            label = { Text("Khóa món", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BackgroundDark,
                selectedTextColor = GoldPrimary,
                indicatorColor = GoldPrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

@Composable
fun StaffPinDialog(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BackgroundDark)
                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "PIN",
                tint = GoldPrimary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ĐĂNG NHẬP POS STAFF",
                color = GoldPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )



            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it
                    errorMsg = null
                },
                placeholder = { Text("Nhập mã PIN nhân viên...", color = Color.Gray, fontSize = 12.sp) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            errorMsg?.let { msg ->
                Text(
                    text = msg,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Hủy", color = Color.LightGray)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (pin == "1234") {
                            onSuccess()
                        } else {
                            errorMsg = "Mã PIN không đúng!"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text(text = "Đăng nhập", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TablePickerDialog(
    currentTable: String,
    onSelectTable: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BackgroundDark)
                .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CHỌN SỐ BÀN PHỤC VỤ",
                color = GoldPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..12).chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { i ->
                            val tableStr = "T$i"
                            val isSelected = tableStr == currentTable
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) GoldPrimary else Color(0xFF, 0x27, 0x23, 0x1E))
                                    .clickable { onSelectTable(tableStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Bàn T$i",
                                    color = if (isSelected) BackgroundDark else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Đóng", color = Color.LightGray)
            }
        }
    }
}
