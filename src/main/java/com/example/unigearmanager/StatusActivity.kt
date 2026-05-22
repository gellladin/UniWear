package com.example.unigearmanager

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unigearmanager.*

class StatusActivity : AppCompatActivity() {
    private lateinit var statusListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        statusListContainer = findViewById(R.id.statusListContainer)
        renderStatuses()
    }

    override fun onResume() {
        super.onResume()
        renderStatuses()
    }

    private fun renderStatuses() {
        statusListContainer.removeAllViews()
        UniWearRepository.orders
            .sortedBy { it.id }
            .forEach { order ->
                statusListContainer.addView(statusCard(order))
            }
    }

    private fun statusCard(order: StudentOrder): View {
        val shownStatus = if (order.claimed) "Claimed" else order.status
        return card().apply {
            addView(label("#${order.id} - ${order.studentName}", size = 18f, bold = true))
            addView(label("${order.itemName} x ${order.quantity}", size = 14f))
            addView(chip(shownStatus, available = shownStatus == "Ready for pickup" || shownStatus == "Claimed").withMargins(top = 8))
            addView(label(statusDescription(shownStatus), size = 13f))

            if (shownStatus == "Queued for release" || shownStatus == "Fabric cutting") {
                addView(primaryButton("Mark Ready for Pickup", red = false).withMargins(top = 10).apply {
                    setOnClickListener {
                        if (UniWearRepository.advanceStatus(order.id)) {
                            UniWearNotifier.notifyStudent(
                                this@StatusActivity,
                                order,
                                "Ready for pickup",
                                "Your ${order.itemName} order is ready to claim."
                            )
                            Toast.makeText(
                                this@StatusActivity,
                                "Student notified: ready for pickup.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        renderStatuses()
                    }
                })
            }
        }.withMargins(top = 14)
    }

    private fun statusDescription(status: String): String {
        return when (status) {
            "Queued for release" -> "Order was submitted but not prepared yet."
            "Fabric cutting" -> "Fabric order is being prepared."
            "Ready for pickup" -> "Student can claim the item."
            "Claimed" -> "Item was already released."
            else -> "Order progress is being updated."
        }
    }
}

