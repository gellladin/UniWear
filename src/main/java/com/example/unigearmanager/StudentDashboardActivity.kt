package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.unigearmanager.*

class StudentDashboardActivity : AppCompatActivity() {
    private var studentId: Int = 0
    private lateinit var studentName: String
    private lateinit var pendingCountText: TextView
    private lateinit var totalCountText: TextView
    private lateinit var readyCountText: TextView
    private lateinit var recentActionsContainer: LinearLayout

    private val repositoryChangeListener = {
        runOnUiThread {
            if (::pendingCountText.isInitialized) {
                renderDashboard()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentId = intent.getIntExtra("STUDENT_ID", 0)
        studentName = intent.getStringExtra("STUDENT_NAME")
            ?: UniWearRepository.getStudentName(studentId).takeIf { studentId in 1..10 }
            ?: "Student"
        setContentView(buildContent())
        renderDashboard()
    }

    override fun onStart() {
        super.onStart()
        UniWearRepository.addChangeListener(repositoryChangeListener)
    }

    override fun onStop() {
        UniWearRepository.removeChangeListener(repositoryChangeListener)
        super.onStop()
    }

    private fun buildContent(): View {
        return ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_scrapbook)
            addView(screenRoot().apply {
                addView(poster().apply {
                    addView(titleTab("Student Dashboard"))
                    addView(label("Welcome, $studentName", size = 20f, bold = true).withMargins(top = 14))
                    addView(label("Track your orders, pending requests, and order history in one place.", size = 14f).apply {
                        setBackgroundResource(R.drawable.bg_note)
                        setPadding(dp(12), dp(12), dp(12), dp(12))
                    }.withMargins(top = 8))

                    addView(summaryRow().withMargins(top = 14))

                    addView(primaryButton("Order Campus Gear", red = true).withMargins(top = 16).apply {
                        setOnClickListener { openStore() }
                    })
                    addView(primaryButton("Pending Orders", red = false).withMargins(top = 10).apply {
                        setOnClickListener { openHistory("PENDING") }
                    })
                    addView(primaryButton("History / Reports", red = false).withMargins(top = 10).apply {
                        setOnClickListener { openHistory("ALL") }
                    })

                    addView(label("Recent Actions", size = 18f, bold = true).withMargins(top = 18))
                    recentActionsContainer = LinearLayout(this@StudentDashboardActivity).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    addView(recentActionsContainer)
                })
            })
        }
    }

    private fun summaryRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            pendingCountText = metric("0", "Pending").also { addView(it.withWeight(right = 8)) }
            totalCountText = metric("0", "Orders").also { addView(it.withWeight(right = 8)) }
            readyCountText = metric("0", "Ready").also { addView(it.withWeight()) }
        }
    }

    private fun metric(value: String, label: String): TextView {
        return TextView(this).apply {
            text = "$value\n$label"
            textSize = 14f
            setTextColor(getColor(R.color.ink))
            gravity = android.view.Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_feature_accent)
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
    }

    private fun renderDashboard() {
        val orders = UniWearRepository.getOrdersForStudent(studentName)
        val pendingOrders = orders.filter { !it.claimed && it.status != "Claimed" && it.status != "Ready for pickup" }
        val readyOrders = orders.filter { it.status == "Ready for pickup" && !it.claimed }

        pendingCountText.text = "${pendingOrders.size}\nPending"
        totalCountText.text = "${orders.size}\nOrders"
        readyCountText.text = "${readyOrders.size}\nReady"

        recentActionsContainer.removeAllViews()
        if (orders.isEmpty()) {
            recentActionsContainer.addView(card().apply {
                addView(label("No orders yet.", size = 14f, bold = true))
                addView(label("Use Order Campus Gear to create your first order.", size = 13f))
            }.withMargins(top = 10))
            return
        }

        orders.take(4).forEach { order ->
            recentActionsContainer.addView(card().apply {
                addView(label("Order #${order.id}: ${order.itemName}", size = 15f, bold = true))
                addView(label("Qty ${order.quantity} | PHP ${String.format("%.2f", order.total)}", size = 13f))
                addView(label("Status: ${order.status}", size = 13f))
                addView(label(UniWearRepository.formatDate(order.orderDate), size = 12f))
            }.withMargins(top = 10))
        }
    }

    private fun openStore() {
        startActivity(Intent(this, StudentStoreActivity::class.java).apply {
            putExtra("STUDENT_ID", studentId)
            putExtra("STUDENT_NAME", studentName)
        })
    }

    private fun openHistory(filter: String) {
        startActivity(Intent(this, StudentOrderHistoryActivity::class.java).apply {
            putExtra("STUDENT_ID", studentId)
            putExtra("STUDENT_NAME", studentName)
            putExtra("ORDER_FILTER", filter)
        })
    }

    private fun View.withWeight(right: Int = 0): View {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(0, 0, dp(right), 0)
        }
        return this
    }
}

