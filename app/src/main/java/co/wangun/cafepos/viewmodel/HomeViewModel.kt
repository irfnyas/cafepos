package co.wangun.cafepos.viewmodel

import android.util.DisplayMetrics
import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.util.SessionUtils
import cowanguncafepos.Active_order
import java.text.SimpleDateFormat
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class HomeViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }

    fun calColumns(size: Int): Int {
        val displayMetrics: DisplayMetrics = cxt.resources.displayMetrics
        val dpWidth = displayMetrics.heightPixels / displayMetrics.density
        return (dpWidth / size).toInt()
    }

    fun getTodayOrderForTable(num: Int): List<String> {
        return db.orderQueries.selectAllTodayForTable(num.toLong(), getTodayDateDb())
            .executeAsList().mapIndexed { i, item -> "Order ${i + 1} - Created at ${item.time}" }
    }

    fun getTablesAmount(): Int {
        val amount = su.get(SessionUtils.TablesAmount_INT) as Int
        return if (amount == 0) {
            su.set(SessionUtils.TablesAmount_INT, 1); 1
        } else amount
    }

    fun parseTime(text: String): String {
        return text.split(" ").last()
    }

    fun putTablesAmount(input: Int) {
        su.set(SessionUtils.TablesAmount_INT, input.toString().toInt())
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.ROOT).format(Date())
    }

    fun getTodayDateDb(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    }

    fun getTime(): String {
        return SimpleDateFormat("HH:mm", Locale.ROOT).format(Date())
    }
}