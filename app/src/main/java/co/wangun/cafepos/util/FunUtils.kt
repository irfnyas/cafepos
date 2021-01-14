package co.wangun.cafepos.util

import android.util.DisplayMetrics
import co.wangun.cafepos.App.Companion.cxt
import java.text.SimpleDateFormat
import java.util.*


class FunUtils {

    fun calColumns(size: Int): Int {
        val displayMetrics: DisplayMetrics = cxt.resources.displayMetrics
        val dpWidth = displayMetrics.heightPixels / displayMetrics.density
        return (dpWidth / size).toInt()
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date())
    }
}