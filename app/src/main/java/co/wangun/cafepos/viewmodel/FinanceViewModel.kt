package co.wangun.cafepos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.TABLE_MATERIAL
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.fu
import cowanguncafepos.Material
import cowanguncafepos.Product
import cowanguncafepos.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue

@Suppress("MemberVisibilityCanBePrivate")
class FinanceViewModel : ViewModel() {

    data class FinanceItem(
        var name: String,
        var category: String,
        var price: Double,
        var recipes: MutableList<Cogs>,
        var revenue: Double,
        var cost: Double,
        var profit: Double,
        var sold: Int
    )

    data class Cogs(
        var name: String,
        var desc: String,
        var category: String,
        var unit: String,
        var price: Double,
        var mass: Double,
    )

    private val TAG by lazy { javaClass.simpleName }
    val keywordFilter by lazy { MutableStateFlow("") }
    val catsFilter by lazy { MutableStateFlow(mutableListOf<String>()) }
    val footerStr by lazy { MutableStateFlow(emptyList<String>()) }

    lateinit var products: MutableList<Product>
    lateinit var materials: List<Material>
    lateinit var todayOrders: List<Pair<String, Int>>
    lateinit var lastInventories: List<Cogs>

    fun initData() {
        db.transactionWithResult<Any?> {
            products = db.productQueries
                .selectAll()
                .executeAsList()
                .toMutableList()

            materials = db.materialQueries
                .selectAll()
                .executeAsList()

            todayOrders = db.orderQueries
                .selectAllNameAmountSumToday()
                .executeAsList()
                .map { Pair("${it.name}", it.sum?.toInt() ?: 0) }

            lastInventories = db.inventoryQueries
                .selectAllLast()
                .executeAsList()
                .map {
                    Cogs(
                        it.name,
                        it.desc_ ?: "",
                        it.category ?: "",
                        it.unit ?: "",
                        it.price ?: 0.0,
                        it.mass ?: 0.0
                    )
                }

            null
        }
    }

