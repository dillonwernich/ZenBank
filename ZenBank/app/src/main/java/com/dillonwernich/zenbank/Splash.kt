//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Splash : AppCompatActivity() {

    private val notificationPermissionRequestCode = 1001 // Request code for notification permission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_splash) // Set the content view for the splash screen

        // Check if notification permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {

            // Request the permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                notificationPermissionRequestCode // Specify the request code
            )
        } else {
            // Proceed with the app flow if permission is already granted
            proceedToLogin()
        }
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with notifications
                proceedToLogin()
            } else {
                // Permission denied, you can show a message or take action
                proceedToLogin() // Continue to login even if permission is denied
            }
        }
    }

    private fun proceedToLogin() {
        // Navigate to Login activity after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, Login::class.java) // Create an Intent for the Login activity
            startActivity(intent) // Start the Login activity
            finish() // Finish the Splash activity to prevent returning to it
        }, 5000) // Delay of 5000 milliseconds (5 second)
    }
}
