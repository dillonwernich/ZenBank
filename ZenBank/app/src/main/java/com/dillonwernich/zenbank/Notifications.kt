//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Notifications : AppCompatActivity() {

    // Declare UI elements
    private lateinit var notificationDisplay: TextView // TextView for displaying notifications

    // Firebase authentication and database references
    private lateinit var firebaseAuth: FirebaseAuth // Firebase authentication instance
    private lateinit var database: FirebaseDatabase // Firebase database instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_notifications) // Set the content view

        // Initialize the TextView for displaying notifications
        notificationDisplay = findViewById(R.id.notificationDisplay)

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Fetch and display notifications from the database
        fetchAndDisplayNotifications()
    }

    // Method to fetch and display notifications
    private fun fetchAndDisplayNotifications() {
        val userId = firebaseAuth.currentUser?.uid ?: return // Ensure user is logged in
        val notificationsRef = database.getReference("Users").child(userId).child("Notifications") // Reference to user's notifications

        // Add a listener to fetch notifications
        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = StringBuilder() // StringBuilder for constructing notifications

                // Check if notifications exist
                if (snapshot.hasChildren()) {
                    // Loop through all notifications and append to StringBuilder
                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(String::class.java) // Get notification as String
                        notification?.let {
                            notifications.append("$it\n\n") // Add spacing between notifications
                        }
                    }
                    notificationDisplay.text = notifications.toString() // Display all notifications
                } else {
                    // Display message if no notifications are found
                    notificationDisplay.text = "No notifications available."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors and display error message
                notificationDisplay.text = "Failed to load notifications: ${error.message}"
            }
        })
    }
}
