package co.wangun.cafepos.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import co.wangun.cafepos.App.Companion.cxt
import java.text.SimpleDateFormat
import java.util.*


class FunUtils {

    fun hideKeyboard(view: View) {
        val imm = cxt.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}