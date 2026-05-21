package com.example.expensetracker

import android.app.Application
import com.example.expensetracker.data.SupabaseClientProvider

class ExpenseTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClientProvider.init(applicationContext)
    }
}
