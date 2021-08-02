package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.du
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("MemberVisibilityCanBePrivate")
class HistoryViewModel: ViewModel() {

    data class Transaction(
        val invoice: String,
        val table: Int,
        val payment: String,
        val dateTime: String,
        val creator: String,
        val products: List<Triple<String, Int, Double>>
    )

    private val TAG: String by lazy { javaClass.simpleName }
    val dbOrder by lazy { db.orderQueries }
    val keywordFilter by lazy { MutableStateFlow("") }
    val dateFilter by lazy { MutableStateFlow(getTodayDateRange()) }

    fun selectAll(): List<Any> {
        return getAllItems().toMutableList().apply {
            if (keywordFilter.value.isNotBlank()) {
                retainAll { order ->
                    val hasName = order.products.map { it.first }.any {
                        it.contains(keywordFilter.value, true)
                    }
                    val hasInvoice = order.invoice.contains(
                        keywordFilter.value, true
                    )
                    hasName || hasInvoice
                }
            }
        }
    }

    fun getAllItems(): List<Transaction> {
        val startDate = dateFilter.value.split("_")[0]
        val endDate = dateFilter.value.split("_")[1]
        val dataList = dbOrder.selectAllByDateRange(startDate, endDate).executeAsList()

        return dataList.distinctBy { it.invoice }.map { data ->
            Transaction(
                data.invoice ?: "",
                data.num?.toInt() ?: 0,
                data.payment ?: "",
                data.date ?: "",
                data.creator ?: "",
                dataList.filter { it.invoice == data.invoice }.map {
                    Triple(
                        it.name ?: "",
                        it.amount?.toInt() ?: 0,
                        it.price ?: 0.0
                    )
                }
            )
        }
    }

    fun getTodayDateRange(): String {
        return "${du.dateYmd()}_${du.dateYmd()}"
    }

    fun productsPriceSum(item: Transaction): Double {
        return item.products.map { it.third }.sum()
    }

    fun setKeywordFilter(input: String) {
        keywordFilter.value = input
    }

    fun setDateFilter(start: String? = null, end: String? = null) {
        dateFilter.value = if (start.isNullOrBlank()) getTodayDateRange() else "${start}_${end}"
    }
}