    fun getAllItems(i: Int?): List<Any> {
        val list = mutableListOf<FinanceItem>()

        if (i != 2) {
            products.forEach { product ->
                val name = product.name
                val category = product.category ?: "No Category"
                val price = product.price ?: 0.0

                // set amount
                val sold = if (i == 0) todayOrders.find {
                    it.first == product.name
                }?.second ?: 0 else 1

                // set recipes
                val recipes = mutableListOf<Cogs>()
                getAllRecipesByParent(product.name).forEach { productRecipe ->
                    val material = materials.find { productRecipe.name == it.name }
                    val isRaw = material?.is_raw == 1L

                    val oCogs = Cogs(
                        material?.name ?: "",
                        material?.desc ?: "",
                        material?.category ?: "",
                        material?.unit ?: "",
                        0.0,
                        productRecipe.mass ?: 0.0
                    )

                    if (!isRaw) {
                        getAllRecipesByParent(material?.name ?: "").forEach { parentRecipe ->
                            val lastInv = lastInventories.find { parentRecipe.name == it.name }
                            val rawPricePerUnit = lastInv?.price?.div(lastInv.mass) ?: 0.0
                            val parentRecipeMass = parentRecipe.mass ?: 0.0
                            val parentMass = material?.mass ?: 0.0
                            oCogs.price += rawPricePerUnit * parentRecipeMass / parentMass
                        }
                    } else {
                        val lastInv = lastInventories.find { productRecipe.name == it.name }
                        val rawPricePerUnit = lastInv?.price?.div(lastInv.mass) ?: 0.0
                        val parentRecipeMass = productRecipe.mass ?: 0.0
                        val parentMass = material?.mass ?: 0.0
                        oCogs.price += rawPricePerUnit * parentRecipeMass / parentMass
                    }

                    recipes.add(oCogs)
                }

                // set recipes cost

                // calculate total
                val cost = recipes.map { it.mass.absoluteValue * it.price * -1 }.sum() * sold
                val revenue = price * sold
                val profit = revenue - cost

                // add to list
                list.add(
                    FinanceItem(
                        name, category,
                        price, recipes,
                        revenue, cost,
                        profit, sold
                    )
                )
            }
        } else {
            materials.forEach { material ->
                // init val
                val name = material.name
                val category = material.category ?: ""
                val lastInv = lastInventories.find { it.name == material.name }
                val recipes = mutableListOf<Cogs>()

                // add this to recipes
                recipes.add(
                    Cogs(
                        material.name,
                        material.desc ?: "",
                        material.category ?: "",
                        material.unit ?: "",
                        lastInv?.price ?: 0.0,
                        lastInv?.mass ?: material.mass ?: 0.0
                    )
                )

                // add raw for refined recipes
                getAllRecipesByParent(material.name).forEach { parentRecipe ->
                    val raw = materials.find { parentRecipe.name == it.name }
                    val oCogs = Cogs(
                        raw?.name ?: "",
                        raw?.desc ?: "",
                        raw?.category ?: "",
                        raw?.unit ?: "",
                        0.0,
                        raw?.mass ?: 0.0
                    )

                    val parentRecipeLastInv =
                        lastInventories.find { parentRecipe.name == it.name }
                    val rawPricePerUnit =
                        parentRecipeLastInv?.price?.div(parentRecipeLastInv.mass) ?: 0.0

                    val parentRecipeMass = parentRecipe.mass ?: 0.0
                    val parentMass = material.mass ?: 0.0

                    Log.d(TAG, "getAllItems: $parentRecipeLastInv")
                    Log.d(TAG, "getAllItems: $rawPricePerUnit")
                    Log.d(TAG, "getAllItems: $parentRecipeMass")
                    Log.d(TAG, "getAllItems: $parentMass")

                    oCogs.price += rawPricePerUnit * parentRecipeMass
                    recipes.add(oCogs)
                }

                // calculate total
                val cost = recipes.map { it.price * -1 }.sum()

                // add to list
                list.add(
                    FinanceItem(
                        name, category,
                        0.0, recipes,
                        0.0, cost,
                        0.0, 0
                    )
                )
            }
        }

        // update footer
        updateFooter(i, list)

        // return
        return selectAll(list)
    }

    private fun updateFooter(i: Int?, list: List<FinanceItem>) {
        Log.d(TAG, "updateFooter: $list")
        footerStr.value = when (i) {
            0 -> {
                val str1 = "Total"
                val str2 = fu.toLocale(list.map { it.sold }.sum().toDouble())
                val str3 = fu.toLocale(list.map { it.revenue }.sum())
                val str4 = fu.toLocale(list.map { it.profit }.sum())
                listOf(str1, str2, str3, str4)
            }
            1 -> {
                val str1 = "Average"
                val str2 = fu.toLocale(list.map { it.price }.average())
                val str3 = fu.toLocale(list.map { it.cost }.average())
                val str4 = fu.toLocale(list.map { it.cost / it.price * 100 }.average())
                listOf(str1, str2, str3, "$str4%")
            }
            2 -> {
                val str1 = "Average"
                val str2 = fu.toLocale(list.map { it.cost }.average())
                val str3 = fu.toLocale(list.map { massSum(it) }.average())
                val str4 = fu.toLocale(list.map { it.cost / massSum(it) }.average())
                listOf(str1, str2, str3, "$str4%")
            }
            else -> listOf("?".repeat(4))
        }
    }

    fun massSum(it: FinanceItem): Double {
        return it.recipes.map { r -> r.mass }.sum()
    }

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
                    is FinanceItem -> it.name.contains(keywordFilter.value, true)
                    else -> false
                }
            }
            if (catsFilter.value.isNotEmpty()) retainAll {
                when (it) {
                    is FinanceItem -> catsFilter.value.contains(it.category)
                    else -> false
                }
            }
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