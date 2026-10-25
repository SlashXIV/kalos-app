package com.kalos.app.core.data.remote

import android.util.Log
import com.kalos.app.core.domain.model.Food
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.kalos.app.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a barcode lookup, so the caller can give the user honest feedback. */
sealed interface OffLookupResult {
    data class Found(val food: Food) : OffLookupResult
    /** OpenFoodFacts responded but has no (usable) product for this barcode. */
    data object NotFound : OffLookupResult
    /** No network / server error — the lookup couldn't be performed. */
    data object Unavailable : OffLookupResult
}

/**
 * Resolves a barcode to a [Food] via the OpenFoodFacts REST API.
 *
 * Deliberately isolated: this is the only place the app touches the network. It is called
 * opportunistically after a local cache miss, never on the critical path of a manual entry.
 *
 * Uses the v0 endpoint (whose `status` is a stable integer: 1 found / 0 not found) with
 * HttpURLConnection + kotlinx.serialization, to avoid pulling in a networking library.
 */
@Singleton
class OpenFoodFactsDataSource @Inject constructor() {

    suspend fun lookup(barcode: String): OffLookupResult = withContext(Dispatchers.IO) {
        val clean = barcode.trim()
        // EAN-8/13, UPC-A/E: digits only; anything else never reaches the URL.
        if (clean.length !in 6..14 || !clean.all(Char::isDigit)) return@withContext OffLookupResult.NotFound
        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "https://world.openfoodfacts.org/api/v0/product/$clean.json" +
                    "?fields=product_name,brands,nutriments,serving_quantity"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                // OpenFoodFacts asks callers to identify themselves.
                setRequestProperty("User-Agent", "Kalos/${BuildConfig.VERSION_NAME} (Android; offline-first fitness app)")
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                // v0 answers 200 even for unknown products (status 0); a non-200 is a real
                // availability problem, except 404 which we treat as "not found".
                return@withContext if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    OffLookupResult.NotFound
                } else {
                    OffLookupResult.Unavailable
                }
            }
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            decodeOffFood(body, clean)?.let { OffLookupResult.Found(it) } ?: OffLookupResult.NotFound
        } catch (e: Exception) {
            Log.w(TAG, "OpenFoodFacts lookup failed for $clean", e)
            OffLookupResult.Unavailable
        } finally {
            conn?.disconnect()
        }
    }

    companion object {
        private const val TAG = "OpenFoodFacts"
    }
}

private val offJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/** Maps an OpenFoodFacts v0 response to a pre-filled custom food, or null when not found or empty. */
internal fun decodeOffFood(body: String, barcode: String): Food? {
    val resp = offJson.decodeFromString<OffResponse>(body)
    if (resp.status != 1) return null
    return resp.product?.toFoodOrNull(barcode)
}

private fun OffProduct.toFoodOrNull(barcode: String): Food? {
    val name = productName?.trim().orEmpty()
    val n = nutriments
    // Some products only carry energy in kJ: 1 kcal = 4.184 kJ.
    val kcal = n?.kcal ?: n?.kj?.div(4.184)
    val hasAnyNutriment = n != null &&
        listOf(kcal, n.proteins, n.carbs, n.fat, n.fiber).any { it != null }
    // Reject only genuinely empty entries (no name AND no nutritional data). A named
    // product with 0 kcal (water, diet drinks, coffee…) is perfectly valid and pre-fills.
    if (name.isBlank() && !hasAnyNutriment) return null
    // serving_quantity is a number or a numeric string depending on the product.
    val servingG = servingQuantity?.contentOrNull?.toDoubleOrNull()?.toFloat()?.takeIf { it > 0f }
    return Food(
        name = name.ifBlank { "Produit $barcode" },
        brand = brands?.substringBefore(",")?.trim().orEmpty(),
        kcalPer100g = kcal?.toFloat()?.coerceAtLeast(0f) ?: 0f,
        proteinPer100g = n?.proteins?.toFloat()?.coerceAtLeast(0f) ?: 0f,
        carbsPer100g = n?.carbs?.toFloat()?.coerceAtLeast(0f) ?: 0f,
        fatPer100g = n?.fat?.toFloat()?.coerceAtLeast(0f) ?: 0f,
        fiberPer100g = n?.fiber?.toFloat()?.coerceAtLeast(0f) ?: 0f,
        defaultServingG = servingG ?: 100f,
        isCustom = true,
        barcode = barcode,
    )
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
    @SerialName("serving_quantity") val servingQuantity: JsonPrimitive? = null,
)

@Serializable
private data class OffNutriments(
    @SerialName("energy-kcal_100g") val kcal: Double? = null,
    @SerialName("energy-kj_100g") val kj: Double? = null,
    @SerialName("proteins_100g") val proteins: Double? = null,
    @SerialName("carbohydrates_100g") val carbs: Double? = null,
    @SerialName("fat_100g") val fat: Double? = null,
    @SerialName("fiber_100g") val fiber: Double? = null,
)
