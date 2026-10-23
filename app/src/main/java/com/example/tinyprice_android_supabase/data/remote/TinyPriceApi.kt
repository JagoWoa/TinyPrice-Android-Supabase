package com.example.tinyprice_android_supabase.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TinyPriceApi {
    @POST("search-products")
    suspend fun searchProducts(
        @Body request: SearchProductsRequest,
    ): Response<SearchProductsResponse>
}
