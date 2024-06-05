package com.example.foodtracker

import android.app.Application
import com.example.foodtracker.data.AppDatabase


class FoodTrackerApplication: Application() {

    val database by lazy {
        AppDatabase.getDatabase(this)
    }

}
