package co.wangun.cafepos.viewmodel

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

    fun getThisMonthOrders(): List<String> {
        return db.orderQueries
                .selectDistinctThisMonth()
                .executeAsList()
                .sortedWith(compareBy ({ it.date }, { it.time }))
                .reversed()
                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getRangedOrders(start: String, end: String): List<String> {
        return db.orderQueries
                .selectDistinctRange(start, end)
                .executeAsList()
                .sortedWith(compareBy ({ it.date }, { it.time }))
                .reversed()
                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getDefaultDateRange(): String {
        val today = du.getTodayDmy()
        val firstOfMonth = today.replaceBefore("-","01")
        return "$firstOfMonth until Today"
    }
}