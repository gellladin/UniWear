package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class StudentLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_login)

        val backButton = findViewById<MaterialButton>(R.id.btnBackStudent)
        val studentIdInputLayout = findViewById<TextInputLayout>(R.id.studentIdInputLayout)
        val emailInputLayout = findViewById<TextInputLayout>(R.id.emailStudentInputLayout)
        val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordStudentInputLayout)
        val studentIdInput = findViewById<TextInputEditText>(R.id.studentIdInput)
        val emailInput = findViewById<TextInputEditText>(R.id.emailStudentInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordStudentInput)
        val loginButton = findViewById<MaterialButton>(R.id.btnStudentLoginSubmit)
        val signUpLink = findViewById<TextView>(R.id.tvStudentSignUp)

        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Auto-populate email when student ID is entered
        studentIdInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val studentId = s?.toString()?.toIntOrNull() ?: 0
                if (studentId in 1..10) {
                    val studentName = UniWearRepository.getStudentName(studentId)
                    val email = studentName.lowercase() + "@student.edu"
                    emailInput.setText(email)
                } else {
                    emailInput.setText("")
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        loginButton.setOnClickListener {
            // Clear previous errors
            studentIdInputLayout.error = null
            emailInputLayout.error = null
            passwordInputLayout.error = null

            val studentId = studentIdInput.text?.toString()?.trim().orEmpty()
            val email = emailInput.text?.toString()?.trim().orEmpty()
            val password = passwordInput.text?.toString().orEmpty()

            var hasError = false
            val studentIdNumber = studentId.toIntOrNull() ?: 0
            if (studentIdNumber !in 1..10) {
                studentIdInputLayout.error = "Enter a valid Student ID from 1 to 10"
                hasError = true
            }
            if (email.isEmpty()) {
                emailInputLayout.error = "Email address is required"
                hasError = true
            }
            if (password.isEmpty()) {
                passwordInputLayout.error = "Password is required"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            val studentName = UniWearRepository.getStudentName(studentIdNumber)
            Toast.makeText(this, "Welcome $studentName!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StudentDashboardActivity::class.java).apply {
                putExtra("STUDENT_ID", studentIdNumber)
                putExtra("STUDENT_NAME", studentName)
            })
            finish()
        }

        signUpLink.setOnClickListener {
            val studentIdNumber = studentIdInput.text?.toString()?.trim()?.toIntOrNull() ?: 0
            val studentName = UniWearRepository.getStudentName(studentIdNumber).takeIf { studentIdNumber in 1..10 }
                ?: "Student"
            Toast.makeText(this, "Student account ready.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, StudentDashboardActivity::class.java).apply {
                putExtra("STUDENT_ID", studentIdNumber)
                putExtra("STUDENT_NAME", studentName)
            })
            finish()
        }
    }
}

