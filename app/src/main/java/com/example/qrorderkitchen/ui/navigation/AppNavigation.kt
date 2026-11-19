package com.example.qrorderkitchen.ui.navigation

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.ui.platform.LocalContext
import com.example.qrorderkitchen.utils.SoundManager
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightSurfaceSubtle
import com.example.qrorderkitchen.ui.theme.TextMuted
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrorderkitchen.data.models.StaffModel
import com.example.qrorderkitchen.data.models.StaffRole
import com.example.qrorderkitchen.ui.auth.LoginScreen
import com.example.qrorderkitchen.ui.components.CapsuleToast
import com.example.qrorderkitchen.ui.components.ConfirmDialog
import com.example.qrorderkitchen.ui.customer.CustomerCartScreen
import com.example.qrorderkitchen.ui.customer.CustomerMenuScreen
import com.example.qrorderkitchen.ui.customer.LiveTrackingScreen
import com.example.qrorderkitchen.ui.customer.CustomerAccountScreen
import androidx.compose.material.icons.filled.Person
import com.example.qrorderkitchen.ui.staff.DishManageScreen
import com.example.qrorderkitchen.ui.staff.FloorPlanScreen
import com.example.qrorderkitchen.ui.staff.KdsScreen
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.LightBgWarm
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightBorderStrong
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
import com.example.qrorderkitchen.ui.viewmodel.StaffTab

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    auroraViewModel: AuroraViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val showLogoutDialog by authViewModel.showLogoutDialog.collectAsState()

    var toastMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        launch {
            authViewModel.toastMessage.collect { msg ->
                toastMsg = msg
            }
        }
        launch {
            auroraViewModel.toastEvent.collect { msg ->
                toastMsg = msg
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(LightBgWarm)) {
        when (val state = authState) {
            is AuthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AuroraGold)
                }
            }
            is AuthState.Unauthenticated -> {
                LoginScreen(authViewModel = authViewModel)
            }
            is AuthState.Authenticated -> {
                RoleWorkspaceScreen(
                    staff = state.staff,
                    auroraViewModel = auroraViewModel,
                    authViewModel = authViewModel,
                    onLogoutClick = { authViewModel.showLogoutDialog.value = true }
                )
            }
        }

        CapsuleToast(
            message = toastMsg,
            onDismiss = { toastMsg = null }
        )

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            ConfirmDialog(
                title = "Đăng xuất ca làm việc",
                message = "Bạn có chắc chắn muốn đăng xuất tài khoản nhân viên hiện tại không?",
                onConfirm = { authViewModel.logout(auroraViewModel) },
                onDismiss = { authViewModel.showLogoutDialog.value = false }
            )
        }
    }
}

