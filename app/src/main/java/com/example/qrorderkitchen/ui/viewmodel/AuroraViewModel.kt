package com.example.qrorderkitchen.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrorderkitchen.data.repository.AuroraRepository
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.models.Order
import com.example.qrorderkitchen.models.OrderItem
import com.example.qrorderkitchen.models.SelectedOption
import com.example.qrorderkitchen.models.ServiceRequest
import com.example.qrorderkitchen.models.TableModel
import com.example.qrorderkitchen.data.models.StaffRole
import com.example.qrorderkitchen.models.Customer
import com.example.qrorderkitchen.models.TransferDishItem
import com.example.qrorderkitchen.models.Voucher
import com.example.qrorderkitchen.AuroraApp
import com.example.qrorderkitchen.utils.SoundManager
import com.example.qrorderkitchen.utils.TableUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppMode {
    CUSTOMER, STAFF
}

enum class CustomerTab {
    MENU, CART, LIVE_TRACKING, ACCOUNT
}

enum class StaffTab {
    FLOOR_PLAN, KDS, DISH_MANAGEMENT
}

data class CartItem(
    val cartItemKey: String = "",
    val dish: Dish,
    var quantity: Int = 1,
    var basePrice: Double = 0.0,
    var optionsPrice: Double = 0.0,
    var unitPrice: Double = 0.0,
    var selectedOptions: List<SelectedOption> = emptyList(),
    var optionsSummary: String = "",
    var optionsSummaryEn: String = "",
    var note: String = ""
)

class AuroraViewModel : ViewModel() {
    private val repository = AuroraRepository()

    // Mode
    private val _appMode = MutableStateFlow(AppMode.STAFF)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    // Customer Navigation
    private val _customerTab = MutableStateFlow(CustomerTab.MENU)
    val customerTab: StateFlow<CustomerTab> = _customerTab.asStateFlow()

    // Staff Navigation
    private val _staffTab = MutableStateFlow(StaffTab.FLOOR_PLAN)
    val staffTab: StateFlow<StaffTab> = _staffTab.asStateFlow()

    // Current Table (Default "T4")
    private val _currentTableId = MutableStateFlow("T4")
    val currentTableId: StateFlow<String> = _currentTableId.asStateFlow()

    // Dishes State
    private val _dishes = MutableStateFlow<List<Dish>>(emptyList())
    val dishes: StateFlow<List<Dish>> = _dishes.asStateFlow()

    // Firestore Tables State
    private val _firestoreTables = MutableStateFlow<List<TableModel>>(emptyList())
    val firestoreTables: StateFlow<List<TableModel>> = _firestoreTables.asStateFlow()

