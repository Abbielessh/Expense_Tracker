package com.example.expensetracker.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.plugins.HttpTimeout

object SupabaseClientProvider {
    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "expensetracker"
                host = "auth-callback"
                flowType = FlowType.PKCE
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
}
