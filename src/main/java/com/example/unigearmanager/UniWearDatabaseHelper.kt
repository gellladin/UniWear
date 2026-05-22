package com.example.unigearmanager

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UniWearDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE gear_items (
                id INTEGER PRIMARY KEY,
                emoji TEXT NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL,
                stock INTEGER NOT NULL,
                unit TEXT NOT NULL,
                audience TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE student_orders (
                id INTEGER PRIMARY KEY,
                item_id INTEGER NOT NULL,
                student_name TEXT NOT NULL,
                item_name TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                total REAL NOT NULL,
                status TEXT NOT NULL,
                paid REAL NOT NULL,
                claimed INTEGER NOT NULL,
                order_date INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE student_notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_name TEXT NOT NULL,
                order_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS student_notifications")
        db.execSQL("DROP TABLE IF EXISTS student_orders")
        db.execSQL("DROP TABLE IF EXISTS gear_items")
        onCreate(db)
    }

    fun getItems(): MutableList<GearItem> {
        val items = mutableListOf<GearItem>()
        readableDatabase.query(
            "gear_items",
            null,
            null,
            null,
            null,
            null,
            "id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                items.add(
                    GearItem(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                        stock = cursor.getInt(cursor.getColumnIndexOrThrow("stock")),
                        unit = cursor.getString(cursor.getColumnIndexOrThrow("unit")),
                        audience = cursor.getString(cursor.getColumnIndexOrThrow("audience"))
                    )
                )
            }
        }
        return items
    }

    fun getOrders(): MutableList<StudentOrder> {
        val orders = mutableListOf<StudentOrder>()
        readableDatabase.query(
            "student_orders",
            null,
            null,
            null,
            null,
            null,
            "order_date DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                orders.add(
                    StudentOrder(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        itemId = cursor.getInt(cursor.getColumnIndexOrThrow("item_id")),
                        studentName = cursor.getString(cursor.getColumnIndexOrThrow("student_name")),
                        itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name")),
                        quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                        total = cursor.getDouble(cursor.getColumnIndexOrThrow("total")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        paid = cursor.getDouble(cursor.getColumnIndexOrThrow("paid")),
                        claimed = cursor.getInt(cursor.getColumnIndexOrThrow("claimed")) == 1,
                        orderDate = cursor.getLong(cursor.getColumnIndexOrThrow("order_date"))
                    )
                )
            }
        }
        return orders
    }

    fun getNotifications(): MutableList<StudentNotification> {
        val notifications = mutableListOf<StudentNotification>()
        readableDatabase.query(
            "student_notifications",
            null,
            null,
            null,
            null,
            null,
            "id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                notifications.add(
                    StudentNotification(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        studentName = cursor.getString(cursor.getColumnIndexOrThrow("student_name")),
                        orderId = cursor.getInt(cursor.getColumnIndexOrThrow("order_id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        message = cursor.getString(cursor.getColumnIndexOrThrow("message")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
                    )
                )
            }
        }
        return notifications
    }

    fun insertItem(item: GearItem) {
        writableDatabase.insertWithOnConflict(
            "gear_items",
            null,
            itemValues(item),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateItem(item: GearItem) {
        writableDatabase.update(
            "gear_items",
            itemValues(item),
            "id = ?",
            arrayOf(item.id.toString())
        )
    }

    fun insertOrder(order: StudentOrder) {
        writableDatabase.insertWithOnConflict(
            "student_orders",
            null,
            orderValues(order),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun updateOrder(order: StudentOrder) {
        writableDatabase.update(
            "student_orders",
            orderValues(order),
            "id = ?",
            arrayOf(order.id.toString())
        )
    }

    fun insertNotification(notification: StudentNotification): StudentNotification {
        val values = notificationValues(notification)
        if (notification.id > 0) {
            values.put("id", notification.id)
        }
        val id = writableDatabase.insertWithOnConflict(
            "student_notifications",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        ).toInt()
        return notification.copy(id = id)
    }

    fun replaceItems(items: List<GearItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("gear_items", null, null)
            items.forEach { db.insert("gear_items", null, itemValues(it)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replaceOrders(orders: List<StudentOrder>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("student_orders", null, null)
            orders.forEach { db.insert("student_orders", null, orderValues(it)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replaceNotifications(notifications: List<StudentNotification>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("student_notifications", null, null)
            notifications.forEach {
                val values = notificationValues(it)
                values.put("id", it.id)
                db.insert("student_notifications", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun seed(db: SQLiteDatabase) {
        defaultItems.forEach { db.insert("gear_items", null, itemValues(it)) }
        defaultOrders.forEach { db.insert("student_orders", null, orderValues(it)) }
        defaultNotifications.forEach {
            db.insert("student_notifications", null, notificationValues(it))
        }
    }

    private fun itemValues(item: GearItem): ContentValues {
        return ContentValues().apply {
            put("id", item.id)
            put("emoji", item.emoji)
            put("name", item.name)
            put("category", item.category)
            put("price", item.price)
            put("stock", item.stock)
            put("unit", item.unit)
            put("audience", item.audience)
        }
    }

    private fun orderValues(order: StudentOrder): ContentValues {
        return ContentValues().apply {
            put("id", order.id)
            put("item_id", order.itemId)
            put("student_name", order.studentName)
            put("item_name", order.itemName)
            put("quantity", order.quantity)
            put("total", order.total)
            put("status", order.status)
            put("paid", order.paid)
            put("claimed", if (order.claimed) 1 else 0)
            put("order_date", order.orderDate)
        }
    }

    private fun notificationValues(notification: StudentNotification): ContentValues {
        return ContentValues().apply {
            put("student_name", notification.studentName)
            put("order_id", notification.orderId)
            put("title", notification.title)
            put("message", notification.message)
            put("status", notification.status)
            put("created_at", System.currentTimeMillis())
        }
    }

    companion object {
        private const val DATABASE_NAME = "UniWear_manager.db"
        private const val DATABASE_VERSION = 1

        val defaultItems = listOf(
            GearItem(1, "ID", "Official ID Lanyard", "School Accessory", 85.0, 34, "pcs", "All students"),
            GearItem(2, "LAB", "Nursing Lab Coat Fabric", "Specialized Fabric", 160.0, 0, "yards", "Nursing Department"),
            GearItem(3, "ENG", "Engineering Lanyard", "Department Gear", 95.0, 7, "pcs", "Engineering Department"),
            GearItem(4, "PE", "PE Uniform Set", "Complete Outfit", 750.0, 18, "sets", "College Departments"),
            GearItem(5, "HD", "Souvenir Hoodie", "Campus Souvenir", 980.0, 5, "pcs", "Campus Souvenir Shop")
        )

        val defaultOrders = listOf(
            StudentOrder(1001, 1, "Mika Reyes", "Official ID Lanyard", 1, 85.0, "Ready for pickup", 85.0, false, System.currentTimeMillis() - (2 * 60 * 60 * 1000)),
            StudentOrder(1002, 2, "Leo Santos", "Nursing Lab Coat Fabric", 3, 480.0, "Fabric cutting", 300.0, false, System.currentTimeMillis() - (4 * 60 * 60 * 1000)),
            StudentOrder(1003, 4, "Ana Cruz", "PE Uniform Set", 1, 750.0, "Queued for release", 0.0, false, System.currentTimeMillis() - (6 * 60 * 60 * 1000)),
            StudentOrder(1004, 1, "Mika Reyes", "Official ID Lanyard", 2, 170.0, "Queued for release", 100.0, false, System.currentTimeMillis() - (24 * 60 * 60 * 1000)),
            StudentOrder(1005, 3, "Leo Santos", "Engineering Lanyard", 1, 95.0, "Ready for pickup", 95.0, true, System.currentTimeMillis() - (48 * 60 * 60 * 1000))
        )

        val defaultNotifications = listOf(
            StudentNotification(
                1,
                "Mika Reyes",
                1001,
                "Ready for pickup",
                "Your Official ID Lanyard order is ready to claim.",
                "Ready for pickup"
            )
        )
    }
}

