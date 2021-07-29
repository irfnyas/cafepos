package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.Gravity.TOP
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.viewbinding.library.fragment.viewBinding
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import co.wangun.cafepos.App.Companion.TABLE_ORDER
import co.wangun.cafepos.App.Companion.TABLE_PRODUCT
import co.wangun.cafepos.App.Companion.du
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogOrderBinding
import co.wangun.cafepos.databinding.FragmentOrderBinding
import co.wangun.cafepos.databinding.ItemOrderBinding
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.util.SessionUtils.Companion.LoggedInUserNick_STR
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.OrderViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import cowanguncafepos.Active_order
import pl.kremblewski.android.simplerecyclerviewadapter.Adapter
import pl.kremblewski.android.simplerecyclerviewadapter.adapter


@SuppressLint("SetTextI18n")
class OrderFragment: Fragment(R.layout.fragment_order) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: OrderViewModel by viewModels()
    private val vb: FragmentOrderBinding by viewBinding()
    private val args: OrderFragmentArgs by navArgs()
    private lateinit var mainAdapter: Adapter

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        vm.initOrder(args.tableOrder, args.dateOrder, args.timeOrder)
        initFun()
    }

    // init
    //
    private fun initFun() {
        initView(false)
        initBtn()
    }

    private fun initBtn() {
        vb.apply {
            btnBack.setOnClickListener { createSaveDialog() }
            btnNewItem.setOnClickListener {
                if(avm.countTable(TABLE_PRODUCT) != 0) createItemDialog()
                else Toast.makeText(
                    requireContext(),
                    getString(R.string.msg_product_empty),
                    Toast.LENGTH_LONG).show()
            }
            btnPrint.setOnClickListener { createPrintDialog() }
            btnPrintAdv.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Printing advance receipt...",
                    Toast.LENGTH_SHORT).show()
            }

            // view
            btnNewItem.alpha = if(avm.countTable(TABLE_PRODUCT) != 0) 1f else 0.25f
        }
    }

    private fun initView(submitOnly: Boolean = true) {
        setTitle()
        setRecycler(submitOnly)
        setPrice()
        setInvoice()
        setPayment()
    }

    // set view
    //
    private fun setTitle() {
        val num = if(args.tableOrder == 0) "Closed Bill" else args.tableOrder
        vb.textTitleOrder.text = "Table $num - ${du.getTodayDate()} - ${args.timeOrder}"
    }

    private fun setInvoice() {
        val invoice =
            if(vm.ordersTemp.isNotEmpty()) vm.ordersTemp[0].invoice
            else vm.invoiced(
                args.tableOrder.toLong(),
                args.dateOrder.replace("-",""),
                args.timeOrder.replace(":",""))
        vb.textInvoiceOrder.text = invoice
    }

    private fun setPayment() {
        vb.editPayment.apply {
            isEnabled = false
            threshold = 99
            setTextColor(getColor(
                requireContext(),
                R.color.grey_900))
            setAdapter(ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                vm.selectAllPayments()
            ))
            setText("${adapter.getItem(0)}")
            doAfterTextChanged { clearFocus() }
        }
    }

    private fun setRecycler(submitOnly: Boolean = true) {
        // init val
        val list = vm.ordersTemp
        val items = list.mapIndexed { index, item ->
            FunUtils.Items(index, item)
        }

        if(!submitOnly) {
            // init adapter
            mainAdapter = adapter {
                register { bind: ItemOrderBinding, item: FunUtils.Items, _ ->
                    val it = item.item as Active_order

                    fun putPriceRecycler() {
                        val amount = bind.textAmountOrder.text.toString().toInt()
                        val price = it.price?.times(amount) ?: 0.0
                        bind.textPriceOrder.text = fu.toLocale(price)
                        setPrice()
                    }

                    bind.textNameOrder.text = it.name
                    bind.textAmountOrder.text = "${it.amount}"
                    if (it.note.isNullOrBlank()) bind.textNoteOrder.visibility = GONE
                    else bind.textNoteOrder.text = it.note

                    bind.btnMinOrder.setOnClickListener { _ ->
                        val newAmount = "${bind.textAmountOrder.text}".toLong() - 1
                        if (newAmount > 0) {
                            bind.textAmountOrder.text = "$newAmount"
                            putItem(it, newAmount, it.note, false)
                            putPriceRecycler()
                        } else createRemoveDialog(it)
                    }

                    bind.btnPlusOrder.setOnClickListener { _ ->
                        val newAmount = bind.textAmountOrder.text.toString().toLong() + 1
                        val newPrice = it.price?.times(newAmount) ?: 0.0

                        bind.textAmountOrder.text = "$newAmount"
                        bind.textPriceOrder.text = fu.toLocale(newPrice, true)
                        putItem(it, newAmount, it.note, false)
                        putPriceRecycler()
                    }

                    bind.btnNoteOrder.setOnClickListener { _ ->
                        createNoteDialog(it, "${bind.textAmountOrder.text}")
                    }

                    putPriceRecycler()
                }
            }

            // set adapter
            vb.rvMain.adapter = mainAdapter

            // helper
            GravitySnapHelper(TOP).attachToRecyclerView(vb.rvMain)
        }

        // submit
        mainAdapter.submitList(items)

        // empty layout
        vb.layEmpty.root.visibility = if (list.isEmpty()) VISIBLE else GONE
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
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            input(
                hint = "Input any note or leave it empty...",
                prefill = "${item.note}", allowEmpty = true
            ) { _, input ->
                putItem(item, amount.toLong(),
                    "${input.trim()}",
                    true)
            }
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
            cornerRadius(24f)
            customView(
                view = binding.root,
                scrollable = true,
                horizontalPadding = true
            )
            binding.apply {
                // reset focus
                root.requestFocus()

                // spinner
                spinnerCategories.apply {
                    root.apply {
                        hint = "Category"
                        isHelperTextEnabled = false
                    }
                    edit.apply {
                        // disabled
                        isEnabled = false
                        setTextColor(getColor(
                            requireContext(),
                            R.color.grey_900))

                        // after text
                        doAfterTextChanged { cat ->
                            vm.updateProductsByCats("$cat")
                        }

                        // set text
                        setText(vm.allCats[0])

                        // set adapter
                        setAdapter(ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            vm.allCats
                        ))
                    }
                }

                spinnerProducts.apply {
                    root.apply {
                        hint = "Chosen Item"
                        isHelperTextEnabled = false
                    }
                    edit.apply {
                        isEnabled = false
                        setTextColor(getColor(
                            requireContext(),
                            R.color.grey_900))
                    }
                }

                editItemOrder.doAfterTextChanged {
                    vm.allProducts.value.filter { item ->
                        item.name.contains("$it")
                    }.apply {
                        if (isNotEmpty()) spinnerProducts.edit.setText(this[0].name)
                    }
                }

                // live data
                vm.allProducts.asLiveData().observe(viewLifecycleOwner) {
                    spinnerProducts.edit.apply {
                        setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                it.map { product -> product.name }
                            )
                        )
                    }
                }

                // dialog btn
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") {
                    val chosen = vm.allProducts.value.find { item ->
                        item.name == "${spinnerProducts.edit.text}"
                    }
                    val isListed = vm.ordersTemp.find { item ->
                        item.name == "${spinnerProducts.edit.text}"
                    }
                    if(chosen == null || isListed != null) {
                        spinnerProducts.root.error =
                            if(isListed != null) getString(R.string.edit_duplicated)
                            else getString(R.string.edit_empty)
                    } else {
                        putItem(
                            Active_order(
                                vm.ordersTemp.size.toLong(),
                                chosen.name, 1,
                                chosen.price ?: 0.0, "",
                                vm.tableOrder, vm.dateOrder, vm.timeOrder,
                                "${su.get(LoggedInUserNick_STR)}",
                                "${vb.editPayment.text}",
                                "${vb.textInvoiceOrder.text}"
                            ), 1, "", true,
                        )
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
                message(text = getString(R.string.msg_save_warning))
                negativeButton(text = "Don't Save") { backToHome() }
                positiveButton(text = "Save") { postOrder(null) }
                neutralButton(text = "Back")
            }
        } else backToHome()
    }

    // etc
    //
    private fun printOrder(printer: String) {
        val invoice = "${vb.textInvoiceOrder.text}"
        fu.print(printer, invoice)
        backToHome()
    }

    private fun backToHome() {
        findNavController().popBackStack()
    }

    private fun setPrice() {
        var totalPrice = 0.0
        vm.ordersTemp.forEach { totalPrice += (it.price ?: 0.0) * (it.amount ?: 1) }
        vb.textTotalOrder.text = fu.toLocale(totalPrice, true)
    }

    private fun postOrder(printer: String?) {
        // del old order
        vm.selectAllThisInvoice().forEach { vm.delOrder(it.id) }

        // post new order
        val lastId = avm.idIncrement(TABLE_ORDER)
        vm.ordersTemp.mapIndexed { index, it ->
            Active_order(
                lastId + index, it.name, it.amount, it.price,it.note,
                it.num, it.date, it.time, it.creator,
                "${vb.editPayment.text}", it.invoice
            )
        }.forEach {
            vm.postOrder(it)
        }

        // print order
        if(!printer.isNullOrBlank()) printOrder(printer)
        else backToHome()
    }

    private fun putItem(item: Active_order, amount: Long, note: String?, refresh: Boolean) {
        vm.postItemTemp(
            Active_order(
                item.id, item.name, amount, item.price, note,
                args.tableOrder.toLong(), args.dateOrder, args.timeOrder,
                "${su.get(LoggedInUserNick_STR)}", "?", item.invoice
            )
        )
        if(refresh) initView()
    }
}