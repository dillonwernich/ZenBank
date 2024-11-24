//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class Settings : AppCompatActivity() {

    // Declare UI elements
    private lateinit var profilePicture: ImageView // ImageView for profile picture
    private lateinit var editProfilePicture: Button // Button to edit profile picture
    private lateinit var nameAndSurname: EditText // EditText for user's name and surname
    private lateinit var updateButton: Button // Button to update user details
    private lateinit var language: Spinner // Spinner for language selection
    private lateinit var deleteAccount: TextView // TextView to delete account

    // Firebase services
    private lateinit var firebaseAuth: FirebaseAuth // Firebase authentication instance
    private lateinit var database: FirebaseDatabase // Firebase database instance
    private lateinit var storage: FirebaseStorage // Firebase storage instance

    // Profile picture URI
    private var selectedImageUri: Uri? = null // Holds the selected image URI

    // Array of languages for the Spinner
    private val languages = arrayOf("English", "Afrikaans") // Available languages
    private var currentLanguage: String = "en" // Default language is English

    // SharedPreferences for storing language preferences locally
    private lateinit var sharedPreferences: SharedPreferences // SharedPreferences instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_settings) // Set the content view

        // Initialize UI elements
        profilePicture = findViewById(R.id.settings_profile_imageView)
        editProfilePicture = findViewById(R.id.settings_profile_button)
        nameAndSurname = findViewById(R.id.settings_name_edtxt)
        updateButton = findViewById(R.id.settings_update_button)
        language = findViewById(R.id.settings_language_spinner)
        deleteAccount = findViewById(R.id.delete_account)

        // Initialize Firebase services
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("ZenBankPrefs", MODE_PRIVATE)

        // Load user data from Firebase
        loadUserData()

        // Handle profile picture selection
        val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                profilePicture.setImageURI(it) // Set selected image to ImageView
            }
        }

        editProfilePicture.setOnClickListener {
            getImage.launch("image/*") // Launch image picker
        }

        // Populate the Spinner with languages
        setupLanguageSpinner()

        // Update user details on button click
        updateButton.setOnClickListener {
            if (validateInput()) {
                updateUserDetails() // Update user details if input is valid
            }
        }

        // Delete account on click
        deleteAccount.setOnClickListener {
            showDeleteConfirmationDialog() // Show confirmation dialog before deleting account
        }
    }

    // Method to load user data and display it in the UI
    private fun loadUserData() {
        val user = firebaseAuth.currentUser ?: return // Ensure user is logged in

        database.getReference("Users").child(user.uid).get().addOnSuccessListener { snapshot ->
            val userData = snapshot.getValue(User::class.java) // Retrieve user data
            userData?.let {
                nameAndSurname.setText(it.name) // Set name in EditText

                // Load profile picture using Glide
                it.profilePictureUrl?.let { url ->
                    Glide.with(this).load(url).into(profilePicture)
                } ?: profilePicture.setImageResource(R.drawable.defaultprofileicon) // Default image if no picture exists

                // Set the selected language in the Spinner
                currentLanguage = it.language ?: "en"
                setSpinnerSelectionForLanguage(currentLanguage)
            }
        }
    }

    // Set up the Spinner with languages
    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages) // Create adapter
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        language.adapter = adapter // Set the adapter for the Spinner

        // Set listener for language selection
        language.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Update current language based on selection
                currentLanguage = when (position) {
                    0 -> "en" // English
                    1 -> "af" // Afrikaans
                    else -> "en"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    // Helper method to set the Spinner selection based on the current language
    private fun setSpinnerSelectionForLanguage(lang: String) {
        when (lang) {
            "en" -> language.setSelection(0) // English
            "af" -> language.setSelection(1) // Afrikaans
        }
    }

    // Validate user input before updating
    private fun validateInput(): Boolean {
        val newName = nameAndSurname.text.toString().trim() // Get name input

        // Validate name
        if (newName.isEmpty()) {
            nameAndSurname.error = "Name cannot be empty!" // Show error if empty
            nameAndSurname.requestFocus() // Request focus on the EditText
            return false
        }

        return true // Input is valid
    }

    // Update user details in Firebase
    private fun updateUserDetails() {
        val user = firebaseAuth.currentUser ?: return // Get current user
        val newName = nameAndSurname.text.toString().trim() // Get updated name

        var success = true // Flag for successful updates
        val tasks = mutableListOf<Task<*>>() // List to hold tasks for updates

        // Show a loading indicator
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Updating user details...")
            setCancelable(false)
            show()
        }

        // Update user's details in the database
        val userRef = database.getReference("Users").child(user.uid)
        tasks.add(userRef.child("name").setValue(newName)) // Update name
        tasks.add(userRef.child("language").setValue(currentLanguage)) // Save selected language to Firebase

        // Save language to SharedPreferences for future app use
        saveLanguageToPreferences(currentLanguage)

        // Upload and update profile picture if changed
        selectedImageUri?.let { uri ->
            val storageRef = storage.reference.child("profile_pictures/${user.uid}.jpg")
            tasks.add(storageRef.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    success = false
                    Toast.makeText(this, "Profile picture upload failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    throw task.exception ?: Exception("Profile picture upload failed")
                }
                storageRef.downloadUrl // Get the download URL
            }.continueWithTask { task ->
                val url = task.result
                userRef.child("profilePictureUrl").setValue(url.toString()) // Update profile picture URL
            }.addOnFailureListener {
                success = false
                Toast.makeText(this, "Profile picture update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            })
        }

        // Complete all tasks and handle the result
        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            if (success) {
                Toast.makeText(this, "Update Successful!", Toast.LENGTH_SHORT).show()
                setLocale(currentLanguage) // Update app language globally
            } else {
                Toast.makeText(this, "Update Failed!", Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss() // Dismiss the loading dialog
        }
    }

    // Method to change app language
    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        // Save language to SharedPreferences
        saveLanguageToPreferences(lang)

        // Restart the entire app to apply the language change
        val intent = Intent(this, Dashboard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent) // Restart the app with the new language
        finishAffinity() // Close all activities
    }

    // Save the selected language to SharedPreferences
    private fun saveLanguageToPreferences(lang: String) {
        val sharedPreferences = getSharedPreferences("ZenBankPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("app_language", lang) // Save the selected language
        editor.apply() // Apply changes
    }

    // Show a confirmation dialog before deleting the account
    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Account")
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.")
        builder.setPositiveButton("Yes") { dialog, _ ->
            deleteAccountAndData() // Call function to delete account
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Cancel the action
        }
        builder.show()
    }

    // Delete user account from Firebase Auth, Firebase Realtime Database, and RoomDB
    private fun deleteAccountAndData() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Deleting account...")
            setCancelable(false)
            show()
        }

        val user = firebaseAuth.currentUser ?: return

        // Reference to the user's data in the Firebase Realtime Database
        val userRef = database.getReference("Users").child(user.uid)

        // Remove the user's data from Firebase Realtime Database
        userRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Successfully deleted data from Firebase Realtime Database
                // Now delete the user from Firebase Authentication
                user.delete().addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // Successfully deleted from Firebase Authentication
                        // Now delete user data from RoomDB
                        lifecycleScope.launch(Dispatchers.IO) {
                            val appDatabase = AppDatabase.getDatabase(this@Settings)
                            val userEmail = user.email ?: return@launch // Ensure email is not null
                            appDatabase.userDao().deleteUserByEmail(userEmail) // Delete the user from RoomDB
                            runOnUiThread {
                                progressDialog.dismiss()
                                Toast.makeText(this@Settings, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                // Redirect to login screen
                                val intent = Intent(this@Settings, Login::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finishAffinity() // Close all activities
                            }
                        }
                    } else {
                        // Failed to delete the user from Firebase Authentication
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to delete account from Firebase Auth: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Failed to delete user data from Firebase Realtime Database
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to delete user data from Firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
