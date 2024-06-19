package com.example.foodtracker.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


/**
 * Converts milliseconds to local date.
 */
private fun convertMillisToLocalDate(millis: Long): LocalDate {
    return Instant
        .ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

/**
 * Converts milliseconds to local date with format.
 */
private fun convertMillisToLocalDateWithFormatter(
    date: LocalDate,
    dateTimeFormatter: DateTimeFormatter
): LocalDate {
    //Convert the date to a long in millis using a date formatter.
    val dateInMillis = LocalDate.parse(date.format(dateTimeFormatter), dateTimeFormatter)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    //Convert the millis to a localDate object.
    return Instant
        .ofEpochMilli(dateInMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

/**
 * Converts a date to a string.
 */
private fun dateToString(date: LocalDate): String {
    val dateFormatter = DateTimeFormatter.ISO_DATE
    val dateInMillis = convertMillisToLocalDateWithFormatter(date, dateFormatter)
    return dateFormatter.format(dateInMillis)
}

fun millisToString(millis: Long?) = when (millis) {
    is Long -> dateToString(convertMillisToLocalDate(millis))
    else -> LocalDate.now().format(DateTimeFormatter.ISO_DATE).toString()
}

fun addWeeksToDate(expiryDate: String, number: Long): String {
    val formatter = DateTimeFormatter.ISO_DATE
    val localDate = try {
        LocalDate.parse(expiryDate, formatter)
    } catch (e: Exception) {
        LocalDate.now()
    }
    val oneWeekLater = localDate.plus(number, ChronoUnit.WEEKS)
    return oneWeekLater.toString()
}
