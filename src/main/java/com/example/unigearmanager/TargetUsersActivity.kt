package com.example.unigearmanager

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.unigearmanager.*

class TargetUsersActivity : AppCompatActivity() {
    private lateinit var activeUsersContainer: LinearLayout
    private val repositoryChangeListener = {
        runOnUiThread {
            if (::activeUsersContainer.isInitialized) {
                renderActiveUsers()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        renderActiveUsers()
    }

    override fun onStart() {
        super.onStart()
        UniWearRepository.addChangeListener(repositoryChangeListener)
    }

    override fun onStop() {
        UniWearRepository.removeChangeListener(repositoryChangeListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (::activeUsersContainer.isInitialized) {
            renderActiveUsers()
        }
    }

    private fun buildContent(): View {
        return ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_scrapbook)
            addView(screenRoot().apply {
                addView(poster().apply {
                    addView(titleTab("Active Users"))
                    addView(label("Students who have placed orders", size = 14f).apply {
                        setBackgroundResource(R.drawable.bg_note)
                        setPadding(dp(12), dp(12), dp(12), dp(12))
                    }.withMargins(top = 14))
                    activeUsersContainer = LinearLayout(this@TargetUsersActivity).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    addView(activeUsersContainer.withMargins(top = 14))
                })
            })
        }
    }

    private fun renderActiveUsers() {
        activeUsersContainer.removeAllViews()
        
        // Get all unique students who have placed orders
        val activeStudents = UniWearRepository.orders
            .map { it.studentName }
            .distinct()
            .sorted()
        
        if (activeStudents.isEmpty()) {
            activeUsersContainer.addView(card().apply {
                addView(label("No active orders yet", size = 14f, bold = true))
                addView(label("Students who place orders will appear here.", size = 13f))
            }.withMargins(top = 10))
            return
        }

        activeUsersContainer.addView(label("Total Active Users: ${activeStudents.size}", size = 16f, bold = true).withMargins(top = 10))

        activeStudents.forEach { studentName ->
            val studentOrders = UniWearRepository.orders.filter { it.studentName == studentName }
            val pendingOrders = studentOrders.filter { !it.claimed && it.status != "Claimed" && it.status != "Ready for pickup" }
            val readyOrders = studentOrders.filter { it.status == "Ready for pickup" && !it.claimed }

            activeUsersContainer.addView(card().apply {
                addView(label("📋 $studentName", size = 16f, bold = true))
                addView(label("Total Orders: ${studentOrders.size}", size = 13f))
                addView(label("Pending: ${pendingOrders.size} | Ready: ${readyOrders.size}", size = 13f))
                
                // Show last order
                studentOrders.maxByOrNull { it.orderDate }?.let { lastOrder ->
                    addView(label("Last Order: ${lastOrder.itemName} (${lastOrder.quantity}x) - ${lastOrder.status}", size = 12f))
                }
            }.withMargins(top = 10))
        }
    }

    private fun View.withMargins(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): View {
        val density = resources.displayMetrics.density
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins((left * density).toInt(), (top * density).toInt(), (right * density).toInt(), (bottom * density).toInt())
        }
        return this
    }
}

