package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class StudentOrderHistoryActivity : AppCompatActivity() {
    private lateinit var studentName: String
    private var studentId: Int = 0
    private var orderFilter: String = "ALL"
    private val repositoryChangeListener = {
        runOnUiThread {
            if (::studentName.isInitialized) {
                loadOrderHistory()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_order_history)

        studentId = intent.getIntExtra("STUDENT_ID", 0)
        studentName = intent.getStringExtra("STUDENT_NAME")
            ?: UniWearRepository.getStudentName(studentId).takeIf { studentId in 1..10 }
            ?: "Student"
        orderFilter = intent.getStringExtra("ORDER_FILTER") ?: "ALL"

        val isPendingOnly = orderFilter == "PENDING"
        findViewById<TextView>(R.id.txtHistoryTitle).text = if (isPendingOnly) "Pending Orders" else "My Order History"
        findViewById<TextView>(R.id.txtHistorySubtitle).text = if (isPendingOnly) {
            "Orders still waiting for payment, processing, or release"
        } else {
            "History and reports of your campus gear orders"
        }

        findViewById<MaterialButton>(R.id.btnBackHistory).setOnClickListener {
            startActivity(Intent(this, StudentDashboardActivity::class.java).apply {
                putExtra("STUDENT_ID", studentId)
                putExtra("STUDENT_NAME", studentName)
            })
            finish()
        }

        findViewById<MaterialButton>(R.id.btnContinueShopping).setOnClickListener {
            startActivity(Intent(this, StudentStoreActivity::class.java).apply {
                putExtra("STUDENT_ID", studentId)
                putExtra("STUDENT_NAME", studentName)
            })
            finish()
        }

        loadOrderHistory()
    }

    override fun onStart() {
        super.onStart()
        UniWearRepository.addChangeListener(repositoryChangeListener)
    }

    override fun onStop() {
        UniWearRepository.removeChangeListener(repositoryChangeListener)
        super.onStop()
    }

    private fun loadOrderHistory() {
        val allStudentOrders = UniWearRepository.getOrdersForStudent(studentName)
        val orders = if (orderFilter == "PENDING") {
            allStudentOrders.filter { !it.claimed && it.status != "Claimed" && it.status != "Ready for pickup" }
        } else {
            allStudentOrders
        }
        
        // Update summary stats
        val totalOrders = allStudentOrders.size
        val readyCount = allStudentOrders.count { it.status == "Ready for pickup" && !it.claimed }
        val claimedCount = allStudentOrders.count { it.claimed }

        findViewById<TextView>(R.id.txtTotalOrders).text = totalOrders.toString()
        findViewById<TextView>(R.id.txtReadyPickup).text = readyCount.toString()
        findViewById<TextView>(R.id.txtClaimedOrders).text = claimedCount.toString()

        // Display orders
        val container = findViewById<LinearLayout>(R.id.ordersListContainer)
        container.removeAllViews()

        if (orders.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = if (orderFilter == "PENDING") {
                    "No pending orders right now."
                } else {
                    "No orders yet. Start shopping!"
                }
                setBackgroundResource(R.drawable.bg_note)
                setPadding(dp(16), dp(24), dp(16), dp(24))
                setTextColor(getColor(R.color.muted))
            }
            container.addView(emptyView.withMargins(top = 12))
            return
        }

        orders.forEach { order ->
            container.addView(createOrderCard(order).withMargins(top = 12))
        }
    }

    private fun createOrderCard(order: StudentOrder): LinearLayout {
        return card().apply {
            addView(
                label("Order #${order.id} - ${order.itemName}", size = 16f, bold = true)
            )
            addView(
                label("Qty: ${order.quantity} | Total: PHP ${String.format("%.2f", order.total)}", size = 13f)
                    .withMargins(top = 6)
            )
            addView(
                label("Status: ${order.status}", size = 13f).withMargins(top = 6)
            )
            addView(
                label(UniWearRepository.formatDate(order.orderDate), size = 12f)
                    .withMargins(top = 6)
            )

            // Payment status
            val paymentStatus = if (order.balance <= 0) {
                "Fully Paid"
            } else {
                "Unpaid: PHP ${String.format("%.2f", order.balance)}"
            }
            addView(
                label(paymentStatus, size = 12f).withMargins(top = 6)
            )

            // Claim status
            if (order.claimed) {
                addView(
                    chip("Claimed", available = true).withMargins(top = 10)
                )
            } else if (order.status == "Ready for pickup") {
                addView(
                    primaryButton("Claim Order", red = true).withMargins(top = 10).apply {
                        setOnClickListener {
                            val message = if (UniWearRepository.claimOrder(order.id)) {
                                "Order #${order.id} claimed!"
                            } else {
                                UniWearRepository.claimBlockReason(order) ?: "Order cannot be claimed yet."
                            }
                            Toast.makeText(this@StudentOrderHistoryActivity, message, Toast.LENGTH_SHORT).show()
                            loadOrderHistory()
                        }
                    }
                )
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_poster)
            elevation = 2f
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    }

    private fun label(text: String, size: Float = 14f, bold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(getColor(R.color.ink))
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun chip(text: String, available: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setBackgroundResource(if (available) R.drawable.bg_feature_accent else R.drawable.bg_note)
            setTextColor(getColor(R.color.ink))
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }
    }

    private fun primaryButton(text: String, red: Boolean = false): MaterialButton {
        return MaterialButton(this).apply {
            this.text = text
            isAllCaps = false
            minHeight = dp(48)
            minWidth = 0
            insetTop = 0
            insetBottom = 0
            setBackgroundColor(getColor(if (red) R.color.maroon else R.color.yellow))
            setTextColor(getColor(if (red) R.color.white else R.color.ink))
            cornerRadius = dp(14)
        }
    }

    private fun LinearLayout.withMargins(
        start: Int = 0,
        top: Int = 0,
        end: Int = 0,
        bottom: Int = 0
    ): LinearLayout {
        (layoutParams as? LinearLayout.LayoutParams)?.setMargins(
            dp(start), dp(top), dp(end), dp(bottom)
        ) ?: apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(start), dp(top), dp(end), dp(bottom))
            }
        }
        return this
    }
}

