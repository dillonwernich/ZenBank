//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate

class Pay : AppCompatActivity() {

    // Declare UI elements
    private lateinit var payRecipient: Spinner // Spinner for selecting recipient bank
    private lateinit var recipientAccount: EditText // EditText for recipient's account number
    private lateinit var payAccount: Spinner // Spinner for selecting user's account
    private lateinit var payAmount: EditText // EditText for payment amount
    private lateinit var payReference: EditText // EditText for payment reference
    private lateinit var payButton: Button // Button to initiate payment

    // Firebase services
    private lateinit var firebaseAuth: FirebaseAuth // Firebase authentication instance
    private lateinit var database: FirebaseDatabase // Firebase database instance

    // Notification Channel ID and attributes
    private val CHANNEL_ID = "payment_notification_channel"
    private val CHANNEL_NAME = "Payment Notifications"
    private val CHANNEL_DESCRIPTION = "Notifications for payment transactions"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_pay) // Set the content view

        // Initialize views
        payRecipient = findViewById(R.id.pay_bank_spinner)
        recipientAccount = findViewById(R.id.pay_recipient_account_number_edtxt)
        payAccount = findViewById(R.id.pay_your_account_spinner)
        payAmount = findViewById(R.id.pay_amount_edtxt)
        payReference = findViewById(R.id.pay_reference_edtxt)
        payButton = findViewById(R.id.pay_pay_button)

        // Initialize Firebase services
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Set up recipient bank Spinner
        val recipientAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.recipientBank, // Array of bank options
            android.R.layout.simple_spinner_item
        )
        recipientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        payRecipient.adapter = recipientAdapter

        // Set up account Spinner for user's accounts
        updateAccountSpinner()

        // Handle pay button click
        payButton.setOnClickListener {
            performPayment() // Perform the payment action
        }

        // Create Notification Channel
        createNotificationChannel() // Set up notification channel
    }

    // Create a notification channel for payment notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH // Set the importance level
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION // Set channel description
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel) // Create the notification channel
        }
    }

    // Update the Spinner for accounts based on user's data
    private fun updateAccountSpinner() {
        val userId = firebaseAuth.currentUser?.uid ?: return // Ensure user is logged in
        val userRef = database.getReference("Users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java) // Get user data
                user?.let {
                    // Create account options based on user's balances
                    val accountOptions = listOf(
                        "Debit Account - R${"%.2f".format(it.debitBalance)}",
                        "Savings Account - R${"%.2f".format(it.savingsBalance)}"
                    )
                    val accountAdapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        accountOptions
                    )
                    accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    payAccount.adapter = accountAdapter // Set the adapter for the account Spinner
                }
            } else {
                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show() // Handle case when user data not found
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show() // Handle failure to retrieve data
        }
    }

    // Perform payment based on user inputs
    private fun performPayment() {
        val selectedAccount = payAccount.selectedItem.toString() // Get selected account
        val recipientBank = payRecipient.selectedItem.toString() // Get selected recipient bank
        val recipientAccountNum = recipientAccount.text.toString().trim() // Get recipient account number
        val amountStr = payAmount.text.toString().trim() // Get payment amount
        val reference = payReference.text.toString().trim() // Get payment reference

        // Validate user inputs
        if (recipientAccountNum.isEmpty() || amountStr.isEmpty() || reference.isEmpty()) {
            Toast.makeText(this, "Please enter all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull() // Parse amount to Double
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = firebaseAuth.currentUser?.uid ?: return // Get user ID
        val userRef = database.getReference("Users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java) // Get user data
                user?.let {
                    // Determine the new balance based on the selected account
                    val newBalance = when {
                        selectedAccount.startsWith("Debit Account") && it.debitBalance >= amount -> it.debitBalance - amount
                        selectedAccount.startsWith("Savings Account") && it.savingsBalance >= amount -> it.savingsBalance - amount
                        else -> {
                            Toast.makeText(this, "Insufficient balance.", Toast.LENGTH_SHORT).show() // Insufficient funds
                            return@addOnSuccessListener
                        }
                    }

                    // Update the user's balance in Firebase
                    val balanceUpdateTask = if (selectedAccount.startsWith("Debit Account")) {
                        userRef.child("debitBalance").setValue(newBalance)
                    } else {
                        userRef.child("savingsBalance").setValue(newBalance)
                    }

                    balanceUpdateTask.addOnCompleteListener { task -> // Handle completion of balance update
                        if (task.isSuccessful) {
                            // Prepare notification message
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val currentDate = dateFormat.format(Date())
                            val notificationMessage = """
                            PAYMENT
                            Date: $currentDate
                            From: $selectedAccount
                            To: $recipientBank
                            Account Number: $recipientAccountNum
                            Amount: R${"%.2f".format(amount)}
                            Reference: $reference
                        """.trimIndent()

                            sendPaymentNotification(notificationMessage) // Send payment notification
                            val notificationRef = database.getReference("Users").child(userId).child("Notifications").push()
                            notificationRef.setValue(notificationMessage) // Save notification in Firebase
                            recipientAccount.text.clear() // Clear input fields
                            payAmount.text.clear()
                            payReference.text.clear()
                            Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show() // Show success message
                            startActivity(Intent(this, Dashboard::class.java)) // Navigate to Dashboard
                            finish()
                        } else {
                            Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show() // Show error message
                        }
                    }
                }
            } else {
                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show() // Handle case when user data not found
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show() // Handle failure to retrieve data
        }
    }

    // Send a payment notification
    private fun sendPaymentNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                return
            }
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.zenbank)  // Replace with your actual icon
            .setContentTitle("Payment Successful")
            .setContentText(message) // Set the content text for the notification
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Notification will be dismissed when clicked

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build()) // Show the notification
        }
    }

    // Handle the result of the notification permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
