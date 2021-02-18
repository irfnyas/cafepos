package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.viewbinding.library.fragment.viewBinding
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.wangun.cafepos.App.Companion.du
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogReceiptBinding
import co.wangun.cafepos.databinding.FragmentHistoryBinding
import co.wangun.cafepos.viewmodel.HistoryViewModel
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.date.dayOfMonth
import com.afollestad.date.month
import com.afollestad.date.year
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import java.util.*

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

    // init
    //
    private fun initFun() {
        initBtn()
        initView()
    }

    private fun initBtn() {
        bind.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnDateRange.setOnClickListener { createStartDateDialog() }
        }
    }

    private fun initView() {
        initRecycler(vm.getThisMonthOrders())
        initDateRange()
    }

    private fun initDateRange() {
        bind.btnDateRange.text = vm.getDefaultDateRange()
    }

    private fun initRecycler(list: List<String>) {
        val source = Source.fromList(list)
        val presenter = Presenter.simple(requireContext(), R.layout.item_history, 0)
        { view, item: String ->
            // init val
            val itemSplit = item.split("?")
            val num = itemSplit[0]
            val date = itemSplit[1]
            val time = itemSplit[2]

            // init view
            val invText = view.findViewById<AppCompatTextView>(R.id.text_invoice_history)
            val tableText = view.findViewById<AppCompatTextView>(R.id.text_table_history)
            val dateText = view.findViewById<AppCompatTextView>(R.id.text_date_history)
            val timeText = view.findViewById<AppCompatTextView>(R.id.text_time_history)
            val detailBtn = view.findViewById<FloatingActionButton>(R.id.btn_detail_history)

            // set view
            invText.text = avm.invoiceInReceipt(item,false)
            tableText.text = "Table $num"
            dateText.text = date
            timeText.text = time
            detailBtn.setOnClickListener { createDetailDialog(item) }
        }

        // build adapter
        Adapter.builder(viewLifecycleOwner)
                .addSource(source)
                .addPresenter(presenter)
                .into(bind.rvHistories)

        // if list empty
        bind.layEmpty.root.visibility = if(list.isEmpty()) VISIBLE else GONE
    }

    // dialog
    //
    private fun createDetailDialog(tableInput: String) {
        val binding = DialogReceiptBinding.inflate(LayoutInflater.from(requireContext()))
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            customView(view = binding.root,
                    scrollable = true,
                    dialogWrapContent = true
            )
            binding.apply {
                // set text
                textInvoiceReceipt.text = avm.invoiceInReceipt(tableInput, true)
                textTableReceipt.text = avm.tableInReceipt(tableInput)
                textTotalReceipt.text = avm.totalInReceipt(tableInput)

                // set recycler
                val list = avm.getDetailOrders(tableInput)
                val source = Source.fromList(list)
                val presenter = Presenter.simple(
                        requireContext(), R.layout.item_receipt, 0
                ) { view, item: String -> (view as AppCompatTextView).text = item }
                Adapter.builder(viewLifecycleOwner)
                        .addSource(source)
                        .addPresenter(presenter)
                        .into(rvItemsReceipt)

                // set dialog btn
                negativeButton(text = "Back")
                positiveButton(text = "Print") {
                    createPrintDialog(
                            "${textInvoiceReceipt.text}",
                            "${textTotalReceipt.text}",
                            list
                    )
                }
            }
        }
    }

    private fun createPrintDialog(invoice: String, total: String, items: List<String>) {
        val printers = avm.getPrinters()
        val list = printers.map { "${it.name} - ${it.address}" }

        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Select Printer")
            negativeButton(text = "Back")
            positiveButton(text = "Print")
            listItemsSingleChoice(items = list) { _, _, text ->
                val printer = text.split(" - ")[1]
                fu.print(printer, invoice, total, items)
            }
            if (list.isEmpty()) {
                message(text = getString(R.string.msg_list_empty))
            }
        }
    }

    private fun createStartDateDialog() {
        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Date Filter")
            message(text = "Select start date")
            datePicker(maxDate = Calendar.getInstance()) { _, date ->
                val dateDmy = du.getDmyFromCal(date)
                val nowDmy = du.getDmyFromCal(Calendar.getInstance())
                if (dateDmy != nowDmy) createEndDateDialog(this, date)
                else {
                    filterToday()
                    dismiss()
                }
            }
            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = "Confirm")
        }
    }

    private fun createEndDateDialog(startDialog: MaterialDialog, startDate: Calendar) {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Date Filter")
            message(text = "Start from ${du.getDmyFromCal(startDate)} until...")
            datePicker(maxDate = Calendar.getInstance(), minDate = startDate) { _, date ->
                startDialog.dismiss()
                filterDate(startDate, date)
            }
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
        }
    }

    // etc
    //
    private fun filterToday() {
        // update list
        val start = du.getYmdFromCal(Calendar.getInstance())
        initRecycler(vm.getRangedOrders(start, start))

        // update btn text
        bind.btnDateRange.text = "Today"
    }

    private fun filterDate(startCal: Calendar, endCal: Calendar) {
        //update list
        val start = du.getYmdFromCal(startCal)
        val end = du.getYmdFromCal(endCal)
        initRecycler(vm.getRangedOrders(start, end))

        // update btn tex
        val startStr = du.getDmyFromCal(startCal)
        var endStr = du.getDmyFromCal(endCal)
        val now = du.getDmyFromCal(Calendar.getInstance())
        if(endStr == now) endStr = "Today"
        val btnText = if (startStr == endStr) startStr else "$startStr until $endStr"
        bind.btnDateRange.text = btnText
    }
}