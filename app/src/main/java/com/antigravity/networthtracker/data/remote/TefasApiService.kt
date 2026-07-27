package com.antigravity.networthtracker.data.remote

import com.antigravity.networthtracker.data.remote.dto.TefasResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface TefasApiService {

    @FormUrlEncoded
    @POST("api/DB/BindHistoryInfo")
    @Headers(
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "X-Requested-With: XMLHttpRequest",
        "Accept: application/json, text/javascript, */*; q=0.01"
    )
    suspend fun getFundHistory(
        @Field("fontip") fontip: String = "YAT",
        @Field("bastarih") bastarih: String,
        @Field("bittarih") bittarih: String,
        @Field("fonkod") fonkod: String = ""
    ): TefasResponseDto

    companion object {
        const val BASE_URL = "https://www.tefas.gov.tr/"
    }
}
