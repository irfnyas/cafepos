package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogOrderBinding
import co.wangun.cafepos.databinding.FragmentOrderBinding
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUserNick_STR
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.OrderViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import cowanguncafepos.Active_order


@SuppressLint("SetTextI18n")
class OrderFragment: Fragment(R.layout.fragment_order) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: OrderViewModel by viewModels()
    private val bind: FragmentOrderBinding by viewBinding()
    private val args: OrderFragmentArgs by navArgs()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        vm.initOrder(args.tableOrder, args.dateOrder, args.timeOrder)
        initFun()
    }

    // init
    //
    private fun initFun() {
        initView()
        initBtn()
    }

    private fun initBtn() {
        bind.apply {
            btnBack.setOnClickListener { createSaveDialog() }
            btnNewItem.setOnClickListener { createItemDialog() }
            btnPrint.setOnClickListener { createPrintDialog() }
        }
    }

    private fun initView() {
        setTitle()
        setRecycler()
        setPrice()
        setInvoice()
        setPayment()
    }

    // set view
    //
    private fun setTitle() {
        bind.textTitleOrder.text =
                "Table ${args.tableOrder} - ${avm.getTodayDate()} - ${args.timeOrder}"
    }

    private fun setInvoice() {
        val tableInput = "#${args.tableOrder}" +
                args.dateOrder.replace("-","") +
                args.timeOrder.replace(":","")
        bind.textInvoiceOrder.text = tableInput
    }

    private fun setPayment() {
        if(vm.ordersTemp.isNotEmpty() && vm.ordersTemp[0].payment != "cash") {
           bind.radioCard.isChecked = true
        }
    }

    private fun setRecycler() {
        val source = vm.ordersTemp
        val presenter = Presenter.simple(requireContext(), R.layout.item_order, 0)
        { view, item: Active_order ->
            view.apply {
                // init view
                val nameText = findViewById<AppCompatTextView>(R.id.text_name_order)
                val amountText = findViewById<AppCompatTextView>(R.id.text_amount_order)
                val priceText = findViewById<AppCompatTextView>(R.id.text_price_order)
                val noteText = findViewById<AppCompatTextView>(R.id.text_note_order)
                val minBtn = findViewById<FloatingActionButton>(R.id.btn_min_order)
                val plusBtn = findViewById<FloatingActionButton>(R.id.btn_plus_order)
                val noteBtn = findViewById<FloatingActionButton>(R.id.btn_note_order)

                // init inside fun
                fun putPriceRecycler() {
                    val amount = amountText.text.toString().toInt()
                    val price = item.price?.times(amount) ?: 0.0
                    priceText.text = avm.withCurrency(price)
                    setPrice()
                }

                // set view
                nameText.text = item.name
                amountText.text = "${item.amount}"
                if (item.note.isNullOrBlank()) noteText.visibility = View.GONE
                else noteText.text = item.note
                putPriceRecycler()

                // init inside btn
                minBtn.setOnClickListener {
                    val newAmount = "${amountText.text}".toLong() - 1
                    if (newAmount > 0) {
                        amountText.text = "$newAmount"
                        putItem(item, newAmount, item.note, false)
                        putPriceRecycler()
                    } else createRemoveDialog(item)
                }

                plusBtn.setOnClickListener {
                    val newAmount = amountText.text.toString().toLong() + 1
                    val newPrice = item.price?.times(newAmount) ?: 0.0

                    amountText.text = "$newAmount"
                    priceText.text = avm.withCurrency(newPrice)
                    putItem(item, newAmount, item.note, false)
                    putPriceRecycler()
                }

                noteBtn.setOnClickListener {
                    createNoteDialog(item, "${amountText.text}")
                }
            }
        }

        // build adapter
        Adapter.builder(viewLifecycleOwner)
                .addSource(Source.fromList(source))
                .addPresenter(presenter)
                .into(bind.rvOrders)
    }

    // dialog
    //
    private fun createRemoveDialog(item: Active_order) {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Remove ${item.name} from order?")
            cornerRadius(24f)
            negativeButton(text = "Back")
            positiveButton(text = "Remove") {
                vm.delItemTemp(item)
                initView()
            }
        }
    }

    private fun createNoteDialog(item: Active_order, amount: String) {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Note for Item")
            cornerRadius(24f)
            cancelable(false)
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            input(
                    hint = "Input the note or leave it empty...",
                    prefill = "${item.note}", allowEmpty = true
            ) { _, input -> putItem(item, amount.toLong(), "$input", true) }
            getInputField().apply {
                gravity = Gravity.CENTER
                post { selectAll() }
                setBackgroundColor(resources.getColor(
                        android.R.color.transparent, null
                ))
            }
        }
    }

    private fun createItemDialog() {
        val binding = DialogOrderBinding.inflate(LayoutInflater.from(requireContext()))
        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            lifecycleOwner(viewLifecycleOwner)
            title(text = "New Item")
            cancelable(false)
            cornerRadius(24f)
            customView(
                    view = binding.root,
                    scrollable = true,
                    horizontalPadding = true
            )
            binding.apply {
                // get data
                val allItems = vm.getAllMenu()
                val allOrders = vm.getAllOrders()

                // set spinner
                spinnerItems.setItems(allItems.map { it.name })
                editItemOrder.doAfterTextChanged {
                    if (it.isNullOrBlank()) {
                        // show all items if input is blank
                        spinnerItems.setItems(allItems.map { item -> item.name })
                    } else {
                        // create list filtered by input
                        val list = allItems.filter { item -> item.name.contains(it) }
                        spinnerItems.setItems(list.map { item -> item.name })

                        // set chosen item from the first row
                        if (list.isNotEmpty()) spinnerItems.selectItemByIndex(0)
                        else spinnerItems.apply { text = ""; hint = "No item found" }
                    }
                }

                // dialog btn
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") {
                    val chosen = allItems.find { item -> item.name == spinnerItems.text }
                    val isAdded = allOrders.find { item -> item.name == spinnerItems.text }
                    if(chosen == null || isAdded != null) {
                        spinnerItems.apply {
                            setHintTextColor(
                                    ContextCompat.getColor(requireContext(), R.color.red_900)
                            )
                            if(isAdded != null) {
                                hint = "${chosen?.name} is already added"
                                text = ""
                            }
                        }
                    } else {
                        val order = Active_order(
                                vm.countOrder() + 1, chosen.name, 1,
                                chosen.price ?: 0.0, "",
                                vm.tableOrder, vm.dateOrder, vm.timeOrder,
                                "${su.get(LoggedInUserNick_STR)}",
                                vm.getPayment(bind.radioCash.isChecked)
                        )
                        putItem(order, 1, "", true)
                        dismiss()
                    }
                }
            }
        }
    }

    private fun createPrintDialog() {
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
                postOrder(printer)
            }
            if (list.isEmpty()) {
                message(text = getString(R.string.msg_list_empty))
            }
        }
    }

    private fun createSaveDialog() {
        if(vm.isDirty) {
            MaterialDialog(requireContext()).show {
                lifecycleOwner(viewLifecycleOwner)
                cornerRadius(24f)
                title(text = "Save Order?")
                message(text = "It is not recommended to save this order without printing the receipt.")
                negativeButton(text = "Don't Save") { backToHome() }
                positiveButton(text = "Save") { postOrder(null) }
                neutralButton(text = "Back")
            }
        } else backToHome()
    }

    // etc
    //
    private fun printOrder(printer: String) {
        val invoice = "${bind.textInvoiceOrder.text}"
        val total = "${bind.textTotalOrder.text}"
        val tableInput = "${args.tableOrder}?${args.dateOrder}?${args.timeOrder}"
        val items = avm.getDetailOrders(tableInput)
        fu.print(printer, invoice, total, items)
        backToHome()
    }

    private fun backToHome() {
        findNavController().popBackStack()
    }

    private fun setPrice() {
        var totalPrice = 0.0
        vm.ordersTemp.forEach { totalPrice += (it.price ?: 0.0) * (it.amount ?: 1) }
        bind.textTotalOrder.text = avm.withCurrency(totalPrice)
    }

    private fun postOrder(printer: String?) {
        // del old order
        vm.getAllOrders().forEach { vm.delOrder(it.id) }

        // post new order
        vm.ordersTemp.map {
            Active_order(
                    it.id, it.name, it.amount, it.price,it.note,
                    it.num, it.date, it.time, it.creator,
                    vm.getPayment(bind.radioCash.isChecked)
            )
        }.forEach { vm.postOrder(it) }

        // print order
        if(!printer.isNullOrBlank()) printOrder(printer)
        else backToHome()
    }

    private fun putItem(item: Active_order, amount: Long, note: String?, refresh: Boolean) {
        vm.postItemTemp(
                Active_order(
                        item.id, item.name, amount, item.price, note,
                        args.tableOrder.toLong(), args.dateOrder, args.timeOrder,
                        "${su.get(LoggedInUserNick_STR)}", "cash"
                )
        )
        if(refresh) initView()
    }
}