@Composable
fun RoleWorkspaceScreen(
    staff: StaffModel,
    auroraViewModel: AuroraViewModel,
    authViewModel: AuthViewModel,
    onLogoutClick: () -> Unit
) {
    val appMode by auroraViewModel.appMode.collectAsState()
    val customerTab by auroraViewModel.customerTab.collectAsState()
    val staffTab by auroraViewModel.staffTab.collectAsState()
    val cartItems by auroraViewModel.cartItems.collectAsState()

    LaunchedEffect(staff.id) {
        if (staff.staffRole == StaffRole.CUSTOMER_KIOSK) {
            auroraViewModel.setAppMode(AppMode.CUSTOMER)
        } else if (staff.staffRole == StaffRole.KITCHEN) {
            auroraViewModel.setStaffTab(StaffTab.KDS)
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val showExitDialog by authViewModel.showExitDialog.collectAsState()

    // Khi người dùng bấm nút Back hệ thống lúc đang ở trong bàn, tự động giải phóng bàn & xóa giỏ hàng
    BackHandler(enabled = appMode == AppMode.CUSTOMER && staff.staffRole != StaffRole.CUSTOMER_KIOSK) {
        auroraViewModel.exitTableAndReturnToFloorPlan()
    }

    // Khi người dùng bấm nút Back hệ thống ở màn hình chính, hỏi xác nhận đăng xuất ca làm việc và thoát app
    BackHandler(enabled = appMode == AppMode.STAFF || staff.staffRole == StaffRole.CUSTOMER_KIOSK) {
        authViewModel.showExitDialog.value = true
    }

    if (showExitDialog) {
        ConfirmDialog(
            title = "Thoát ứng dụng",
            message = "Bạn có chắc chắn muốn đăng xuất ca làm việc và thoát ứng dụng không?",
            onConfirm = {
                authViewModel.logoutAndExit(activity, auroraViewModel)
            },
            onDismiss = { authViewModel.showExitDialog.value = false }
        )
    }

    Scaffold(
        topBar = {
            StaffTopBarHeader(
                staff = staff,
                appMode = appMode,
                auroraViewModel = auroraViewModel,
                onLogoutClick = onLogoutClick
            )
        },
        bottomBar = {
            if (appMode == AppMode.CUSTOMER || staff.staffRole == StaffRole.CUSTOMER_KIOSK) {
                KioskBottomBar(
                    currentTab = customerTab,
                    cartCount = cartItems.values.sumOf { it.quantity },
                    onTabSelected = { auroraViewModel.setCustomerTab(it) }
                )
            } else {
                RbacStaffBottomBar(
                    staffRole = staff.staffRole,
                    currentTab = staffTab,
                    onTabSelected = { auroraViewModel.setStaffTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (appMode == AppMode.CUSTOMER || staff.staffRole == StaffRole.CUSTOMER_KIOSK) {
                when (customerTab) {
                    CustomerTab.MENU -> CustomerMenuScreen(viewModel = auroraViewModel)
                    CustomerTab.CART -> CustomerCartScreen(viewModel = auroraViewModel)
                    CustomerTab.LIVE_TRACKING -> LiveTrackingScreen(viewModel = auroraViewModel, authViewModel = authViewModel)
                    CustomerTab.ACCOUNT -> CustomerAccountScreen(viewModel = auroraViewModel, authViewModel = authViewModel)
                }
            } else {
                when (staff.staffRole) {
                    StaffRole.CUSTOMER_KIOSK -> {
                        when (customerTab) {
                            CustomerTab.MENU -> CustomerMenuScreen(viewModel = auroraViewModel)
                            CustomerTab.CART -> CustomerCartScreen(viewModel = auroraViewModel)
                            CustomerTab.LIVE_TRACKING -> LiveTrackingScreen(viewModel = auroraViewModel, authViewModel = authViewModel)
                            CustomerTab.ACCOUNT -> CustomerAccountScreen(viewModel = auroraViewModel, authViewModel = authViewModel)
                        }
                    }
                    StaffRole.KITCHEN -> {
                        when (staffTab) {
                            StaffTab.DISH_MANAGEMENT -> DishManageScreen(viewModel = auroraViewModel)
                            else -> KdsScreen(viewModel = auroraViewModel, authViewModel = authViewModel)
                        }
                    }
                    StaffRole.WAITER, StaffRole.MANAGER -> {
                        when (staffTab) {
                            StaffTab.FLOOR_PLAN -> FloorPlanScreen(viewModel = auroraViewModel, authViewModel = authViewModel)
                            StaffTab.KDS -> KdsScreen(viewModel = auroraViewModel, authViewModel = authViewModel)
                            StaffTab.DISH_MANAGEMENT -> DishManageScreen(viewModel = auroraViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffTopBarHeader(
    staff: StaffModel,
    appMode: AppMode = AppMode.STAFF,
    auroraViewModel: AuroraViewModel? = null,
    onLogoutClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val currentTableId by auroraViewModel?.currentTableId?.collectAsState() ?: remember { mutableStateOf("B1") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface)
            .statusBarsPadding()
            .border(0.5.dp, LightBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 14.dp, vertical = if (isLandscape) 8.dp else 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Emblem AU Logo
            Box(
                modifier = Modifier
                    .size(if (isLandscape) 36.dp else 40.dp)
                    .clip(CircleShape)
                    .background(TextPrimary)
                    .border(1.5.dp, AuroraGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AU",
                    color = AuroraGold,
                    fontSize = if (isLandscape) 12.sp else 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (appMode == AppMode.CUSTOMER) "AURORA · ĐẶT MÓN BÀN $currentTableId" else "AURORA · ${staff.staffRole.displayName.uppercase()}",
                    color = AuroraGoldHover,
                    fontSize = if (isLandscape) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${staff.name} (@${staff.username})",
                    color = TextSecondary,
                    fontSize = if (isLandscape) 11.sp else 13.sp
                )
            }

            // Return to Floor Plan Button when Staff is in Order mode
            if (appMode == AppMode.CUSTOMER && staff.staffRole != StaffRole.CUSTOMER_KIOSK) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(WebGoldPrimary)
                        .clickable { auroraViewModel?.exitTableAndReturnToFloorPlan() }
                        .padding(horizontal = if (isLandscape) 12.dp else 14.dp, vertical = if (isLandscape) 6.dp else 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "← Sơ đồ Bàn",
                        color = LightSurface,
                        fontSize = if (isLandscape) 12.sp else 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // Logout Pill Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(LightSurface)
                    .border(1.dp, LightBorderStrong, RoundedCornerShape(50.dp))
                    .clickable { onLogoutClick() }
                    .padding(horizontal = if (isLandscape) 12.dp else 14.dp, vertical = if (isLandscape) 6.dp else 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đăng xuất",
                    color = TextPrimary,
                    fontSize = if (isLandscape) 12.sp else 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun KioskBottomBar(
    currentTab: CustomerTab,
    cartCount: Int,
    onTabSelected: (CustomerTab) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val iconSize = if (isLandscape) 20.dp else 24.dp
    val labelSize = if (isLandscape) 10.sp else 12.5.sp

    NavigationBar(
        containerColor = WebCardBg,
        contentColor = WebTextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 58.dp else 66.dp)
            .border(1.dp, WebBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        NavigationBarItem(
            selected = currentTab == CustomerTab.MENU,
            onClick = { onTabSelected(CustomerTab.MENU) },
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Menu", modifier = Modifier.size(iconSize)) },
            label = { Text("Thực đơn", fontSize = labelSize, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WebGoldDark,
                selectedTextColor = WebGoldDark,
                indicatorColor = WebSurfacePill,
                unselectedIconColor = WebTextSecondary,
                unselectedTextColor = WebTextSecondary
            )
        )

        NavigationBarItem(
            selected = currentTab == CustomerTab.CART,
            onClick = { onTabSelected(CustomerTab.CART) },
            icon = {
                if (cartCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = WebGoldPrimary, contentColor = androidx.compose.ui.graphics.Color.White) {
                                Text(text = cartCount.toString(), fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 9.sp else 11.sp)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", modifier = Modifier.size(iconSize))
                    }
                } else {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", modifier = Modifier.size(iconSize))
                }
            },
            label = { Text("Giỏ hàng", fontSize = labelSize, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WebGoldDark,
                selectedTextColor = WebGoldDark,
                indicatorColor = WebSurfacePill,
                unselectedIconColor = WebTextSecondary,
                unselectedTextColor = WebTextSecondary
            )
        )

        NavigationBarItem(
            selected = currentTab == CustomerTab.LIVE_TRACKING,
            onClick = { onTabSelected(CustomerTab.LIVE_TRACKING) },
            icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Live Tracking", modifier = Modifier.size(iconSize)) },
            label = { Text("Hóa đơn", fontSize = labelSize, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WebGoldDark,
                selectedTextColor = WebGoldDark,
                indicatorColor = WebSurfacePill,
                unselectedIconColor = WebTextSecondary,
                unselectedTextColor = WebTextSecondary
            )
        )
    }
}

@Composable
fun RbacStaffBottomBar(
    staffRole: StaffRole,
    currentTab: StaffTab,
    onTabSelected: (StaffTab) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val iconSize = if (isLandscape) 20.dp else 24.dp
    val labelSize = if (isLandscape) 10.sp else 12.5.sp

    NavigationBar(
        containerColor = LightSurface,
        contentColor = TextPrimary,
        modifier = Modifier.height(if (isLandscape) 56.dp else 64.dp)
    ) {
        if (staffRole == StaffRole.MANAGER || staffRole == StaffRole.WAITER) {
            NavigationBarItem(
                selected = currentTab == StaffTab.FLOOR_PLAN,
                onClick = { onTabSelected(StaffTab.FLOOR_PLAN) },
                icon = { Icon(Icons.Default.TableRestaurant, contentDescription = "Floor Plan", modifier = Modifier.size(iconSize)) },
                label = { Text("Sơ đồ Bàn", fontSize = labelSize, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LightSurface,
                    selectedTextColor = AuroraGoldHover,
                    indicatorColor = AuroraGold,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }

        NavigationBarItem(
            selected = currentTab == StaffTab.KDS,
            onClick = { onTabSelected(StaffTab.KDS) },
            icon = { Icon(Icons.Default.Restaurant, contentDescription = "KDS", modifier = Modifier.size(iconSize)) },
            label = { Text("Đơn Bếp KDS", fontSize = labelSize, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LightSurface,
                selectedTextColor = AuroraGoldHover,
                indicatorColor = AuroraGold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        if (staffRole == StaffRole.MANAGER || staffRole == StaffRole.KITCHEN) {
            NavigationBarItem(
                selected = currentTab == StaffTab.DISH_MANAGEMENT,
                onClick = { onTabSelected(StaffTab.DISH_MANAGEMENT) },
                icon = { Icon(Icons.Default.Fastfood, contentDescription = "Dishes", modifier = Modifier.size(iconSize)) },
                label = { Text("Khóa Món", fontSize = labelSize, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LightSurface,
                    selectedTextColor = AuroraGoldHover,
                    indicatorColor = AuroraGold,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
