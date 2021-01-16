package co.wangun.cafepos.view.fragment

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentHomeBinding
import co.wangun.cafepos.util.SessionUtils.Companion.TablesAmount_INT
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
    private val binding: FragmentHomeBinding by viewBinding()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    private fun initFun() {
        initRecycler()
        initBtn()
    }

    private fun initBtn() {
        binding.apply {
            btnEditTables.setOnClickListener { createTablesDialog() }
            btnEditMenu.setOnClickListener { navigateToMenuFragment() }
        }
    }

    private fun navigateToMenuFragment() {
        val action = R.id.action_homeFragment_to_menuFragment
        findNavController().navigate(action)
    }

    private fun navigateToOrderFragment(num: Int) {
        val action = HomeFragmentDirections.actionHomeFragmentToOrderFragment(num)
        findNavController().navigate(action)
    }

    private fun createTablesDialog() {
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
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
        avm.putTablesAmount(input)
        initRecycler()
    }

    private fun initRecycler() {
        val list = IntRange(1, avm.getTablesAmount()).toList()
        Adapter.builder(viewLifecycleOwner)
                .addSource(Source.fromList(list))
                .addPresenter(
                        Presenter.simple(
                                cxt, R.layout.item_table, 0
                        ) { view, num: Int ->
                            view as MaterialButton
                            view.apply {
                                text = "$num"
                                setOnClickListener { createOrderDialog(num) }
                            }
                        })
                .into(binding.rvTables)
    }

    private fun createOrderDialog(num: Int) {
        val data = listOf("Order 1 - Created at 20:35", "Order 2 - Created at 20:34", "Order 3 - Created at 20:33")
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            cancelable(false)
            title(text = "Table $num - ${avm.getTodayDate()}")
            listItems(items = data, waitForPositiveButton = false) {
                _, _, text -> orderClicked("$text") }
            positiveButton(text = "New Order") { navigateToOrderFragment(num) }
            negativeButton(text = "Back")
        }
    }

    private fun orderClicked(text: String) {
        Toast.makeText(cxt, text, Toast.LENGTH_SHORT).show()
    }
}