package co.wangun.cafepos.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import androidx.navigation.findNavController
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val bind: ActivityMainBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(bind.root)
        initFun()
    }

    private fun initFun() {
        cxt = this
    }

    override fun onBackPressed() {
        when (findNavController(R.id.nav_host).currentDestination?.label) {
            "HomeFragment" -> { }
            else -> super.onBackPressed()
        }
    }
}