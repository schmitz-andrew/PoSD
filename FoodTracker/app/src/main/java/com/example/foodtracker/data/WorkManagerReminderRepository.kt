package com.example.foodtracker.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.foodtracker.workers.ReminderWorker
import kotlinx.coroutines.flow.firstOrNull
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.time.toJavaDuration


class WorkManagerReminderRepository(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    private val db = AppDatabase.getDatabase(context)

    @Throws(NoSuchElementException::class, DateTimeParseException::class)
    suspend fun scheduleReminder(productId: Int, expiryEta: kotlin.time.Duration) {
        val product = db.productDao().getProductById(productId).firstOrNull()
        if (product?.expiryDate == null) {
            throw NoSuchElementException("No product found with id $productId with an expiry date")
        }

        val reminderDate = LocalDate.parse(product.expiryDate) - expiryEta.toJavaDuration()
        val initialDelay = Duration.between(LocalDate.now(), reminderDate).coerceAtLeast(Duration.ZERO)

        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(
                ReminderWorker.PRODUCT_NAME_KEY to product.name,
                ReminderWorker.QUANTITY_KEY to product.quantity,
                ReminderWorker.TIME_UNTIL_EXPIRY_KEY to expiryEta.toComponents { days, hours, _, _, _ -> "$days days $hours hours" }
            ))
            .setInitialDelay(initialDelay)
            .build()

        workManager.enqueueUniqueWork("$productId${product.name}${product.expiryDate}", ExistingWorkPolicy.REPLACE, work)
    }
}
