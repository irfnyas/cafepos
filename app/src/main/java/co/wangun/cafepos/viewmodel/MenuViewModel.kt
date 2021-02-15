package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Active_order
import cowanguncafepos.Menu
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class MenuViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }

    fun postMenu(menu: Menu) {
        db.menuQueries.insert(menu)
    }

    fun isMenuListed(name: String): Boolean {
        return db.menuQueries.selectAll().executeAsList().find { it.name == name } != null
    }

    fun getAllMenu(): List<Menu> {
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
}