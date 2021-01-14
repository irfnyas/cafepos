package co.wangun.cafepos.util

import android.content.SharedPreferences
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.util.SessionUtils.Sp.ed
import co.wangun.cafepos.util.SessionUtils.Sp.sp

class SessionUtils {

    // obj
    private object Sp {
        val sp: SharedPreferences = cxt.getSharedPreferences("SP", 0)
        val ed: SharedPreferences.Editor = sp.edit()
    }

    // fun
    fun get(key: String): Any? {
        return when (key.split("_").last()) {
            "STR" -> return getStr(key)
            "INT" -> return getInt(key)
            "BOOL" -> return getBool(key)
            "FLO" -> return getFloat(key)
            "LON" -> return getLong(key)
            else -> null
        }
    }

    fun set(key: String, value: Any) {
        when (key.split("_").last()) {
            "STR" -> setStr(key, value as String)
            "INT" -> setInt(key, value as Int)
            "BOOL" -> setBool(key, value as Boolean)
            "FLO" -> setFloat(key, value as Float)
            "LON" -> setLong(key, value as Long)
        }
    }

    private fun getStr(key: String): String? = sp.getString(key, "")
    private fun getInt(key: String): Int = sp.getInt(key, 0)
    private fun getBool(key: String): Boolean = sp.getBoolean(key, false)
    private fun getFloat(key: String): Float = sp.getFloat(key, 0.0F)
    private fun getLong(key: String): Long = sp.getLong(key, 0)

    private fun setStr(key: String, value: String) = ed.putString(key, value).apply()
    private fun setInt(key: String, value: Int) = ed.putInt(key, value).apply()
    private fun setBool(key: String, value: Boolean) = ed.putBoolean(key, value).apply()
    private fun setFloat(key: String, value: Float) = ed.putFloat(key, value).apply()
    private fun setLong(key: String, value: Long) = ed.putLong(key, value).apply()

    // const
    companion object {
        const val TablesAmount_INT = "TablesAmount_INT"
        const val TablesSpansCount_INT = "TablesSpansCount_INT"
    }
}

/*
  fun get(key: String): String? = sp.getString(key, "")
  fun set(key: String, value: String) = ed.putString(key, value).apply()
  fun clearSession() = ed.clear().apply()

     var token
      get() = sp.getString("token", "")
      set(i) = ed.putString("token", i).apply()
   */