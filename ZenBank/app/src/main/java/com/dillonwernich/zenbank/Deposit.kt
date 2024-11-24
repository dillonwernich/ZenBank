//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate

class Deposit : AppCompatActivity() {

    // UI elements
    private lateinit var accountSpinner: Spinner
    private lateinit var depositAmount: EditText
    private lateinit var depositReference: EditText
    private lateinit var depositButton: Button

    // Firebase instances
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Notification Channel ID and attributes
    private val CHANNEL_ID = "deposit_notification_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_deposit)

        // Initialize views
        accountSpinner = findViewById(R.id.deposit_select_account_spinner)
        depositAmount = findViewById(R.id.deposit_amount_edtxt)
        depositReference = findViewById(R.id.deposit_reference_edtxt)
        depositButton = findViewById(R.id.deposit_deposit_button)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Load accounts into Spinner
        loadAccountData()

        // Create Notification Channel
        createNotificationChannel()

        // Handle deposit button click
        depositButton.setOnClickListener {
            performDeposit()
        }
    }

    // Create a notification channel for deposit notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Deposit Notifications"
            val descriptionText = "Notifications for deposits into accounts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = descriptionText
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Load account data from Firebase and populate the Spinner
    private fun loadAccountData() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val userRef = database.getReference("Users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    val accountList = listOf(
                        "Debit Account - Balance: R${"%.2f".format(it.debitBalance)}",
                        "Savings Account - Balance: R${"%.2f".format(it.savingsBalance)}"
                    )
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    accountSpinner.adapter = adapter
                }
            } else {
                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle deposit action
    private fun performDeposit() {
        val selectedAccount = accountSpinner.selectedItem.toString()
        val amountStr = depositAmount.text.toString().trim()
        val reference = depositReference.text.toString().trim()

        // Validate input fields
        if (amountStr.isEmpty() || reference.isEmpty()) {
            Toast.makeText(this, "Please enter all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = firebaseAuth.currentUser?.uid ?: return
        val userRef = database.getReference("Users").child(userId)

        // Update user's balance in Firebase
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    val (newBalance, accountName) = when {
                        selectedAccount.startsWith("Debit Account") -> it.debitBalance + amount to "Debit Account"
                        selectedAccount.startsWith("Savings Account") -> it.savingsBalance + amount to "Savings Account"
                        else -> return@addOnSuccessListener
                    }

                    userRef.child(if (selectedAccount.startsWith("Debit Account")) "debitBalance" else "savingsBalance").setValue(newBalance).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Prepare and send a notification
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val currentDate = dateFormat.format(Date())
                            val notificationMessage = "DEPOSIT\nDate: $currentDate\nAmount: R${"%.2f".format(amount)}\nAccount: $accountName\nReference: $reference"

                            // Check permission and send notification
                            sendDepositNotification(notificationMessage)

                            // Save notification to Firebase
                            val notificationRef = database.getReference("Users").child(userId).child("Notifications").push()
                            notificationRef.setValue(notificationMessage)

                            // Clear fields and show success message
                            depositAmount.text.clear()
                            depositReference.text.clear()
                            Toast.makeText(this, "Deposit Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, Dashboard::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Deposit failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show()
        }
    }

    // Send a notification for the deposit
    private fun sendDepositNotification(message: String) {
        // Check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                return
            }
        }

        // Build and display the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.zenbank)  // Replace with actual icon
            .setContentTitle("Deposit Notification")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}