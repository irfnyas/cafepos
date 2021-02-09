package co.wangun.cafepos.view.fragment

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentHomeBinding
import co.wangun.cafepos.util.SessionUtils
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUserId_LON
import co.wangun.cafepos.util.SessionUtils.Companion.TablesAmount_INT
import co.wangun.cafepos.viewmodel.HomeViewModel
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.button.MaterialButton
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source


class HomeFragment: Fragment(R.layout.fragment_home) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: HomeViewModel by viewModels()
    private val bind: FragmentHomeBinding by viewBinding()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    private fun initFun() {
        val userId = (SessionUtils().get(LoggedInUserId_LON) ?: -1) as Long
        if(userId < 0) navigateToLoginFragment() else {
            initRecycler()
            initBtn()
        }
    }

    private fun initBtn() {
        bind.apply {
            btnEditTables.setOnClickListener { createTablesDialog() }
            btnEditMenu.setOnClickListener { navigateToMenuFragment() }
            btnOrderHistory.setOnClickListener { navigateToHistoryFragment() }
        }
    }

    private fun navigateToHistoryFragment() {
        val action = R.id.action_homeFragment_to_historyFragment
        findNavController().navigate(action)
    }

    private fun navigateToMenuFragment() {
        val action = R.id.action_homeFragment_to_menuFragment
        findNavController().navigate(action)
    }

    private fun navigateToLoginFragment() {
        val action = R.id.action_homeFragment_to_loginFragment
        findNavController().navigate(action)
    }

    private fun navigateToOrderFragment(num: Int, date: String, time: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToOrderFragment(num, time, date)
        findNavController().navigate(action)
    }

    private fun createTablesDialog() {
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            cancelable(false)
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            title(text = "Tables Amount")
            input(
                    hint = "Input the amount...",
                    prefill = "${su.get(TablesAmount_INT)}",
                    inputType = InputType.TYPE_CLASS_NUMBER
            ) { _, input -> putTablesAmount(input.toString().toInt()) }
            getInputField().apply {
                gravity = Gravity.CENTER
                post { selectAll() }
                setBackgroundColor(resources.getColor(
                        android.R.color.transparent, null
                ))
            }
        }
    }

    private fun putTablesAmount(input: Int) {
        vm.putTablesAmount(input)
        initRecycler()
    }

    private fun initRecycler() {
        // init val
        val list = IntRange(1, vm.getTablesAmount()).toList()
        val presenter = Presenter.simple(
                cxt, R.layout.item_table, 0
        ) { view, item: Int ->
            (view as MaterialButton).apply {
                text = "$item"
                setOnClickListener {
                    createOrderDialog(item)
                }
            }
        }

        // set adapter
        Adapter.builder(viewLifecycleOwner)
                .addSource(Source.fromList(list))
                .addPresenter(presenter)
                .into(bind.rvTables)
    }

    private fun createOrderDialog(num: Int) {
        // init val
        val list = vm.getTodayOrderForTable(num).sortedDescending()

        // create dialog
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Table $num (${vm.getTodayDate()})")
            listItems(items = list, waitForPositiveButton = false) { _, _, text ->
                navigateToOrderFragment(num, vm.getTodayDateDb(), vm.parseTime("$text"))
            }

            negativeButton(text = "Back")
            positiveButton(text = "New Order") {
                navigateToOrderFragment(num, vm.getTodayDateDb(), vm.getTime())
            }
        }
    }
}