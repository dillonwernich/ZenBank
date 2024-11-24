//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Dashboard : AppCompatActivity() {

    // Declare UI elements
    private lateinit var mainScreenNav: Spinner
    private lateinit var debitAccountTxt: TextView
    private lateinit var savingsAccountTxt: TextView
    private lateinit var debitBalanceTxt: TextView
    private lateinit var savingsBalanceTxt: TextView
    private lateinit var pay: Button
    private lateinit var transfer: Button
    private lateinit var deposit: Button
    private lateinit var logout: Button

    // Firebase authentication and database references
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_dashboard)

        // Initialize UI elements
        mainScreenNav = findViewById(R.id.main_spinner)
        debitAccountTxt = findViewById(R.id.main_debit_account_txt)
        savingsAccountTxt = findViewById(R.id.main_savings_account_txt)
        debitBalanceTxt = findViewById(R.id.main_debit_balance_txt)
        savingsBalanceTxt = findViewById(R.id.main_savings_balance_txt)
        pay = findViewById(R.id.main_pay_button)
        transfer = findViewById(R.id.main_transfer_button)
        deposit = findViewById(R.id.main_deposit_button)
        logout = findViewById(R.id.main_logout_button)

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Set up the main screen navigation spinner
        setupMainScreenNav()

        // Set up buttons for navigating to different activities
        setupButtons()

        // Fetch and display user account details from Firebase
        fetchAndDisplayUserDetails()
    }

    // Set default Spinner selection on resume
    override fun onResume() {
        super.onResume()
        mainScreenNav.setSelection(0) // Set the Spinner back to "Dashboard" (index 0)
    }

    // Method to set up Spinner with navigation options
    private fun setupMainScreenNav() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.main_screen_nav, // Array of navigation options
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mainScreenNav.adapter = adapter

        // Set listener for Spinner item selection
        mainScreenNav.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Navigate based on the selected option
                when (position) {
                    0 -> {} // Do nothing for "Dashboard"
                    1 -> startActivity(Intent(this@Dashboard, Settings::class.java))
                    2 -> startActivity(Intent(this@Dashboard, Notifications::class.java))
                    3 -> startActivity(Intent(this@Dashboard, CurrencyConversions::class.java))
                    4 -> startActivity(Intent(this@Dashboard, About::class.java))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    // Method to set up button click listeners
    private fun setupButtons() {
        pay.setOnClickListener {
            startActivity(Intent(this, Pay::class.java)) // Navigate to the Pay screen
        }

        transfer.setOnClickListener {
            startActivity(Intent(this, Transfer::class.java)) // Navigate to the Transfer screen
        }

        deposit.setOnClickListener {
            startActivity(Intent(this, Deposit::class.java)) // Navigate to the Deposit screen
        }

        logout.setOnClickListener {
            firebaseAuth.signOut() // Sign out the user
            startActivity(Intent(this, Login::class.java)) // Return to the Login screen
            finish() // Optional: finish MainActivity to prevent returning via back button
        }
    }

    // Fetch and display the user's account information from Firebase
    private fun fetchAndDisplayUserDetails() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val userRef = database.child("Users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        // Log the retrieved user data for debugging purposes
                        Log.d("Dashboard", "User data: $user")

                        // Update UI with user account information
                        debitAccountTxt.text = "Debit Account - ${it.debitAccountNumber}"
                        debitBalanceTxt.text = "Balance: R${"%.2f".format(it.debitBalance)}"
                        savingsAccountTxt.text = "Savings Account - ${it.savingsAccountNumber}"
                        savingsBalanceTxt.text = "Balance: R${"%.2f".format(it.savingsBalance)}"
                    } ?: run {
                        Log.d("Dashboard", "User data is null")
                    }
                } else {
                    Log.d("Dashboard", "Snapshot does not exist")
                    debitAccountTxt.text = "Debit Account: Unavailable"
                    debitBalanceTxt.text = "Balance: Unavailable"
                    savingsAccountTxt.text = "Savings Account: Unavailable"
                    savingsBalanceTxt.text = "Balance: Unavailable"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Dashboard", "Error fetching data: ${error.message}")
                Toast.makeText(this@Dashboard, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
