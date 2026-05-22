package com.example.unigearmanager

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.unigearmanager.*

class InventoryActivity : AppCompatActivity() {
    private lateinit var inventoryListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        inventoryListContainer = findViewById(R.id.inventoryListContainer)
        renderInventory()

        findViewById<MaterialButton>(R.id.btnAddStock).setOnClickListener {
            showChooseStockDialog()
        }
        findViewById<MaterialButton>(R.id.btnAddNewItem).setOnClickListener {
            showAddNewItemDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        renderInventory()
    }

    private fun renderInventory() {
        inventoryListContainer.removeAllViews()
        UniWearRepository.items.forEach { item ->
            inventoryListContainer.addView(
                TextView(this).apply {
                    text = "${item.name}\n${item.stock} ${item.unit} remaining${if (item.stock <= 7) " - Low Stock" else ""}"
                    textSize = 17f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(getColor(R.color.ink))
                    setBackgroundResource(if (item.stock <= 7) R.drawable.bg_feature_accent_red else R.drawable.bg_feature_accent)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                }.withMargins(top = 12)
            )
        }
    }

    private fun showChooseStockDialog() {
        val itemNames = UniWearRepository.items.map { "${it.name} (${it.stock} ${it.unit})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose Stock Type")
            .setItems(itemNames) { _, which ->
                showAddStockAmountDialog(UniWearRepository.items[which])
            }
            .show()
    }

    private fun showAddStockAmountDialog(item: GearItem) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "How many ${item.unit}?"
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }

        AlertDialog.Builder(this)
            .setTitle("Add Stock")
            .setMessage(item.name)
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val amount = input.text.toString().toIntOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Enter a valid stock amount.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                UniWearRepository.restock(item, amount)
                renderInventory()
                Toast.makeText(this, "Stock count updated.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddNewItemDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }
        val emojiInput = dialogInput("Emoji")
        val nameInput = dialogInput("Item name")
        val categoryInput = dialogInput("Category")
        val priceInput = dialogInput("Price")
        val stockInput = dialogInput("Starting stock")
        val unitInput = dialogInput("Unit, e.g. pcs, sets, yards")
        val audienceInput = dialogInput("Audience")

        listOf(emojiInput, nameInput, categoryInput, priceInput, stockInput, unitInput, audienceInput).forEach {
            form.addView(it.withMargins(top = 8))
        }

        AlertDialog.Builder(this)
            .setTitle("Add New Item")
            .setView(form)
            .setPositiveButton("Add") { _, _ ->
                val emoji = emojiInput.text.toString().trim()
                val name = nameInput.text.toString().trim()
                val category = categoryInput.text.toString().trim()
                val price = priceInput.text.toString().toDoubleOrNull()
                val stock = stockInput.text.toString().toIntOrNull()
                val unit = unitInput.text.toString().trim()
                val audience = audienceInput.text.toString().trim()

                if (emoji.isEmpty() || name.isEmpty() || category.isEmpty() || price == null || stock == null || unit.isEmpty()) {
                    Toast.makeText(this, "Complete the item details first.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                UniWearRepository.addItem(
                    emoji = emoji,
                    name = name,
                    category = category,
                    price = price,
                    stock = stock,
                    unit = unit,
                    audience = audience.ifEmpty { "All students" }
                )
                renderInventory()
                Toast.makeText(this, "New item added.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dialogInput(hintValue: String): EditText {
        return EditText(this).apply {
            hint = hintValue
            setSingleLine(true)
            setTextColor(getColor(R.color.ink))
            setHintTextColor(getColor(R.color.muted))
        }
    }
}

