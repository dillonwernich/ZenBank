//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {
    // Endpoint to get the latest exchange rates with respect to USD
    @GET("053679e771a0d2cb79d5d4ac/latest/USD")
    fun getExchangeRates(): Call<ResponseDataClass>
}