    // Active Orders State
    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())
    val activeOrders: StateFlow<List<Order>> = _activeOrders.asStateFlow()

    // Service Requests State
    private val _serviceRequests = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val serviceRequests: StateFlow<List<ServiceRequest>> = _serviceRequests.asStateFlow()

    // Cart State
    private val _cartItems = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cartItems: StateFlow<Map<String, CartItem>> = _cartItems.asStateFlow()

    // Anti-Spam / Anti-Duplicate Order Submission Guard
    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()
    private var lastOrderSubmitTime = 0L

    // Kitchen Struck Items State (Item Strike-through in KDS)
    private val _struckItemKeys = MutableStateFlow<Set<String>>(emptySet())
    val struckItemKeys: StateFlow<Set<String>> = _struckItemKeys.asStateFlow()

    // Search Query & Category Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("TẤT CẢ")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Toast Capsule Message Flow
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Dialog States
    val showPinDialog = MutableStateFlow(false)
    val showDishDetail = MutableStateFlow<Dish?>(null)
    val selectedTableDetail = MutableStateFlow<TableModel?>(null)
    val showReceiptModal = MutableStateFlow<List<Order>?>(null)
    val showConfirmDialog = MutableStateFlow<Triple<String, String, () -> Unit>?>(null)
    val showTransferForTable = MutableStateFlow<TableModel?>(null)
    val transferSourceTable = MutableStateFlow<TableModel?>(null)
    val splitTransferSession = MutableStateFlow<Pair<TableModel, TableModel>?>(null)

    // Attached Customer per table for loyalty points
    private val _attachedCustomers = MutableStateFlow<Map<String, Customer>>(emptyMap())
    val attachedCustomers: StateFlow<Map<String, Customer>> = _attachedCustomers.asStateFlow()

    fun getAttachedCustomer(tableId: String): Customer? {
        val clean = tableId.trim().replace("Table ", "", ignoreCase = true).replace("table_", "", ignoreCase = true).replace("Bàn ", "", ignoreCase = true).uppercase()
        return _attachedCustomers.value[clean]
    }

    fun attachCustomerToTable(tableId: String, customer: Customer) {
        val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
        _attachedCustomers.value = _attachedCustomers.value + (clean to customer)
        viewModelScope.launch {
            repository.updateTableCustomer(clean, customer)
        }
    }

    fun detachCustomerFromTable(tableId: String) {
        val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
        _attachedCustomers.value = _attachedCustomers.value - clean
        _usePointsForTable.value = _usePointsForTable.value - clean
        viewModelScope.launch {
            repository.updateTableCustomer(clean, null)
            repository.updateTableUsePoints(clean, false)
        }
    }

    // Applied Voucher per table
    private val _appliedVouchers = MutableStateFlow<Map<String, Voucher?>>(emptyMap())
    val appliedVouchers: StateFlow<Map<String, Voucher?>> = _appliedVouchers.asStateFlow()

    fun getAppliedVoucher(tableId: String): Voucher? {
        val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
        return _appliedVouchers.value[clean]
    }

    fun applyVoucherToTable(tableId: String, voucher: Voucher?) {
        val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
        val oldVoucher = _appliedVouchers.value[clean]
        _appliedVouchers.value = if (voucher != null) {
            _appliedVouchers.value + (clean to voucher)
        } else {
            _appliedVouchers.value - clean
        }
        viewModelScope.launch {
            repository.updateTableVoucher(clean, voucher)
            if (voucher != null) {
                val codeToClaim = if (voucher.code.isNotBlank()) voucher.code else voucher.id
                repository.claimVoucherForTable(codeToClaim, clean)
            } else if (oldVoucher != null) {
                // Tuyệt đối không giải phóng nếu voucher này đã được dùng thanh toán
                if (!oldVoucher.status.equals("used", ignoreCase = true)) {
                    val codeToRelease = if (oldVoucher.code.isNotBlank()) oldVoucher.code else oldVoucher.id
                    if (codeToRelease.isNotBlank()) {
                        repository.releaseVoucherCode(codeToRelease)
                    }
                }
            }
        }
    }

    // Use points toggle per table
    private val _usePointsForTable = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val usePointsForTable: StateFlow<Map<String, Boolean>> = _usePointsForTable.asStateFlow()

    fun isUsingPoints(tableId: String): Boolean {
        val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
        return _usePointsForTable.value[clean] ?: false
    }

    fun setUsePointsForTable(tableId: String, use: Boolean) {
        val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
        _usePointsForTable.value = _usePointsForTable.value + (clean to use)
        viewModelScope.launch {
            repository.updateTableUsePoints(clean, use)
        }
    }

    suspend fun searchCustomerByPhone(phone: String): Customer? {
        return repository.searchCustomerByPhone(phone)
    }

    suspend fun registerCustomer(phone: String, name: String): Customer? {
        return repository.registerCustomer(phone, name)
    }

    suspend fun getVoucherByCode(code: String, tableId: String? = null): Voucher? {
        return repository.getVoucherByCode(code, tableId)
    }

    // Trạng thái theo dõi đơn hàng và yêu cầu phục vụ mới để phát chuông
    private var isOrdersInitialized = false
    private val knownOrderIds = mutableSetOf<String>()

    private var isServiceInitialized = false
    private val knownServiceIds = mutableSetOf<String>()

    init {
        // Collect Dishes
        viewModelScope.launch {
            repository.getDishesFlow().collect { list ->
                _dishes.value = list
            }
        }

        // Collect Firestore Tables & Realtime Customer / Voucher / UsePoints Sync
        viewModelScope.launch {
            repository.getTablesFlow().collect { list ->
                _firestoreTables.value = list
                val custMap = mutableMapOf<String, Customer>()
                val voucherMap = mutableMapOf<String, Voucher?>()
                val pointsMap = mutableMapOf<String, Boolean>()
                for (t in list) {
                    val c = t.tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                    if (t.attachedCustomer != null) {
                        custMap[c] = t.attachedCustomer!!
                    }
                    if (t.appliedVoucher != null) {
                        voucherMap[c] = t.appliedVoucher
                    }
                    pointsMap[c] = t.usePoints
                }
                _attachedCustomers.value = custMap
                _appliedVouchers.value = voucherMap
                _usePointsForTable.value = pointsMap
            }
        }

        // Collect Orders
        viewModelScope.launch {
            repository.getOrdersFlow().collect { orders ->
                // Batch-collect struck item keys to avoid redundant StateFlow emissions per item
                val newStruckKeys = _struckItemKeys.value.toMutableSet()
                val newPendingOrders = mutableListOf<Order>()

                for (ord in orders) {
                    if (ord.items.isEmpty()) {
                        ord.items = repository.getItemsForOrder(ord.id)
                    }
                    for (item in ord.items) {
                        if (item.is_completed) {
                            val k = if (item.id.isNotBlank()) item.id else "${ord.id}_${item.dish_name}"
                            newStruckKeys.add(k)
                        }
                    }

                    // Phát hiện đơn pending mới
                    if (isOrdersInitialized && !knownOrderIds.contains(ord.id) && ord.status == "pending") {
                        newPendingOrders.add(ord)
                    }
                    knownOrderIds.add(ord.id)
                }

                _struckItemKeys.value = newStruckKeys
                _activeOrders.value = orders

                if (!isOrdersInitialized) {
                    isOrdersInitialized = true
                } else if (newPendingOrders.isNotEmpty()) {
                    // CHỈ PHÁT CHUÔNG CHO BẾP (Khi đang ở màn hình Đơn Bếp KDS)
                    // Bỏ chuông ngoài sơ đồ bàn
                    if (_staffTab.value == StaffTab.KDS) {
                        try {
                            val firstTable = newPendingOrders.first().table_id.uppercase()
                            SoundManager.playOrderChime(AuroraApp.instance)
                            showToast("🔔 BÀN $firstTable VỪA GỬI ĐƠN HÀNG MỚI!")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // Collect Service Requests
        viewModelScope.launch {
            repository.getServiceRequestsFlow().collect { list ->
                val newPendingRequests = mutableListOf<ServiceRequest>()
                for (req in list) {
                    if (isServiceInitialized && !knownServiceIds.contains(req.id) && req.status == "pending") {
                        newPendingRequests.add(req)
                    }
                    knownServiceIds.add(req.id)
                }

                _serviceRequests.value = list

                if (!isServiceInitialized) {
                    isServiceInitialized = true
                } else if (newPendingRequests.isNotEmpty()) {
                    try {
                        val req = newPendingRequests.first()
                        val tableCode = req.table_id.uppercase()
                        val typeStr = if (req.type == "checkout") "THANH TOÁN TẠI BÀN" else "PHỤC VỤ"
                        val noteStr = if (req.note.isNotBlank()) ": ${req.note}" else ""
                        SoundManager.playServiceChime(AuroraApp.instance, req.type)
                        showToast("🔔 BÀN $tableCode YÊU CẦU $typeStr$noteStr!")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }

    // Thoát bàn trở về Sơ đồ Bàn: Giải phóng khóa nhân viên, tự động xóa giỏ hàng, và đặt lại bàn trống nếu chưa gửi đơn
    fun exitTableAndReturnToFloorPlan() {
        val tableId = _currentTableId.value
        if (tableId.isNotBlank()) {
            // 1. Giải phóng khóa nhân viên phụ trách trên Firestore ngay khi thoát bàn
            repository.releaseTableStaff(tableId)

            // 2. Kiểm tra xem bàn này có đơn hàng đang hoạt động không
            val hasActiveOrders = _activeOrders.value.any { ord ->
                TableUtils.isSameTable(ord.table_id, tableId) && ord.status != "settled" && ord.status != "paid"
            }

            // 3. Nếu là bàn trống và chưa gửi order nào, giữ nguyên/trả về trạng thái available (chưa có khách)
            if (!hasActiveOrders) {
                repository.updateTableStatus(tableId, "available")
            }
        }

        // 4. Luôn tự động xóa giỏ hàng khi thoát bàn
        clearCart()

        // 5. Đặt lại tab về MENU
        _customerTab.value = CustomerTab.MENU

        // 6. Quay trở lại chế độ STAFF (Sơ đồ Bàn)
        _appMode.value = AppMode.STAFF
    }

    fun setCustomerTab(tab: CustomerTab) {
        _customerTab.value = tab
    }

    fun setStaffTab(tab: StaffTab) {
        _staffTab.value = tab
    }

    fun setTableId(tableId: String) {
        _currentTableId.value = tableId
        showToast("Đã chọn Bàn $tableId")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Cart Operations
    fun addToCart(
        dish: Dish,
        quantityDelta: Int = 1,
        note: String = "",
        selectedOptions: List<SelectedOption> = emptyList(),
        optionsSummary: String = "",
        optionsSummaryEn: String = "",
        optionsPrice: Double = 0.0
    ) {
        if (!dish.isCurrentlyAvailable) {
            showToast("Món ${dish.name} hiện đang tạm khóa hoặc hết hàng!")
            return
        }
        val current = _cartItems.value.toMutableMap()
        val optKey = selectedOptions.map { "${it.item_name}:${it.price}" }.sorted().joinToString("|")
        val itemKey = "${dish.id}__${optKey}__${note}"
        val basePrice = dish.price
        val effectiveOptionsPrice = if (optionsPrice > 0.0) optionsPrice else selectedOptions.sumOf { it.price }
        val unitPrice = basePrice + effectiveOptionsPrice

        val existing = current[itemKey]
        if (existing != null) {
            val newQty = existing.quantity + quantityDelta
            if (newQty <= 0) {
                current.remove(itemKey)
            } else {
                current[itemKey] = existing.copy(quantity = newQty, note = if (note.isNotBlank()) note else existing.note)
            }
        } else if (quantityDelta > 0) {
            current[itemKey] = CartItem(
                cartItemKey = itemKey,
                dish = dish,
                quantity = quantityDelta,
                basePrice = basePrice,
                optionsPrice = effectiveOptionsPrice,
                unitPrice = unitPrice,
                selectedOptions = selectedOptions,
                optionsSummary = optionsSummary,
                optionsSummaryEn = optionsSummaryEn,
                note = note
            )
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(itemKey: String, delta: Int) {
        val current = _cartItems.value.toMutableMap()
        val existing = current[itemKey] ?: return
        val newQty = existing.quantity + delta
        if (newQty <= 0) {
            current.remove(itemKey)
        } else {
            current[itemKey] = existing.copy(quantity = newQty)
        }
        _cartItems.value = current
    }

    fun updateCartItemNote(itemKey: String, newNote: String) {
        val current = _cartItems.value.toMutableMap()
        val existing = current[itemKey] ?: return
        current[itemKey] = existing.copy(note = newNote)
        _cartItems.value = current
    }

    fun removeFromCart(itemKey: String) {
        val current = _cartItems.value.toMutableMap()
        current.remove(itemKey)
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyMap()
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }

    // Submit Customer Order with Anti-Spam / Anti-Duplicate Guard
    fun submitCustomerOrder(context: Context) {
        val now = System.currentTimeMillis()
        if (_isSubmittingOrder.value || (now - lastOrderSubmitTime < 2500L)) {
            // Đang xử lý gửi đơn hoặc vừa mới gửi trong vòng 2.5s -> Chặn tuyệt đối spam trùng đơn
            return
        }

        val itemsMap = _cartItems.value
        if (itemsMap.isEmpty()) {
            showToast("Giỏ hàng của bạn đang trống!")
            return
        }

        // Kiểm tra xem có món nào trong giỏ đã bị khóa hoặc hết hàng không
        val lockedItem = itemsMap.values.find { !it.dish.isCurrentlyAvailable }
        if (lockedItem != null) {
            showToast("Món ${lockedItem.dish.name} hiện đã hết hàng hoặc tạm khóa, vui lòng xóa khỏi giỏ!")
            return
        }

        // Lập tức kích hoạt cờ khóa trước khi chạy Coroutine
        _isSubmittingOrder.value = true
        lastOrderSubmitTime = now

        val tableId = _currentTableId.value
        val itemsList = itemsMap.values.map { cart ->
            val effectiveOptionsPrice = if (cart.optionsPrice > 0.0) cart.optionsPrice else cart.selectedOptions.sumOf { it.price }
            val effectiveBasePrice = if (cart.basePrice > 0.0) cart.basePrice else cart.dish.price
            val effectiveUnitPrice = if (cart.unitPrice > 0.0) cart.unitPrice else (effectiveBasePrice + effectiveOptionsPrice)
            val isCombo = cart.dish.is_combo || cart.dish.category.equals("COMBO", ignoreCase = true)
            OrderItem(
                dish_id = cart.dish.id,
                dish_name = cart.dish.name,
                dish_name_en = cart.dish.name_en,
                quantity = cart.quantity,
                price = effectiveUnitPrice,
                base_price = effectiveBasePrice,
                options_price = effectiveOptionsPrice,
                options_summary = cart.optionsSummary,
                options_summary_en = cart.optionsSummaryEn,
                is_combo = isCombo,
                combo_summary = if (isCombo) cart.optionsSummary else "",
                selected_options = cart.selectedOptions,
                note = cart.note
            )
        }
        val total = itemsList.sumOf { it.quantity * it.price }

        viewModelScope.launch {
            try {
                val orderId = repository.createOrder(tableId, itemsList, total)
                if (orderId != null) {
                    clearCart()
                    showToast("Đã đặt món thành công cho Bàn $tableId!")
                    setCustomerTab(CustomerTab.LIVE_TRACKING)
                } else {
                    showToast("Đặt món thất bại hoặc đơn đã được ghi nhận!")
                }
            } catch (e: Exception) {
                showToast("Lỗi gửi đơn: ${e.message}")
            } finally {
                _isSubmittingOrder.value = false
            }
        }
    }

    // Request Service / Checkout with confirmation dialog
    fun confirmRequestService(context: Context, type: String) {
        val typeTitle = if (type == "checkout") "yêu cầu thanh toán" else "gọi nhân viên hỗ trợ"
        showConfirmDialog.value = Triple(
            "Xác nhận gọi phục vụ",
            "Bạn có chắc chắn muốn $typeTitle đến Bàn ${_currentTableId.value} không?"
        ) {
            viewModelScope.launch {
                val ok = repository.requestService(_currentTableId.value, type)
                if (ok) {
                    showToast("Đã gửi yêu cầu tới nhân viên!")
                    repository.ringAndVibrate(context)
                } else {
                    showToast("Gửi yêu cầu thất bại!")
                }
            }
        }
    }

    // Staff / POS Operations
    fun cancelSingleDish(orderId: String, itemIndex: Int, cancelQty: Int, reason: String, staffName: String, staffRole: StaffRole) {
        if (staffRole != StaffRole.MANAGER) {
            showToast("QUYỀN HẠN BỊ TỪ CHỐI: Chỉ Quản lý (Manager) mới có quyền hủy món!")
            return
        }
        viewModelScope.launch {
            val success = repository.cancelSingleDish(orderId, itemIndex, cancelQty, reason, staffName)
            if (success) {
                showToast("Đã hủy món thành công!")
            } else {
                showToast("Hủy món thất bại!")
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String, staffName: String, staffRole: StaffRole) {
        if (staffRole != StaffRole.MANAGER) {
            showToast("⛔ QUYỀN HẠN BỊ TỪ CHỐI: Chỉ Quản lý (Manager) mới có quyền hủy đợt gọi món!")
            return
        }
        viewModelScope.launch {
            val success = repository.cancelOrder(orderId, reason, staffName)
            if (success) {
                showToast("Đã hủy đợt gọi món thành công!")
                selectedTableDetail.value = null
            } else {
                showToast("Hủy đợt gọi món thất bại!")
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        repository.updateOrderStatus(orderId, newStatus)
        showToast("Đã cập nhật trạng thái đơn!")
    }

    fun resolveServiceRequest(requestId: String) {
        repository.resolveServiceRequest(requestId)
        showToast("Đã xử lý xong yêu cầu!")
    }

    fun settleTable(tableId: String) {
        val cust = getAttachedCustomer(tableId)
        settleTableWithDetails(
            tableId = tableId,
            paymentMethod = "cash",
            voucherCode = null,
            customer = cust,
            pointsUsed = 0.0,
            netPaid = 0.0,
            billTotal = 0.0
        )
    }

    fun settleTableWithDetails(
        tableId: String,
        paymentMethod: String = "cash",
        voucherCode: String? = null,
        customer: Customer? = null,
        pointsUsed: Double = 0.0,
        netPaid: Double = 0.0,
        billTotal: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                repository.settleTableOrders(
                    tableId = tableId,
                    paymentMethod = paymentMethod,
                    voucherCode = voucherCode,
                    customer = customer,
                    pointsUsed = pointsUsed,
                    netPaid = netPaid,
                    billTotal = billTotal
                )
                val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                _appliedVouchers.value = _appliedVouchers.value - clean
                _usePointsForTable.value = _usePointsForTable.value - clean
                detachCustomerFromTable(tableId)
                clearCart() // Khi kết bàn thì cũng tự động xóa sạch giỏ hàng
                selectedTableDetail.value = null
                showTransferForTable.value = null
                transferSourceTable.value = null
                splitTransferSession.value = null
                showReceiptModal.value = null
                showToast("Đã thanh toán và trả Bàn $tableId!")
            } catch (e: Exception) {
                val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                if (e.message == "VOUCHER_ALREADY_USED") {
                    _appliedVouchers.value = _appliedVouchers.value - clean
                    showToast("CẢNH BÁO: Mã voucher này đã được thanh toán tại bàn khác! Đã tự động gỡ bỏ.")
                } else if (e.message == "VOUCHER_LOCKED_BY_ANOTHER_TABLE") {
                    _appliedVouchers.value = _appliedVouchers.value - clean
                    showToast("CẢNH BÁO: Mã voucher này đang bị áp dụng tại bàn khác! Đã tự động gỡ bỏ.")
                } else {
                    showToast("Lỗi thanh toán: ${e.localizedMessage}")
                }
            }
        }
    }

    fun startTableTransfer(table: TableModel, staffUsername: String = "", staffName: String = "") {
        transferSourceTable.value = table
        selectedTableDetail.value = null
        if (staffUsername.isNotBlank()) {
            repository.lockTableForTransfer(table.tableId, staffUsername, staffName)
        }
        showToast("Vui lòng chọn bàn đích trên sơ đồ để chuyển/gộp")
    }

    fun cancelTransferSelection() {
        transferSourceTable.value?.let { src ->
            repository.unlockTableTransfer(src.tableId)
        }
        transferSourceTable.value = null
    }

    fun selectDestinationTable(targetTable: TableModel) {
        val source = transferSourceTable.value ?: return
        if (source.tableId.equals(targetTable.tableId, ignoreCase = true)) {
            showToast("Bàn đích không được trùng với Bàn nguồn (${source.tableId})!")
            return
        }
        if (!targetTable.transferringByUsername.isNullOrBlank()) {
            showToast("⚠️ Bàn ${targetTable.tableId} đang được ${targetTable.transferringByName ?: "NV khác"} điều chuyển món!")
            return
        }
        transferSourceTable.value = null
        splitTransferSession.value = Pair(source, targetTable)
    }

    fun cancelSplitTransfer() {
        splitTransferSession.value?.let { (source, _) ->
            repository.unlockTableTransfer(source.tableId)
        }
        splitTransferSession.value = null
    }

    fun commitSplitTransfer(
        sourceTableId: String,
        targetTableId: String,
        originalSourceOrders: List<Order>,
        remainingSourceItems: List<TransferDishItem>,
        movedItems: List<TransferDishItem>
    ) {
        viewModelScope.launch {
            val success = repository.commitSplitTransfer(
                sourceTableId = sourceTableId,
                targetTableId = targetTableId,
                originalSourceOrders = originalSourceOrders,
                remainingSourceItems = remainingSourceItems,
                movedItems = movedItems
            )
            repository.unlockTableTransfer(sourceTableId)
            if (success) {
                splitTransferSession.value = null
                selectedTableDetail.value = null
                showReceiptModal.value = null
                showToast("✓ Đã lưu và chuyển bàn thành công!")
            } else {
                showToast("⚠️ Lưu chuyển bàn thất bại, vui lòng thử lại!")
            }
        }
    }

    fun transferTable(fromTableId: String, toTableId: String) {
        viewModelScope.launch {
            val success = repository.transferTableOrders(fromTableId, toTableId)
            if (success) {
                selectedTableDetail.value = null
                showReceiptModal.value = null
                showToast("✓ Đã chuyển/gộp Bàn $fromTableId sang Bàn $toTableId!")
            } else {
                showToast("⚠️ Chuyển/gộp bàn thất bại, vui lòng thử lại!")
            }
        }
    }

    fun toggleItemCompleted(orderId: String, item: OrderItem) {
        val key = if (item.id.isNotBlank()) item.id else "${orderId}_${item.dish_name}"
        val isCurrentlyStruck = _struckItemKeys.value.contains(key) || item.is_completed
        val newStruck = !isCurrentlyStruck

        _struckItemKeys.value = if (newStruck) {
            _struckItemKeys.value + key
        } else {
            _struckItemKeys.value - key
        }
        item.is_completed = newStruck

        if (item.id.isNotBlank()) {
            repository.toggleOrderItemCompleted(item.id, newStruck)
        }
        showToast(if (newStruck) "✓ Đã gạch xong: ${item.dish_name}" else "Đã bỏ gạch: ${item.dish_name}")
    }

    fun toggleDish(dishId: String, isAvailable: Boolean) {
        repository.toggleDishAvailability(dishId, isAvailable)
        showToast(if (isAvailable) "Đã mở bán lại món!" else "Đã tạm khóa món!")
    }

    fun toggleDishTimed(dishId: String, lockedUntil: com.google.firebase.Timestamp?) {
        repository.toggleDishTimed(dishId, lockedUntil)
        showToast(if (lockedUntil == null) "Đã mở khóa món!" else "Đã khóa tạm món theo thời gian!")
    }

    fun assignStaffToTable(tableId: String, staffUsername: String, staffName: String) {
        repository.assignStaffToTable(tableId, staffUsername, staffName)
    }

    fun releaseAllTablesForStaff(staffUsername: String) {
        viewModelScope.launch {
            repository.releaseAllTablesForStaff(staffUsername)
        }
    }

    fun clearSessionState() {
        _appMode.value = AppMode.STAFF
        _staffTab.value = StaffTab.FLOOR_PLAN
        _customerTab.value = CustomerTab.MENU
        clearCart()
        selectedTableDetail.value = null
        showTransferForTable.value = null
        showReceiptModal.value = null
    }
}
