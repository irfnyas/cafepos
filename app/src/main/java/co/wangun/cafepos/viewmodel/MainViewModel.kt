package co.wangun.cafepos.viewmodel

import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.util.SessionUtils.Companion.TablesAmount_INT
import cowanguncafepos.Menu
import java.text.SimpleDateFormat
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class MainViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }

    fun withCurrency(num: Double): String {
        return "£ %.2f".format(num)
    }
}