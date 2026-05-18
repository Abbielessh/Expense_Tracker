package com.example.expensetracker.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Serializable
data class ExchangeRateResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

object CurrencyRepository {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val rateCache = mutableMapOf<String, Double>()
    private val mutex = Mutex()

    suspend fun getRate(from: String, to: String): Double {
        if (from == to) return 1.0

        val cacheKey = "${from}_${to}"
        mutex.withLock {
            rateCache[cacheKey]?.let { return it }
        }

        return try {
            val response = client.get("https://api.frankfurter.dev/v2/latest?base=$from&symbols=$to")
            val text = response.bodyAsText()
            val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<ExchangeRateResponse>(text)
            val rate = parsed.rates[to] ?: 1.0
            
            mutex.withLock {
                rateCache[cacheKey] = rate
                // also cache reverse
                rateCache["${to}_${from}"] = 1.0 / rate
            }
            rate
        } catch (e: Exception) {
            e.printStackTrace()
            1.0 // fallback
        }
    }

    suspend fun convertAmount(amount: Double, from: String, to: String): Double {
        if (from == to) return amount
        val rate = getRate(from, to)
        return amount * rate
    }

    fun getRateSync(from: String, to: String): Double {
        if (from == to) return 1.0
        return rateCache["${from}_${to}"] ?: 1.0
    }
}
