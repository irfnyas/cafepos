package co.wangun.cafepos.db

import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.Database
import co.wangun.cafepos.R
import com.squareup.sqldelight.android.AndroidSqliteDriver

object DbClient {
    val sqlDriver: AndroidSqliteDriver by lazy {
        AndroidSqliteDriver(
            Database.Schema, cxt, "${cxt.getString(R.string.app_name)}.db"
        )
    }
}