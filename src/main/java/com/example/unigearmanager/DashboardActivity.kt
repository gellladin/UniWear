package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class DashboardActivity : AppCompatActivity() {
    private val repositoryChangeListener = {
        runOnUiThread {
            renderMetrics()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        findViewById<MaterialButton>(R.id.btnCatalog).setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnOrders).setOnClickListener {
            startActivity(Intent(this, StaffOrderProcessingActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnInventory).setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnPayments).setOnClickListener {
            startActivity(Intent(this, PaymentsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnDistribution).setOnClickListener {
            startActivity(Intent(this, DistributionActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnUsers).setOnClickListener {
            startActivity(Intent(this, TargetUsersActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnAnalytics).setOnClickListener {
            startActivity(Intent(this, StaffAnalyticsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnStatus).setOnClickListener {
            startActivity(Intent(this, AvailabilityActivity::class.java))
        }
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
        renderMetrics()
    }

    private fun renderMetrics() {
        findViewById<TextView>(R.id.txtStudentsServedMetric).text =
            UniWearRepository.studentsServed.toString()
        findViewById<TextView>(R.id.txtActiveOrdersMetric).text =
            UniWearRepository.activeOrders.toString()
        findViewById<TextView>(R.id.txtGearItemsMetric).text =
            UniWearRepository.items.size.toString()
        findViewById<TextView>(R.id.txtGearItemsBadge).text =
            UniWearRepository.items.size.toString()
        findViewById<TextView>(R.id.txtItemsSummary).text =
            "${UniWearRepository.items.size}\nItems"
        findViewById<TextView>(R.id.txtActiveOrdersSummary).text =
            "${UniWearRepository.activeOrders}\nOrders"
        findViewById<TextView>(R.id.txtLowStockSummary).text =
            "${UniWearRepository.lowStockItems}\nLow Stock"
        findViewById<TextView>(R.id.txtOrderValueMetric).text =
            formatCompactPeso(UniWearRepository.totalPaid)
        findViewById<TextView>(R.id.txtPaidSummary).text =
            "${UniWearRepository.fullyPaidOrders}\nPaid"
    }

    private fun formatCompactPeso(amount: Double): String {
        return if (amount >= 1000.0) {
            "PHP %.1fk".format(amount / 1000.0)
        } else {
            "PHP %.0f".format(amount)
        }
    }
}

