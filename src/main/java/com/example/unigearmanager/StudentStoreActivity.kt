package com.example.unigearmanager

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayoutimport com.example.unigearmanager.*import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class StudentStoreActivity : AppCompatActivity() {
    private lateinit var studentIdInput: EditText
    private lateinit var studentNameDisplay: TextView
    private lateinit var itemListContainer: LinearLayout
    private lateinit var notificationListContainer: LinearLayout
    private var currentStudentId: Int = 0
    private var currentStudentName: String = ""
    private val repositoryChangeListener = {
        runOnUiThread {
            if (::itemListContainer.isInitialized && ::notificationListContainer.isInitialized) {
                renderItems()
                renderNotifications()
            }
        }
    }

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val message = if (granted) {
            "App notifications enabled."
        } else {
            "Orders will still appear in your in-app notifications."
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UniWearNotifier.ensureChannel(this)
        askForNotificationPermission()
        currentStudentId = intent.getIntExtra("STUDENT_ID", 0)
        currentStudentName = intent.getStringExtra("STUDENT_NAME").orEmpty()
        setContentView(buildContent())
        showOfflineSyncWarningIfNeeded()
        
        // Pre-populate student ID if passed from intent
        if (currentStudentId in 1..10 && ::studentIdInput.isInitialized) {
            studentIdInput.setText(currentStudentId.toString())
            // Update display name immediately
            if (::studentNameDisplay.isInitialized) {
                studentNameDisplay.text = "Name: $currentStudentName"
            }
        }
        
        renderItems()
        renderNotifications()
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
        if (::itemListContainer.isInitialized) {
            renderItems()
            renderNotifications()
        }
    }

    private fun buildContent(): View {
        return ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_scrapbook)
            addView(screenRoot().apply {
                addView(poster().apply {
                    addView(titleTab("Student Storefront"))
                    addView(label("Order campus gear and watch your app notifications for pickup updates.", size = 14f).apply {
                        setBackgroundResource(R.drawable.bg_note)
                        setPadding(dp(12), dp(12), dp(12), dp(12))
                    }.withMargins(top = 14))
                    addView(label("Student Details", size = 18f, bold = true).withMargins(top = 14))
                    addView(label("Enter your Student ID (1-10)", size = 13f).withMargins(top = 6))
                    studentIdInput = EditText(this@StudentStoreActivity).apply {
                        hint = "Student ID"
                        setSingleLine(true)
                        inputType = InputType.TYPE_CLASS_NUMBER
                        setTextColor(getColor(R.color.ink))
                        setHintTextColor(getColor(R.color.muted))
                        setBackgroundResource(R.drawable.bg_note)
                        setPadding(dp(12), 0, dp(12), 0)
                    }
                    addView(studentIdInput.withMargins(top = 8).apply {
                        layoutParams.height = dp(52)
                    })
                    studentNameDisplay = TextView(this@StudentStoreActivity).apply {
                        text = "Name: Not loaded"
                        textSize = 14f
                        setTextColor(getColor(R.color.ink))
                        setBackgroundResource(R.drawable.bg_note)
                        setPadding(dp(12), dp(12), dp(12), dp(12))
                    }
                    addView(studentNameDisplay.withMargins(top = 8))
                    studentIdInput.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            val studentId = s?.toString()?.toIntOrNull() ?: 0
                            if (studentId in 1..10) {
                                currentStudentId = studentId
                                currentStudentName = UniWearRepository.getStudentName(studentId)
                                studentNameDisplay.text = "Name: $currentStudentName"
                                renderNotifications()
                                renderItems()
                            } else {
                                studentNameDisplay.text = "Name: Invalid ID"
                                currentStudentId = 0
                                currentStudentName = ""
                                renderNotifications()
                                renderItems()
                            }
                        }
                        override fun afterTextChanged(s: Editable?) = Unit
                    })
                    addView(primaryButton("Back to Dashboard", red = false).withMargins(top = 12).apply {
                        setOnClickListener {
                            startActivity(Intent(this@StudentStoreActivity, StudentDashboardActivity::class.java).apply {
                                putExtra("STUDENT_ID", currentStudentId)
                                putExtra("STUDENT_NAME", currentStudentName)
                            })
                            finish()
                        }
                    })
                    addView(primaryButton("View My Orders", red = false).withMargins(top = 12).apply {
                        setOnClickListener {
                            if (currentStudentId == 0) {
                                Toast.makeText(this@StudentStoreActivity, "Enter a valid Student ID (1-10)", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(this@StudentStoreActivity, StudentOrderHistoryActivity::class.java)
                                intent.putExtra("STUDENT_NAME", currentStudentName)
                                intent.putExtra("STUDENT_ID", currentStudentId)
                                startActivity(intent)
                            }
                        }
                    })
                    addView(label("Available Items", size = 18f, bold = true).withMargins(top = 16))
                    itemListContainer = LinearLayout(this@StudentStoreActivity).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    addView(itemListContainer)
                    addView(label("My Notifications", size = 18f, bold = true).withMargins(top = 16))
                    notificationListContainer = LinearLayout(this@StudentStoreActivity).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    addView(notificationListContainer)
                })
            })
        }
    }

    private fun renderItems() {
        itemListContainer.removeAllViews()
        UniWearRepository.items.forEach { item ->
            itemListContainer.addView(studentItemCard(item))
        }
    }

    private fun studentItemCard(item: GearItem): LinearLayout {
        val quantityInput = EditText(this).apply {
            hint = "Qty"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
            setSingleLine(true)
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.ink))
            setHintTextColor(getColor(R.color.muted))
            setBackgroundResource(R.drawable.bg_note)
            setPadding(dp(10), 0, dp(10), 0)
        }

        return card().apply {
            addView(label("${item.emoji} ${item.name}", size = 18f, bold = true))
            addView(label("${formatPeso(item.price)} per ${item.unitLabel()} | ${stockLabel(item)}", size = 13f))
            addView(label(item.audience, size = 13f))
            addView(quantityInput.withMargins(top = 10).apply {
                layoutParams.height = dp(48)
            })
            addView(primaryButton(if (item.isAvailable) "Place Order" else "Unavailable", red = item.isAvailable).withMargins(top = 10).apply {
                isEnabled = item.isAvailable
                setOnClickListener {
                    placeStudentOrder(item, quantityInput.text.toString().toIntOrNull())
                }
            })
        }.withMargins(top = 12) as LinearLayout
    }

    private fun placeStudentOrder(item: GearItem, quantity: Int?) {
        if (currentStudentName.isEmpty()) {
            Toast.makeText(this, "Enter a valid Student ID (1-10).", Toast.LENGTH_SHORT).show()
            return
        }
        if (quantity == null || quantity <= 0) {
            Toast.makeText(this, "Enter a valid quantity.", Toast.LENGTH_SHORT).show()
            return
        }
        if (quantity > item.stock) {
            Toast.makeText(this, "Only ${item.stock} ${item.unit} available.", Toast.LENGTH_SHORT).show()
            return
        }

        val order = UniWearRepository.placeOrder(currentStudentName, item, quantity)
        if (order == null) {
            Toast.makeText(this, "${item.name} is unavailable.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "Order #${order.id} for ${order.itemName} was submitted."
        UniWearNotifier.notifyStudent(this, order, "Order submitted", message)
        Toast.makeText(this, "Order submitted. Watch My Notifications for updates.", Toast.LENGTH_SHORT).show()
        renderItems()
        renderNotifications()
    }

    private fun renderNotifications() {
        notificationListContainer.removeAllViews()
        val studentName = currentStudentName
        if (studentName.isEmpty()) {
            notificationListContainer.addView(emptyNotificationCard("Enter your name to see your order updates."))
            return
        }

        val notifications = UniWearRepository.notificationsFor(studentName)
        if (notifications.isEmpty()) {
            notificationListContainer.addView(emptyNotificationCard("No notifications yet."))
            return
        }

        notifications.forEach { notification ->
            notificationListContainer.addView(card().apply {
                addView(label(notification.title, size = 16f, bold = true))
                addView(label(notification.message, size = 13f))
                addView(chip("Order #${notification.orderId} - ${notification.status}", available = true).withMargins(top = 8))
            }.withMargins(top = 10))
        }
    }

    private fun emptyNotificationCard(message: String): View {
        return card().apply {
            addView(label(message, size = 13f))
        }.withMargins(top = 10)
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !UniWearNotifier.canNotify(this)) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showOfflineSyncWarningIfNeeded() {
        if (!UniWearRepository.isRemoteSyncEnabled) {
            Toast.makeText(
                this,
                "Firebase is not configured. Orders are saved only on this phone.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stockLabel(item: GearItem): String {
        if (item.stock <= 0) return "Unavailable"
        return "${item.stock} ${item.unit} available"
    }

    private fun formatPeso(amount: Double): String = "PHP %.0f".format(amount)

    private fun GearItem.unitLabel(): String {
        return if (unit.endsWith("s") && unit.length > 1) unit.dropLast(1) else unit
    }
}

