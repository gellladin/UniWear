package com.example.unigearmanager

import android.content.Context
import java.util.concurrent.CopyOnWriteArraySet

data class GearItem(
    val id: Int,
    val emoji: String,
    val name: String,
    val category: String,
    val price: Double,
    var stock: Int,
    val unit: String,
    val audience: String
) {
    val isAvailable: Boolean
        get() = stock > 0
}

data class StudentOrder(
    val id: Int,
    val itemId: Int,
    val studentName: String,
    val itemName: String,
    var quantity: Int,
    var total: Double,
    var status: String,
    var paid: Double,
    var claimed: Boolean,
    val orderDate: Long = System.currentTimeMillis()
) {
    val balance: Double
        get() = total - paid
}

data class StudentNotification(
    val id: Int,
    val studentName: String,
    val orderId: Int,
    val title: String,
    val message: String,
    val status: String
)

object UniWearRepository {
    private var database: UniWearDatabaseHelper? = null
    private var firestore: UniWearFirestoreDataSource? = null
    private val changeListeners = CopyOnWriteArraySet<() -> Unit>()

    val studentMap = mapOf(
        1 to "Adrian",
        2 to "Sheila",
        3 to "Maria",
        4 to "Juan",
        5 to "Angela",
        6 to "Rafael",
        7 to "Sophia",
        8 to "Marcus",
        9 to "Isabella",
        10 to "Daniel"
    )

    val items = UniWearDatabaseHelper.defaultItems.toMutableList()
    val orders = UniWearDatabaseHelper.defaultOrders.toMutableList()
    val notifications = UniWearDatabaseHelper.defaultNotifications.toMutableList()

    fun init(context: Context) {
        if (database != null) return
        database = UniWearDatabaseHelper(context.applicationContext)
        items.replaceWith(database!!.getItems())
        orders.replaceWith(database!!.getOrders())
        notifications.replaceWith(database!!.getNotifications())
        firestore = UniWearFirestoreDataSource.create(context.applicationContext)?.also { remote ->
            remote.seedMissingCollections(
                UniWearDatabaseHelper.defaultItems,
                UniWearDatabaseHelper.defaultOrders,
                UniWearDatabaseHelper.defaultNotifications
            )
            remote.startListening(
                onItemsChanged = { remoteItems ->
                    if (remoteItems.isNotEmpty()) {
                        items.replaceWith(remoteItems.sortedBy { it.id })
                        database?.replaceItems(remoteItems)
                        notifyDataChanged()
                    }
                },
                onOrdersChanged = { remoteOrders ->
                    orders.replaceWith(remoteOrders)
                    database?.replaceOrders(remoteOrders)
                    notifyDataChanged()
                },
                onNotificationsChanged = { remoteNotifications ->
                    notifications.replaceWith(remoteNotifications)
                    database?.replaceNotifications(remoteNotifications)
                    notifyDataChanged()
                }
            )
        }
    }

    val isRemoteSyncEnabled: Boolean
        get() = firestore != null

    fun addChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    fun removeChangeListener(listener: () -> Unit) {
        changeListeners.remove(listener)
    }

    fun getStudentName(studentId: Int): String {
        return studentMap[studentId] ?: "Unknown Student"
    }

    fun itemById(id: Int): GearItem? = items.firstOrNull { it.id == id }

    fun addItem(
        name: String,
        category: String,
        price: Double,
        stock: Int,
        unit: String,
        audience: String,
        emoji: String
    ): GearItem {
        val item = GearItem(
            id = (items.maxOfOrNull { it.id } ?: 0) + 1,
            emoji = emoji,
            name = name,
            category = category,
            price = price,
            stock = stock,
            unit = unit,
            audience = audience
        )
        items.add(item)
        database?.insertItem(item)
        firestore?.saveItem(item)
        return item
    }

    val studentsServed: Int
        get() = orders.map { it.studentName }.distinct().size

    val activeOrders: Int
        get() = orders.count { !it.claimed }

    val fullyPaidOrders: Int
        get() = orders.count { it.balance <= 0.0 }

