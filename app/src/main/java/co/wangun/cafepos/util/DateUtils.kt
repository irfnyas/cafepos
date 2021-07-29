package co.wangun.cafepos.util

import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale.ROOT

class DateUtils {
    fun dateYmd(date: Any? = null): String {
        return SimpleDateFormat("yyyy-MM-dd", ROOT).format(date ?: Date())
    }

    fun dateTimeYmd(date: Any? = null): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", ROOT).format(date ?: Date())
    }

    fun getTime(): String {
        return SimpleDateFormat("HH:mm:ss", ROOT).format(Date())
    }

    fun parseTime(text: String): String {
        return text.split(" ").last()
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("d MMM yyyy", ROOT).format(Date())
    }

    fun getTodayDmy(): String {
        return SimpleDateFormat("dd-MM-yyyy", ROOT).format(Date())
    }

    fun getDmyFromCal(cal: Calendar): String {
        val day = if(cal.dayOfMonth > 9) "${cal.dayOfMonth}" else "0${cal.dayOfMonth}"
        val month = if(cal.month + 1 > 9) "${cal.month + 1}" else "0${cal.month + 1}"
        val year = "${cal.year}"
        return "$day-$month-$year"
    }

    fun getYmdFromCal(cal: Calendar): String {
        val day = if(cal.dayOfMonth > 9) "${cal.dayOfMonth}" else "0${cal.dayOfMonth}"
        val month = if(cal.month + 1 > 9) "${cal.month + 1}" else "0${cal.month + 1}"
        val year = "${cal.year}"
        return "$year-$month-$day"
    }

    fun dateTimeFromCal(cal: Calendar): String {
        return dateTimeYmd(cal.timeInMillis)
    }
}