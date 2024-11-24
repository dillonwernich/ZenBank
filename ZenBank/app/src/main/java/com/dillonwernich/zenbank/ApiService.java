//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    // GET request to fetch API information
    @GET("about") // Change this to your actual endpoint
    Call<ApiResponse> getApiInfo(); // Method to get the API response
}
