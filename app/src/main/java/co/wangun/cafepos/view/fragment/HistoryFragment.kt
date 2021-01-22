package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentHistoryBinding
import co.wangun.cafepos.viewmodel.HistoryViewModel
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source

@SuppressLint("SetTextI18n")
class HistoryFragment: Fragment(R.layout.fragment_history) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: HistoryViewModel by viewModels()
    private val bind: FragmentHistoryBinding by viewBinding()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    private fun initFun() {
        initRecycler()
        initBtn()
    }

    private fun initBtn() {
        bind.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
        }
    }

    private fun initRecycler() {
        val source = vm.getAllOrders().sortedDescending()
        Adapter.builder(viewLifecycleOwner)
                .addSource(Source.fromList(source))
                .addPresenter(Presenter.simple(cxt, R.layout.item_history, 0)
                { view, item: String -> view.apply {
                    // init val
                    val itemSplit = item.split("?")
                    val num = itemSplit[0]
                    val date = itemSplit[1]
                    val time = itemSplit[2]

                    // init view
                    val invText = findViewById<AppCompatTextView>(R.id.text_invoice_history)
                    val tableText = findViewById<AppCompatTextView>(R.id.text_table_history)
                    val dateText = findViewById<AppCompatTextView>(R.id.text_date_history)
                    val timeText = findViewById<AppCompatTextView>(R.id.text_time_history)
                    val detailBtn = findViewById<FloatingActionButton>(R.id.btn_detail_history)

                    // set view
                    invText.text = avm.invoiceInReceipt(item,false)
                    tableText.text = "Table $num"
                    dateText.text = date
                    timeText.text = time
                    detailBtn.setOnClickListener { createDetailDialog(item) }
                } })
                .into(bind.rvHistories)
    }

    private fun createDetailDialog(tableInput: String) {
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            customView(
                    R.layout.dialog_receipt,
                    horizontalPadding = true,
                    scrollable = true,
                    dialogWrapContent = true
            )
            cornerRadius(24f)
            negativeButton(text = "Back")
            positiveButton(text = "Print")

            view.apply {
                // init view
                val invoiceText = findViewById<AppCompatTextView>(R.id.text_invoice_receipt)
                val tableText = findViewById<AppCompatTextView>(R.id.text_table_receipt)
                val totalText = findViewById<AppCompatTextView>(R.id.text_total_receipt)
                val rvItems = findViewById<RecyclerView>(R.id.rv_items_receipt)

                // set text
                invoiceText.text = avm.invoiceInReceipt(tableInput, true)
                tableText.text = avm.tableInReceipt(tableInput)
                totalText.text = avm.totalInReceipt(tableInput)

                // set recycler
                val source = vm.getDetailOrders(tableInput)
                Adapter.builder(viewLifecycleOwner)
                        .addSource(Source.fromList(source))
                        .addPresenter(Presenter.simple(cxt, R.layout.item_receipt, 0
                        ) { view, item: String -> (view as AppCompatTextView).text = item })
                        .into(rvItems)
            }
        }
    }
}