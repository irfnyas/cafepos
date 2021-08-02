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
    val materials by lazy { getAllMaterials(null) }

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
        var isRaw: Boolean,
        var acquiredMass: Double,
        var acquiredPrice: Double,
        var reducedMass: Double,
        var reducedPrice: Double,
        var usedByProductMass: Double,
        var usedByRefinedMass: Double,
        var usedTotalMass: Double,
        var actualMass: Double,
        var actualPrice: Double
    )

    fun setKeywordFilter(input: String) {
        keywordFilter.value = input
    }

    fun setCatsFilter(input: List<CharSequence>) {
        catsFilter.value = input.map { "$it" }.toMutableList()
    }

    fun selectAll(list: List<Any>): List<Any> {
        return list.toMutableList().apply {
            if (keywordFilter.value.isNotBlank()) retainAll {
                when (it) {
                    is Joined -> {
                        val hasName = it.name.contains(keywordFilter.value, true)
                        val hasDesc = it.desc?.contains(keywordFilter.value, true) == true
                        hasName || hasDesc
                    }
                    is Summary -> it.name.contains(keywordFilter.value, true)
                    else -> false
                }
            }
            if (catsFilter.value.isNotEmpty()) retainAll {
                when (it) {
                    is Joined -> catsFilter.value.contains(it.category)
                    is Summary -> catsFilter.value.contains(it.category)
                    else -> false
                }
            }
        }
    }

    fun getSummaries(): List<Summary> {
        val list = mutableListOf<Summary>()

        materials.forEach { mat ->
            val name = mat.name
            val category = mat.category ?: "?"
            val unit = mat.unit ?: "?"
            val isRaw = mat.is_raw == 1L
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
                    name, category, unit, isRaw,
                    acquireMass, acquirePrice,
                    reduceMass, reducePrice,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
                )
            )
        }

        // check used
        getAllAmountOrders().forEach { pair ->
            getAllRecipesByParent(pair.first).forEach { rec ->
                list.find { it.name == rec.name }?.let {
                    it.usedByProductMass += rec.mass?.times(pair.second) ?: 0.0
                }
            }
        }

        // convert refined to raw
        list.forEach { sum ->
            if (!sum.isRaw) {
                getAllRecipesByParent(sum.name).forEach { rec ->
                    list.find { it.name == rec.name }?.let {
                        val refinedMass =
                            materials.find { mat -> mat.name == sum.name }?.mass ?: 0.0
                        val rawMassNeeded = rec.mass ?: 1.0
                        val usedByRefined = rawMassNeeded / refinedMass * sum.usedByProductMass
                        it.usedByRefinedMass += usedByRefined
                    }
                }
            }
        }

        // calculate actual
        list.forEach {
            it.usedTotalMass = it.usedByProductMass + it.usedByRefinedMass
            it.actualMass = it.acquiredMass + it.reducedMass - it.usedTotalMass
            it.actualPrice = it.acquiredPrice + it.reducedPrice
        }

        // show raw only
        list.retainAll { it.isRaw }

        // return
        return list
    }

    fun getAllItems(i: Int?): List<Any> {
        return selectAll(
            when (i) {
                0 -> getSummaries()
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

    fun getAllMaterials(i: Int?): List<Material> {
        return when (i) {
            0 -> dbMaterial.selectAllRawMaterials()
            1 -> dbMaterial.selectAllRefinedMaterials()
            else -> dbMaterial.selectAll()
        }.executeAsList()
    }

    fun getAllAmountOrders(): List<Pair<String, Double>> {
        return db.orderQueries.selectAllNameAmountSum().executeAsList().map {
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