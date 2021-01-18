package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Active_order
import cowanguncafepos.Menu
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class OrderViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }
    private var tableOrder: Long = 0
    private var dateOrder: String = ""
    private var timeOrder: String = ""

    fun initOrder(table: Int, date: String, time: String) {
        tableOrder = table.toLong()
        dateOrder = date
        timeOrder = time
    }

    fun getAllOrders(): List<Active_order> {
        return db.orderQueries
            .selectAllTimeForTable(tableOrder, dateOrder, timeOrder)
            .executeAsList()
    }

    fun countOrder(): Long {
        return db.orderQueries.count().executeAsOne()
    }

    fun postOrder(order: Active_order) {
        return db.orderQueries.insert(order)
    }

    fun delOrder(id: Long) {
        db.orderQueries.delete(id)
    }

    fun getAllMenu(): List<Menu> {
        return MenuViewModel().getAllMenu()
    }
}