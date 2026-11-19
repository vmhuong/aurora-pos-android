package com.example.qrorderkitchen.data.repository

import android.content.Context
import android.media.RingtoneManager
import android.os.Vibrator
import com.example.qrorderkitchen.models.ComboGroup
import com.example.qrorderkitchen.models.ComboItem
import com.example.qrorderkitchen.models.Customer
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.models.OptionGroup
import com.example.qrorderkitchen.models.OptionItem
import com.example.qrorderkitchen.models.Order
import com.example.qrorderkitchen.models.OrderItem
import com.example.qrorderkitchen.models.ServiceRequest
import com.example.qrorderkitchen.models.TableModel
import com.example.qrorderkitchen.models.TransferDishItem
import com.example.qrorderkitchen.models.Voucher
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuroraRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun parseOrderTimestamp(doc: DocumentSnapshot): Timestamp {
        val ts = doc.getTimestamp("created_at")
            ?: doc.getTimestamp("client_time")
            ?: doc.getTimestamp("timestamp")
        if (ts != null) return ts

        val millis = doc.getLong("created_at") ?: doc.getLong("client_time") ?: doc.getLong("timestamp")
        if (millis != null) return Timestamp(Date(millis))

        val str = doc.getString("created_at") ?: doc.getString("client_time") ?: doc.getString("timestamp")
        if (!str.isNullOrBlank()) {
            try {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(str)
                if (date != null) return Timestamp(date)
            } catch (_: Exception) {}
        }

        return Timestamp.now()
    }

    // Real-time Flow for Dishes
    fun getDishesFlow(): Flow<List<Dish>> = callbackFlow {
        val listener = db.collection("dishes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val dish = doc.toObject(Dish::class.java)?.apply { id = doc.id } ?: return@mapNotNull null
                        // Đảm bảo options được parse chuẩn xác 100% từ Firestore Map/List
                        val rawOptions = doc.get("options") as? List<*>
                        if (rawOptions != null && rawOptions.isNotEmpty()) {
                            val parsedGroups = mutableListOf<OptionGroup>()
                            for (rawGroup in rawOptions) {
                                if (rawGroup is Map<*, *>) {
                                    val gName = rawGroup["name"] as? String ?: ""
                                    val gNameEn = rawGroup["name_en"] as? String ?: ""
                                    val gType = rawGroup["type"] as? String ?: "single"
                                    val gRequired = (rawGroup["required"] as? Boolean) ?: false
                                    val rawItems = rawGroup["items"] as? List<*> ?: emptyList<Any>()
                                    val parsedItems = mutableListOf<OptionItem>()
                                    for (rawItem in rawItems) {
                                        if (rawItem is Map<*, *>) {
                                            val iName = rawItem["name"] as? String ?: ""
                                            val iNameEn = rawItem["name_en"] as? String ?: ""
                                            val iPrice = when (val p = rawItem["price"]) {
                                                is Number -> p.toDouble()
                                                is String -> p.toDoubleOrNull() ?: 0.0
                                                else -> 0.0
                                            }
                                            val iDefault = (rawItem["is_default"] as? Boolean) ?: false
                                            parsedItems.add(OptionItem(iName, iNameEn, iPrice, iDefault))
                                        }
                                    }
                                    parsedGroups.add(OptionGroup(gName, gNameEn, gType, gRequired, parsedItems))
                                }
                            }
                            dish.options = parsedGroups
                        }

                        // Parse is_combo & combo_groups cho các gói Combo đồng bộ chuẩn Web
                        val rawIsCombo = doc.getBoolean("is_combo") ?: dish.category.equals("COMBO", ignoreCase = true)
                        dish.is_combo = rawIsCombo

                        val rawComboGroups = doc.get("combo_groups") as? List<*>
                        if (rawComboGroups != null && rawComboGroups.isNotEmpty()) {
                            val parsedComboGroups = mutableListOf<ComboGroup>()
                            for (rawGroup in rawComboGroups) {
                                if (rawGroup is Map<*, *>) {
                                    val title = rawGroup["group_title"] as? String ?: ""
                                    val titleEn = rawGroup["group_title_en"] as? String ?: ""
                                    val minVal = (rawGroup["min"] as? Number)?.toInt() ?: 1
                                    val maxVal = (rawGroup["max"] as? Number)?.toInt() ?: minVal
                                    val rawItems = rawGroup["items"] as? List<*> ?: emptyList<Any>()
                                    val parsedItems = mutableListOf<ComboItem>()
                                    for (rawItem in rawItems) {
                                        if (rawItem is Map<*, *>) {
                                            val dId = rawItem["dish_id"] as? String ?: ""
                                            val iName = rawItem["name"] as? String ?: ""
                                            val iNameEn = rawItem["name_en"] as? String ?: ""
                                            val iPrice = when (val pr = rawItem["price"]) {
                                                is Number -> pr.toDouble()
                                                is String -> pr.toDoubleOrNull() ?: 0.0
                                                else -> 0.0
                                            }
                                            val iExtra = when (val ex = rawItem["extra"]) {
                                                is Number -> ex.toDouble()
                                                is String -> ex.toDoubleOrNull() ?: 0.0
                                                else -> 0.0
                                            }
                                            parsedItems.add(ComboItem(dId, iName, iNameEn, iPrice, iExtra))
                                        }
                                    }
                                    parsedComboGroups.add(ComboGroup(title, titleEn, minVal, maxVal, parsedItems))
                                }
                            }
                            dish.combo_groups = parsedComboGroups
                        }

                        dish
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Tables synced directly with Firebase Firestore `tables` collection
    fun getTablesFlow(): Flow<List<TableModel>> = callbackFlow {
        val listener = db.collection("tables")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "available"
                    val name = doc.getString("name") ?: doc.getString("tableName") ?: doc.id
                    val cleanId = (doc.id.ifBlank { name }).trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                    val staffUsername = doc.getString("assigned_staff_username")
                    val staffName = doc.getString("assigned_staff_name")
                    val transferringByUsername = doc.getString("transferring_by_username")
                    val transferringByName = doc.getString("transferring_by_name")

                    val attachedCustomerRaw = doc.get("attached_customer") as? Map<*, *>
                    val custObj = if (attachedCustomerRaw != null) {
                        Customer(
                            id = (attachedCustomerRaw["id"] as? String) ?: (attachedCustomerRaw["phone"] as? String) ?: "",
                            name = (attachedCustomerRaw["name"] as? String) ?: "",
                            phone = (attachedCustomerRaw["phone"] as? String) ?: "",
                            tier = (attachedCustomerRaw["tier"] as? String) ?: "member",
                            points = when (val p = attachedCustomerRaw["points"]) {
                                is Number -> p.toDouble()
                                is String -> p.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            },
                            total_spend = when (val s = attachedCustomerRaw["total_spend"]) {
                                is Number -> s.toDouble()
                                is String -> s.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                        )
                    } else null

                    val appliedVoucherRaw = doc.get("applied_voucher") as? Map<*, *>
                    // Nếu bàn đang available (bàn trống) thì tuyệt đối không tải voucher cũ còn sót lại từ phiên trước
                    if (status.equals("available", ignoreCase = true) && appliedVoucherRaw != null) {
                        db.collection("tables").document(cleanId).update("applied_voucher", null)
                    }
                    val voucherObj = if (appliedVoucherRaw != null && !status.equals("available", ignoreCase = true)) {
                        val idRaw = (appliedVoucherRaw["id"] as? String)?.trim() ?: ""
                        val codeRaw = (appliedVoucherRaw["code"] as? String)?.trim() ?: ""
                        val validId = if (idRaw.isNotBlank()) idRaw else codeRaw
                        val validCode = if (codeRaw.isNotBlank()) codeRaw else validId

                        val vVal = when (val v = appliedVoucherRaw["value"] ?: appliedVoucherRaw["discount_value"]) {
                            is Number -> v.toDouble()
                            is String -> v.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        val dAmt = when (val da = appliedVoucherRaw["discount_amount"]) {
                            is Number -> da.toDouble()
                            is String -> da.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        val finalVal = if (vVal > 0.0) vVal else dAmt

                        Voucher(
                            id = validId,
                            code = validCode,
                            campaign_id = (appliedVoucherRaw["campaign_id"] as? String) ?: "",
                            campaign_name = (appliedVoucherRaw["campaign_name"] as? String) ?: "",
                            type = (appliedVoucherRaw["type"] as? String) ?: (appliedVoucherRaw["discount_type"] as? String) ?: "fixed",
                            value = finalVal,
                            discount_value = finalVal,
                            discount_amount = dAmt,
                            max_discount = when (val m = appliedVoucherRaw["max_discount"]) {
                                is Number -> m.toDouble()
                                is String -> m.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            },
                            min_bill = when (val mb = appliedVoucherRaw["min_bill"] ?: appliedVoucherRaw["min_order_value"]) {
                                is Number -> mb.toDouble()
                                is String -> mb.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            },
                            specific_dish_id = (appliedVoucherRaw["specific_dish_id"] as? String) ?: "",
                            specific_dish_name = (appliedVoucherRaw["specific_dish_name"] as? String) ?: "",
                            status = (appliedVoucherRaw["status"] as? String) ?: "available"
                        )
                    } else null

                    val usePoints = doc.getBoolean("use_points") ?: false

                    TableModel(
                        tableId = cleanId,
                        tableName = name,
                        status = status,
                        assignedStaffUsername = staffUsername,
                        assignedStaffName = staffName,
                        transferringByUsername = transferringByUsername,
                        transferringByName = transferringByName,
                        attachedCustomer = custObj,
                        appliedVoucher = voucherObj,
                        usePoints = usePoints
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Active Orders with complete items resolution
    fun getOrdersFlow(): Flow<List<Order>> = callbackFlow {
        var ordersList = emptyList<Order>()
        var itemsMap = emptyMap<String, List<OrderItem>>()

        fun emitCombined() {
            val combined = ordersList.map { order ->
                if (order.items.isEmpty()) {
                    val its = itemsMap[order.id] ?: emptyList()
                    order.copy(items = its)
                } else {
                    order
                }
            }
            trySend(combined.sortedByDescending { it.created_at })
        }

        val ordersListener = db.collection("orders")
            .whereIn("status", listOf("pending", "cooking", "done"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                ordersList = snapshot?.documents?.mapNotNull { doc ->
                    val ord = doc.toObject(Order::class.java)?.apply { id = doc.id } ?: return@mapNotNull null
                    if (ord.created_at == null) {
                        ord.created_at = parseOrderTimestamp(doc)
                    }
                    // Parse trực tiếp các items từ Document orders để luôn có note và options
                    val rawItems = doc.get("items") as? List<*>
                    if (rawItems != null && rawItems.isNotEmpty()) {
                        val parsedItems = mutableListOf<OrderItem>()
                        for (rawItem in rawItems) {
                            if (rawItem is Map<*, *>) {
                                val dName = (rawItem["dish_name"] as? String) ?: (rawItem["name"] as? String) ?: ""
                                val dNameEn = (rawItem["dish_name_en"] as? String) ?: (rawItem["name_en"] as? String) ?: ""
                                val dId = (rawItem["dish_id"] as? String) ?: (rawItem["id"] as? String) ?: ""
                                val qty = when (val q = rawItem["quantity"]) {
                                    is Number -> q.toInt()
                                    is String -> q.toIntOrNull() ?: 1
                                    else -> 1
                                }
                                val price = when (val p = rawItem["price"]) {
                                    is Number -> p.toDouble()
                                    is String -> p.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val basePrice = when (val p = rawItem["base_price"]) {
                                    is Number -> p.toDouble()
                                    is String -> p.toDoubleOrNull() ?: 0.0
                                    else -> price
                                }
                                val optPrice = when (val p = rawItem["options_price"]) {
                                    is Number -> p.toDouble()
                                    is String -> p.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val optSummary = (rawItem["options_summary"] as? String) ?: ""
                                val optSummaryEn = (rawItem["options_summary_en"] as? String) ?: ""
                                val isCombo = (rawItem["is_combo"] as? Boolean) ?: false
                                val comboSummary = (rawItem["combo_summary"] as? String) ?: ""
                                val note = (rawItem["note"] as? String) ?: ""
                                val isCompleted = (rawItem["is_completed"] as? Boolean) ?: false
                                parsedItems.add(
                                    OrderItem(
                                        id = "${doc.id}_$dName",
                                        order_id = doc.id,
                                        dish_id = dId,
                                        dish_name = dName,
                                        dish_name_en = dNameEn,
                                        quantity = qty,
                                        price = price,
                                        base_price = basePrice,
                                        options_price = optPrice,
                                        options_summary = optSummary,
                                        options_summary_en = optSummaryEn,
                                        is_combo = isCombo,
                                        combo_summary = comboSummary,
                                        note = note,
                                        is_completed = isCompleted
                                    )
                                )
                            }
                        }
                        ord.items = parsedItems
                    }
                    ord
                } ?: emptyList()
                emitCombined()
            }

        val itemsListener = db.collection("order_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val allItems = snapshot?.documents?.mapNotNull { doc ->
                    val it = doc.toObject(OrderItem::class.java)?.apply { id = doc.id } ?: return@mapNotNull null
                    if (it.note.isBlank()) {
                        it.note = doc.getString("note") ?: ""
                    }
                    if (it.options_summary.isBlank()) {
                        it.options_summary = doc.getString("options_summary") ?: ""
                    }
                    if (it.combo_summary.isBlank()) {
                        it.combo_summary = doc.getString("combo_summary") ?: ""
                    }
                    if (!it.is_combo) {
                        it.is_combo = doc.getBoolean("is_combo") ?: false
                    }
                    it
                } ?: emptyList()
                itemsMap = allItems.groupBy { it.order_id }
                emitCombined()
            }

        awaitClose {
            ordersListener.remove()
            itemsListener.remove()
        }
    }

    // Real-time Flow for Orders of a specific table
    fun getTableOrdersFlow(tableId: String): Flow<List<Order>> = callbackFlow {
        val listener = db.collection("orders")
            .whereEqualTo("table_id", tableId)
            .whereIn("status", listOf("pending", "cooking", "done"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val ord = doc.toObject(Order::class.java)?.apply { id = doc.id } ?: return@mapNotNull null
                    if (ord.created_at == null) {
                        ord.created_at = parseOrderTimestamp(doc)
                    }
                    val rawItems = doc.get("items") as? List<*>
                    if (rawItems != null && rawItems.isNotEmpty()) {
                        val parsedItems = mutableListOf<OrderItem>()
                        for (rawItem in rawItems) {
                            if (rawItem is Map<*, *>) {
                                val dName = (rawItem["dish_name"] as? String) ?: (rawItem["name"] as? String) ?: ""
                                val dNameEn = (rawItem["dish_name_en"] as? String) ?: (rawItem["name_en"] as? String) ?: ""
                                val dId = (rawItem["dish_id"] as? String) ?: (rawItem["id"] as? String) ?: ""
                                val qty = when (val q = rawItem["quantity"]) {
                                    is Number -> q.toInt()
                                    is String -> q.toIntOrNull() ?: 1
                                    else -> 1
                                }
                                val price = when (val p = rawItem["price"]) {
                                    is Number -> p.toDouble()
                                    is String -> p.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val basePrice = when (val p = rawItem["base_price"]) {
                                    is Number -> p.toDouble()
                                    is String -> p.toDoubleOrNull() ?: 0.0
                                    else -> price
                                }
                                val optPrice = when (val p = rawItem["options_price"]) {
                                    is Number -> p.toDouble()
                                    is String -> p.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val optSummary = (rawItem["options_summary"] as? String) ?: ""
                                val optSummaryEn = (rawItem["options_summary_en"] as? String) ?: ""
                                val isCombo = (rawItem["is_combo"] as? Boolean) ?: false
                                val comboSummary = (rawItem["combo_summary"] as? String) ?: ""
                                val note = (rawItem["note"] as? String) ?: ""
                                val isCompleted = (rawItem["is_completed"] as? Boolean) ?: false
                                parsedItems.add(
                                    OrderItem(
                                        id = "${doc.id}_$dName",
                                        order_id = doc.id,
                                        dish_id = dId,
                                        dish_name = dName,
                                        dish_name_en = dNameEn,
                                        quantity = qty,
                                        price = price,
                                        base_price = basePrice,
                                        options_price = optPrice,
                                        options_summary = optSummary,
                                        options_summary_en = optSummaryEn,
                                        is_combo = isCombo,
                                        combo_summary = comboSummary,
                                        note = note,
                                        is_completed = isCompleted
                                    )
                                )
                            }
                        }
                        ord.items = parsedItems
                    }
                    ord
                } ?: emptyList()
                trySend(list.sortedByDescending { it.created_at })
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Service Requests
    fun getServiceRequestsFlow(): Flow<List<ServiceRequest>> = callbackFlow {
        val listener = db.collection("service_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ServiceRequest::class.java)?.apply { id = doc.id }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Fetch order items for an order
    suspend fun getItemsForOrder(orderId: String): List<OrderItem> {
        return try {
            val snap = db.collection("order_items")
                .whereEqualTo("order_id", orderId)
                .get()
                .await()
            snap.documents.mapNotNull { it.toObject(OrderItem::class.java)?.apply { id = it.id } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Update order status
    fun updateOrderStatus(orderId: String, newStatus: String) {
        db.collection("orders").document(orderId).update("status", newStatus)
    }

    // Anti-duplicate Order Mutex & Cache
    private val orderSubmissionMutex = Mutex()
    private var lastSubmittedOrderTime = 0L
    private var lastSubmittedItemsSignature = ""

    // Create a new Order (from Customer or Staff)
    suspend fun createOrder(tableId: String, items: List<OrderItem>, totalAmount: Double): String? {
        val signature = "${tableId}_${totalAmount}_${items.joinToString { "${it.dish_id}_${it.quantity}_${it.options_summary}" }}"
        val now = System.currentTimeMillis()

        return orderSubmissionMutex.withLock {
            if (signature == lastSubmittedItemsSignature && (now - lastSubmittedOrderTime < 3500L)) {
                android.util.Log.w("AuroraRepo", "Chặn đơn trùng do spam/mạng lag: $signature")
                return@withLock null
            }

            try {
                val orderData = hashMapOf(
                    "table_id" to tableId,
                    "total_amount" to totalAmount,
                    "status" to "pending",
                    "created_at" to Timestamp.now(),
                    "items" to items.map { item ->
                        hashMapOf(
                            "dish_id" to item.dish_id,
                            "dish_name" to item.dish_name,
                            "dish_name_en" to item.dish_name_en,
                            "price" to item.price,
                            "base_price" to item.base_price,
                            "options_price" to item.options_price,
                            "options_summary" to item.options_summary,
                            "options_summary_en" to item.options_summary_en,
                            "is_combo" to item.is_combo,
                            "combo_summary" to item.combo_summary,
                            "selected_options" to item.selected_options.map { opt ->
                                hashMapOf(
                                    "group_name" to opt.group_name,
                                    "group_name_en" to opt.group_name_en,
                                    "item_name" to opt.item_name,
                                    "item_name_en" to opt.item_name_en,
                                    "price" to opt.price
                                )
                            },
                            "quantity" to item.quantity,
                            "note" to item.note,
                            "is_completed" to item.is_completed
                        )
                    }
                )
                val batch = db.batch()
                val orderRef = db.collection("orders").document()
                val orderId = orderRef.id

                batch.set(orderRef, orderData)

                for (item in items) {
                    val itemRef = db.collection("order_items").document()
                    val itemData = hashMapOf(
                        "order_id" to orderId,
                        "dish_id" to item.dish_id.ifBlank { item.dish_name },
                        "dish_name" to item.dish_name,
                        "dish_name_en" to item.dish_name_en,
                        "price" to item.price,
                        "base_price" to item.base_price,
                        "options_price" to item.options_price,
                        "options_summary" to item.options_summary,
                        "options_summary_en" to item.options_summary_en,
                        "is_combo" to item.is_combo,
                        "combo_summary" to item.combo_summary,
                        "selected_options" to item.selected_options.map { opt ->
                            hashMapOf(
                                "group_name" to opt.group_name,
                                "group_name_en" to opt.group_name_en,
                                "item_name" to opt.item_name,
                                "item_name_en" to opt.item_name_en,
                                "price" to opt.price
                            )
                        },
                        "quantity" to item.quantity,
                        "note" to item.note
                    )
                    batch.set(itemRef, itemData)
                }

                // Cập nhật trạng thái bàn nguyên tử trong cùng batch
                val cleanTable = tableId.trim().replace(Regex("^(Table|Bàn|Ban)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                val tableDocRef = db.collection("tables").document(cleanTable)
                batch.set(tableDocRef, hashMapOf(
                    "status" to "waiting",
                    "updated_at" to Timestamp.now()
                ), SetOptions.merge())

                // Commit 100% nguyên tử trong 1 transaction duy nhất:
                batch.commit().await()

                lastSubmittedOrderTime = System.currentTimeMillis()
                lastSubmittedItemsSignature = signature

                orderId
            } catch (_: Exception) {
                null
            }
        }
    }

    // Cancel an order with reason (Hủy đơn hàng an toàn chuẩn Production - Chỉ Quản lý)
    suspend fun cancelOrder(orderId: String, reason: String = "Khách đổi ý / Gọi nhầm", staffName: String = "Quản lý"): Boolean {
        return try {
            val orderDoc = db.collection("orders").document(orderId).get().await()
            val tableId = orderDoc.getString("table_id") ?: ""

            val batch = db.batch()
            val orderRef = db.collection("orders").document(orderId)
            batch.update(orderRef, mapOf(
                "status" to "cancelled",
                "cancel_reason" to reason,
                "cancelled_by" to staffName,
                "cancelled_at" to Timestamp.now()
            ))

            // Nếu bàn không còn đơn nào đang hoạt động, tự động giải phóng bàn
            if (tableId.isNotBlank()) {
                val cleanTableId = tableId.trim().replace(Regex("^(Table|Bàn|Ban)\\s*", RegexOption.IGNORE_CASE), "").trim().uppercase()
                val tableOrders = db.collection("orders")
                    .whereEqualTo("table_id", cleanTableId)
                    .get().await()

                val otherActive = tableOrders.documents.filter { doc ->
                    doc.id != orderId && (doc.getString("status") ?: "") !in listOf("settled", "paid", "transferred", "cancelled")
                }
                if (otherActive.isEmpty()) {
                    val tableRef = db.collection("tables").document(cleanTableId)
                    batch.set(tableRef, mapOf(
                        "status" to "available",
                        "assigned_staff_username" to null,
                        "assigned_staff_name" to null,
                        "updated_at" to Timestamp.now()
                    ), com.google.firebase.firestore.SetOptions.merge())
                }
            }

            batch.commit().await()
            android.util.Log.d("AuroraRepo", "cancelOrder SUCCESS: $orderId by $staffName ($reason)")
            true
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "cancelOrder error: ${e.message}", e)
            false
        }
    }

    // Cancel a single dish inside an order (Hủy từng món lẻ chuẩn Production giống Web Admin - Chỉ Quản lý)
    suspend fun cancelSingleDish(
        orderId: String,
        itemIndex: Int,
        cancelQty: Int,
        reason: String = "Khách đổi ý / Gọi nhầm",
        staffName: String = "Quản lý"
    ): Boolean {
        return try {
            val orderRef = db.collection("orders").document(orderId)
            val orderDoc = orderRef.get().await()
            if (!orderDoc.exists()) return false

            val tableId = orderDoc.getString("table_id") ?: ""
            val orderObj = orderDoc.toObject(Order::class.java) ?: return false
            val currentItems = orderObj.items.toMutableList()

            if (itemIndex < 0 || itemIndex >= currentItems.size) return false

            val targetItem = currentItems[itemIndex]
            val oldQty = targetItem.quantity
            val actualCancelQty = minOf(cancelQty, oldQty)
            val remainingQty = oldQty - actualCancelQty
            val deductedAmount = targetItem.price * actualCancelQty
            val newTotal = maxOf(0.0, orderObj.total_amount - deductedAmount)

            if (remainingQty > 0) {
                currentItems[itemIndex] = targetItem.copy(quantity = remainingQty)
            } else {
                currentItems.removeAt(itemIndex)
            }

            val batch = db.batch()

            // Nếu toàn bộ món trong đơn bị hủy sạch -> Chuyển đơn sang cancelled
            if (currentItems.isEmpty()) {
                batch.update(orderRef, mapOf(
                    "status" to "cancelled",
                    "total_amount" to 0.0,
                    "items" to emptyList<OrderItem>(),
                    "cancel_reason" to reason,
                    "cancelled_by" to staffName,
                    "cancelled_at" to Timestamp.now()
                ))

                // Nếu bàn không còn đơn nào khác đang hoạt động, giải phóng bàn
                if (tableId.isNotBlank()) {
                    val cleanTableId = tableId.trim().replace(Regex("^(Table|Bàn|Ban)\\s*", RegexOption.IGNORE_CASE), "").trim().uppercase()
                    val tableOrders = db.collection("orders")
                        .whereEqualTo("table_id", cleanTableId)
                        .get().await()

                    val otherActive = tableOrders.documents.filter { doc ->
                        doc.id != orderId && (doc.getString("status") ?: "") !in listOf("settled", "paid", "transferred", "cancelled")
                    }
                    if (otherActive.isEmpty()) {
                        val tableRef = db.collection("tables").document(cleanTableId)
                        batch.set(tableRef, mapOf(
                            "status" to "available",
                            "assigned_staff_username" to null,
                            "assigned_staff_name" to null,
                            "updated_at" to Timestamp.now()
                        ), com.google.firebase.firestore.SetOptions.merge())
                    }
                }
            } else {
                batch.update(orderRef, mapOf(
                    "items" to currentItems,
                    "total_amount" to newTotal
                ))
            }

            // Đồng bộ sub-collection order_items nếu có
            try {
                val itemSnap = db.collection("order_items")
                    .whereEqualTo("order_id", orderId)
                    .get().await()

                for (itDoc in itemSnap.documents) {
                    val itName = itDoc.getString("dish_name") ?: ""
                    val itDishId = itDoc.getString("dish_id") ?: ""
                    if (itName == targetItem.dish_name || (targetItem.dish_id.isNotBlank() && itDishId == targetItem.dish_id)) {
                        if (remainingQty > 0) {
                            batch.update(itDoc.reference, "quantity", remainingQty)
                        } else {
                            batch.delete(itDoc.reference)
                        }
                    }
                }
            } catch (itErr: Exception) {
                android.util.Log.w("AuroraRepo", "Sync order_items error: ${itErr.message}")
            }

            batch.commit().await()
            android.util.Log.d("AuroraRepo", "cancelSingleDish SUCCESS: ${targetItem.dish_name} x$actualCancelQty on order $orderId by $staffName ($reason)")
            true
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "cancelSingleDish error: ${e.message}", e)
            false
        }
    }

    // Request Service or Checkout
    suspend fun requestService(tableId: String, type: String): Boolean {
        return try {
            val requestData = hashMapOf(
                "table_id" to tableId,
                "type" to type, // "assistance" | "checkout"
                "status" to "pending",
                "created_at" to Timestamp.now()
            )
            db.collection("service_requests").add(requestData).await()

            // Sync Table State to Firestore
            val newTableStatus = if (type == "checkout") "checkout" else "assistance"
            updateTableStatus(tableId, newTableStatus)

            true
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "transferTableOrders error", e)
            false
        }
    }

    // Resolve Service Request
    fun resolveServiceRequest(requestId: String) {
        db.collection("service_requests").document(requestId).update("status", "resolved")
    }

    // Settle all orders for a table (Payment / Checkout) with Voucher & Loyalty
    suspend fun settleTableOrders(
        tableId: String,
        paymentMethod: String = "cash",
        voucherCode: String? = null,
        customer: Customer? = null,
        pointsUsed: Double = 0.0,
        netPaid: Double = 0.0,
        billTotal: Double = 0.0
    ) {
        try {
            val cleanTable = tableId.trim().replace("Table ", "", ignoreCase = true).replace("table_", "", ignoreCase = true).replace("Bàn ", "", ignoreCase = true)
            
            // 0. Pre-settlement re-validation: Chống 1 voucher thanh toán 2 bàn cùng lúc
            if (!voucherCode.isNullOrBlank()) {
                val cleanVoucher = voucherCode.trim().uppercase()
                val vSnap = db.collection("voucher_codes").document(cleanVoucher).get().await()
                if (vSnap.exists()) {
                    val status = vSnap.getString("status") ?: "available"
                    val appliedTable = vSnap.getString("applied_table") ?: ""
                    val cleanApplied = appliedTable.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                    val cleanThis = cleanTable.uppercase()

                    if (status.equals("used", ignoreCase = true)) {
                        updateTableVoucher(cleanTable, null)
                        throw IllegalStateException("VOUCHER_ALREADY_USED")
                    }
                    if (status.equals("applied", ignoreCase = true) && cleanApplied.isNotBlank() && cleanApplied != cleanThis) {
                        updateTableVoucher(cleanTable, null)
                        throw IllegalStateException("VOUCHER_LOCKED_BY_ANOTHER_TABLE")
                    }
                }
            }

            // 1. Mark voucher used if present
            if (!voucherCode.isNullOrBlank()) {
                try {
                    db.collection("voucher_codes").document(voucherCode.trim().uppercase()).update(mapOf(
                        "status" to "used",
                        "used_at" to com.google.firebase.Timestamp.now(),
                        "used_table_id" to cleanTable,
                        "applied_table" to null,
                        "applied_at" to null
                    )).await()
                } catch (vErr: Exception) {
                    android.util.Log.w("AuroraRepo", "voucher mark used error", vErr)
                }
            }

            // 2. Update customer loyalty if present
            if (customer != null && customer.id.isNotBlank()) {
                try {
                    val earnedPts = Math.round(netPaid * customer.getEarnRate()).toDouble()
                    val newPoints = (customer.points - pointsUsed + earnedPts).coerceAtLeast(0.0)
                    val newSpend = customer.total_spend + netPaid
                    val newTier = when {
                        newSpend >= 30000000 -> "vip"
                        newSpend >= 15000000 -> "platinum"
                        newSpend >= 5000000 -> "gold"
                        newSpend >= 2000000 -> "silver"
                        else -> "member"
                    }

                    db.collection("customers").document(customer.id).update(mapOf(
                        "points" to newPoints,
                        "total_spend" to newSpend,
                        "tier" to newTier,
                        "last_active" to com.google.firebase.Timestamp.now()
                    )).await()

                    db.collection("loyalty_transactions").add(mapOf(
                        "customer_id" to customer.id,
                        "customer_name" to customer.name,
                        "customer_phone" to customer.phone,
                        "table_name" to cleanTable,
                        "bill_total" to billTotal,
                        "net_paid" to netPaid,
                        "points_used" to pointsUsed,
                        "points_earned" to earnedPts,
                        "points_balance" to newPoints,
                        "created_at" to com.google.firebase.Timestamp.now()
                    )).await()
                } catch (cErr: Exception) {
                    android.util.Log.w("AuroraRepo", "loyalty transaction error", cErr)
                }
            }

            // 3. Update active orders to settled
            val orders = db.collection("orders")
                .whereNotIn("status", listOf("settled", "paid", "cancelled"))
                .get()
                .await()

            val matchingOrders = orders.documents.filter { doc ->
                val ordTable = doc.getString("table_id") ?: ""
                val cleanOrdTable = ordTable.trim().replace("Table ", "", ignoreCase = true).replace("table_", "", ignoreCase = true).replace("Bàn ", "", ignoreCase = true)
                cleanOrdTable.equals(cleanTable, ignoreCase = true)
            }

            // Chống xung đột đồng thời: Nếu bàn đã được thanh toán bởi thiết bị khác
            if (matchingOrders.isEmpty()) {
                android.util.Log.w("AuroraRepo", "settleTableOrders: Bàn $cleanTable không còn đơn active (có thể đã được thanh toán)")
                return
            }

            for (doc in matchingOrders) {
                val updateMap = mutableMapOf<String, Any>(
                    "status" to "settled",
                    "settled_at" to com.google.firebase.Timestamp.now(),
                    "payment_method" to paymentMethod
                )
                doc.reference.update(updateMap)
            }

            // 4. Also resolve service requests for this table
            val serviceReqs = db.collection("service_requests")
                .whereEqualTo("status", "pending")
                .get()
                .await()

            for (doc in serviceReqs.documents) {
                val reqTable = doc.getString("table_id") ?: ""
                val cleanReqTable = reqTable.trim().replace("Table ", "", ignoreCase = true).replace("table_", "", ignoreCase = true).replace("Bàn ", "", ignoreCase = true)
                if (cleanReqTable.equals(cleanTable, ignoreCase = true)) {
                    doc.reference.update("status", "resolved")
                }
            }

            // Reset Table State & Release Staff Assignment on Firestore
            releaseTableStaff(tableId)
            updateTableStatus(tableId, "available")
            updateTableCustomer(cleanTable, null)
            updateTableVoucher(cleanTable, null)
            updateTableUsePoints(cleanTable, false)
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "settleTableOrders error", e)
            if (e is IllegalStateException) {
                throw e
            }
        }
    }

    suspend fun searchCustomerByPhone(phone: String): Customer? {
        return try {
            val cleanPhone = phone.replace(Regex("[^0-9]"), "").let {
                if (it.startsWith("84") && it.length >= 11) "0" + it.substring(2) else it
            }
            val snap = db.collection("customers").whereEqualTo("phone", cleanPhone).get().await()
            if (!snap.isEmpty) {
                val doc = snap.documents[0]
                doc.toObject(Customer::class.java)?.apply { id = doc.id }
            } else null
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "searchCustomerByPhone error", e)
            null
        }
    }

    suspend fun registerCustomer(phone: String, name: String): Customer? {
        return try {
            val cleanPhone = phone.replace(Regex("[^0-9]"), "").let {
                if (it.startsWith("84") && it.length >= 11) "0" + it.substring(2) else it
            }
            val newCust = Customer(
                phone = cleanPhone,
                name = name,
                points = 0.0,
                total_spend = 0.0,
                tier = "member",
                created_at = com.google.firebase.Timestamp.now()
            )
            val docRef = db.collection("customers").add(newCust).await()
            newCust.apply { id = docRef.id }
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "registerCustomer error", e)
            null
        }
    }

    suspend fun getVoucherByCode(code: String, tableId: String? = null): Voucher? {
        return try {
            val cleanCode = code.trim().uppercase()
            val doc = db.collection("voucher_codes").document(cleanCode).get().await()
            if (doc.exists()) {
                val v = doc.toObject(Voucher::class.java)?.apply { id = doc.id } ?: return null
                if (v.code.isBlank()) {
                    v.code = doc.id
                }
                v
            } else {
                // Fallback: Tìm theo field code nếu document ID là mã tự sinh
                val querySnap = db.collection("voucher_codes")
                    .whereEqualTo("code", cleanCode)
                    .limit(1)
                    .get()
                    .await()
                if (!querySnap.isEmpty) {
                    val qDoc = querySnap.documents[0]
                    val v = qDoc.toObject(Voucher::class.java)?.apply { id = qDoc.id } ?: return null
                    if (v.code.isBlank()) {
                        v.code = qDoc.id
                    }
                    v
                } else null
            }
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "getVoucherByCode error", e)
            null
        }
    }

    // Transfer or Merge all orders from one table to another
    suspend fun transferTableOrders(fromTableId: String, toTableId: String): Boolean {
        return try {
            val cleanFrom = fromTableId.trim().replace("Table ", "", ignoreCase = true).replace("table_", "", ignoreCase = true).replace("Bàn ", "", ignoreCase = true)

            // 1. Move active orders (quét toàn bộ các đơn chưa thanh toán/chưa hủy)
            val activeOrdersSnap = db.collection("orders")
                .whereNotIn("status", listOf("settled", "paid", "cancelled"))
                .get()
                .await()

            val activeOrders = activeOrdersSnap.documents.filter { doc ->
                val ordTable = doc.getString("table_id") ?: ""
                val cleanOrdTable = ordTable.trim().replace("Table ", "", ignoreCase = true).replace("table_", "", ignoreCase = true).replace("Bàn ", "", ignoreCase = true)
                cleanOrdTable.equals(cleanFrom, ignoreCase = true)
            }

            // Chống xung đột đồng thời: Bàn nguồn không còn đơn nào (đã được chuyển hoặc đóng bởi thiết bị khác)
            if (activeOrders.isEmpty()) {
                android.util.Log.w("AuroraRepo", "transferTableOrders: Bàn $cleanFrom không có đơn hoạt động nào để chuyển (có thể đã được xử lý)")
                return false
            }

            for (doc in activeOrders) {
                doc.reference.update(mapOf(
                    "table_id" to toTableId,
                    "transferred_from" to cleanFrom,
                    "transferred_at" to com.google.firebase.Timestamp.now()
                )).await()
            }

            // 2. Move pending service requests
            val activeReqsSnap = db.collection("service_requests")
                .whereEqualTo("status", "pending")
                .get()
                .await()

            for (doc in activeReqsSnap.documents) {
                val reqTable = doc.getString("table_id") ?: ""
                val cleanReqTable = reqTable.trim().replace("Table ", "", ignoreCase = true).replace("table_", "", ignoreCase = true).replace("Bàn ", "", ignoreCase = true)
                if (cleanReqTable.equals(cleanFrom, ignoreCase = true)) {
                    doc.reference.update("table_id", toTableId).await()
                }
            }

            val cleanTo = toTableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()

            // 3. Move Customer, Voucher and UsePoints to target table, then reset source table
            val sourceDoc = db.collection("tables").document(cleanFrom).get().await()
            val attachedCust = sourceDoc.get("attached_customer")
            val appliedVouch = sourceDoc.get("applied_voucher")
            val usePts = sourceDoc.getBoolean("use_points") ?: false

            val targetDoc = db.collection("tables").document(cleanTo).get().await()
            val existingTargetCust = targetDoc.get("attached_customer")
            val existingTargetVoucher = targetDoc.get("applied_voucher")

            val targetPayload = mutableMapOf<String, Any?>(
                "status" to "dining",
                "tableId" to cleanTo,
                "name" to toTableId,
                "transferred_to" to com.google.firebase.firestore.FieldValue.delete(),
                "transferred_at" to com.google.firebase.firestore.FieldValue.delete(),
                "updated_at" to com.google.firebase.Timestamp.now()
            )
            // Nếu bàn đích đã có khách, giữ nguyên tài khoản bàn đích. Nếu bàn đích trống, nhận khách từ bàn nguồn
            if (existingTargetCust == null && attachedCust != null) {
                targetPayload["attached_customer"] = attachedCust
            }
            if (existingTargetVoucher == null && appliedVouch != null) {
                targetPayload["applied_voucher"] = appliedVouch
            }
            if (usePts) targetPayload["use_points"] = true
            db.collection("tables").document(cleanTo).set(targetPayload, SetOptions.merge()).await()

            // Reset source table (free up) và lưu vết transferred_to
            releaseTableStaff(fromTableId)
            db.collection("tables").document(cleanFrom).set(
                mapOf(
                    "status" to "available",
                    "transferred_to" to toTableId,
                    "transferred_at" to com.google.firebase.Timestamp.now(),
                    "attached_customer" to null,
                    "applied_voucher" to null,
                    "use_points" to false,
                    "current_session_id" to null,
                    "assigned_staff_username" to null,
                    "assigned_staff_name" to null,
                    "assignedStaffUsername" to null,
                    "assignedStaffName" to null,
                    "updated_at" to com.google.firebase.Timestamp.now()
                ),
                SetOptions.merge()
            ).await()

            // 4. Update target table status to dining
            updateTableStatus(toTableId, "dining")

            true
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "transferTableOrders error", e)
            false
        }
    }

    // Commit Split or Complete Transfer of Items between tables (100% Atomic WriteBatch)
    suspend fun commitSplitTransfer(
        sourceTableId: String,
        targetTableId: String,
        originalSourceOrders: List<Order>,
        remainingSourceItems: List<TransferDishItem>,
        movedItems: List<TransferDishItem>
    ): Boolean {
        return try {
            if (movedItems.isEmpty()) {
                return true
            }

            // Case 1: All items moved from source (100% full table move)
            if (remainingSourceItems.isEmpty()) {
                return transferTableOrders(sourceTableId, targetTableId)
            }

            // Case 2: Partial item transfer (tách món / chuyển một phần qua Atomic Batch)
            val batch = db.batch()

            // A. Update items remaining on Source Table
            for (order in originalSourceOrders) {
                val orderRemaining = remainingSourceItems.filter { it.orderId == order.id }
                val newOrderTotal = orderRemaining.sumOf { it.price * it.quantity }

                for (origItem in order.items) {
                    val rem = orderRemaining.find { it.originalDocId == origItem.id }
                    if (origItem.id.isNotBlank()) {
                        if (rem == null || rem.quantity <= 0) {
                            // Item was completely transferred to target table
                            batch.delete(db.collection("order_items").document(origItem.id))
                        } else if (rem.quantity < origItem.quantity) {
                            // Item quantity was reduced
                            batch.update(db.collection("order_items").document(origItem.id), "quantity", rem.quantity)
                        }
                    }
                }

                // Update order total amount (chuẩn hóa status = "transferred" khi chuyển hết món)
                if (orderRemaining.isEmpty()) {
                    batch.update(
                        db.collection("orders").document(order.id),
                        mapOf(
                            "status" to "transferred",
                            "total_amount" to 0,
                            "items" to emptyList<Map<String, Any>>(),
                            "transferred_to" to targetTableId,
                            "transferred_at" to com.google.firebase.Timestamp.now(),
                            "note" to "Đã tách/chuyển hết món sang Bàn $targetTableId"
                        )
                    )
                } else {
                    val remainingItemsList = orderRemaining.map { itm ->
                        hashMapOf(
                            "dish_id" to itm.dishId,
                            "dish_name" to itm.dishName,
                            "dish_name_en" to itm.dishNameEn,
                            "price" to itm.price,
                            "base_price" to itm.basePrice,
                            "options_price" to itm.optionsPrice,
                            "quantity" to itm.quantity,
                            "note" to itm.note,
                            "is_completed" to itm.isCompleted,
                            "is_combo" to itm.isCombo,
                            "combo_summary" to itm.comboSummary,
                            "options_summary" to itm.optionsSummary,
                            "options_summary_en" to itm.optionsSummaryEn,
                            "selected_options" to itm.selectedOptions.map { opt ->
                                hashMapOf(
                                    "group_name" to opt.group_name,
                                    "group_name_en" to opt.group_name_en,
                                    "item_name" to opt.item_name,
                                    "item_name_en" to opt.item_name_en,
                                    "price" to opt.price
                                )
                            }
                        )
                    }
                    batch.update(
                        db.collection("orders").document(order.id),
                        mapOf(
                            "total_amount" to newOrderTotal,
                            "items" to remainingItemsList
                        )
                    )
                }
            }

            // B. Add moved items to Target Table
            val allActiveSnap = db.collection("orders")
                .whereNotIn("status", listOf("settled", "paid", "transferred", "cancelled"))
                .get()
                .await()

            val cleanTarget = targetTableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
            val cleanSource = sourceTableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()

            // Kiểm tra bảo toàn thông tin khách hàng trên Bàn Đích (Single Source of Truth từ tables document)
            val targetTableDoc = db.collection("tables").document(cleanTarget).get().await()
            val targetCustMap = targetTableDoc.get("attached_customer") as? Map<*, *>

            val existingTargetOrderDoc = allActiveSnap.documents.find { doc ->
                val ordTable = doc.getString("table_id") ?: ""
                val cleanOrdTable = ordTable.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                cleanOrdTable == cleanTarget
            }

            val movedTotal = movedItems.sumOf { it.price * it.quantity }
            val movedMappedItems = movedItems.map { item ->
                hashMapOf(
                    "dish_id" to item.dishId,
                    "dish_name" to item.dishName,
                    "dish_name_en" to item.dishNameEn,
                    "price" to item.price,
                    "base_price" to item.basePrice,
                    "options_price" to item.optionsPrice,
                    "quantity" to item.quantity,
                    "note" to item.note,
                    "is_completed" to item.isCompleted,
                    "is_combo" to item.isCombo,
                    "combo_summary" to item.comboSummary,
                    "options_summary" to item.optionsSummary,
                    "options_summary_en" to item.optionsSummaryEn,
                    "selected_options" to item.selectedOptions.map { opt ->
                        hashMapOf(
                            "group_name" to opt.group_name,
                            "group_name_en" to opt.group_name_en,
                            "item_name" to opt.item_name,
                            "item_name_en" to opt.item_name_en,
                            "price" to opt.price
                        )
                    }
                )
            }

            if (existingTargetOrderDoc != null) {
                val targetOrderId = existingTargetOrderDoc.id
                val currentTotal = existingTargetOrderDoc.getDouble("total_amount") ?: 0.0

                for (item in movedItems) {
                    val itemRef = db.collection("order_items").document()
                    val itemData = hashMapOf(
                        "order_id" to targetOrderId,
                        "dish_id" to item.dishId,
                        "dish_name" to item.dishName,
                        "dish_name_en" to item.dishNameEn,
                        "price" to item.price,
                        "base_price" to item.basePrice,
                        "options_price" to item.optionsPrice,
                        "quantity" to item.quantity,
                        "note" to item.note,
                        "is_completed" to item.isCompleted,
                        "is_combo" to item.isCombo,
                        "combo_summary" to item.comboSummary,
                        "options_summary" to item.optionsSummary,
                        "options_summary_en" to item.optionsSummaryEn,
                        "selected_options" to item.selectedOptions.map { opt ->
                            hashMapOf(
                                "group_name" to opt.group_name,
                                "group_name_en" to opt.group_name_en,
                                "item_name" to opt.item_name,
                                "item_name_en" to opt.item_name_en,
                                "price" to opt.price
                            )
                        },
                        "created_at" to com.google.firebase.Timestamp.now()
                    )
                    batch.set(itemRef, itemData)
                }

                val existingRawItems = (existingTargetOrderDoc.get("items") as? List<*>)
                    ?.filterIsInstance<Map<String, Any>>()
                    ?.toMutableList() ?: mutableListOf()
                existingRawItems.addAll(movedMappedItems)

                val updateTargetMap = mutableMapOf<String, Any>(
                    "total_amount" to (currentTotal + movedTotal),
                    "items" to existingRawItems
                )
                // Đảm bảo đơn hàng bàn đích gắn đúng tài khoản khách nếu chưa có
                if (existingTargetOrderDoc.getString("customer_name").isNullOrBlank() && targetCustMap != null) {
                    updateTargetMap["customer_id"] = (targetCustMap["id"] as? String) ?: (targetCustMap["phone"] as? String) ?: ""
                    updateTargetMap["customer_name"] = (targetCustMap["name"] as? String) ?: ""
                    updateTargetMap["customer_phone"] = (targetCustMap["phone"] as? String) ?: ""
                    updateTargetMap["customer_tier"] = (targetCustMap["tier"] as? String) ?: "member"
                }

                batch.update(existingTargetOrderDoc.reference, updateTargetMap)
            } else {
                val newOrderRef = db.collection("orders").document()
                val newOrderId = newOrderRef.id
                val newOrderData = hashMapOf<String, Any?>(
                    "table_id" to cleanTarget,
                    "total_amount" to movedTotal,
                    "status" to "cooking",
                    "created_at" to com.google.firebase.Timestamp.now(),
                    "note" to "Tách món từ Bàn $cleanSource",
                    "items" to movedMappedItems
                )
                // Gắn ngay tài khoản khách hàng của bàn đích vào đơn hàng mới tạo
                if (targetCustMap != null) {
                    newOrderData["customer_id"] = (targetCustMap["id"] as? String) ?: (targetCustMap["phone"] as? String) ?: ""
                    newOrderData["customer_name"] = (targetCustMap["name"] as? String) ?: ""
                    newOrderData["customer_phone"] = (targetCustMap["phone"] as? String) ?: ""
                    newOrderData["customer_tier"] = (targetCustMap["tier"] as? String) ?: "member"
                }
                batch.set(newOrderRef, newOrderData)

                for (item in movedItems) {
                    val itemRef = db.collection("order_items").document()
                    val itemData = hashMapOf(
                        "order_id" to newOrderId,
                        "dish_id" to item.dishId,
                        "dish_name" to item.dishName,
                        "dish_name_en" to item.dishNameEn,
                        "price" to item.price,
                        "base_price" to item.basePrice,
                        "options_price" to item.optionsPrice,
                        "quantity" to item.quantity,
                        "note" to item.note,
                        "is_completed" to item.isCompleted,
                        "is_combo" to item.isCombo,
                        "combo_summary" to item.comboSummary,
                        "options_summary" to item.optionsSummary,
                        "options_summary_en" to item.optionsSummaryEn,
                        "selected_options" to item.selectedOptions.map { opt ->
                            hashMapOf(
                                "group_name" to opt.group_name,
                                "group_name_en" to opt.group_name_en,
                                "item_name" to opt.item_name,
                                "item_name_en" to opt.item_name_en,
                                "price" to opt.price
                            )
                        },
                        "created_at" to com.google.firebase.Timestamp.now()
                    )
                    batch.set(itemRef, itemData)
                }
            }

            // Commit toàn bộ các lệnh ghi nguyên tử 100% trong 1 giao dịch duy nhất
            batch.commit().await()

            // C. Update Table statuses (Bảo toàn tài khoản khách hàng trên tables/{cleanTarget})
            updateTableStatus(cleanTarget, "dining")
            if (remainingSourceItems.isEmpty()) {
                releaseTableStaff(cleanSource)
                updateTableStatus(cleanSource, "available")
                updateTableCustomer(cleanSource, null)
                updateTableVoucher(cleanSource, null)
                updateTableUsePoints(cleanSource, false)
            } else {
                updateTableStatus(cleanSource, "dining")

                // D. Re-validate Voucher on Source Table after partial split (Đề xuất 2)
                try {
                    val cleanFrom = sourceTableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
                    val sourceDoc = db.collection("tables").document(cleanFrom).get().await()
                    val appliedVoucher = sourceDoc.get("applied_voucher") as? Map<*, *>
                    if (appliedVoucher != null) {
                        val vCode = (appliedVoucher["code"] as? String) ?: (appliedVoucher["id"] as? String) ?: ""
                        val vType = (appliedVoucher["type"] as? String) ?: (appliedVoucher["discount_type"] as? String) ?: "fixed"
                        val vMinBill = when (val mb = appliedVoucher["min_bill"] ?: appliedVoucher["min_order_value"]) {
                            is Number -> mb.toDouble()
                            is String -> mb.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        val vDishId = (appliedVoucher["specific_dish_id"] as? String) ?: ""
                        val vDishName = (appliedVoucher["specific_dish_name"] as? String) ?: ""

                        val remainingTotal = remainingSourceItems.sumOf { it.price * it.quantity }
                        var shouldCancelVoucher = false

                        // Check 1: min_bill condition
                        if (vMinBill > 0 && remainingTotal < vMinBill) {
                            shouldCancelVoucher = true
                        }

                        // Check 2: specific dish voucher
                        if (vType.equals("dish", ignoreCase = true) || vDishId.isNotBlank() || vDishName.isNotBlank()) {
                            val hasDishRemaining = remainingSourceItems.any { itm ->
                                (vDishId.isNotBlank() && itm.dishId == vDishId) ||
                                (vDishName.isNotBlank() && itm.dishName.equals(vDishName, ignoreCase = true))
                            }
                            if (!hasDishRemaining) {
                                shouldCancelVoucher = true
                            }
                        }

                        if (shouldCancelVoucher) {
                            updateTableVoucher(cleanFrom, null)
                            if (vCode.isNotBlank()) {
                                releaseVoucherCode(vCode)
                            }
                        }
                    }
                } catch (vErr: Exception) {
                    android.util.Log.w("AuroraRepo", "Re-validate voucher on split error: ${vErr.message}")
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "commitSplitTransfer atomic batch error", e)
            false
        }
    }

    // Toggle Order Item completed (strike-through)
    fun toggleOrderItemCompleted(itemId: String, isCompleted: Boolean) {
        if (itemId.isNotBlank()) {
            db.collection("order_items").document(itemId).update("is_completed", isCompleted)
        }
    }

    // Lock/Assign table to a single staff member
    fun assignStaffToTable(tableId: String, staffUsername: String, staffName: String) {
        val data = hashMapOf<String, Any?>(
            "assigned_staff_username" to staffUsername,
            "assigned_staff_name" to staffName,
            "updated_at" to Timestamp.now()
        )
        db.collection("tables").document(tableId).set(data, com.google.firebase.firestore.SetOptions.merge())
    }

    // Release table when session ends or staff logs out
    fun releaseTableStaff(tableId: String) {
        val data = hashMapOf<String, Any?>(
            "assigned_staff_username" to null,
            "assigned_staff_name" to null,
            "updated_at" to Timestamp.now()
        )
        db.collection("tables").document(tableId).set(data, com.google.firebase.firestore.SetOptions.merge())
    }

    // Lock table for transfer so other staff cannot interfere
    fun lockTableForTransfer(tableId: String, staffUsername: String, staffName: String) {
        val data = hashMapOf<String, Any?>(
            "transferring_by_username" to staffUsername,
            "transferring_by_name" to staffName,
            "updated_at" to Timestamp.now()
        )
        db.collection("tables").document(tableId).set(data, com.google.firebase.firestore.SetOptions.merge())
    }

    // Unlock table transfer when finished or canceled
    fun unlockTableTransfer(tableId: String) {
        val data = hashMapOf<String, Any?>(
            "transferring_by_username" to null,
            "transferring_by_name" to null,
            "updated_at" to Timestamp.now()
        )
        db.collection("tables").document(tableId).set(data, com.google.firebase.firestore.SetOptions.merge())
    }

    // Release all tables locked by a specific staff member when logging out
    suspend fun releaseAllTablesForStaff(staffUsername: String) {
        if (staffUsername.isBlank()) return
        try {
            val batch = db.batch()
            var count = 0
            val seenDocIds = mutableSetOf<String>()

            val tables = db.collection("tables")
                .whereEqualTo("assigned_staff_username", staffUsername)
                .get()
                .await()
            for (doc in tables.documents) {
                seenDocIds.add(doc.id)
                batch.update(
                    doc.reference,
                    mapOf(
                        "assigned_staff_username" to null,
                        "assigned_staff_name" to null,
                        "transferring_by_username" to null,
                        "transferring_by_name" to null
                    )
                )
                count++
            }

            val transTables = db.collection("tables")
                .whereEqualTo("transferring_by_username", staffUsername)
                .get()
                .await()
            for (doc in transTables.documents) {
                if (!seenDocIds.contains(doc.id)) {
                    seenDocIds.add(doc.id)
                    batch.update(
                        doc.reference,
                        mapOf(
                            "transferring_by_username" to null,
                            "transferring_by_name" to null
                        )
                    )
                    count++
                }
            }

            if (count > 0) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepository", "releaseAllTablesForStaff error: ${e.message}")
        }
    }

    // Blocking release helper for Service.onTaskRemoved or app exit
    fun releaseAllTablesForStaffBlocking(staffUsername: String, timeoutSec: Long = 3) {
        if (staffUsername.isBlank()) return
        try {
            val tablesTask = db.collection("tables")
                .whereEqualTo("assigned_staff_username", staffUsername)
                .get()
            val tables = com.google.android.gms.tasks.Tasks.await(tablesTask, timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
            val batch = db.batch()
            var count = 0
            val seenDocIds = mutableSetOf<String>()

            for (doc in tables.documents) {
                seenDocIds.add(doc.id)
                batch.update(
                    doc.reference,
                    mapOf(
                        "assigned_staff_username" to null,
                        "assigned_staff_name" to null,
                        "transferring_by_username" to null,
                        "transferring_by_name" to null
                    )
                )
                count++
            }

            val transTask = db.collection("tables")
                .whereEqualTo("transferring_by_username", staffUsername)
                .get()
            val transTables = com.google.android.gms.tasks.Tasks.await(transTask, timeoutSec, java.util.concurrent.TimeUnit.SECONDS)

            for (doc in transTables.documents) {
                if (!seenDocIds.contains(doc.id)) {
                    seenDocIds.add(doc.id)
                    batch.update(
                        doc.reference,
                        mapOf(
                            "transferring_by_username" to null,
                            "transferring_by_name" to null
                        )
                    )
                    count++
                }
            }

            if (count > 0) {
                val commitTask = batch.commit()
                com.google.android.gms.tasks.Tasks.await(commitTask, timeoutSec, java.util.concurrent.TimeUnit.SECONDS)
            }
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepository", "releaseAllTablesForStaffBlocking error: ${e.message}")
        }
    }

    // Sync or update table status on Firestore (Luôn chuẩn hóa document ID sạch như A1, B2)
    fun updateTableStatus(tableId: String, status: String) {
        val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
        val displayName = if (tableId.startsWith("Bàn ", true)) tableId else "Bàn $clean"
        val tableData = hashMapOf(
            "tableId" to clean,
            "name" to displayName,
            "status" to status,
            "updated_at" to Timestamp.now()
        )
        db.collection("tables").document(clean).set(tableData, com.google.firebase.firestore.SetOptions.merge())
    }

    // Toggle Dish Availability
    fun toggleDishAvailability(dishId: String, isAvailable: Boolean) {
        val data = hashMapOf<String, Any?>(
            "is_available" to isAvailable,
            "locked_until" to null
        )
        db.collection("dishes").document(dishId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("AuroraRepo", "toggleDishAvailability SUCCESS: $dishId -> $isAvailable")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuroraRepo", "toggleDishAvailability FAILED: $dishId", e)
            }
    }

    fun toggleDishTimed(dishId: String, lockedUntil: Timestamp?) {
        val isAvailable = (lockedUntil == null)
        val data = hashMapOf<String, Any?>(
            "is_available" to isAvailable,
            "locked_until" to lockedUntil
        )
        db.collection("dishes").document(dishId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("AuroraRepo", "toggleDishTimed SUCCESS: $dishId -> $isAvailable, until=$lockedUntil")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AuroraRepo", "toggleDishTimed FAILED: $dishId", e)
            }
    }

    // Sync Table Customer
    suspend fun updateTableCustomer(tableId: String, customer: Customer?) {
        try {
            val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
            val custMap = customer?.let { c ->
                hashMapOf<String, Any?>(
                    "id" to c.id,
                    "name" to c.name,
                    "phone" to c.phone,
                    "tier" to c.tier,
                    "points" to c.points,
                    "total_spend" to c.total_spend
                )
            }
            db.collection("tables").document(clean).set(
                mapOf(
                    "attached_customer" to custMap,
                    "updated_at" to Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "updateTableCustomer error", e)
        }
    }

    // Sync Table Voucher
    suspend fun updateTableVoucher(tableId: String, voucher: Voucher?) {
        try {
            val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
            val voucherMap: HashMap<String, Any?>? = if (voucher != null) {
                hashMapOf<String, Any?>(
                    "id" to voucher.id,
                    "code" to (if (voucher.code.isNotBlank()) voucher.code else voucher.id),
                    "campaign_id" to voucher.campaign_id,
                    "campaign_name" to voucher.campaign_name,
                    "type" to voucher.type,
                    "discount_type" to voucher.type,
                    "value" to voucher.value,
                    "discount_value" to voucher.value,
                    "min_bill" to voucher.min_bill,
                    "max_discount" to voucher.max_discount,
                    "specific_dish_id" to voucher.specific_dish_id,
                    "specific_dish_name" to voucher.specific_dish_name,
                    "status" to voucher.status
                )
            } else null
            db.collection("tables").document(clean).set(
                mapOf(
                    "applied_voucher" to voucherMap,
                    "updated_at" to Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "updateTableVoucher error", e)
        }
    }

    // Sync Table Use Points
    suspend fun updateTableUsePoints(tableId: String, usePoints: Boolean) {
        try {
            val clean = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
            db.collection("tables").document(clean).set(
                mapOf(
                    "use_points" to usePoints,
                    "updated_at" to Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("AuroraRepo", "updateTableUsePoints error", e)
        }
    }

    // Exclusive lock voucher code for table
    suspend fun claimVoucherForTable(voucherCode: String, tableId: String): Boolean {
        return try {
            val clean = voucherCode.trim().uppercase()
            val cleanTable = tableId.trim().replace(Regex("^(Table|Bàn|table_)\\s*", RegexOption.IGNORE_CASE), "").uppercase()
            if (clean.isNotBlank()) {
                db.collection("voucher_codes").document(clean).update(
                    mapOf(
                        "status" to "applied",
                        "applied_table" to cleanTable,
                        "applied_at" to com.google.firebase.Timestamp.now()
                    )
                ).await()
                android.util.Log.d("AuroraRepo", "claimVoucherForTable SUCCESS: $clean locked for $cleanTable")
                true
            } else false
        } catch (e: Exception) {
            android.util.Log.w("AuroraRepo", "claimVoucherForTable error: ${e.message}")
            false
        }
    }

    // Release Voucher Code back to available (CHỈ giải phóng nếu đang ở trạng thái applied, TUYỆT ĐỐI không hồi sinh voucher đã used)
    suspend fun releaseVoucherCode(voucherCode: String) {
        try {
            val clean = voucherCode.trim().uppercase()
            if (clean.isNotBlank()) {
                val docRef = db.collection("voucher_codes").document(clean)
                val snap = docRef.get().await()
                if (snap.exists()) {
                    val status = snap.getString("status") ?: "available"
                    // Nếu voucher đã thanh toán (status == 'used'), tuyệt đối không đổi về available và không xóa used_at
                    if (status.equals("used", ignoreCase = true)) {
                        android.util.Log.d("AuroraRepo", "releaseVoucherCode SKIPPED: $clean is already USED")
                        return
                    }
                    docRef.update(
                        mapOf(
                            "status" to "available",
                            "applied_table" to null,
                            "applied_at" to null
                        )
                    ).await()
                    android.util.Log.d("AuroraRepo", "releaseVoucherCode SUCCESS: $clean released to available")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AuroraRepo", "releaseVoucherCode error: ${e.message}")
        }
    }

    // Sound and vibration helper
    fun ringAndVibrate(context: Context) {
        try {
            com.example.qrorderkitchen.utils.SoundManager.playServiceChime(context)
        } catch (_: Exception) {}
    }
}
