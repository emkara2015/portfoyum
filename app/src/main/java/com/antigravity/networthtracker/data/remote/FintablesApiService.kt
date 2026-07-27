package com.antigravity.networthtracker.data.remote

import com.antigravity.networthtracker.data.remote.dto.FintablesFundDto
import retrofit2.http.GET
import retrofit2.http.Headers

interface FintablesApiService {

    @GET("funds/")
    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept: application/json, text/plain, */*"
    )
    suspend fun getAllFunds(): List<FintablesFundDto>

    companion object {
        const val BASE_URL = "https://api.fintables.com/"
    }
}
