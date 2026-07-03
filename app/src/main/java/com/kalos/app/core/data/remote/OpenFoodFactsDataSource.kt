package com.kalos.app.core.data.remote

import android.util.Log
import com.kalos.app.core.domain.model.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a barcode to a [Food] via the OpenFoodFacts REST API.
 *
 * Deliberately isolated: this is the only place the app touches the network. It is called
 * opportunistically after a local cache miss, never on the critical path of a manual entry,
 * and every failure mode (no network, HTTP error, product not found, unusable macros)
 * degrades gracefully to `null` so the caller falls back to manual creation.
 *
 * Uses HttpURLConnection + kotlinx.serialization to avoid pulling in a networking library.
 */
@Singleton
class OpenFoodFactsDataSource @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend fun lookup(barcode: String): Food? = withContext(Dispatchers.IO) {
        val clean = barcode.trim()
        if (clean.isEmpty()) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "https://world.openfoodfacts.org/api/v2/product/$clean.json" +
                    "?fields=product_name,brands,nutriments"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                // OpenFoodFacts asks callers to identify themselves.
                setRequestProperty("User-Agent", "Kalos/3.14 (Android; offline-first fitness app)")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val resp = json.decodeFromString<OffResponse>(body)
            if (resp.status != 1 || resp.product == null) return@withContext null
            resp.product.toFoodOrNull(clean)
        } catch (e: Exception) {
            Log.w(TAG, "OpenFoodFacts lookup failed for $clean", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun OffProduct.toFoodOrNull(barcode: String): Food? {
        val n = nutriments ?: return null
        val kcal = n.kcal?.toFloat() ?: 0f
        // Validation: reject products without usable energy data rather than inserting zeros.
        if (kcal <= 0f) return null
        return Food(
            name = productName?.trim().orEmpty().ifBlank { "Produit $barcode" },
            brand = brands?.substringBefore(",")?.trim().orEmpty(),
            kcalPer100g = kcal,
            proteinPer100g = n.proteins?.toFloat()?.coerceAtLeast(0f) ?: 0f,
            carbsPer100g = n.carbs?.toFloat()?.coerceAtLeast(0f) ?: 0f,
            fatPer100g = n.fat?.toFloat()?.coerceAtLeast(0f) ?: 0f,
            fiberPer100g = n.fiber?.toFloat()?.coerceAtLeast(0f) ?: 0f,
            isCustom = true,
            barcode = barcode,
        )
    }

    companion object {
        private const val TAG = "OpenFoodFacts"
    }
}

@Serializable
private data class OffResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
private data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val nutriments: OffNutriments? = null,
)

@Serializable
private data class OffNutriments(
    @SerialName("energy-kcal_100g") val kcal: Double? = null,
    @SerialName("proteins_100g") val proteins: Double? = null,
    @SerialName("carbohydrates_100g") val carbs: Double? = null,
    @SerialName("fat_100g") val fat: Double? = null,
    @SerialName("fiber_100g") val fiber: Double? = null,
)
