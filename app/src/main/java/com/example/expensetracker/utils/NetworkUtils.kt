package com.example.expensetracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtils {
    fun isOnline(context: Context): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false

            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: SecurityException) {
            Log.e("ExpenseSupabase", "Missing network state permission", e)
            false
        } catch (e: Exception) {
            Log.e("ExpenseSupabase", "Network check failed", e)
            false
        }
    }

    fun classifyError(e: Exception): String {
        val raw = e.message?.lowercase() ?: ""
        return when {
            raw.contains("timeout") || e.javaClass.simpleName.contains("Timeout") ->
                "Request timed out. Please check your internet and try again."
            raw.contains("401") || raw.contains("unauthorized") || raw.contains("jwt") ->
                "Session expired. Please log in again."
            raw.contains("network") || raw.contains("unable to resolve") ||
            raw.contains("connection") || raw.contains("socket") ->
                "Network error. Please check your internet connection."
            else -> e.message ?: "An unexpected error occurred."
        }
    }
}
