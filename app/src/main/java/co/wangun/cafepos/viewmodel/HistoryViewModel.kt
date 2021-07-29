package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db

@Suppress("MemberVisibilityCanBePrivate")
class HistoryViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }

    fun getAllOrders(): List<String> {
        return db.orderQueries
            .selectDistinct()
            .executeAsList()
            .map { "${it.invoice}" }
//                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getTodayOrders(): List<String> {
        return db.orderQueries
            .selectDistinctToday()
            .executeAsList()
            .map { "${it.invoice}" }
    }

    fun getThisMonthOrders(): List<String> {
        return db.orderQueries
            .selectDistinctThisMonth()
            .executeAsList()
            .map { "${it.invoice}" }
    }

    fun getOrderByInvoice(invoice: String): List<String> {
        // return from db
        return db.orderQueries
            .selectAllByInvoice(invoice)
            .executeAsList()
            .map { "${it.invoice}" }
    }

    fun getRangedOrders(start: String, end: String): List<String> {
        return db.orderQueries
            .selectDistinctByDateRange(start, end)
            .executeAsList()
            .map { "${it.invoice}" }
    }

    fun getDefaultDateRange(): String {
        return "Today"
    }
}