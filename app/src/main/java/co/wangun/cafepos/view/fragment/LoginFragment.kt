package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.cu
import co.wangun.cafepos.App.Companion.isDebug
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentLoginBinding
import co.wangun.cafepos.viewmodel.LoginViewModel

@SuppressLint("SetTextI18n")
class LoginFragment: Fragment(R.layout.fragment_login) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val vm: LoginViewModel by viewModels()
    private val bind: FragmentLoginBinding by viewBinding()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    private fun initFun() {
        initBtn()
    }

    private fun initBtn() {
        bind.apply {
            imgLogo.setOnClickListener {
                if(isDebug) {
                    editUser.setText("master_admin")
                    editPass.setText("123456")
                }
            }

            btnLogin.setOnClickListener {
                val user = "${editUser.text}"
                val pass = "${editPass.text}"
                if(checkValid(user, pass)) navToHome()
            }
        }
    }

    private fun checkValid(user: String, pass: String): Boolean {
        // init val
        var valid = true

        // check field not empty
        bind.layUser.error = if(user.isBlank()) {
            valid = false; getString(R.string.login_empty)
        } else ""

        bind.layPass.error = if(pass.isBlank()) {
            valid = false; getString(R.string.login_empty)
        } else ""

        // check credential
        if(valid) valid = vm.isLoginValid(user, pass)

        // throw error if credential not found
        if(!valid) {
            bind.layUser.error = getString(R.string.login_invalid)
            bind.layPass.error = " "
        }

        // return valid
        return valid
    }

    private fun navToHome() {
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }
}