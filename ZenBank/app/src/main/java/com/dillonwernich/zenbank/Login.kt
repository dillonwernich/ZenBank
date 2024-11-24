// Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class Login : AppCompatActivity() {

    private lateinit var GoogleSSO: ImageButton // Button for Google Sign-In
    private lateinit var Biometric: Button // Button for biometric authentication
    private lateinit var OfflineBtn: Button // Button for offline access
    private lateinit var googleSignInClient: GoogleSignInClient // Google Sign-In client
    private lateinit var firebaseAuth: FirebaseAuth // Firebase authentication instance
    private lateinit var database: FirebaseDatabase // Firebase database instance

    // Biometric authentication elements
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // SharedPreferences for storing user account info
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_login)

        // Initialize UI elements
        GoogleSSO = findViewById(R.id.login_google_imageButton)
        Biometric = findViewById(R.id.login_biometric_button)
        OfflineBtn = findViewById(R.id.offline_button)

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("ZenBankPrefs", Context.MODE_PRIVATE)

        // Initialize Google Sign-In client
        googleSignInClient = setupGoogleSignInClient()

        // Set click listener for Google Sign-In button
        GoogleSSO.setOnClickListener {
            startGoogleSignIn()
        }

        // Setup biometric authentication
        setupBiometricLogin()

        // Set click listener for Biometric authentication button
        Biometric.setOnClickListener {
            authenticateWithBiometrics()
        }

        // Set click listener for offline button
        OfflineBtn.setOnClickListener {
            startActivity(Intent(this, Offline::class.java)) // Navigate to the Offline screen
        }
    }

    // Set up Google Sign-In client
    private fun setupGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Get from your Google service json
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(this, gso)
    }

    // Launch Google Sign-In with account selection prompt
    private fun startGoogleSignIn() {
        googleSignInClient.signOut() // Ensures the account selection prompt appears each time
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent) // Launch the Google Sign-In intent
    }

    // ActivityResultLauncher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task.result)
        } catch (e: ApiException) {
            Log.e("Login", "Google Sign-In failed: ${e.statusCode}", e)
            // Display a meaningful message to the user instead of crashing
            Toast.makeText(this, "Google Sign-In failed. Error Code: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Login", "Unexpected error: ${e.message}", e)
            Toast.makeText(this, "Unexpected error occurred during sign-in", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleGoogleSignInResult(account: GoogleSignInAccount?) {
        try {
            account?.let {
                Log.d("Login", "Google Sign-In account retrieved: ${it.email}")
                val credential = GoogleAuthProvider.getCredential(it.idToken, null)
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Login", "Firebase sign-in successful.")
                        val firebaseUser = firebaseAuth.currentUser
                        firebaseUser?.let { user ->
                            saveGoogleAccountInfo(it)
                            checkIfNewUser(user.uid)
                        }
                    } else {
                        Log.e("Login", "Firebase sign-in failed: ${task.exception?.message}")
                        handleSignInError(task.exception)
                    }
                }
            } ?: run {
                Log.e("Login", "Google Sign-In canceled or no account selected.")
                Toast.makeText(this, "Google Sign-In failed: No account selected.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("Login", "Google Sign-In ApiException: ${e.statusCode}")
            handleApiException(e)
        } catch (e: Exception) {
            Log.e("Login", "Google Sign-In unexpected error: ${e.localizedMessage}")
            Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleApiException(e: ApiException) {
        when (e.statusCode) {
            GoogleSignInStatusCodes.DEVELOPER_ERROR -> {
                Log.e("Login", "Developer error occurred: ${e.statusCode}")
                Toast.makeText(this, "Configuration error. Please check your setup.", Toast.LENGTH_SHORT).show()
            }
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                Log.e("Login", "Sign-in cancelled by user.")
                Toast.makeText(this, "Sign-in cancelled. Please try again.", Toast.LENGTH_SHORT).show()
            }
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> {
                Log.e("Login", "Sign-in failed.")
                Toast.makeText(this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
            GoogleSignInStatusCodes.NETWORK_ERROR -> {
                Log.e("Login", "Network error occurred.")
                Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.e("Login", "Unknown sign-in error: ${e.statusCode}")
                Toast.makeText(this, "An unknown error occurred. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignInError(exception: Exception?) {
        exception?.let {
            if (it is FirebaseAuthException) {
                Log.e("Login", "Firebase Auth Error: ${it.message}")
                Toast.makeText(this, "Authentication failed: ${it.message}", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("Login", "Unknown Sign-In Error: ${it.message}")
                Toast.makeText(this, "Sign-In failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Sign-In failed for unknown reasons.", Toast.LENGTH_SHORT).show()
        }
    }

    // Save the Google account info in SharedPreferences
    private fun saveGoogleAccountInfo(account: GoogleSignInAccount) {
        val editor = sharedPreferences.edit()
        editor.putString("google_email", account.email)
        editor.putString("google_displayName", account.displayName)
        editor.apply() // Save changes
    }

    // Check if user is new and register if necessary
    private fun checkIfNewUser(userId: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Authenticating...")
            setCancelable(false)
            show()
        }

        val userRef = database.getReference("Users").child(userId)
        userRef.get().addOnCompleteListener { task ->
            progressDialog.dismiss()
            if (task.isSuccessful) {
                val result = task.result
                if (result.exists()) {
                    val existingUser = result.getValue(User::class.java)
                    existingUser?.let {
                        saveUserToRoom(it)
                        startActivity(Intent(this, Dashboard::class.java))
                        finish()
                    }
                } else {
                    registerNewUser(userId)
                }
            } else {
                Log.e("Login", "Failed to fetch user: ${task.exception?.message}")
                Toast.makeText(this, "Failed to authenticate. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Register a new user in Firebase Realtime Database
    private fun registerNewUser(userId: String) {
        val firebaseUser = firebaseAuth.currentUser
        firebaseUser?.let { user ->
            val newUser = User(
                email = user.email ?: "",
                name = user.displayName ?: "",
                language = "en",
                debitAccountNumber = generateAccountNumber(),
                debitBalance = 0.0,
                savingsAccountNumber = generateAccountNumber(),
                savingsBalance = 0.0,
                profilePictureUrl = user.photoUrl?.toString()
            )

            Log.d("Login", "Registering new user: $newUser")

            // Save new user data in Firebase
            database.getReference("Users").child(userId).setValue(newUser).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToRoom(newUser)  // Save new user to RoomDB
                    Toast.makeText(this, "Welcome, ${user.displayName}!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Dashboard::class.java)) // Navigate to Dashboard
                    finish()
                } else {
                    Log.e("Login", "Registration failed: ${task.exception?.message}")
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Log.e("Login", "Firebase user is null.")
        }
    }

    // Generate random account number for the user
    private fun generateAccountNumber(): String {
        return "ACC${(10000000..99999999).random()}" // Generate random account number
    }

    // Setup Biometric Login prompt
    private fun setupBiometricLogin() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                // Authenticate with saved Google credentials
                authenticateWithSavedGoogleAccount()
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setNegativeButtonText("Cancel")
            .build()
    }

    // Start biometric authentication
    private fun authenticateWithBiometrics() {
        biometricPrompt.authenticate(promptInfo)
    }

    // Authenticate with saved Google account info after successful biometric login
    private fun authenticateWithSavedGoogleAccount() {
        val savedEmail = sharedPreferences.getString("google_email", null)

        if (savedEmail != null) {
            // Try to sign in the user silently with Google credentials
            val account = GoogleSignIn.getLastSignedInAccount(this)
            if (account != null && account.email == savedEmail) {
                // Re-authenticate silently with Firebase using the same Google account
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        fetchUserData() // Fetch user data upon successful sign-in
                    } else {
                        Toast.makeText(this, "Failed to sign in with Google", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Fallback to Google SSO if account not found or different account is detected
                startGoogleSignIn()
            }
        } else {
            // No saved Google account info, prompt for Google SSO
            startGoogleSignIn()
        }
    }

    // Fetch user data after successful login
    private fun fetchUserData() {
        val currentUser = firebaseAuth.currentUser ?: return
        val userRef = database.getReference("Users").child(currentUser.uid)

        userRef.get().addOnSuccessListener { snapshot ->
            val userData = snapshot.getValue(User::class.java)
            userData?.let {
                saveUserToRoom(it)  // Save fetched user data to RoomDB
                startActivity(Intent(this, Dashboard::class.java)) // Navigate to Dashboard
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to save user data to RoomDB
    private fun saveUserToRoom(user: User) {
        // Initialize RoomDB
        val appDatabase = AppDatabase.getDatabase(this)
        val userDao = appDatabase.userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            userDao.insertUser(user) // Insert user into Room database
            Log.d("Login", "User data saved to RoomDB: ${user.email}")
        }
    }
}
