package com.example.foodtracker

import android.app.Activity.RESULT_CANCELED
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import com.example.foodtracker.ui.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class MainActivityTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()


    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun getAppName(activity: MainActivity): String {
        return activity.getString(R.string.app_name)
    }

    @Test
    fun mainActivity_topAppBar_hasTitle() {
        composeTestRule.onNodeWithText(getAppName(composeTestRule.activity)).assertIsDisplayed()
    }

    @Test
    fun mainActivity_topAppBar_hasManualAddButton() {
        composeTestRule.onNodeWithContentDescription("Add a product")
            .assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText("Add a product").assertIsDisplayed()
    }

    @Test
    fun mainActivity_topAppBar_scanBarcodeButton() {
        // stub intent to scan barcode
        intending(toPackage("com.google.android.gms")).respondWith(ActivityResult(RESULT_CANCELED, Intent()))
        composeTestRule.onNodeWithContentDescription("Scan barcode")
            .assertIsDisplayed().performClick()

        // ensure that no add item popup is open
        composeTestRule.onNodeWithText(getAppName(composeTestRule.activity)).assertIsDisplayed()
    }

}
