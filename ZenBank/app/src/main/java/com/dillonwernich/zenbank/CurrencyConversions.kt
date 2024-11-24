//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.dillonwernich.zenbank.databinding.ActivityCurrencyConversionsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Activity for performing currency conversions
class CurrencyConversions : AppCompatActivity() {

    // View binding for accessing UI elements
    private lateinit var binding: ActivityCurrencyConversionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Inflate layout using ViewBinding
        binding = ActivityCurrencyConversionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set click listener for the convert button
        binding.btnConvert.setOnClickListener {
            val usdAmountInput = binding.etUSDAmount.text.toString()

            // Check if user input is not empty
            if (usdAmountInput.isNotEmpty()) {
                val usdAmount = usdAmountInput.toDouble() // Parse input to double
                getData(usdAmount) // Call API to convert the entered amount
            } else {
                // Display message if the input field is empty
                Toast.makeText(this, "Please enter a valid USD amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetches conversion rates from the API and performs the currency conversion
    private fun getData(usdAmount: Double) {
        // Create and show a progress dialog while fetching data
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Converting...") // Set a message for the loading dialog
            show() // Display the loading dialog
        }

        // Make an API call to get the currency conversion rates
        CurrencyIns.apiInterface.getExchangeRates().enqueue(object : Callback<ResponseDataClass?> {
            override fun onResponse(
                call: Call<ResponseDataClass?>,
                response: Response<ResponseDataClass?>
            ) {
                // Get the response body from the API
                val body = response.body()

                // Check if the response body contains conversion rates
                if (body != null && body.conversion_rates.isNotEmpty()) {
                    // Perform the currency conversion using the rates
                    val zarAmount = usdAmount * (body.conversion_rates["ZAR"] ?: 1.0)
                    val eurAmount = usdAmount * (body.conversion_rates["EUR"] ?: 1.0)
                    val gbpAmount = usdAmount * (body.conversion_rates["GBP"] ?: 1.0)
                    val cadAmount = usdAmount * (body.conversion_rates["CAD"] ?: 1.0)
                    val audAmount = usdAmount * (body.conversion_rates["AUD"] ?: 1.0)

                    // Update the UI with the converted values, formatted to 2 decimal places
                    binding.tvZARCurrency.text = "ZAR = R%.2f".format(zarAmount)
                    binding.tvEURCurrency.text = "EUR = €%.2f".format(eurAmount)
                    binding.tvGBPCurrency.text = "GBP = £%.2f".format(gbpAmount)
                    binding.tvCADCurrency.text = "CAD = $%.2f".format(cadAmount)
                    binding.tvAUDCurrency.text = "AUD = $%.2f".format(audAmount)

                } else {
                    // Display message if no conversion data is received
                    Toast.makeText(this@CurrencyConversions, "No data received", Toast.LENGTH_SHORT).show()
                }

                // Dismiss the progress dialog
                progressDialog.dismiss()
            }

            // Handle failure in fetching data from the API
            override fun onFailure(call: Call<ResponseDataClass?>, t: Throwable) {
                // Dismiss the progress dialog
                progressDialog.dismiss()

                // Display error message in case of API failure
                Toast.makeText(
                    this@CurrencyConversions,
                    "Failed to fetch from API",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
