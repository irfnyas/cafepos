package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db

@Suppress("MemberVisibilityCanBePrivate")
class HistoryViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }

    fun getAllOrders(): List<String> {
        return db.orderQueries
                .selectAllDistinct()
                .executeAsList()
                .sortedWith(compareBy ({ it.date }, { it.time })).reversed()
                .map { "${it.num}?${it.date}?${it.time}" }
    }

    fun getDetailOrders(tableInput: String): List<String> {
        val itemSplit = tableInput.split("?")
        val num = itemSplit[0].toLong()
        val date = itemSplit[1]
        val time = itemSplit[2]

        return db.orderQueries
            .selectAllTimeForTable(num, date, time)
            .executeAsList().map {
            MainViewModel().itemInReceipt(
                it.name ?: "", it.amount ?: 0, it.price ?: 0.0
            )
        }
    }
}