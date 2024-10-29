package com.denizcan.gidergunlugu

data class CurrencyResponse(
    val success: Boolean,
    val quotes: Map<String, Double> // "quotes" içinde USD bazlı döviz kurları bulunur
)
