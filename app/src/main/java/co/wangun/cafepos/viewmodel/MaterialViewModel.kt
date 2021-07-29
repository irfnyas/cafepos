package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.TABLE_MATERIAL
import co.wangun.cafepos.App.Companion.db
import cowanguncafepos.Material
import cowanguncafepos.Recipe
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("MemberVisibilityCanBePrivate")
class MaterialViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }
    val dbMaterial by lazy { db.materialQueries }
    val keywordFilter by lazy { MutableStateFlow("") }
    val catsFilter by lazy { MutableStateFlow(mutableListOf<String>()) }
    val recipesTemp by lazy { MutableStateFlow(mutableListOf<Recipe>())}

    fun setKeywordFilter(input: String) {
        keywordFilter.value = input
    }

    fun setCatsFilter(input: List<CharSequence>) {
        catsFilter.value = input.map { "$it" }.toMutableList()
    }

    fun setRecipes(list: List<Recipe>) {
        recipesTemp.value = list.toMutableList()
    }

    fun selectAll(list: MutableList<Material>): List<Material> {
        return list.apply {
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

    fun getAllItems(i: Int?): List<Material> {
        val list = when(i) {
            0 -> dbMaterial.selectAllRawMaterials()
            1 -> dbMaterial.selectAllRefinedMaterials()
            else -> dbMaterial.selectAll()
        }
        return selectAll(list.executeAsList().toMutableList())
    }

    fun getAllRawMaterials(): List<Material> {
        return dbMaterial
            .selectAllRawMaterials()
            .executeAsList()
    }

    fun getAllUnits(): List<String> {
        return dbMaterial
            .selectAllUnits()
            .executeAsList()
            .map { "${it.unit}" }
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