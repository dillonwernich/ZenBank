//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Offline : AppCompatActivity() {

    private lateinit var debitAccountTxt: TextView // TextView for debit account number
    private lateinit var savingsAccountTxt: TextView // TextView for savings account number
    private lateinit var debitBalanceTxt: TextView // TextView for debit balance
    private lateinit var savingsBalanceTxt: TextView // TextView for savings balance

    // Firebase instances
    private lateinit var firebaseAuth: FirebaseAuth // Firebase authentication instance
    private lateinit var database: FirebaseDatabase // Firebase database instance

    // Room Database instance
    private lateinit var appDatabase: AppDatabase // Room database instance
    private lateinit var userDao: UserDao // Data Access Object for user

    // SharedPreferences for storing last logged-in user email
    private lateinit var sharedPreferences: SharedPreferences // SharedPreferences instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_offline) // Set the content view

        // Initialize UI elements
        debitAccountTxt = findViewById(R.id.offline_debit_account_txt)
        savingsAccountTxt = findViewById(R.id.offline_savings_account_txt)
        debitBalanceTxt = findViewById(R.id.offline_debit_balance_txt)
        savingsBalanceTxt = findViewById(R.id.offline_savings_balance_txt)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize Room database and DAO
        appDatabase = AppDatabase.getDatabase(this)
        userDao = appDatabase.userDao()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("ZenBankPrefs", Context.MODE_PRIVATE)

        // Display the last logged-in user's account information from Room automatically
        displayUserDataFromRoom()
    }

    // Display user data from the local Room database
    private fun displayUserDataFromRoom() {
        val userEmail = getLastLoggedInEmail() // Get the last logged-in user's email

        if (userEmail == null) {
            showToast("User email not found.") // Show error message if email is not found
            Log.e("Offline", "User email is null. Cannot fetch data from RoomDB.")
            return
        }

        // Launch a coroutine to fetch user data from Room
        lifecycleScope.launch(Dispatchers.IO) {
            val user = userDao.getUserByEmail(userEmail) // Fetch user by email

            if (user != null) {
                withContext(Dispatchers.Main) {
                    // Update UI with the cached user data
                    debitAccountTxt.text = "Debit Account - "+user.debitAccountNumber
                    savingsAccountTxt.text = "Savings Account - " + user.savingsAccountNumber
                    debitBalanceTxt.text = "R${"%.2f".format(user.debitBalance)}"
                    savingsBalanceTxt.text = "R${"%.2f".format(user.savingsBalance)}"
                }
            } else {
                withContext(Dispatchers.Main) {
                    // Handle case where no user data is found
                    debitAccountTxt.text = "Debit Account: Unavailable"
                    debitBalanceTxt.text = "Unavailable"
                    savingsAccountTxt.text = "Savings Account: Unavailable"
                    savingsBalanceTxt.text = "Unavailable"
                }
            }
        }
    }

    // Get the last logged-in user's email from SharedPreferences
    private fun getLastLoggedInEmail(): String? {
        val email = sharedPreferences.getString("google_email", null) // Retrieve email from SharedPreferences
        if (email == null) {
            Log.e("Offline", "No email found in SharedPreferences.")
        }
        return email // Return the email
    }

    // Helper method to show toast messages
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@Offline, message, Toast.LENGTH_SHORT).show() // Display a toast message
        }
    }

    // Check if the device is online
    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo // Get network info
        return networkInfo != null && networkInfo.isConnected // Return true if connected
    }
}
