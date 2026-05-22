package com.example.unigearmanager

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unigearmanager.*

class DistributionActivity : AppCompatActivity() {
    private lateinit var distributionListContainer: LinearLayout
    private lateinit var distributionEmptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_distribution)

        distributionListContainer = findViewById(R.id.distributionListContainer)
        distributionEmptyMessage = findViewById(R.id.distributionEmptyMessage)
        renderDistributionLog()
    }

    override fun onResume() {
        super.onResume()
        renderDistributionLog()
    }

    private fun renderDistributionLog() {
        val orders = UniWearRepository.orders
            .filter { !it.claimed }
            .sortedBy { it.id }

        distributionListContainer.removeAllViews()
        distributionEmptyMessage.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE

        orders.forEach { order ->
            distributionListContainer.addView(distributionCard(order))
        }
    }

    private fun distributionCard(order: StudentOrder): View {
        val blockReason = UniWearRepository.claimBlockReason(order)
        val statusText = blockReason ?: "Ready to release"

        return card().apply {
            addView(label("#${order.id} - ${order.studentName}", size = 18f, bold = true))
            addView(label("${order.itemName} x ${order.quantity} | ${order.status}", size = 14f))
            addView(chip(statusText, available = blockReason == null).withMargins(top = 8))
            addView(primaryButton("Edit Order", red = false).withMargins(top = 10).apply {
                setOnClickListener { showEditQuantityDialog(order) }
            })
            addView(primaryButton("Check Off Claimed Item").withMargins(top = 10).apply {
                isEnabled = blockReason == null
                setOnClickListener {
                    if (UniWearRepository.claimOrder(order.id)) {
                        Toast.makeText(
                            this@DistributionActivity,
                            "Claim recorded in distribution log.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@DistributionActivity,
                            UniWearRepository.claimBlockReason(order) ?: "Order cannot be released yet.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    renderDistributionLog()
                }
            })
        }.withMargins(top = 14)
    }

    private fun showEditQuantityDialog(order: StudentOrder) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(order.quantity.toString())
            setSelectAllOnFocus(true)
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Order Quantity")
            .setMessage(order.itemName)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val quantity = input.text.toString().toIntOrNull()
                if (quantity != null && UniWearRepository.updateOrderQuantity(order.id, quantity)) {
                    Toast.makeText(this, "Order updated.", Toast.LENGTH_SHORT).show()
                    renderDistributionLog()
                } else {
                    Toast.makeText(this, "Not enough stock or invalid quantity.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

