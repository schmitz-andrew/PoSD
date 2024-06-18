package com.example.foodtracker.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.foodtracker.workers.ReminderWorker
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaDuration


enum class ExpiryEta { ONE_WEEK, ONE_DAY }

class WorkManagerReminderRepository(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    private val db = AppDatabase.getDatabase(context)

    @Throws(NoSuchElementException::class, DateTimeParseException::class)
    suspend fun scheduleReminder(productId: Int, expiryEnum: ExpiryEta) {
        val expiryEta = when (expiryEnum) {
            ExpiryEta.ONE_WEEK -> Period.ofDays(7)
            ExpiryEta.ONE_DAY -> Period.ofDays(1)
        }

        val product = db.productDao().getProductById(productId).firstOrNull()
        if (product?.expiryDate == null) {
            throw NoSuchElementException("No product found with id $productId with an expiry date")
        }

        val productExpiry = LocalDate.parse(product.expiryDate)
        val today = LocalDate.now()
        val fromNowToExpiry = Period.between(today, productExpiry)
        val daysUntilReminder = fromNowToExpiry - expiryEta
        val reminderDate = if (!daysUntilReminder.isNegative && !daysUntilReminder.isZero) productExpiry - expiryEta else today

        val periodLeftOnReminder = if (reminderDate == today) fromNowToExpiry else expiryEta
        val initialDelay = Period.between(today, reminderDate).days
            .coerceAtLeast(0).toDuration(DurationUnit.DAYS).toJavaDuration()

        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(
                ReminderWorker.PRODUCT_NAME_KEY to product.name,
                ReminderWorker.QUANTITY_KEY to product.quantity,
                ReminderWorker.TIME_UNTIL_EXPIRY_KEY to when(val d = periodLeftOnReminder.days) {
                    1 -> "$d day"
                    else -> "$d days"
                }
            ))
            .setInitialDelay(initialDelay)
            .build()

        workManager.enqueueUniqueWork(
            getUniqueWorkName(expiryEnum, productId, product.name, product.expiryDate),
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    fun cancelReminder(productId: Int, productName: String, expiryDate: String, expiryEta: ExpiryEta) {
        workManager.cancelUniqueWork(getUniqueWorkName(expiryEta, productId, productName, expiryDate))
    }

    private fun getUniqueWorkName(
        expiryEta: ExpiryEta,
        productId: Int,
        productName: String,
        expiryDate: String
    ) = "$expiryEta$productId$productName$expiryDate"
}
