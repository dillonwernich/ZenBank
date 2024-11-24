//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class About : AppCompatActivity() {

    private lateinit var apiResultTextView: TextView // TextView to display API results
    private lateinit var fetchDataButton: Button // Button to initiate API call

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_about) // Set the content view to the corresponding layout

        // Initialize views
        apiResultTextView = findViewById(R.id.about_us_display)
        fetchDataButton = findViewById(R.id.fetch_data_button)

        // Set up logging interceptor for OkHttp (remove this if logging is not needed)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Set logging level
        }

        // Create OkHttpClient with custom timeouts and logging
        val client = OkHttpClient.Builder()
            .addInterceptor(logging) // Add logging interceptor
            .connectTimeout(30, TimeUnit.SECONDS) // Set connection timeout
            .readTimeout(30, TimeUnit.SECONDS) // Set read timeout
            .writeTimeout(30, TimeUnit.SECONDS) // Set write timeout
            .build()

        // Gson setup for lenient JSON parsing
        val gson = GsonBuilder().setLenient().create()

        // Retrofit instance setup
        val retrofit = Retrofit.Builder()
            .baseUrl("https://rattle-reflective-monitor.glitch.me/api/") // Set your base URL
            .client(client) // Set the OkHttp client
            .addConverterFactory(GsonConverterFactory.create(gson)) // Set the Gson converter
            .build()

        // Create API service instance
        val apiService: ApiService = retrofit.create(ApiService::class.java)

        // Set click listener to fetch data when the button is clicked
        fetchDataButton.setOnClickListener {
            fetchApiData(apiService) // Call method to fetch API data
        }
    }

    // Method to fetch data from the API
    private fun fetchApiData(apiService: ApiService) {
        // Initialize and show ProgressDialog
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Fetching data...")
            setCancelable(false)
            show()
        }

        val call: Call<ApiResponse> = apiService.getApiInfo() // Prepare the API call
        call.enqueue(object : Callback<ApiResponse?> {
            override fun onResponse(call: Call<ApiResponse?>, response: Response<ApiResponse?>) {
                // Dismiss the ProgressDialog
                progressDialog.dismiss()

                // Check if the response is successful
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    // Update TextView with API response details
                    apiResultTextView.text = """
                        Title: ${apiResponse.title}
                        Description: ${apiResponse.description}
                        Version: ${apiResponse.version}
                        Developer: ${apiResponse.developer.name}
                        Contact Email: ${apiResponse.support.contactEmail}
                        Phone: ${apiResponse.support.phone}
                        Last Updated: ${apiResponse.lastUpdated}
                        Features:
                        ${apiResponse.features.joinToString("\n") { "- $it" }}
                    """.trimIndent()
                } else {
                    // Handle error response
                    val errorCode = response.code()
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    apiResultTextView.text = "Failed to get API response: $errorMessage (Code: $errorCode)"
                }
            }

            override fun onFailure(call: Call<ApiResponse?>, t: Throwable) {
                // Dismiss the ProgressDialog on failure
                progressDialog.dismiss()

                // Handle call failure
                apiResultTextView.text = "Error: ${t.message}"
            }
        })
    }
}
