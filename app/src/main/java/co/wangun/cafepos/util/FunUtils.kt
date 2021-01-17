package co.wangun.cafepos.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
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
}