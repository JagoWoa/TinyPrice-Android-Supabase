package com.example.tinyprice_android_supabase.data.repository

import com.example.tinyprice_android_supabase.data.remote.ProductOfferDto
import com.example.tinyprice_android_supabase.data.remote.SearchProductsRequest
import com.example.tinyprice_android_supabase.data.remote.SearchProductsResponse
import com.example.tinyprice_android_supabase.data.remote.TinyPriceApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class RemoteDealsRepositoryTest {
    @Test fun mapsOffersAndLimitsResponseToTen() = runTest {
        val repository = repository {
            Response.success(SearchProductsResponse(offers = (1..12).map {
                ProductOfferDto(id = "$it", productName = "Switch", sourceUrl = "https://example.com/$it")
            }))
        }
        val offers = repository.search("Switch")
        assertEquals(10, offers.size)
        assertEquals("https://example.com/1", offers.first().sourceUrl)
        assertEquals("Ver en fuente", offers.first().price)
    }

    @Test fun usesControlledBackendError() = runTest {
        val repository = repository {
            Response.error(503, """{"error":"La búsqueda en vivo aún no está configurada."}"""
                .toResponseBody("application/json".toMediaType()))
        }
        val error = runCatching { repository.search("Switch") }.exceptionOrNull()
        assertTrue(error is RemoteSearchException)
        assertEquals("La búsqueda en vivo aún no está configurada.", error?.message)
    }

    @Test fun networkFailureBecomesReadableError() = runTest {
        val repository = repository { throw java.io.IOException("internal address") }
        val error = runCatching { repository.search("Switch") }.exceptionOrNull()
        assertTrue(error is RemoteSearchException)
        assertFalse(error?.message.orEmpty().contains("internal address"))
    }

    @Test fun cancellationIsNotConvertedIntoNetworkFailure() = runTest {
        val repository = repository { throw CancellationException("cancelled") }
        val error = runCatching { repository.search("Switch") }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }

    @Test fun missingBodyIsNotReportedAsSuccessfulEmptySearch() = runTest {
        val repository = repository { Response.success<SearchProductsResponse>(null) }
        assertTrue(runCatching { repository.search("Switch") }.exceptionOrNull() is RemoteSearchException)
    }

    private fun repository(block: suspend () -> Response<SearchProductsResponse>) = RemoteDealsRepository(
        api = object : TinyPriceApi {
            override suspend fun searchProducts(request: SearchProductsRequest) = block()
        },
    )
}
