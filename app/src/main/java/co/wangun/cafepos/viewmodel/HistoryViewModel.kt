package co.wangun.cafepos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.du

@Suppress("MemberVisibilityCanBePrivate")
class HistoryViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }

    fun getAllOrders(): List<String> {
        return db.orderQueries
                .selectAllDistinct()
                .executeAsList()
                .sortedWith(compareBy ({ it.date }, { it.time }))
                .reversed()
                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getTodayOrders(): List<String> {
        return db.orderQueries
                .selectDistinctToday()
                .executeAsList()
                .sortedWith(compareBy ({ it.date }, { it.time }))
                .reversed()
                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getThisMonthOrders(): List<String> {
        return db.orderQueries
                .selectDistinctThisMonth()
                .executeAsList()
                .sortedWith(compareBy ({ it.date }, { it.time }))
                .reversed()
                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getOrderByInvoice(invoice: String): List<String> {
        return try {
            // find last index
            val last = invoice.lastIndex

            // parse time
            val time = invoice.slice(IntRange(last - 5, last)).toMutableList().apply {
                add(2, ':')
                add(5,':')
            }.joinToString("")

            // parse date
            val date = invoice.slice(IntRange(last - 13, last - 6)).toMutableList().apply {
                add(4,'-') //2021-0101
                add(7,'-')
            }.joinToString("")

            // parse num
            val num = invoice.slice(IntRange(0, last - 14)).toLong()
            Log.d(TAG, "getOrderByInvoice: $num, $date, $time")

            // return from db
            db.orderQueries
                    .selectDistinctByInvoice(num, date, time)
                    .executeAsList()
                    .sortedWith(compareBy ({ it.date }, { it.time }))
                    .reversed()
                    .map { "${it.num}?${it.date}?${it.time}" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getRangedOrders(start: String, end: String): List<String> {
        return db.orderQueries
                .selectDistinctByDateRange(start, end)
                .executeAsList()
                .sortedWith(compareBy ({ it.date }, { it.time }))
                .reversed()
                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getDefaultDateRange(): String {
        return "Today"
    }
}