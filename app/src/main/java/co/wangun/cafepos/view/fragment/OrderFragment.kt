package co.wangun.cafepos.view.fragment

import android.os.Bundle
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentOrderBinding
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.button.MaterialButton
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source


class OrderFragment: Fragment(R.layout.fragment_order) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val binding: FragmentOrderBinding by viewBinding()
    val args: OrderFragmentArgs by navArgs()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    private fun initFun() {
        initRecycler()
        initView()
    }

    private fun initView() {
        val titleText = "Table ${args.numTable}"
        binding.textTitle.text = titleText
    }

    private fun initRecycler() {
        val data = IntRange(1, 24).toList()

        Adapter.builder(viewLifecycleOwner)
            .addSource(Source.fromList(data))
            .addPresenter(
                Presenter.simple(
                    cxt, R.layout.item_table, 0
                ) { view, num: Int -> view as MaterialButton
                    view.apply {
                        text = "$num"
                        setOnClickListener { createTableDialog(num) }
                    }
                })
            .into(binding.rvTables)
    }

    private fun createTableDialog(num: Int) {
        val data = listOf("Order 1 - Created at 20:35", "Order 2 - Created at 20:34", "Order 3 - Created at 20:33")
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Table $num - ${avm.getTodayDate()}")
            listItems(items = data, waitForPositiveButton = false) { dialog, _, text ->
                Toast.makeText(cxt, "$text", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            negativeButton(text = "Back")
            positiveButton(text = "New Order")
        }
    }
}