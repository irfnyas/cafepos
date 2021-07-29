package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Active_order
import cowanguncafepos.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates

@Suppress("MemberVisibilityCanBePrivate")
class OrderViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }
    val dbProduct by lazy { db.productQueries }
    val dbPayment by lazy { db.paymentQueries }

    var isDirty by Delegates.notNull<Boolean>()
    var tableOrder by Delegates.notNull<Long>()

    lateinit var dateOrder: String
    lateinit var timeOrder: String
    lateinit var ordersTemp: MutableList<Active_order>

    val allProducts by lazy { MutableStateFlow(listOf<Product>()) }
    val allCats by lazy { getAllCategories() }

    fun initOrder(table: Int, date: String, time: String) {
        tableOrder = table.toLong()
        dateOrder = date
        timeOrder = time
        isDirty = false
        ordersTemp = selectAllThisInvoice().toMutableList()
    }

    fun selectAllThisInvoice(): List<Active_order> {
        return db.orderQueries
            .selectAllByTableAndDateTime(tableOrder, dateOrder, timeOrder)
            .executeAsList()
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

        // list now dirty
        if(!isDirty) isDirty = true
    }

    fun delOrder(id: Long) {
        db.orderQueries.delete(id)
    }

    fun getPayment(isCashChecked: Boolean): String {
        return if(isCashChecked) "cash" else "card"
    }

    fun getAllCategories(): List<String> {
        return mutableListOf("All Categories").apply {
            dbProduct.selectAllCategories()
                .executeAsList()
                .map { "${it.category}" }
                .forEach { this.add(it) }
        }
    }

    fun updateProductsByCats(cat: String = "") {
        allProducts.value =
            if(cat == allCats[0] || cat.isBlank()) dbProduct.selectAll().executeAsList()
            else dbProduct.selectAllByCategories(cat).executeAsList()
    }

    fun selectAllPayments(): List<String> {
        return dbPayment.selectAll().executeAsList().map { it.name }
    }

    fun invoiced(table: Long, date: String, time: String): String {
        return "#${table}${date}${time}"
    }
}