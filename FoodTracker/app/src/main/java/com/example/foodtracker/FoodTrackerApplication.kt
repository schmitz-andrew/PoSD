package com.example.foodtracker

import android.app.Application
import com.example.foodtracker.data.AppDatabase
import com.example.foodtracker.data.WorkManagerReminderRepository


class FoodTrackerApplication: Application() {

    val database by lazy {
        AppDatabase.getDatabase(this)
    }

    val reminderRepository by lazy {
        WorkManagerReminderRepository(this)
    }

}
