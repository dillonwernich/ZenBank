//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Transfer : AppCompatActivity() {

    // Declare UI elements
    private lateinit var transferFrom: Spinner // Spinner for selecting the account to transfer from
    private lateinit var transferTo: Spinner // Spinner for selecting the account to transfer to
    private lateinit var transferAmount: EditText // EditText for the transfer amount
    private lateinit var transferReference: EditText // EditText for the transfer reference
    private lateinit var transferButton: Button // Button to initiate the transfer

    // Firebase services
    private lateinit var firebaseAuth: FirebaseAuth // Firebase authentication instance
    private lateinit var database: FirebaseDatabase // Firebase database instance

    // Notification Channel ID
    private val CHANNEL_ID = "transfer_notification_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_transfer) // Set the content view

        // Initialize UI elements
        transferFrom = findViewById(R.id.transfer_from_spinner)
        transferTo = findViewById(R.id.transfer_to_spinner)
        transferAmount = findViewById(R.id.transfer_amount_edtxt)
        transferReference = findViewById(R.id.transfer_reference_edtxt)
        transferButton = findViewById(R.id.transfer_transfer_button)

        // Initialize Firebase services
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Load user account data into Spinners
        loadAccountData()

        // Set up transfer button click event
        transferButton.setOnClickListener {
            performTransfer() // Call method to perform the transfer
        }

        // Create Notification Channel
        createNotificationChannel()
    }

    // Create a notification channel for transfer notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Transfer Notifications"
            val descriptionText = "Notifications for account transfers"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText // Set the channel description
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel) // Create the channel
        }
    }

    // Load user account data and populate the Spinners
    private fun loadAccountData() {
        val userId = firebaseAuth.currentUser?.uid ?: return // Ensure user is logged in
        val userRef = database.getReference("Users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java) // Get user data
                user?.let {
                    // Populate the "from account" Spinner
                    val fromAccountList = listOf(
                        "Debit Account - Balance: R${"%.2f".format(it.debitBalance)}",
                        "Savings Account - Balance: R${"%.2f".format(it.savingsBalance)}"
                    )
                    val fromAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fromAccountList)
                    fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    transferFrom.adapter = fromAdapter

                    // Populate the "to account" Spinner based on the "from account" selection
                    transferFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val toOptions = when (position) {
                                0 -> listOf("Savings Account - Balance: R${"%.2f".format(it.savingsBalance)}")
                                1 -> listOf("Debit Account - Balance: R${"%.2f".format(it.debitBalance)}")
                                else -> emptyList()
                            }
                            val toAdapter = ArrayAdapter(this@Transfer, android.R.layout.simple_spinner_item, toOptions)
                            toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            transferTo.adapter = toAdapter // Update "to account" Spinner options
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Do nothing
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

    // Perform the transfer action
    private fun performTransfer() {
        val fromAccount = transferFrom.selectedItem.toString() // Get selected "from" account
        val toAccount = transferTo.selectedItem.toString() // Get selected "to" account
        val amountStr = transferAmount.text.toString().trim() // Get transfer amount
        val reference = transferReference.text.toString().trim() // Get transfer reference

        // Validate input fields
        if (amountStr.isEmpty() || reference.isEmpty()) {
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

        userRef.get().addOnSuccessListener { snapshot -> // Retrieve user data
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java) // Get user object
                user?.let {
                    // Validate current balances
                    val (fromAccountBalance, toAccountBalance) = when {
                        fromAccount.startsWith("Debit Account") -> {
                            if (it.debitBalance < amount) {
                                Toast.makeText(this, "Insufficient balance in Debit Account.", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            it.debitBalance to if (toAccount.startsWith("Debit Account")) it.debitBalance else it.savingsBalance
                        }
                        fromAccount.startsWith("Savings Account") -> {
                            if (it.savingsBalance < amount) {
                                Toast.makeText(this, "Insufficient balance in Savings Account.", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            it.savingsBalance to if (toAccount.startsWith("Debit Account")) it.debitBalance else it.savingsBalance
                        }
                        else -> return@addOnSuccessListener
                    }

                    // Calculate new balances after transfer
                    val newFromBalance = fromAccountBalance - amount
                    val newToBalance = toAccountBalance + amount

                    // Create tasks to update both accounts
                    val updateFromTask = when {
                        fromAccount.startsWith("Debit Account") -> userRef.child("debitBalance").setValue(newFromBalance)
                        fromAccount.startsWith("Savings Account") -> userRef.child("savingsBalance").setValue(newFromBalance)
                        else -> null
                    }

                    val updateToTask = when {
                        toAccount.startsWith("Debit Account") -> userRef.child("debitBalance").setValue(newToBalance)
                        toAccount.startsWith("Savings Account") -> userRef.child("savingsBalance").setValue(newToBalance)
                        else -> null
                    }

                    val tasks = mutableListOf<Task<Void>>() // List to hold update tasks
                    updateFromTask?.let { tasks.add(it) }
                    updateToTask?.let { tasks.add(it) }

                    // Perform the database update tasks and show result
                    Tasks.whenAll(tasks).addOnCompleteListener { task -> // Wait for all tasks to complete
                        if (task.isSuccessful) {
                            // Prepare a notification for the transfer
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val currentDate = dateFormat.format(Date())

                            val notificationMessage = "TRANSFER\n" +
                                    "Date: $currentDate\n" +
                                    "Amount: R${"%.2f".format(amount)}\n" +
                                    "From: ${fromAccount.substringBefore(" - ")}\n" +
                                    "To: ${toAccount.substringBefore(" - ")}\n" +
                                    "Reference: $reference"

                            // Check for notification permission and send notification
                            sendTransferNotification(notificationMessage)

                            // Save the notification to Firebase
                            val notificationRef = database.getReference("Users").child(userId).child("Notifications").push()
                            notificationRef.setValue(notificationMessage)

                            // Clear input fields
                            transferAmount.text.clear()
                            transferReference.text.clear()

                            // Show success message and navigate back to the main screen
                            Toast.makeText(this, "Transfer Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, Dashboard::class.java)) // Navigate to Dashboard
                            finish() // Close Transfer activity
                        } else {
                            Toast.makeText(this, "Transfer failed. Please try again.", Toast.LENGTH_SHORT).show()
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

    // Send a transfer notification
    private fun sendTransferNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
                return
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.zenbank)  // Replace with your actual icon
            .setContentTitle("Transfer Notification")
            .setContentText(message) // Set the notification content
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set notification priority
            .setAutoCancel(true) // Automatically dismiss notification when tapped

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build()) // Show the notification
        }
    }
}
