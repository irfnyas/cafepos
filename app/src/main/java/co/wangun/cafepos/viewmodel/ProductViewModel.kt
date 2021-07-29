package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.TABLE_MATERIAL
import co.wangun.cafepos.App.Companion.TABLE_PRODUCT
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Recipe
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("MemberVisibilityCanBePrivate")
class ProductViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }
    val dbProduct by lazy { db.productQueries }
    val dbMaterial by lazy { db.materialQueries }

    val keywordFilter by lazy { MutableStateFlow("") }
    val catsFilter by lazy { MutableStateFlow(mutableListOf<String>()) }
    val recipesTemp by lazy { MutableStateFlow(mutableListOf<Recipe>()) }

    fun setKeywordFilter(input: String) {
        keywordFilter.value = input
    }

    fun getAllCats(): List<String> {
        return MainViewModel().getAllCategories(TABLE_PRODUCT)
    }

    fun setCatsFilter(input: List<CharSequence>) {
        catsFilter.value = input.map { "$it" }.toMutableList()
    }

    fun isProductListed(str: String): Boolean {
        return dbProduct.selectUnique(str, str).executeAsOneOrNull() != null
    }

    fun selectAll(table: String): List<Any> {
        return when(table) {
            TABLE_PRODUCT -> dbProduct.selectAll().executeAsList().toMutableList().apply {
                if(keywordFilter.value.isNotBlank()) retainAll {
                    val hasName = it.name.contains(keywordFilter.value, true)
                    val hasDesc = it.desc?.contains(keywordFilter.value, true) == true
                    hasName || hasDesc
                }
                if(catsFilter.value.isNotEmpty()) retainAll {
                    catsFilter.value.contains(it.category)
                }
            }
            TABLE_MATERIAL -> dbMaterial.selectAll().executeAsList()
            else -> emptyList()
        }
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

    fun isRecipeListed(value: String): Boolean {
        return recipesTemp.value.find { it.name == value } != null
    }
}