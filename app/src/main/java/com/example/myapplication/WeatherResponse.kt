package com.example.myapplication

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Response structures to capture Location Name, Temperature, Condition, Humidity, and Wind Speed
data class WeatherResponse(
    val name: String,
    val main: MainData,
    val weather: List<WeatherCondition>,
    val wind: WindData
)

data class MainData(val temp: Double, val humidity: Int)
data class WeatherCondition(val main: String, val description: String)
data class WindData(val speed: Double)

interface WeatherApiService {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Response<WeatherResponse>
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.openweathermap.org/"

    val api: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}