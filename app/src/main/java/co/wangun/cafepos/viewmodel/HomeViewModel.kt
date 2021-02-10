package co.wangun.cafepos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.cu
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUserNick_STR
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUser_STR
import co.wangun.cafepos.util.SessionUtils.Companion.TablesAmount_INT
import cowanguncafepos.User
import java.text.SimpleDateFormat
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class HomeViewModel: ViewModel() {

    private val TAG by lazy { javaClass.simpleName }

    fun getTodayOrderForTable(num: Int): List<String> {
        return db.orderQueries.selectAllTodayForTable(num.toLong(), getTodayDateDb())
            .executeAsList().mapIndexed { i, item -> "Order ${ i + 1 } - Created at ${item.time}" }
    }

    fun getTablesAmount(): Int {
        val amount = su.get(TablesAmount_INT) as Int
        return if (amount == 0) {
            su.set(TablesAmount_INT, 1); 1
        } else amount
    }

    fun parseTime(text: String): String {
        return text.split(" ").last()
    }

    fun putTablesAmount(input: Int) {
        su.set(TablesAmount_INT, input.toString().toInt())
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.ROOT).format(Date())
    }

    fun getTodayDateDb(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    }

    fun getTime(): String {
        return SimpleDateFormat("HH:mm", Locale.ROOT).format(Date())
    }

    fun getNick(): String {
        return "${su.get(LoggedInUserNick_STR)}"
    }

    fun putPass(newPass: String) {
        val name = "${su.get(LoggedInUser_STR)}"
        val user = db.userQueries.find(name).executeAsOneOrNull()
        if(user != null) {
            val newUser = User(user.id, user.name, cu.encrypt(newPass), user.nick, user.role)
            db.userQueries.insert(newUser)
        }
    }

    fun isOldPassValid(input: String): Boolean {
        val name = "${su.get(LoggedInUser_STR)}"
        val user = db.userQueries.find(name).executeAsOneOrNull()
        val passDecrypted = cu.decrypt(user?.pass ?: "")
        return input == passDecrypted
    }

    fun logout() {
        su.set(LoggedInUser_STR, "")
        su.set(LoggedInUserNick_STR, "")
    }
}