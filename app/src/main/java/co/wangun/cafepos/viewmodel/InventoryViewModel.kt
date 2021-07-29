package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.TABLE_MATERIAL
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Material
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("MemberVisibilityCanBePrivate")
class InventoryViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }
    val dbMaterial by lazy { db.materialQueries }
    val dbInventory by lazy { db.inventoryQueries }
    val keywordFilter by lazy { MutableStateFlow("") }
    val catsFilter by lazy { MutableStateFlow(mutableListOf<String>()) }
    val materials by lazy { getAllMaterials() }

    data class Joined(
        var id: Long,
        var name: String,
        var desc: String?,
        var mass: Double?,
        var price: Double?,
        var datetime: String?,
        var category: String?,
        var unit: String?
    )

    fun setKeywordFilter(input: String) {
        keywordFilter.value = input
    }

    fun setCatsFilter(input: List<CharSequence>) {
        catsFilter.value = input.map { "$it" }.toMutableList()
    }

    fun selectAll(list: List<Joined>): List<Joined> {
        return list.toMutableList().apply {
            if(keywordFilter.value.isNotBlank()) retainAll {
                val hasName = it.name.contains(keywordFilter.value, true)
                val hasDesc = it.desc?.contains(keywordFilter.value, true) == true
                hasName || hasDesc
            }
            if(catsFilter.value.isNotEmpty()) retainAll {
                catsFilter.value.contains(it.category)
            }
        }
    }

    fun getAllItems(i: Int?): List<Joined> {
        return selectAll(
            when(i) {
                1 -> dbInventory.selectAllPositive().executeAsList().map {
                    Joined(it.id, it.name, it.desc, it.mass,
                        it.price, it.datetime, it.category, it.unit)
                }
                2 -> dbInventory.selectAllNegative().executeAsList().map {
                    Joined(it.id, it.name, it.desc, it.mass,
                        it.price, it.datetime, it.category, it.unit)
                }
                else -> dbInventory.selectAll().executeAsList().map {
                    Joined(it.id, it.name, it.desc, it.mass,
                        it.price, it.datetime, it.category, it.unit)
                }
            }
        )
    }

    fun getAllMaterials(): List<Material> {
        return dbMaterial.selectAll().executeAsList()
    }

    fun getAllCats(): List<String> {
        return MainViewModel().getAllCategories(TABLE_MATERIAL)
    }

    fun getCatsFilterIndices(): IntArray {
        val arr = mutableListOf<Int>()
        getAllCats().forEachIndexed { index, it ->
            if(catsFilter.value.contains(it)) arr.add(index)
        }
        return arr.toIntArray()
    }

    fun hasCatsFilter(): Boolean {
        return catsFilter.value.size != 0
    }
}