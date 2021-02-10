package co.wangun.cafepos.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.R
import co.wangun.cafepos.db.DbClient.sqlDriver


class FunUtils {

    fun hideKeyboard(view: View) {
        val imm = cxt.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun resetDb() {
        sqlDriver.close()
        cxt.deleteDatabase("${cxt.getString(R.string.app_name)}.db")
    }

    fun calColumns(size: Int): Int {
        val displayMetrics: DisplayMetrics = cxt.resources.displayMetrics
        val dpWidth = displayMetrics.heightPixels / displayMetrics.density
        return (dpWidth / size).toInt()
    }

    fun print(printer: String, invoice: String, table: String, total: String, items: List<String>) {
        Toast.makeText(cxt, "Printing $invoice in $printer...", Toast.LENGTH_SHORT).show()
    }
}