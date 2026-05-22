package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class StaffOrderProcessingActivity : AppCompatActivity() {
    private enum class OrderTab {
        Pending,
        Ready,
        History
    }

    private var selectedTab = OrderTab.Pending
    private val repositoryChangeListener = {
        runOnUiThread {
            loadStats()
            renderOrders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_order_processing)

        findViewById<MaterialButton>(R.id.btnBackProcessing).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        findViewById<MaterialButton>(R.id.btnTabPending).setOnClickListener {
            selectedTab = OrderTab.Pending
            renderOrders()
            updateTabButtons()
        }

        findViewById<MaterialButton>(R.id.btnTabReady).setOnClickListener {
            selectedTab = OrderTab.Ready
            renderOrders()
            updateTabButtons()
        }

        findViewById<MaterialButton>(R.id.btnTabAll).setOnClickListener {
            selectedTab = OrderTab.History
            renderOrders()
            updateTabButtons()
        }

        loadStats()
        renderOrders()
        updateTabButtons()
        showOfflineSyncWarningIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        UniWearRepository.addChangeListener(repositoryChangeListener)
    }

    override fun onStop() {
        UniWearRepository.removeChangeListener(repositoryChangeListener)
        super.onStop()
    }

    private fun loadStats() {
        val pendingOrders = UniWearRepository.getPendingOrders()
        val readyOrders = UniWearRepository.getReadyForPickupOrders()
        val unpaidAmount = UniWearRepository.getPendingPaymentAmount()

        findViewById<TextView>(R.id.txtPendingCount).text = pendingOrders.size.toString()
        findViewById<TextView>(R.id.txtReadyCount).text = readyOrders.size.toString()
        findViewById<TextView>(R.id.txtUnpaidAmount).text =
            "PHP ${String.format("%.0f", unpaidAmount)}"
    }

    private fun renderOrders() {
        val orders = when (selectedTab) {
            OrderTab.Pending -> UniWearRepository.getPendingOrders()
            OrderTab.Ready -> UniWearRepository.getReadyForPickupOrders()
            OrderTab.History -> UniWearRepository.getAllOrders()
        }

        val container = findViewById<LinearLayout>(R.id.ordersListContainer)
        container.removeAllViews()

        if (orders.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = when (selectedTab) {
                    OrderTab.Pending -> "No pending orders"
                    OrderTab.Ready -> "No ready orders"
                    OrderTab.History -> "No order history"
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
        val item = UniWearRepository.items.find { it.id == order.itemId }
        val emoji = item?.emoji ?: "📦"
        
        return card().apply {
            // Header: Emoji + Item Name + Student Name
            addView(
                label("$emoji ${order.itemName}", size = 18f, bold = true)
            )
            addView(
                label("Ordered by: ${order.studentName}", size = 13f).withMargins(top = 6)
            )
            
            // Item details: Price per unit, quantity, total
            addView(
                label("Price: ${formatPeso(item?.price ?: 0.0)} per ${item?.unit?.dropLastWhile { it == 's' } ?: "unit"} | Quantity: ${order.quantity}", size = 13f)
                    .withMargins(top = 6)
            )
            
            // Category/Audience
            addView(
                label("${item?.category ?: "Item"} - ${item?.audience ?: ""}", size = 13f).withMargins(top = 4)
            )
            
            // Order total
            addView(
                label("Total: PHP ${String.format("%.2f", order.total)}", size = 16f, bold = true)
                    .withMargins(top = 6)
            )
            
            // Status section
            val statusLine = LinearLayout(this@StaffOrderProcessingActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    label("Status: ${order.status}", size = 13f).withWeight()
                )
                addView(
                    chip(
                        when {
                            order.claimed || order.status == "Claimed" -> "Claimed"
                            order.status == "Ready for pickup" -> if (order.balance <= 0) "Paid" else "Unpaid"
                            else -> "Pending"
                        },
                        available = order.claimed || order.status == "Claimed"
                    ).withMargins(left = 8)
                )
            }
            addView(statusLine.withMargins(top = 8))

            // Action buttons
            if (order.claimed || order.status == "Claimed") {
                addView(
                    chip("✓ Claimed", available = true).withMargins(top = 10)
                )
            } else if (order.status != "Ready for pickup") {
                addView(
                    primaryButton("Mark Ready", red = true).withMargins(top = 12).apply {
                        setOnClickListener {
                            if (UniWearRepository.advanceStatus(order.id)) {
                                UniWearNotifier.notifyStudent(
                                    this@StaffOrderProcessingActivity,
                                    order,
                                    "Ready for pickup",
                                    "Your ${order.itemName} order is ready to claim."
                                )
                                Toast.makeText(
                                    this@StaffOrderProcessingActivity,
                                    "Order #${order.id} marked ready and student notified!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadStats()
                                renderOrders()
                            }
                        }
                    }
                )
            } else {
                if (order.balance > 0) {
                    addView(
                        label("Outstanding: PHP ${String.format("%.2f", order.balance)}", size = 12f)
                            .apply { setTextColor(getColor(R.color.maroon)) }
                            .withMargins(top = 8)
                    )
                    addView(
                        primaryButton("Collect Payment", red = true).withMargins(top = 10).apply {
                            setOnClickListener {
                                UniWearRepository.markBalancePaid(order.id)
                                Toast.makeText(
                                    this@StaffOrderProcessingActivity,
                                    "Payment collected!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadStats()
                                renderOrders()
                            }
                        }
                    )
                } else {
                    addView(
                        chip("✓ Fully Paid", available = true).withMargins(top = 8)
                    )
                }
            }
        }
    }

    private fun updateTabButtons() {
        val btnPending = findViewById<MaterialButton>(R.id.btnTabPending)
        val btnReady = findViewById<MaterialButton>(R.id.btnTabReady)
        val btnAll = findViewById<MaterialButton>(R.id.btnTabAll)

        updateTabButton(btnPending, selectedTab == OrderTab.Pending)
        updateTabButton(btnReady, selectedTab == OrderTab.Ready)
        updateTabButton(btnAll, selectedTab == OrderTab.History)
    }

    private fun updateTabButton(button: MaterialButton, isSelected: Boolean) {
        button.setBackgroundColor(getColor(if (isSelected) R.color.maroon else android.R.color.transparent))
        button.setTextColor(getColor(if (isSelected) R.color.white else R.color.ink))
    }

    private fun showOfflineSyncWarningIfNeeded() {
        if (!UniWearRepository.isRemoteSyncEnabled) {
            Toast.makeText(
                this,
                "Firebase is not configured. Orders from another phone cannot appear here yet.",
                Toast.LENGTH_LONG
            ).show()
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

    private fun <T : android.view.View> T.withWeight(
        weight: Float = 1f,
        start: Int = 0,
        top: Int = 0,
        end: Int = 0,
        bottom: Int = 0
    ): T {
        (layoutParams as? LinearLayout.LayoutParams)?.setMargins(
            dp(start), dp(top), dp(end), dp(bottom)
        ) ?: apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
                setMargins(dp(start), dp(top), dp(end), dp(bottom))
            }
        }
        (layoutParams as? LinearLayout.LayoutParams)?.weight = weight
        return this
    }

    private fun formatPeso(amount: Double): String = "PHP %.0f".format(amount)
}


