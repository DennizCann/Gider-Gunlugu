package com.denizcan.gidergunlugu

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {
    @GET("live")
    fun getRates(
        @Query("access_key") apiKey: String,
        @Query("currencies") currencies: String = "TRY",
        @Query("format") format: Int = 1
    ): Call<CurrencyResponse>
}