    val lowStockItems: Int
        get() = items.count { it.stock <= LOW_STOCK_THRESHOLD }

    val totalPaid: Double
        get() = orders.sumOf { it.paid }

    fun claimOrder(orderId: Int): Boolean {
        val order = orders.firstOrNull { it.id == orderId && !it.claimed } ?: return false
        if (!order.isReadyForClaim) return false
        order.claimed = true
        order.status = "Claimed"
        database?.updateOrder(order)
        firestore?.saveOrder(order)
        notifyDataChanged()
        return true
    }

    fun markBalancePaid(orderId: Int): Double {
        val order = orders.firstOrNull { it.id == orderId && it.balance > 0.0 } ?: return 0.0
        val paidNow = order.balance
        order.paid = order.total
        database?.updateOrder(order)
        firestore?.saveOrder(order)
        notifyDataChanged()
        return paidNow
    }

    fun advanceStatus(orderId: Int): Boolean {
        val order = orders.firstOrNull { it.id == orderId } ?: return false
        if (order.claimed || order.status == "Claimed" || order.status == "Ready for pickup") return false
        order.status = "Ready for pickup"
        database?.updateOrder(order)
        firestore?.saveOrder(order)
        addNotification(
            order.studentName,
            order.id,
            "Ready for pickup",
            "Your ${order.itemName} order is ready to claim.",
            order.status
        )
        notifyDataChanged()
        return true
    }

    fun updateOrderQuantity(orderId: Int, newQuantity: Int): Boolean {
        val order = orders.firstOrNull { it.id == orderId } ?: return false
        val item = itemById(order.itemId) ?: return false
        if (newQuantity <= 0) return false

        val quantityDelta = newQuantity - order.quantity
        if (quantityDelta > item.stock) return false

        item.stock -= quantityDelta
        order.quantity = newQuantity
        order.total = item.price * newQuantity
        if (order.paid > order.total) order.paid = order.total
        database?.updateItem(item)
        database?.updateOrder(order)
        firestore?.saveItem(item)
        firestore?.saveOrder(order)
        notifyDataChanged()
        return true
    }

    fun placeOrder(studentName: String, item: GearItem, quantity: Int): StudentOrder? {
        if (!item.isAvailable || quantity <= 0 || quantity > item.stock) return null
        item.stock -= quantity
        val order = StudentOrder(
            id = nextUniqueId(orders.maxOfOrNull { it.id } ?: 1000),
            itemId = item.id,
            studentName = studentName,
            itemName = item.name,
            quantity = quantity,
            total = item.price * quantity,
            status = if (item.category.contains("Fabric")) "Fabric cutting" else "Queued for release",
            paid = 0.0,
            claimed = false
        )
        orders.add(0, order)
        database?.updateItem(item)
        database?.insertOrder(order)
        firestore?.saveItem(item)
        firestore?.saveOrder(order)
        addNotification(
            order.studentName,
            order.id,
            "Order submitted",
            "Order #${order.id} for ${order.itemName} was submitted.",
            order.status
        )
        notifyDataChanged()
        return order
    }

    fun notificationsFor(studentName: String): List<StudentNotification> {
        return notifications
            .filter { it.studentName.equals(studentName, ignoreCase = true) }
            .sortedByDescending { it.id }
    }

    private fun addNotification(
        studentName: String,
        orderId: Int,
        title: String,
        message: String,
        status: String
    ): StudentNotification {
        val nextId = nextUniqueId(notifications.maxOfOrNull { it.id } ?: 0)
        val pendingNotification = StudentNotification(nextId, studentName, orderId, title, message, status)
        val notification = database?.insertNotification(pendingNotification) ?: pendingNotification
        firestore?.saveNotification(notification)
        notifications.add(0, notification)
        notifyDataChanged()
        return notification
    }

    fun restock(item: GearItem, addedStock: Int = 12) {
        item.stock += addedStock
        database?.updateItem(item)
        firestore?.saveItem(item)
        notifyDataChanged()
    }

    fun lowestStockItem(): GearItem? = items.minByOrNull { it.stock }

