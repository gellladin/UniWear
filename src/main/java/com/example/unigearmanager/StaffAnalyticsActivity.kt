package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class StaffAnalyticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_analytics)

        findViewById<MaterialButton>(R.id.btnBackAnalytics).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        loadAnalytics()
    }

    private fun loadAnalytics() {
        // Today's stats
        val (todayOrders, todayTotal) = UniWearRepository.getTodayOrderStats()
        findViewById<TextView>(R.id.txtTodayOrders).text = todayOrders.toString()
        findViewById<TextView>(R.id.txtTodayRevenue).text = formatPeso(todayTotal)

        // Weekly stats
        val (weeklyOrders, weeklyTotal) = UniWearRepository.getWeeklyOrderStats()
        findViewById<TextView>(R.id.txtWeeklyOrders).text = weeklyOrders.toString()
        findViewById<TextView>(R.id.txtWeeklyRevenue).text = formatPeso(weeklyTotal)

        // Most purchased items
        loadMostPurchasedItems()

        // Inventory status
        loadInventoryStatus()
    }

    private fun loadMostPurchasedItems() {
        val mostPurchased = UniWearRepository.getMostPurchasedItemsThisWeek(5)
        val container = findViewById<LinearLayout>(R.id.mostPurchasedContainer)
        container.removeAllViews()

        if (mostPurchased.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No purchases this week"
                setBackgroundResource(R.drawable.bg_note)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setTextColor(getColor(R.color.muted))
            }
            container.addView(emptyView.withMargins(top = 6))
            return
        }

        mostPurchased.forEachIndexed { index, (itemName, quantity) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_poster)
                elevation = 2f
                setPadding(dp(14), dp(12), dp(14), dp(12))
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val rankView = TextView(this).apply {
                text = "${index + 1}."
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.maroon))
                layoutParams = LinearLayout.LayoutParams(dp(30), LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val detailsView = TextView(this).apply {
                text = "$itemName - $quantity units"
                textSize = 13f
                setTextColor(getColor(R.color.ink))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            card.addView(rankView)
            card.addView(detailsView)
            container.addView(card.withMargins(top = 6))
        }
    }

    private fun loadInventoryStatus() {
        val inventory = UniWearRepository.getInventoryStatus()
        val container = findViewById<LinearLayout>(R.id.inventoryContainer)
        container.removeAllViews()

        inventory.forEach { (itemName, stock) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_poster)
                elevation = 2f
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            val nameView = TextView(this).apply {
                text = itemName
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.ink))
            }

            val stockView = TextView(this).apply {
                text = "Stock: $stock units"
                textSize = 12f
                setTextColor(
                    if (stock <= 7) getColor(R.color.maroon) else getColor(R.color.muted)
                )
                setPadding(0, dp(4), 0, 0)
            }

            card.addView(nameView)
            card.addView(stockView)
            container.addView(card.withMargins(top = 6))
        }
    }

    private fun formatPeso(amount: Double): String {
        return if (amount >= 1000.0) {
            "PHP %.1fk".format(amount / 1000.0)
        } else {
            "PHP %.0f".format(amount)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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

