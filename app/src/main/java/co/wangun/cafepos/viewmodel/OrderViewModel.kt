package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Active_order
import cowanguncafepos.Menu
import kotlin.properties.Delegates

@Suppress("MemberVisibilityCanBePrivate")
class OrderViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }
    var isDirty by Delegates.notNull<Boolean>()
    var tableOrder by Delegates.notNull<Long>()
    lateinit var dateOrder: String
    lateinit var timeOrder: String
    lateinit var ordersTemp: MutableList<Active_order>

    fun initOrder(table: Int, date: String, time: String) {
        tableOrder = table.toLong()
        dateOrder = date
        timeOrder = time
        isDirty = false
        ordersTemp = getAllOrders().toMutableList()
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

    fun postItemTemp(order: Active_order) {
        // find item
        val item = ordersTemp.find {
            it.name == order.name
        }

        // check is item added
        val isItemAdded = item != null

        // post item
        if (isItemAdded) {
            val index = ordersTemp.indexOf(item)
            ordersTemp[index] = order
        } else ordersTemp.add(order)

        // list now dirty
        if(!isDirty) isDirty = true
    }

    fun delItemTemp(order: Active_order) {
        val item = ordersTemp.find { it.name == order.name }
        ordersTemp.remove(item)
    }

    fun delOrder(id: Long) {
        db.orderQueries.delete(id)
    }

    fun getAllMenu(): List<Menu> {
        return MenuViewModel().getAllMenu()
    }

    fun getPayment(isCashChecked: Boolean): String {
        return if(isCashChecked) "cash" else "card"
    }
}