    fun mostRequestedItemName(): String {
        return orders
            .groupBy { it.itemName }
            .maxByOrNull { (_, itemOrders) -> itemOrders.sumOf { it.quantity } }
            ?.key
            ?: "No orders yet"
    }

    fun claimBlockReason(order: StudentOrder): String? {
        return when {
            order.claimed -> "Already claimed."
            order.balance > 0.0 -> "Collect payment before release."
            order.status != "Ready for pickup" -> "Mark ready for pickup before release."
            else -> null
        }
    }

    fun getOrdersForStudent(studentName: String): List<StudentOrder> {
        return orders.filter { it.studentName.equals(studentName, ignoreCase = true) }
            .sortedByDescending { it.orderDate }
    }

    fun getPendingOrders(): List<StudentOrder> {
        return orders.filter { !it.claimed && it.status != "Claimed" && it.status != "Ready for pickup" }
            .sortedByDescending { it.orderDate }
    }

    fun getReadyForPickupOrders(): List<StudentOrder> {
        return orders.filter { it.status == "Ready for pickup" && !it.claimed }
            .sortedByDescending { it.orderDate }
    }

    fun getAllOrders(): List<StudentOrder> {
        return orders.sortedByDescending { it.orderDate }
    }

    fun getTodayOrders(): List<StudentOrder> {
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % DAY_MS)
        return orders.filter { it.orderDate >= todayStart }
            .sortedByDescending { it.orderDate }
    }

    fun getWeeklyOrders(): List<StudentOrder> {
        val weekAgo = System.currentTimeMillis() - (7 * DAY_MS)
        return orders.filter { it.orderDate >= weekAgo }
            .sortedByDescending { it.orderDate }
    }

    fun getMostPurchasedItems(limit: Int = 5): List<Pair<String, Int>> {
        return orders
            .groupBy { it.itemName }
            .map { (itemName, itemOrders) -> itemName to itemOrders.sumOf { it.quantity } }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getMostPurchasedItemsThisWeek(limit: Int = 5): List<Pair<String, Int>> {
        return getWeeklyOrders()
            .groupBy { it.itemName }
            .map { (itemName, itemOrders) -> itemName to itemOrders.sumOf { it.quantity } }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getTodayOrderStats(): Pair<Int, Double> {
        val todayOrders = getTodayOrders()
        return todayOrders.size to todayOrders.sumOf { it.total }
    }

    fun getWeeklyOrderStats(): Pair<Int, Double> {
        val weeklyOrders = getWeeklyOrders()
        return weeklyOrders.size to weeklyOrders.sumOf { it.total }
    }

    fun getInventoryStatus(): List<Pair<String, Int>> {
        return items.map { it.name to it.stock }
            .sortedBy { it.second }
    }

    fun getUnpaidOrders(): List<StudentOrder> {
        return orders.filter { it.balance > 0.0 }
            .sortedByDescending { it.orderDate }
    }

    fun getTotalOrderValue(): Double {
        return orders.sumOf { it.total }
    }

    fun getTodayRevenue(): Double {
        return getTodayOrders().sumOf { it.paid }
    }

    fun getWeeklyRevenue(): Double {
        return getWeeklyOrders().sumOf { it.paid }
    }

    fun getCompletedOrders(): Int {
        return orders.count { it.claimed }
    }

    fun getPendingPaymentAmount(): Double {
        return getUnpaidOrders().sumOf { it.balance }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private val StudentOrder.isReadyForClaim: Boolean
        get() = balance <= 0.0 && status == "Ready for pickup"

    private fun <T> MutableList<T>.replaceWith(values: List<T>) {
        clear()
        addAll(values)
    }

    private fun nextUniqueId(currentMax: Int): Int {
        val timeBasedId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        return maxOf(currentMax + 1, timeBasedId)
    }

    private fun notifyDataChanged() {
        changeListeners.toList().forEach { it.invoke() }
    }

    private const val LOW_STOCK_THRESHOLD = 7
    private const val DAY_MS = 24 * 60 * 60 * 1000L
}

