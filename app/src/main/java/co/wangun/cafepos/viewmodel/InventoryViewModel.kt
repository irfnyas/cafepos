package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.TABLE_MATERIAL
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Material
import cowanguncafepos.Recipe
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

    data class Summary(
        var name: String,
        var category: String,
        var unit: String,
        var acquiredMass: Double,
        var acquiredPrice: Double,
        var reducedMass: Double,
        var reducedPrice: Double,
        var usedMass: Double,
        var actualMass: Double,
        var actualPrice: Double
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
            if (catsFilter.value.isNotEmpty()) retainAll {
                catsFilter.value.contains(it.category)
            }
        }
    }

    fun getSummaries(): List<Summary> {
        val list = mutableListOf<Summary>()

        getAllMaterials().forEach { mat ->
            val name = mat.name
            val category = mat.category ?: "?"
            val unit = mat.unit ?: "?"
            var acquireMass = 0.0
            var acquirePrice = 0.0
            var reduceMass = 0.0
            var reducePrice = 0.0

            // check for inventories
            val inventories = dbInventory.selectAll().executeAsList().map {
                Joined(
                    it.id, it.name, it.desc, it.mass,
                    it.price, it.datetime, it.category, it.unit
                )
            }
            inventories.forEach { inv ->
                if (inv.name == name) {
                    if ((inv.mass ?: 0.0) > 0) {
                        acquireMass += inv.mass ?: 0.0
                        acquirePrice += inv.price ?: 0.0
                    } else {
                        reduceMass += inv.mass ?: 0.0
                        reducePrice += inv.price ?: 0.0
                    }
                }
            }

            list.add(
                Summary(
                    name, category, unit,
                    acquireMass, acquirePrice,
                    reduceMass, reducePrice,
                    0.0, 0.0, 0.0
                )
            )
        }

        // check used
        getAllTodayOrders().forEach { pair ->
            getAllRecipesByParent(pair.first).forEach { rec ->
                list.find { it.name == rec.name }?.let {
                    it.usedMass += rec.mass?.times(pair.second) ?: 0.0
                }
            }
        }

        list.forEach {
            it.actualMass = it.acquiredMass + it.reducedMass - it.usedMass
            it.actualPrice = it.acquiredPrice + it.reducedPrice
        }

        return list
    }

    fun getAllItems(i: Int?): List<Any> {
        return selectAll(
            when (i) {
                1 -> dbInventory.selectAllPositive().executeAsList().map {
                    Joined(
                        it.id, it.name, it.desc, it.mass,
                        it.price, it.datetime, it.category, it.unit
                    )
                }
                2 -> dbInventory.selectAllNegative().executeAsList().map {
                    Joined(
                        it.id, it.name, it.desc, it.mass,
                        it.price, it.datetime, it.category, it.unit
                    )
                }
                else -> dbInventory.selectAll().executeAsList().map {
                    Joined(
                        it.id, it.name, it.desc, it.mass,
                        it.price, it.datetime, it.category, it.unit
                    )
                }
            }
        )
    }

    fun getAllMaterials(): List<Material> {
        return dbMaterial.selectAll().executeAsList()
    }

    fun getAllTodayOrders(): List<Pair<String, Double>> {
        return db.orderQueries.selectAllToday().executeAsList().map {
            Pair("${it.name}", it.sum ?: 0.0)
        }
    }

    fun getAllRecipesByParent(name: String): List<Recipe> {
        return MainViewModel().getParentRecipes(name)
    }

    fun getAllCats(): List<String> {
        return MainViewModel().getAllCategories(TABLE_MATERIAL)
    }

    fun getCatsFilterIndices(): IntArray {
        val arr = mutableListOf<Int>()
        getAllCats().forEachIndexed { index, it ->
            if (catsFilter.value.contains(it)) arr.add(index)
        }
        return arr.toIntArray()
    }

    fun hasCatsFilter(): Boolean {
        return catsFilter.value.size != 0
    }
}