package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.TABLE_PRINTER
import co.wangun.cafepos.App.Companion.cu
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.du
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUserNick_STR
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUser_STR
import co.wangun.cafepos.util.SessionUtils.Companion.TablesAmount_INT
import cowanguncafepos.Printer
import cowanguncafepos.User

@Suppress("MemberVisibilityCanBePrivate")
class HomeViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }

    fun getTodayOrderForTable(num: Int): List<String> {
        return db.orderQueries
            .selectAllTodayForTable(num.toLong(), du.dateYmd())
            .executeAsList().mapIndexed { _, item ->
                "Order ${item.invoice} - Created at ${item.time}"
            }
    }

    fun getTablesAmount(): Int {
        val amount = su.get(TablesAmount_INT) as Int
        return if (amount == 0) {
            su.set(TablesAmount_INT, 0); 0
        } else amount
    }

    fun putTablesAmount(input: Int) {
        su.set(TablesAmount_INT, input.toString().toInt())
    }

    fun getNick(): String {
        return "${su.get(LoggedInUserNick_STR)}"
    }

    fun putPass(newPass: String) {
        val name = "${su.get(LoggedInUser_STR)}"
        val user = db.userQueries.find(name).executeAsOneOrNull()
        user?.let {
            val newUser = User(
                it.id, it.name,
                cu.encrypt(newPass),
                it.nick, it.role
            )
            db.userQueries.insert(newUser)
        }
    }

    fun isOldPassValid(input: String): Boolean {
        val name = "${su.get(LoggedInUser_STR)}"
        val user = db.userQueries.find(name).executeAsOneOrNull()
        val passDecrypted = cu.decrypt(user?.pass ?: "")
        return input == passDecrypted
    }

    fun putPrinter(name: String, address: String) {
        val printer = db.printerQueries.find(name).executeAsOneOrNull()
        val id = printer?.id ?: MainViewModel().idIncrement(TABLE_PRINTER)
        val newPrinter = Printer(id, name, address)
        db.printerQueries.insert(newPrinter)
    }

    fun delPrinter(id: Long?) {
        id?.let { db.printerQueries.delete(it) }
    }

    fun isPrinterListed(name: String): Boolean {
        return db.printerQueries.find(name).executeAsOneOrNull() != null
    }

    fun logout() {
        su.set(LoggedInUser_STR, "")
        su.set(LoggedInUserNick_STR, "")
    }
}