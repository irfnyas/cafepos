package co.wangun.cafepos.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.cu
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUserId_LON
import cowanguncafepos.User

@Suppress("MemberVisibilityCanBePrivate")
class LoginViewModel: ViewModel() {

    private val TAG: String by lazy { javaClass.simpleName }

    init {
        val users = db.userQueries.selectAll().executeAsList()
        if(users.isEmpty()) {
            db.userQueries.insert(
                    User(
                            0,
                            "master_admin",
                            cu.encrypt("123456"),
                            "Owner"
                    )
            )
        }
    }

    fun findUser(user: String): User? {
        return db.userQueries.find(user).executeAsOneOrNull()
    }

    fun isLoginValid(name: String, pass: String): Boolean {
        // init val
        var valid = false

        // find user in db
        val foundUser = findUser(name)
        val foundPass = foundUser?.pass ?: ""
        val isPassValid = cu.decrypt(foundPass) == pass
        if (foundUser != null && isPassValid) valid = true

        // save session
        if(valid) su.set(LoggedInUserId_LON, foundUser?.id ?: 0)

        // return valid
        return valid
    }
}