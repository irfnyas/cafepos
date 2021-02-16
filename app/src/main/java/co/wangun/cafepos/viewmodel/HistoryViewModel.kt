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
}