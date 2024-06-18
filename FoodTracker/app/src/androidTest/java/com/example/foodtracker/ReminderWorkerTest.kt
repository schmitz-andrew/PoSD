package com.example.foodtracker

import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.foodtracker.workers.ReminderWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class ReminderWorkerTest {

    private lateinit var context: Context
    private lateinit var manager: NotificationManager

    @get:Rule
    val composeTestRule = createComposeRule()

    @JvmField
    @Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    private val mockInputData = workDataOf(
        ReminderWorker.PRODUCT_NAME_KEY to "Apple",
        ReminderWorker.QUANTITY_KEY to 2,
        ReminderWorker.TIME_UNTIL_EXPIRY_KEY to "1 day 4 hours"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @After
    fun tearDown() {
        manager.cancelAll()
    }


    @Test
    fun reminderWorker_doWork_resultSuccessSchedulesNotification() {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setInputData(mockInputData)
            .build()

        runBlocking {
            val result = worker.doWork()
            assertTrue(result is ListenableWorker.Result.Success)

            composeTestRule.waitUntil { manager.activeNotifications.isNotEmpty() }

            manager.activeNotifications.first().notification.contentIntent.send()

            composeTestRule.onNodeWithText("Food Tracker").assertIsDisplayed()
        }
    }

}
