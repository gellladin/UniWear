package com.example.unigearmanager

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // User Type Selection
        findViewById<MaterialButton>(R.id.btnStudentOption).setOnClickListener {
            startActivity(Intent(this, StudentLoginActivity::class.java))
            finish()
        }

        findViewById<MaterialButton>(R.id.btnStaffOption).setOnClickListener {
            startActivity(Intent(this, StaffLoginActivity::class.java))
            finish()
        }
    }
}

