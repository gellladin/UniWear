package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class OrderActivity : AppCompatActivity() {
    private lateinit var selectedItem: GearItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        val itemId = intent.getIntExtra(EXTRA_ITEM_ID, 1)
        selectedItem = UniWearRepository.itemById(itemId) ?: UniWearRepository.items.first()

        val studentNameInput = findViewById<EditText>(R.id.studentNameInput)
        val quantityInput = findViewById<EditText>(R.id.quantityInput)
        val submitButton = findViewById<MaterialButton>(R.id.btnSubmitOrder)
        val reviewPanel = findViewById<View>(R.id.orderReviewPanel)
        val submittedPanel = findViewById<View>(R.id.orderSubmittedPanel)
        val submittedText = findViewById<TextView>(R.id.orderSubmittedText)

        findViewById<TextView>(R.id.orderItemText).text = selectedItem.name
        findViewById<TextView>(R.id.orderStockText).text =
            "${formatPeso(selectedItem.price)} per ${selectedItem.unitLabel()} | ${selectedItem.stock} ${selectedItem.unit} available"
        updateTotal()

        quantityInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateTotal()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        submitButton.setOnClickListener {
            val studentName = studentNameInput.text.toString().trim()
            val quantity = quantityInput.text.toString().toIntOrNull()

            if (studentName.isEmpty()) {
                Toast.makeText(this, "Enter the student name first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (quantity == null || quantity <= 0) {
                Toast.makeText(this, "Enter a valid quantity.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (quantity > selectedItem.stock) {
                Toast.makeText(this, "Only ${selectedItem.stock} ${selectedItem.unit} available.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val order = UniWearRepository.placeOrder(studentName, selectedItem, quantity)
            if (order == null) {
                Toast.makeText(this, "${selectedItem.name} is unavailable.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            studentNameInput.isEnabled = false
            quantityInput.isEnabled = false
            submitButton.isEnabled = false
            reviewPanel.visibility = View.GONE
            submittedPanel.visibility = View.VISIBLE
            submittedText.text = "Order #${order.id} submitted for ${order.studentName}."
            UniWearNotifier.notifyStudent(
                this,
                order,
                "Order submitted",
                "Order #${order.id} for ${order.itemName} was submitted."
            )
            Toast.makeText(this, "Order submitted. Payments and availability updated.", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnGoPayments).setOnClickListener {
            startActivity(Intent(this, PaymentsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnGoOrderStatus).setOnClickListener {
            startActivity(Intent(this, StatusActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnBackCatalog).setOnClickListener {
            finish()
        }
    }

    private fun updateTotal() {
        val quantity = findViewById<EditText>(R.id.quantityInput)
            .text
            .toString()
            .toIntOrNull()
            ?: 0
        val total = selectedItem.price * quantity
        findViewById<TextView>(R.id.orderTotalText).text =
            "Total: ${formatPeso(total)} (${quantity} x ${formatPeso(selectedItem.price)})"
    }

    private fun formatPeso(amount: Double): String = "PHP %.0f".format(amount)

    private fun GearItem.unitLabel(): String {
        return if (unit.endsWith("s") && unit.length > 1) unit.dropLast(1) else unit
    }

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
    }
}

