package co.wangun.cafepos.viewmodel

import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.util.SessionUtils.Companion.TablesAmount_INT
import cowanguncafepos.Menu
import java.text.SimpleDateFormat
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class MainViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }

    fun getTablesAmount(): Int {
        val amount = su.get(TablesAmount_INT) as Int
        return if (amount == 0) {
            su.set(TablesAmount_INT, 1); 1
        } else amount
    }

    fun putTablesAmount(input: Int) {
        su.set(TablesAmount_INT, input.toString().toInt())
    }

    fun calColumns(size: Int): Int {
        val displayMetrics: DisplayMetrics = App.cxt.resources.displayMetrics
        val dpWidth = displayMetrics.heightPixels / displayMetrics.density
        return (dpWidth / size).toInt()
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date())
    }

    fun withCurrency(num: Double): String {
        return "£ %.2f".format(num)
    }

    fun postMenu(menu: Menu) {
        db.menuQueries.insert(menu)
    }

    fun isMenuListed(name: String): Boolean {
        return db.menuQueries.selectAll().executeAsList().find { it.name == name } != null
    }

    fun getAllMenus(): List<Menu> {
        return db.menuQueries.selectAll().executeAsList()
    }

    fun getAllCategories(): List<String> {
        return db.menuQueries.selectAllCategories().executeAsList().map { "${it.category}" }
    }

    fun countMenu(): Long {
        return db.menuQueries.count().executeAsOne()
    }

    fun deleteMenu(id: Long) {
        db.menuQueries.delete(id)
    }

    fun isMenuFormValid(menu: Menu, isNew: Boolean): Boolean {
        return if (isNew) !(menu.name.isBlank() or menu.category.isNullOrBlank() or isMenuListed(menu.name))
        else !(menu.name.isBlank() or menu.category.isNullOrBlank())
    }
}