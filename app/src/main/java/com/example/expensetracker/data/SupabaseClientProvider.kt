package com.example.expensetracker.data

import android.content.Context
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "SupabaseClientProvider"
private val sessionJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Persists Supabase auth session in SharedPreferences so login survives app restarts. */
private class SharedPreferencesSessionManager(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)

    override suspend fun loadSession(): UserSession? {
        val raw = prefs.getString("session_json", null) ?: return null
        return try {
            sessionJson.decodeFromString<UserSession>(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode saved session, clearing: ${e.message}")
            prefs.edit().remove("session_json").apply()
            null
        }
    }

    override suspend fun saveSession(session: UserSession) {
        try {
            val raw = sessionJson.encodeToString(session)
            prefs.edit().putString("session_json", raw).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save session: ${e.message}")
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove("session_json").apply()
    }
}

object SupabaseClientProvider {
    private lateinit var _client: SupabaseClient
    val client: SupabaseClient get() = _client

    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    fun init(context: Context) {
        if (::_client.isInitialized) return
        _client = createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "expensetracker"
                host = "auth-callback"
                flowType = FlowType.PKCE
                sessionManager = SharedPreferencesSessionManager(context)
            }
            install(Postgrest)
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60000L
                    connectTimeoutMillis = 60000L
                    socketTimeoutMillis = 60000L
                }
            }
        }
    }

    /**
     * Returns true if a session JSON is present in SharedPreferences.
     * This is a fast, offline check that does NOT depend on the Supabase client's
     * in-memory state — useful to detect a stored session before the Auth plugin
     * has finished loading it asynchronously.
     */
    fun hasStoredSession(context: Context): Boolean {
        val prefs = context.getSharedPreferences("supabase_auth_session", Context.MODE_PRIVATE)
        return prefs.contains("session_json")
    }
}
