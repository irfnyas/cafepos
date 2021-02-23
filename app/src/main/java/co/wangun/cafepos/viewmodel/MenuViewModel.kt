package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Menu
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("MemberVisibilityCanBePrivate")
class MenuViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }
    val catsFilter by lazy { getAllCategories().toMutableList() }
    val nameFilter by lazy { MutableStateFlow("") }

    fun setNameFilter(input: String) {
        nameFilter.value = input
    }

    fun setCatsFilter(input: List<CharSequence>) {
        catsFilter.clear()
        input.forEach { catsFilter.add("$it") }
    }

    fun postMenu(menu: Menu) {
        db.menuQueries.insert(menu)
    }

    fun isMenuListed(name: String): Boolean {
        return db.menuQueries.selectAll().executeAsList().find { it.name == name } != null
    }

    fun getAllMenu(): List<Menu> {
        return db.menuQueries.selectAll().executeAsList().sortedBy { it.name }
    }

    fun getAllCategories(): List<String> {
        return db.menuQueries
                .selectAllCategories()
                .executeAsList()
                .map { "${it.category}" }
                .sorted()
    }

    fun countMenu(): Long {
        return db.menuQueries.count().executeAsOne()
    }

    fun deleteMenu(id: Long) {
        db.menuQueries.delete(id)
    }

    fun getLastMenus(): List<Menu> {
        // init val
        val list = getAllMenu().toMutableList()
        val isFilteredByName = nameFilter.value.isNotBlank()
        val isFilteredByCats = catsFilter.isNotEmpty()

        // set name filter
        if(isFilteredByName) list.filter {
            it.name.contains(nameFilter.value, true)
        }

        // set cats filter
        val listTemp = list.toList()
        if(isFilteredByCats) listTemp.forEach {
            if(!catsFilter.contains(it.category)) list.remove(it)
        }

        // return list
        return list
    }

    fun getCatsFilterIndices(): IntArray {
        val arr = mutableListOf<Int>()
        getAllCategories().forEachIndexed { index, it ->
            if(catsFilter.contains(it)) arr.add(index)
        }
        return arr.toIntArray()
    }

    fun hasCatsFilter(): Boolean {
        return getAllCategories().size != catsFilter.size
    }
}