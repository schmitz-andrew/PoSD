package com.example.foodtracker.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters


class ReminderWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        val name = inputData.getString(PRODUCT_NAME_KEY)
        val eta = inputData.getString(TIME_UNTIL_EXPIRY_KEY)
        val quantity = inputData.getInt(QUANTITY_KEY, 1)

        makeProductExpiryReminder(
            "You have $quantity $name left!",
            "Expiring in $eta",
            applicationContext
        )

        return Result.success()
    }

    companion object {
        const val PRODUCT_NAME_KEY = "prod_name"
        const val TIME_UNTIL_EXPIRY_KEY = "expiry_eta"
        const val QUANTITY_KEY = "quantity"
    }

}
