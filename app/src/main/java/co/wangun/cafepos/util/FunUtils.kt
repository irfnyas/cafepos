package co.wangun.cafepos.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.R
import co.wangun.cafepos.db.DbClient.sqlDriver
import co.wangun.cafepos.util.SessionUtils.Companion.Currency_STR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.kremblewski.android.simplerecyclerviewadapter.AdapterItem
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue


@Suppress("MemberVisibilityCanBePrivate")
class FunUtils {

    data class Items(
        override val id: Int,
        val item: Any
    ) : AdapterItem<Int>

    fun runCS(catching: Any?, success: Any?, view: View) {
        CoroutineScope(Main).launch {
            hideKeyboard(view)
            withContext(IO) { catching }
            delay(1000)
            withContext(Main) { success }
        }
    }

    fun hideKeyboard(view: View) {
        Handler(Looper.getMainLooper()).postDelayed( {
            val imm = cxt.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }, 500)
    }

    fun resetDb() {
        sqlDriver.close()
        cxt.deleteDatabase("${cxt.getString(R.string.app_name)}.db")
    }

    fun toLocale(num: Double?, isCurrency: Boolean = false): String {
        val sym = "${su.get(Currency_STR)} "
        var cur = "%.2f".format(num).toDouble()
        if(cur == 0.0) cur = cur.absoluteValue
        val curr = NumberFormat.getNumberInstance(Locale.ITALIAN).format(cur)
        return if(isCurrency) "$sym $curr" else curr
    }

    fun calColumns(size: Int): Int {
        val displayMetrics: DisplayMetrics = cxt.resources.displayMetrics
        val dpWidth = displayMetrics.heightPixels / displayMetrics.density
        return (dpWidth / size).toInt()
    }

    fun print(printer: String, invoice: String) {
        Toast.makeText(cxt, "Printing $invoice in $printer...", Toast.LENGTH_SHORT).show()
    }
}