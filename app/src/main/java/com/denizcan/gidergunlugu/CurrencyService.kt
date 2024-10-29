package com.denizcan.gidergunlugu

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log


class CurrencyService {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.currencylayer.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()


    private val api = retrofit.create(CurrencyApi::class.java)

    fun getCurrencyRates(apiKey: String, callback: (Double?) -> Unit) {
        val call = api.getRates(apiKey)

        call.enqueue(object : Callback<CurrencyResponse> {
            override fun onResponse(call: Call<CurrencyResponse>, response: Response<CurrencyResponse>) {
                if (response.isSuccessful) {
                    val currencyResponse = response.body()
                    if (currencyResponse?.success == true) {
                        val rate = currencyResponse.quotes["USDTRY"]
                        Log.d("CurrencyService", "Döviz kuru başarıyla alındı: $rate")
                        callback(rate)
                    } else {
                        Log.e("CurrencyService", "API Yanıt Hatası: ${response.code()} - ${response.errorBody()?.string()}")
                        callback(null)
                    }
                } else {
                    Log.e("CurrencyService", "API Yanıt Hatası: ${response.code()} - ${response.errorBody()?.string()}")
                    callback(null)
                }
            }

            override fun onFailure(call: Call<CurrencyResponse>, t: Throwable) {
                Log.e("CurrencyService", "API Bağlantı Hatası: ${t.localizedMessage}")
                callback(null)
            }
        })
    }
}
