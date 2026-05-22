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

class PaymentsActivity : AppCompatActivity() {
    private lateinit var paymentListContainer: LinearLayout
    private lateinit var paymentEmptyMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payments)

        paymentListContainer = findViewById(R.id.paymentListContainer)
        paymentEmptyMessage = findViewById(R.id.paymentEmptyMessage)
        renderPaymentTracking()
    }

    override fun onResume() {
        super.onResume()
        renderPaymentTracking()
    }

    private fun renderPaymentTracking() {
        val orders = UniWearRepository.orders
            .filter { it.balance > 0.0 }
            .sortedBy { it.id }

        paymentListContainer.removeAllViews()
        paymentEmptyMessage.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE

        orders.forEach { order ->
            paymentListContainer.addView(paymentCard(order))
        }
    }

    private fun paymentCard(order: StudentOrder): View {
        return card().apply {
            addView(label("#${order.id} - ${order.studentName}", size = 18f, bold = true))
            addView(label("${order.itemName} x ${order.quantity}", size = 14f))
            addView(label("Total: ${formatPeso(order.total)} | Paid: ${formatPeso(order.paid)}", size = 14f))
            addView(chip("Balance: ${formatPeso(order.balance)}", available = false).withMargins(top = 8))
            addView(primaryButton("Edit Order", red = false).withMargins(top = 10).apply {
                setOnClickListener { showEditQuantityDialog(order) }
            })
            addView(primaryButton("Mark Balance Paid").withMargins(top = 10).apply {
                setOnClickListener {
                    val paidNow = UniWearRepository.markBalancePaid(order.id)
                    if (paidNow > 0.0) {
                        Toast.makeText(
                            this@PaymentsActivity,
                            "Payment marked as complete.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    renderPaymentTracking()
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
                    renderPaymentTracking()
                } else {
                    Toast.makeText(this, "Not enough stock or invalid quantity.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatPeso(amount: Double): String = "PHP %.0f".format(amount)
}

