package com.example.unigearmanager

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UniWearFirestoreDataSource private constructor(private val firestore: FirebaseFirestore) {
    private val listeners = mutableListOf<ListenerRegistration>()

    fun startListening(
        onItemsChanged: (List<GearItem>) -> Unit,
        onOrdersChanged: (List<StudentOrder>) -> Unit,
        onNotificationsChanged: (List<StudentNotification>) -> Unit
    ) {
        listeners += firestore.collection(COLLECTION_ITEMS)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                onItemsChanged(snapshot.documents.mapNotNull { it.toGearItem() })
            }

        listeners += firestore.collection(COLLECTION_ORDERS)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                onOrdersChanged(
                    snapshot.documents
                        .mapNotNull { it.toStudentOrder() }
                        .sortedByDescending { it.orderDate }
                )
            }

        listeners += firestore.collection(COLLECTION_NOTIFICATIONS)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                onNotificationsChanged(
                    snapshot.documents
                        .mapNotNull { it.toStudentNotification() }
                        .sortedByDescending { it.id }
                )
            }
    }

    fun uploadSeedData(items: List<GearItem>, orders: List<StudentOrder>, notifications: List<StudentNotification>) {
        items.forEach(::saveItem)
        orders.forEach(::saveOrder)
        notifications.forEach(::saveNotification)
    }

    fun seedMissingCollections(
        items: List<GearItem>,
        orders: List<StudentOrder>,
        notifications: List<StudentNotification>
    ) {
        seedCollectionIfEmpty(COLLECTION_ITEMS) {
            items.forEach(::saveItem)
        }
        seedCollectionIfEmpty(COLLECTION_ORDERS) {
            orders.forEach(::saveOrder)
        }
        seedCollectionIfEmpty(COLLECTION_NOTIFICATIONS) {
            notifications.forEach(::saveNotification)
        }
    }

    fun saveItem(item: GearItem) {
        firestore.collection(COLLECTION_ITEMS)
            .document(item.id.toString())
            .set(item.toMap())
    }

    fun saveOrder(order: StudentOrder) {
        firestore.collection(COLLECTION_ORDERS)
            .document(order.id.toString())
            .set(order.toMap())
    }

    fun saveNotification(notification: StudentNotification) {
        firestore.collection(COLLECTION_NOTIFICATIONS)
            .document(notification.id.toString())
            .set(notification.toMap())
    }

    fun stop() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    private fun seedCollectionIfEmpty(collection: String, seed: () -> Unit) {
        firestore.collection(collection)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) seed()
            }
    }

    companion object {
        private const val COLLECTION_ITEMS = "items"
        private const val COLLECTION_ORDERS = "orders"
        private const val COLLECTION_NOTIFICATIONS = "notifications"

        fun create(context: Context): UniWearFirestoreDataSource? {
            FirebaseApp.getApps(context).firstOrNull()?.let { app ->
                return UniWearFirestoreDataSource(FirebaseFirestore.getInstance(app))
            }

            val apiKey = context.getString(R.string.firebase_api_key)
            val appId = context.getString(R.string.firebase_application_id)
            val projectId = context.getString(R.string.firebase_project_id)
            if (apiKey.startsWith("PASTE_") || appId.startsWith("PASTE_") || projectId.startsWith("PASTE_")) {
                return null
            }

            val app = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
            )

            return UniWearFirestoreDataSource(FirebaseFirestore.getInstance(app))
        }
    }
}

private fun GearItem.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "emoji" to emoji,
        "name" to name,
        "category" to category,
        "price" to price,
        "stock" to stock,
        "unit" to unit,
        "audience" to audience
    )
}

private fun StudentOrder.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "itemId" to itemId,
        "studentName" to studentName,
        "itemName" to itemName,
        "quantity" to quantity,
        "total" to total,
        "status" to status,
        "paid" to paid,
        "claimed" to claimed,
        "orderDate" to orderDate
    )
}

private fun StudentNotification.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "studentName" to studentName,
        "orderId" to orderId,
        "title" to title,
        "message" to message,
        "status" to status
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toGearItem(): GearItem? {
    return GearItem(
        id = getLong("id")?.toInt() ?: return null,
        emoji = getString("emoji").orEmpty(),
        name = getString("name") ?: return null,
        category = getString("category").orEmpty(),
        price = getDouble("price") ?: 0.0,
        stock = getLong("stock")?.toInt() ?: 0,
        unit = getString("unit").orEmpty(),
        audience = getString("audience").orEmpty()
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toStudentOrder(): StudentOrder? {
    return StudentOrder(
        id = getLong("id")?.toInt() ?: return null,
        itemId = getLong("itemId")?.toInt() ?: return null,
        studentName = getString("studentName") ?: return null,
        itemName = getString("itemName") ?: return null,
        quantity = getLong("quantity")?.toInt() ?: 0,
        total = getDouble("total") ?: 0.0,
        status = getString("status").orEmpty(),
        paid = getDouble("paid") ?: 0.0,
        claimed = getBoolean("claimed") ?: false,
        orderDate = getLong("orderDate") ?: 0L
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toStudentNotification(): StudentNotification? {
    return StudentNotification(
        id = getLong("id")?.toInt() ?: return null,
        studentName = getString("studentName") ?: return null,
        orderId = getLong("orderId")?.toInt() ?: return null,
        title = getString("title") ?: return null,
        message = getString("message") ?: return null,
        status = getString("status").orEmpty()
    )
}

