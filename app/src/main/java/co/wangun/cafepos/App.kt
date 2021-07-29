package co.wangun.cafepos

import android.app.Application
import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import co.wangun.cafepos.db.DbClient.sqlDriver
import co.wangun.cafepos.util.CryptUtils
import co.wangun.cafepos.util.DateUtils
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.util.SessionUtils
import co.wangun.cafepos.view.MainActivity
import kotlin.properties.Delegates

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        initFun()
    }

    private fun initFun() {
        // init day/night mode
        setDefaultNightMode(MODE_NIGHT_NO)

        // init debug
        if (applicationInfo.flags and FLAG_DEBUGGABLE != 0) initDebug()
    }

    private fun initDebug() {
        // fu.resetDb()
        isDebug = true
    }

    companion object {
        lateinit var cxt: MainActivity
        var isDebug by Delegates.notNull<Boolean>()
        val db by lazy { Database(sqlDriver) }
        val cu by lazy { CryptUtils() }
        val du by lazy { DateUtils() }
        val fu by lazy { FunUtils() }
        val su by lazy { SessionUtils() }

        const val TABLE_PRODUCT = "TABLE_PRODUCT"
        const val TABLE_MATERIAL = "TABLE_MATERIAL"
        const val TABLE_RECIPE = "TABLE_RECIPE"
        const val TABLE_PAYMENT = "TABLE_PAYMENT"
        const val TABLE_PRINTER = "TABLE_PRINTER"
        const val TABLE_ORDER = "TABLE_ORDER"
        const val TABLE_INVENTORY = "TABLE_INVENTORY"
    }

    /*
     TODO
        /
        fix history page
        order history filter date try same date
        finance page
        printer page
        /
        reformat datetime in order model
        account fragment+vm
        report to csv
        db to google sheet
        merge print text
     */
}