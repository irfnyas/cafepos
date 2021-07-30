package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.TABLE_INVENTORY
import co.wangun.cafepos.App.Companion.TABLE_MATERIAL
import co.wangun.cafepos.App.Companion.TABLE_ORDER
import co.wangun.cafepos.App.Companion.TABLE_PAYMENT
import co.wangun.cafepos.App.Companion.TABLE_PRINTER
import co.wangun.cafepos.App.Companion.TABLE_PRODUCT
import co.wangun.cafepos.App.Companion.TABLE_RECIPE
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.R
import co.wangun.cafepos.util.SessionUtils.Companion.Currency_STR
import cowanguncafepos.*

@Suppress("MemberVisibilityCanBePrivate")
class MainViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }
    val dbMaterial by lazy { db.materialQueries }
    val dbRecipe by lazy { db.recipeQueries }
    val dbProduct by lazy { db.productQueries }
    val dbPayment by lazy { db.paymentQueries }
    val dbPrinter by lazy { db.printerQueries }
    val dbOrder by lazy { db.orderQueries }
    val dbInventory by lazy { db.inventoryQueries }

    fun setDefValue() {
        listOf(
            Triple(su.get(Currency_STR) as String, Currency_STR, cxt.getString(R.string.currency))
        ).forEach {
            if(it.first.isBlank()) su.set(it.second, it.third)
        }

        dbPayment.insert(Payment(1, "Cash", "Default Payment", null))

        // debug
        //dbRecipe.delete(1)
    }

    fun invoiceInReceipt(tableInput: String, forReceipt: Boolean): String {
        val itemSplit = tableInput.split("?")
        val num = "#${itemSplit[0]}"
        val date = itemSplit[1].replace("-","")
        val time = itemSplit[2].replace(":","")
        val static = "Customer Number"
        val inv = "$num$date$time"

        val totalLen = 40
        val staticLen = static.length
        val invLen = inv.length
        val space = " ".repeat(totalLen - staticLen - invLen)

        return if(forReceipt) "$static$space$inv" else inv
    }

    fun tableInReceipt(tableInput: String): String {
        val itemSplit = tableInput.split("?")
        val num = itemSplit[0]
        val date = itemSplit[1]
        val time = itemSplit[2]

        val totalLength = 40
        val table = "Table $num"
        val dateTime = "$date $time"
        val space = " ".repeat(totalLength - table.length - dateTime.length)

        return "$table$space$dateTime"
    }

    fun totalInReceipt(tableInput: String): String {
        val itemSplit = tableInput.split("?")
        val num = itemSplit[0].toLong()
        val date = itemSplit[1]
        val time = itemSplit[2]
        val static = "Total"

        val totalPrice =
            db.orderQueries.selectAllByTableAndDateTime(num, date, time)
                .executeAsList().map {
                    it.price?.times(it.amount ?: 0) ?: 0.0
                }.sum()

        val price = fu.toLocale(totalPrice, true)

        val totalLen = 27
        val staticLen = static.length
        val priceLen = price.length
        val space = " ".repeat(totalLen - staticLen - priceLen)
        return "$static$space$price"
    }

    fun itemInReceipt(nameInput: String, amountInput: Long, priceInput: Double): String {
        var name = nameInput
        val amount = "$amountInput  "
        val price = "    ${fu.toLocale(priceInput * amountInput, true)}"

        val totalLen = 40
        val nameLen = name.length
        val amountLen = amount.length
        val priceLen = price.length

        if (nameLen + amountLen + priceLen > totalLen) {
            val dif = totalLen - (nameLen + amountLen + priceLen)
            name = name.dropLast(dif)
        }

        val space = " ".repeat(totalLen - nameLen - amountLen - priceLen)
        return "${amount}${name}${space}${price}"
    }

    fun getPrinters(): List<Printer> {
        return db.printerQueries.selectAll().executeAsList().sortedBy { it.name }
    }

    fun getDetailOrders(tableInput: String): List<String> {
        val itemSplit = tableInput.split("?")
        val num = itemSplit[0].toLong()
        val date = itemSplit[1]
        val time = itemSplit[2]

        return db.orderQueries
            .selectAllByTableAndDateTime(num, date, time)
            .executeAsList().map {
                itemInReceipt(
                    it.name ?: "",
                    it.amount ?: 0,
                    it.price ?: 0.0
                )
            }
    }

    fun postItem(item: Any, subs: List<Any> = emptyList()) {
        when(item) {
            is Material -> dbMaterial.transactionWithResult {
                // update temp recipes parent
                getParentRecipes(item.name).forEach {
                    deleteItem(it.id, TABLE_RECIPE)
                }

                subs.forEach {
                    it as Recipe
                    postItem(Recipe(
                        it.id, it.name,
                        it.mass, it.unit,
                        item.name
                    ))
                }

                // db insert
                dbMaterial.insert(item); null
            }
            is Recipe -> dbRecipe.transactionWithResult {
                dbRecipe.insert(item); null
            }
            is Product -> dbProduct.transactionWithResult {
                dbProduct.insert(item); null
            }
            is Payment -> dbPayment.transactionWithResult{
                dbPayment.insert(item); null
            }
            is Inventory -> dbInventory.transactionWithResult {
                dbInventory.insert(item); null
            }
            else -> null
        }
    }

    fun idIncrement(table: String): Long {
        return when(table) {
            TABLE_INVENTORY -> dbInventory.lastId().executeAsOneOrNull()?.max ?: 0
            TABLE_MATERIAL -> dbMaterial.lastId().executeAsOneOrNull()?.max ?: 0
            TABLE_PRODUCT -> dbProduct.lastId().executeAsOneOrNull()?.max ?: 0
            TABLE_PAYMENT -> dbPayment.lastId().executeAsOneOrNull()?.max ?: 0
            TABLE_PRINTER -> dbPrinter.lastId().executeAsOneOrNull()?.max ?: 0
            TABLE_RECIPE -> dbRecipe.lastId().executeAsOneOrNull()?.max ?: 0
            TABLE_ORDER -> dbOrder.lastId().executeAsOneOrNull()?.max ?: 0
            else -> 0
        }.plus(1)
    }

    fun countTable(table: String): Int {
        return when(table) {
            TABLE_INVENTORY -> dbInventory.count().executeAsOne()
            TABLE_MATERIAL -> dbMaterial.count().executeAsOne()
            TABLE_PRODUCT -> dbProduct.count().executeAsOne()
            TABLE_PAYMENT -> dbPayment.count().executeAsOne()
            TABLE_PRINTER -> dbPrinter.count().executeAsOne()
            TABLE_RECIPE -> dbRecipe.count().executeAsOne()
            TABLE_ORDER -> dbOrder.count().executeAsOne()
            else -> 0
        }.toInt()
    }

    fun getAllCategories(table: String): List<String> {
        return when(table) {
            TABLE_MATERIAL -> dbMaterial
                .selectAllCategories()
                .executeAsList()
                .map { "${it.category}" }
            TABLE_PRODUCT -> dbProduct
                .selectAllCategories()
                .executeAsList()
                .map { "${it.category}" }
            else -> emptyList()
        }
    }

    fun deleteItem(id: Long, table: String, name: String = "") {
        fun delRecipe() = getParentRecipes(name).forEach {
            deleteItem(it.id, TABLE_RECIPE)
        }

        when(table) {
            TABLE_PRODUCT -> dbProduct.transactionWithResult {
                dbProduct.delete(id)
                delRecipe(); null
            }
            TABLE_MATERIAL -> dbMaterial.transactionWithResult {
                dbMaterial.delete(id)
                delRecipe(); null
            }
            TABLE_RECIPE -> dbRecipe.transactionWithResult {
                dbRecipe.delete(id); null
            }
            TABLE_PAYMENT -> dbPayment.transactionWithResult {
                dbPayment.delete(id); null
            }
            else -> null
        }
    }

    fun isItemListed(value: List<String>, table: String): Boolean {
        return when(table) {
            TABLE_PAYMENT -> dbPayment.find(value[0]).executeAsOneOrNull() != null
            TABLE_MATERIAL -> dbMaterial.find(value[0]).executeAsOneOrNull() != null
            TABLE_RECIPE -> dbRecipe.find(value[0], value[1]).executeAsOneOrNull() != null
            else -> false
        }
    }

    fun getParentRecipes(parent: String): MutableList<Recipe> {
        return dbRecipe.selectAllParentRecipes(parent).executeAsList().toMutableList()
    }
}