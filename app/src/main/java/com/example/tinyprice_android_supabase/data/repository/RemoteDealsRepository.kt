package com.example.tinyprice_android_supabase.data.repository

import com.example.tinyprice_android_supabase.BuildConfig
import com.example.tinyprice_android_supabase.data.model.ProductOffer
import com.example.tinyprice_android_supabase.data.remote.ApiErrorResponse
import com.example.tinyprice_android_supabase.data.remote.SearchProductsRequest
import com.example.tinyprice_android_supabase.data.remote.TinyPriceApi
import com.example.tinyprice_android_supabase.data.remote.TinyPriceApiFactory
import com.example.tinyprice_android_supabase.data.remote.toProductOffer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteDealsRepository(
    private val api: TinyPriceApi = TinyPriceApiFactory.create(),
    private val gson: Gson = Gson(),
) : DealsRepository {
    override val isPreviewData = false

    override suspend fun search(query: String): List<ProductOffer> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchProducts(SearchProductsRequest(query))
            if (!response.isSuccessful) throw RemoteSearchException(response.errorMessage())
            val offers = response.body()?.offers
                ?: throw RemoteSearchException("El servidor devolvió una respuesta no válida. Inténtalo de nuevo.")
            offers.take(10).map { it.toProductOffer() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: RemoteSearchException) {
            throw error
        } catch (_: Exception) {
            throw RemoteSearchException(
                "No se pudo conectar con la búsqueda en vivo. Revisa tu conexión e inténtalo de nuevo.",
            )
        }
    }

    private fun Response<*>.errorMessage(): String {
        if (code() == 404) return "Falta desplegar la función search-products en tu proyecto de Supabase."
        if (code() == 401 || code() == 403) return "Supabase rechazó la solicitud. Revisa la configuración de la función y la clave pública."
        return runCatching {
            errorBody()?.charStream()?.use { reader ->
                gson.fromJson(reader, ApiErrorResponse::class.java)?.error
            }
        }.getOrNull().orEmpty().ifBlank {
            "No fue posible hacer la búsqueda en vivo. Inténtalo de nuevo."
        }
    }
}

class RemoteSearchException(message: String) : Exception(message)

object DealsRepositoryProvider {
    fun create(): DealsRepository = if (BuildConfig.USE_REMOTE_DEALS) {
        RemoteDealsRepository()
    } else {
        PreviewDealsRepository
    }
}
