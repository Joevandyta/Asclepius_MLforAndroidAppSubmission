package com.dicoding.asclepius.api

import com.dicoding.asclepius.data.NewsResponse
import com.yalantis.ucrop.BuildConfig
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ApiService {
    @GET("v2/top-headlines")
    fun getSearchNews(
        @Query("q") query: String,
        @Query("category") category: String,
        @Query("language") language: String,
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>
}