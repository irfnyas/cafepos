package co.wangun.cafepos

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import co.wangun.cafepos.db.DbClient.sqlDriver
import co.wangun.cafepos.util.CryptUtils
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.util.SessionUtils
import co.wangun.cafepos.view.MainActivity
import com.facebook.stetho.Stetho
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
        Stetho.initializeWithDefaults(this)
        isDebug = true
    }

    companion object {
        lateinit var cxt: MainActivity
        var isDebug by Delegates.notNull<Boolean>()
        val db by lazy { Database(sqlDriver) }
        val cu by lazy { CryptUtils() }
        val fu by lazy { FunUtils() }
        val su by lazy { SessionUtils() }
    }

    //
}