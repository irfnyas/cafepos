package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
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
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentOrderBinding
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.MenuViewModel
import co.wangun.cafepos.viewmodel.OrderViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import com.skydoves.powerspinner.PowerSpinnerView
import cowanguncafepos.Active_order
import cowanguncafepos.Menu


@SuppressLint("SetTextI18n")
class OrderFragment: Fragment(R.layout.fragment_order) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: OrderViewModel by viewModels()
    private val bind: FragmentOrderBinding by viewBinding()
    private val args: OrderFragmentArgs by navArgs()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        vm.initOrder(args.tableOrder, args.dateOrder, args.timeOrder)
        initFun()
    }

    private fun initFun() {
        initView()
        initBtn()
    }

    private fun initBtn() {
        bind.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnNewItem.setOnClickListener { createItemDialog() }
        }
    }

    private fun initView() {
        setTitle()
        setRecycler()
        setPrice()
        setInvoice()
    }

    private fun setTitle() {
        val tableInput = "${args.tableOrder}?${args.dateOrder}?${args.timeOrder}"
        bind.textTitleOrder.text = avm.invoiceInReceipt(tableInput, false)
    }

    private fun setInvoice() {
        val tableInput = "#${args.tableOrder}" +
                args.dateOrder.replace("-","") +
                args.timeOrder.replace(":","")
        bind.textInvoiceOrder.text = tableInput
    }

    private fun setRecycler() {
        val source = vm.getAllOrders()
        Adapter.builder(viewLifecycleOwner)
                .addSource(Source.fromList(source))
                .addPresenter(Presenter.simple(cxt, R.layout.item_order, 0)
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
                            val newAmount = amountText.text.toString().toLong() - 1
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
                            createNoteDialog(item)
                        }
                    }
                })
                .into(bind.rvOrders)
    }

    private fun createRemoveDialog(item: Active_order) {
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Remove ${item.name}?")
            cornerRadius(24f)
            negativeButton(text = "Back")
            positiveButton(text = "Remove") { vm.delOrder(item.id); initView() }
        }
    }

    private fun createNoteDialog(item: Active_order) {
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Note for Item")
            cornerRadius(24f)
            cancelable(false)
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")

            input(
                    hint = "Input the note or leave it empty...",
                    prefill = "${item.note}", allowEmpty = true
            ) { _, input -> putItem(item, item.amount ?: 0,"$input", true) }

            getInputField().apply {
                gravity = Gravity.CENTER
                post { selectAll() }
                setBackgroundColor(resources.getColor(
                        android.R.color.transparent, null
                ))
            }
        }
    }

    private fun putItem(item: Active_order, amount: Long, note: String?, refresh: Boolean) {
        vm.postOrder(
                Active_order(
                        item.id, item.name, amount, item.price,
                        note, item.num, item.date, item.time
                )
        )
        if(refresh) initView()
    }

    private fun setPrice() {
        var totalPrice = 0.0
        vm.getAllOrders().forEach { totalPrice += (it.price ?: 0.0) * (it.amount ?: 1) }
        bind.textTotalOrder.text = avm.withCurrency(totalPrice)
    }

    private fun createItemDialog() {
        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "New Item")
            noAutoDismiss()
            cancelable(false)
            customView(R.layout.dialog_order, scrollable = true, horizontalPadding = true)
            cornerRadius(24f)

            val allItems = vm.getAllMenu()
            val allOrders = vm.getAllOrders()
            val search = view.findViewById<TextInputEditText>(R.id.fld_item_order)
            val spinner = view.findViewById<PowerSpinnerView>(R.id.spinner_items)

            spinner.setItems(allItems.map { it.name })
            search.doAfterTextChanged {
                if (it.isNullOrBlank()) spinner.setItems(allItems.map { item -> item.name })
                else {
                    val list = allItems.filter { item -> item.name.contains(it) }
                    spinner.setItems(list.map { item -> item.name })

                    if (list.isNotEmpty()) spinner.selectItemByIndex(0)
                    else spinner.apply { text = ""; hint = "No item found" }
                }
            }

            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = "Add") {
                val chosen = allItems.find { item -> item.name == spinner.text }
                val isAdded = allOrders.find { item -> item.name == spinner.text }
                if(chosen == null || isAdded != null) {
                    spinner.setHintTextColor(ContextCompat.getColor(cxt, R.color.red_900))
                    if(isAdded != null)
                        spinner.apply { hint = "${chosen?.name} is already added"; text = "" }
                } else { prePostOrder(chosen); dismiss() }
            }
        }
    }

    private fun prePostOrder(chosen: Menu?) {
        val order = Active_order(vm.countOrder() + 1, chosen?.name, 1,
                chosen?.price ?: 0.0, "", args.tableOrder.toLong(),
                args.dateOrder, args.timeOrder )
        vm.postOrder(order)
        initView()
    }
}