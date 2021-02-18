package co.wangun.cafepos.util

import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import java.text.SimpleDateFormat
import java.util.*

class DateUtils {
    fun getTodayDateYmd(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    }

    fun getTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date())
    }

    fun parseTime(text: String): String {
        return text.split(" ").last()
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("d MMM yyyy", Locale.ROOT).format(Date())
    }

    fun getTodayDmy(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.ROOT).format(Date())
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
}