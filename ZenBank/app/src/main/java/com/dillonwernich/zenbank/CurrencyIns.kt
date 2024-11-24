//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Singleton object for managing Retrofit instance and API interface
object CurrencyIns {
    // Lazy initialization of Retrofit instance
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/v6/") // Base URL for the API
            .addConverterFactory(GsonConverterFactory.create()) // Converter to parse JSON responses
            .build() // Build the Retrofit instance
    }

    // Lazy initialization of the API interface
    val apiInterface by lazy {
        retrofit.create(ApiInterface::class.java) // Create an instance of ApiInterface
    }
}
