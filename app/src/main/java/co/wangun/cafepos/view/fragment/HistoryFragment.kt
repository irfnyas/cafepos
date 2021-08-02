package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.du
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogReceiptBinding
import co.wangun.cafepos.databinding.FragmentHistoryBinding
import co.wangun.cafepos.databinding.ItemReceiptBinding
import co.wangun.cafepos.databinding.ItemTableBinding
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.viewmodel.HistoryViewModel
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.datetime.datePicker
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import pl.kremblewski.android.simplerecyclerviewadapter.adapter
import java.util.*

@SuppressLint("SetTextI18n")
class HistoryFragment: Fragment(R.layout.fragment_history) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: HistoryViewModel by viewModels()
    private val vb: FragmentHistoryBinding by viewBinding()

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
        vb.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnDate.setOnClickListener { startDateDialog() }
            btnFind.setOnClickListener { findFilterDialog() }
        }
    }

    private fun initView() {
        initHeader()
        initRecycler()

        vm.keywordFilter.asLiveData().observe(viewLifecycleOwner) {
            vb.btnFind.text =
                if (it.isNotBlank()) "\"$it\""
                else "All Transactions"
        }
        vm.dateFilter.asLiveData().observe(viewLifecycleOwner) {
            val split = it.split("_")
            val start = split[0]
            val end = split[1]

            vb.btnDate.text = when {
                it == vm.getTodayDateRange() -> "Today"
                start == end -> "\"$start\""
                else -> "\"${it.replace("_", " - ")}\""
            }
        }
    }

    private fun initHeader() {
        listOf("Invoice", "Table", "Date", "Payment").let { str ->
            vb.rvHeader
                .run { listOf(text1, text2, text3, text4) }
                .forEachIndexed { index, v -> v.text = str[index] }
        }
    }

    private fun initRecycler() {
        val list = vm.selectAll()
        val items = list.mapIndexed { index, item ->
            FunUtils.Items(index, item)
        }

        vb.rvMain.apply {
            adapter = adapter {
                register { bind: ItemTableBinding, item: FunUtils.Items, _ ->
                    val it = item.item as HistoryViewModel.Transaction

                    bind.btnAct.apply {
                        setOnClickListener { _ -> detailDialog(it) }
                        setImageResource(R.drawable.ic_baseline_receipt_long_24)
                    }
                    listOf(
                        it.invoice,
                        "Table ${it.table}",
                        it.dateTime,
                        it.payment
                    ).let { str ->
                        bind.run { listOf(text1, text2, text3, text4) }
                            .forEachIndexed { index, v -> v.text = str[index] }
                    }
                }
            }.apply { submitList(items) }

            // helper
            GravitySnapHelper(Gravity.TOP).attachToRecyclerView(this)
            vb.layEmpty.root.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // dialog
    //
    private fun detailDialog(item: HistoryViewModel.Transaction) {
        val bind = DialogReceiptBinding.inflate(LayoutInflater.from(requireContext()))

        val list = item.products.map {
            avm.itemInReceipt(it.first, it.second, it.third)
        }

        fun initRvItemsReceipt() {
            val items = list.mapIndexed { index, item ->
                FunUtils.Items(index, item)
            }

            bind.rvItemsReceipt.apply {
                adapter = adapter {
                    register { bind: ItemReceiptBinding, item: FunUtils.Items, _ ->
                        bind.root.text = item.item as String
                    }
                }.apply { submitList(items) }

                // helper
                GravitySnapHelper(Gravity.TOP).attachToRecyclerView(this)
            }
        }

        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            customView(
                view = bind.root,
                scrollable = true,
                dialogWrapContent = true
            )
            bind.apply {
                // set text
                textInvoiceReceipt.text = avm.invoiceInReceipt(item.invoice)
                textTableReceipt.text = avm.headerInReceipt(item.table, item.dateTime)
                textTotalReceipt.text = avm.totalInReceipt(vm.productsPriceSum(item))

                // set recycler
                initRvItemsReceipt()

                // set dialog btn
                negativeButton(text = "Back")
                positiveButton(text = "Print") {
                    createPrintDialog(item)
                }
            }
        }
    }

    private fun createPrintDialog(item: HistoryViewModel.Transaction) {
        val list = avm.getPrinters().map { "${it.name} - ${it.address}" }
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Select Printer")
            negativeButton(text = "Back")
            positiveButton(text = "Print")
            listItemsSingleChoice(items = list) { _, _, text ->
                val printer = text.split(" - ")[1]
                fu.print(printer, item.invoice)
            }
            if (list.isEmpty()) message(text = getString(R.string.msg_list_empty))
        }
    }

    private fun findFilterDialog() {
        val isAlreadyFiltered = vm.keywordFilter.value.isNotBlank()
        val preFill = if (isAlreadyFiltered) vm.keywordFilter.value else ""

        fun setFilter(input: String) {
            vm.setKeywordFilter(input)
            initRecycler()
        }

        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Keyword Filter")
            cornerRadius(24f)

            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            if (isAlreadyFiltered) neutralButton(text = "Remove") { setFilter("") }

            input(
                hint = "Input the keyword you are looking for...",
                prefill = preFill
            ) { _, input -> setFilter("$input") }
            getInputField().apply {
                gravity = Gravity.CENTER
                post { selectAll() }
                setBackgroundColor(
                    resources.getColor(
                        android.R.color.transparent, null
                    )
                )
            }
        }
    }

    private fun startDateDialog(startCal: Calendar? = Calendar.getInstance()) {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Date Filter")
            message(text = "Select start date")
            datePicker(
                currentDate = startCal,
                maxDate = Calendar.getInstance(),
            ) { _, date ->
                val dateDmy = du.getYmdFromCal(date)
                val nowDmy = du.dateYmd()
                if (dateDmy != nowDmy) endDateDialog(date)
                else vm.setDateFilter()
            }
            positiveButton(text = "Confirm")
            negativeButton(text = "Back") { dismiss() }

            // show filter today if not filtered by today
            if (vb.btnDate.text.contains("-")) {
                neutralButton(text = "Set Today") {
                    vm.setDateFilter()
                    initRecycler()
                    dismiss()
                }
            }
        }
    }

    private fun endDateDialog(startDate: Calendar) {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Date Filter")
            message(text = "Start from ${du.getDmyFromCal(startDate)} until...")
            datePicker(maxDate = Calendar.getInstance(), minDate = startDate) { _, date ->
                vm.setDateFilter(
                    du.getYmdFromCal(startDate),
                    du.getYmdFromCal(date)
                )
                initRecycler()
            }
            negativeButton(text = "Back") {
                startDateDialog(startDate)
            }
            positiveButton(text = "Confirm")
        }
    }
}