package com.medit.health.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private val indianFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val displayDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    private val isoTimestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.ENGLISH)

    fun todayIso(): String {
        return isoFormat.format(Date())
    }

    fun todayIndian(): String {
        return indianFormat.format(Date())
    }

    fun currentTimestamp(): String {
        return isoTimestampFormat.format(Date())
    }

    fun formatToIndianDate(isoDateStr: String?): String {
        if (isoDateStr.isNullOrBlank()) return "—"
        return try {
            val cleanDate = if (isoDateStr.contains("T")) {
                isoDateStr.substringBefore("T")
            } else if (isoDateStr.contains(" ")) {
                isoDateStr.substringBefore(" ")
            } else {
                isoDateStr
            }
            val parsed = isoFormat.parse(cleanDate)
            if (parsed != null) indianFormat.format(parsed) else isoDateStr
        } catch (e: Exception) {
            isoDateStr
        }
    }

    fun formatTimestamp(timestampStr: String?): String {
        if (timestampStr.isNullOrBlank()) return "—"
        return try {
            val parsed = isoTimestampFormat.parse(timestampStr)
            if (parsed != null) displayDateTimeFormat.format(parsed) else timestampStr
        } catch (e: Exception) {
            try {
                formatToIndianDate(timestampStr)
            } catch (ex: Exception) {
                timestampStr
            }
        }
    }
}
