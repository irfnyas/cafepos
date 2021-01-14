package co.wangun.cafepos

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.util.SessionUtils

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        initFun()
    }

    private fun initFun() {
        // init val
        cxt = this
        fu = FunUtils()
        su = SessionUtils()

        // init day/night mode
        setDefaultNightMode(MODE_NIGHT_NO)

        // init debug
        if (applicationInfo.flags and FLAG_DEBUGGABLE != 0) initDebug()
    }

    private fun initDebug() {
        //
    }

    companion object {
        lateinit var cxt: Context
        lateinit var fu: FunUtils
        lateinit var su: SessionUtils
    }

}