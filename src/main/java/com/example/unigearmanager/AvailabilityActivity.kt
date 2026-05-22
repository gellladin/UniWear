package com.example.unigearmanager

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.unigearmanager.*

class AvailabilityActivity : AppCompatActivity() {
    private lateinit var availabilityListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_availability)

        availabilityListContainer = findViewById(R.id.availabilityListContainer)
        renderAvailability()

        findViewById<MaterialButton>(R.id.btnSendRestockAlert).setOnClickListener {
            val item = UniWearRepository.lowestStockItem()
            if (item == null) {
                Toast.makeText(this, "No inventory items to restock.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UniWearRepository.restock(item)
            renderAvailability()
            Toast.makeText(this, "${item.name} restocked.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        renderAvailability()
    }

    private fun renderAvailability() {
        availabilityListContainer.removeAllViews()
        UniWearRepository.items.forEach { item ->
            availabilityListContainer.addView(
                TextView(this).apply {
                    text = "${item.name}\n${availabilityLabel(item)}"
                    textSize = 17f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(getColor(R.color.ink))
                    setBackgroundResource(if (item.stock > 0) R.drawable.bg_feature_accent else R.drawable.bg_feature_accent_red)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                }.withMargins(top = 12)
            )
        }
    }

    private fun availabilityLabel(item: GearItem): String {
        return if (item.stock > 0) {
            "${item.stock} ${item.unit} available"
        } else {
            "Unavailable"
        }
    }
}

