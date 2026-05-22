package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unigearmanager.*

class CatalogActivity : AppCompatActivity() {
    private lateinit var catalogListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        catalogListContainer = findViewById(R.id.catalogListContainer)
        renderCatalogStock()
    }

    override fun onResume() {
        super.onResume()
        renderCatalogStock()
    }

    private fun renderCatalogStock() {
        catalogListContainer.removeAllViews()
        UniWearRepository.items.forEach { item ->
            catalogListContainer.addView(catalogCard(item))
        }
    }

    private fun catalogCard(item: GearItem): LinearLayout {
        return card().apply {
            addView(label(item.name, size = 18f, bold = true))
            addView(label("${formatPeso(item.price)} per ${item.unit.dropLastWhile { it == 's' }} | ${stockLabel(item)}", size = 13f))
            addView(label(item.category, size = 13f))
            addView(primaryButton(if (item.isAvailable) "Order this item" else "Notify me when restocked", red = item.isAvailable).withMargins(top = 10).apply {
                setOnClickListener {
                    if (item.isAvailable) {
                        startActivity(
                            Intent(this@CatalogActivity, OrderActivity::class.java)
                                .putExtra(OrderActivity.EXTRA_ITEM_ID, item.id)
                        )
                    } else {
                        Toast.makeText(this@CatalogActivity, "Restock alert saved.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }.withMargins(top = 14) as LinearLayout
    }

    private fun stockLabel(item: GearItem): String {
        if (item.stock <= 0) return "Unavailable"
        return "${item.stock} ${item.unit} available"
    }

    private fun formatPeso(amount: Double): String = "PHP %.0f".format(amount)
}

