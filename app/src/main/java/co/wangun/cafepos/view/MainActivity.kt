package co.wangun.cafepos.view

import android.os.Bundle
import android.viewbinding.library.activity.viewBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.ActivityMainBinding
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner

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

    private fun createExitDialog() {
        MaterialDialog(this).show {
            lifecycleOwner(this@MainActivity)
            cornerRadius(24f)
            title(text = "Confirm Exit")
            message(text = "Are you sure you want to exit ${getString(R.string.app_name)}?")
            positiveButton(text = "Exit") { finishAfterTransition() }
            negativeButton(text = "Back")
        }
    }

    override fun onBackPressed() {
        when (findNavController(R.id.nav_host).currentDestination?.label) {
            "fragment_home" -> { createExitDialog() }
            "fragment_login" -> { createExitDialog() }
            else -> super.onBackPressed()
        }
    }
}