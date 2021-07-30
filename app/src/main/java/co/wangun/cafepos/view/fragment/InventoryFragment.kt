package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity.CENTER
import android.view.Gravity.TOP
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.viewbinding.library.fragment.viewBinding
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.TABLE_INVENTORY
import co.wangun.cafepos.App.Companion.du
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogInventoryBinding
import co.wangun.cafepos.databinding.FragmentInventoryBinding
import co.wangun.cafepos.databinding.ItemTableBinding
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.viewmodel.InventoryViewModel
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.datetime.dateTimePicker
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.wajahatkarim3.easyvalidation.core.view_ktx.nonEmpty
import cowanguncafepos.Inventory
import pl.kremblewski.android.simplerecyclerviewadapter.Adapter
import pl.kremblewski.android.simplerecyclerviewadapter.adapter

@SuppressLint("SetTextI18n")
class InventoryFragment: Fragment(R.layout.fragment_inventory) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: InventoryViewModel by viewModels()
    private val vb: FragmentInventoryBinding by viewBinding()
    private lateinit var mainAdapter: Adapter

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    // init
    //
    private fun initFun() {
        initBtn()
        initTab()
    }

    private fun initTab() {
        // init
        initView(0)

        // listener
        vb.tab.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                initView( tab?.position ?: 0)
            }
        })
    }

    private fun initView(i: Int) {
        initHeaderList(i)
        initRecycler(false)

        vm.keywordFilter.asLiveData().observe(viewLifecycleOwner) {
            vb.btnFind.text = if (it.isNullOrBlank()) "All Materials" else "\"$it\""
        }

        vm.catsFilter.asLiveData().observe(viewLifecycleOwner) {
            vb.btnCats.text = if (it.isEmpty()) "All Categories" else {
                when (it.size) {
                    1 -> it[0]
                    2 -> "${it[0]} and ${it[1]}"
                    else -> "${it[0]} and ${it.size - 1} others"
                }
            }
        }
    }

    private fun initHeaderList(i: Int) {
        when(i) {
            0 -> listOf("Material", "Acquired", "Reduced", "Actual")
            1 -> listOf("Material", "Acquired", "Price (outcome)", "Date")
            2 -> listOf("Material", "Reduced", "Price (outcome)", "Date")
            else -> listOf("?", "?", "?", "?")
        }.let { str ->
            vb.rvHeader
                .run { listOf(text1, text2, text3, text4) }
                .forEachIndexed { index, v -> v.text = str[index] }
        }
    }

    private fun initBtn() {
        vb.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnFind.setOnClickListener { findFilterDialog() }
            btnCats.setOnClickListener { catsFilterDialog() }
            btnNew.setOnClickListener {
                if(vm.materials.isNotEmpty()) inventoryDialog()
                else Toast.makeText(
                    requireContext(),
                    getString(R.string.msg_material_empty),
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initRecycler(submitOnly: Boolean = true) {
        val list = vm.getAllItems(vb.tab.selectedTabPosition)
        val items = list.mapIndexed { index, item ->
            FunUtils.Items(index, item)
        }

        if (!submitOnly) {
            // init adapter
            mainAdapter = adapter {
                register { bind: ItemTableBinding, item: FunUtils.Items, _ ->
                    val it = item.item
                    bind.btnAct.setOnClickListener { _ ->
                        when (it) {
                            is InventoryViewModel.Joined -> inventoryDialog(it)
                            is InventoryViewModel.Summary -> summaryDialog(it)
                        }
                    }
                    when (vb.tab.selectedTabPosition) {
                        1 -> {
                            it as InventoryViewModel.Joined
                            listOf(
                                "(${it.category}) ${it.name}",
                                "${it.mass} ${it.unit}",
                                fu.toLocale(it.price?.times(-1)),
                                it.datetime?.dropLast(7)
                            )
                        }
                        2 -> {
                            it as InventoryViewModel.Joined
                            listOf(
                                "(${it.category}) ${it.name}",
                                "${it.mass?.times(-1)} ${it.unit}",
                                fu.toLocale(it.price?.times(-1)),
                                it.datetime?.dropLast(7)
                            )
                        }
                        else -> {
                            it as InventoryViewModel.Summary
                            listOf(
                                "(${it.category}) ${it.name}",
                                "${it.acquiredMass} ${it.unit}",
                                "${it.usedMass} ${it.unit}",
                                "${it.actualMass} ${it.unit}",
                            )
                        }
                    }.let { str ->
                        bind.run { listOf(text1, text2, text3, text4) }
                            .forEachIndexed { index, v -> v.text = str[index] }
                    }
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
    private fun summaryDialog(it: InventoryViewModel.Summary) {
        Toast.makeText(requireContext(), "Detail of ${it.name} summary", Toast.LENGTH_SHORT).show()
    }

    private fun inventoryDialog(item: InventoryViewModel.Joined? = null) {
        val isNew = item == null
        val titleDialog = if (isNew) "New Inventory Modifier" else "Edit Inventory Modifier"
        val bind = DialogInventoryBinding.inflate(LayoutInflater.from(requireContext()))

        fun isFormValid(): Boolean {
            bind.apply {
                var err = 0

                listOf(layName.root, layMass, layUnit, layPrice, layDate).forEach { v ->
                    v.isErrorEnabled = false
                    v.editText?.nonEmpty {
                        v.error = getString(R.string.edit_empty)
                        err++
                    }
                }

                if(
                    vm.materials.find { it.name == "${layName.root.editText?.text }" } == null
                    && layName.root.editText?.text?.isNotBlank() == true
                ) {
                    layName.root.error = getString(R.string.edit_not_exist)
                    err++
                }

                if(
                    "${layMass.editText?.text}".toDouble() == 0.0
                    && layMass.editText?.text?.isNotBlank() == true
                ) {
                    layMass.error = getString(R.string.edit_invalid)
                    err++
                }

                return err == 0
            }
        }

        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            lifecycleOwner(viewLifecycleOwner)
            title(text = titleDialog)
            cornerRadius(24f)
            customView(view = bind.root,
                scrollable = true,
                horizontalPadding = true)

            bind.apply {
                // dialog btn
                if (item != null) {
                    neutralButton(text = "Remove") {
                        fu.runCS(
                            avm.deleteItem(item.id, TABLE_INVENTORY),
                            initRecycler(), root)
                        dismiss()
                    }
                }
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") {
                    if(isFormValid()) {
                        var massDouble = "${layMass.editText?.text}".toDouble()
                        var priceDouble = "${layPrice.editText?.text}".toDouble()
                        if(!radioAcquire.isChecked) massDouble *= -1
                        if(!switchPrice.isChecked) priceDouble *= -1

                        fu.runCS(
                            avm.postItem(Inventory(
                                item?.id ?: avm.idIncrement(TABLE_INVENTORY),
                                "${layName.root.editText?.text?.trim()}",
                                "${layDesc.editText?.text?.trim()}",
                                massDouble, priceDouble,
                                "${layDate.editText?.text?.trim()}:00.000",
                            )), initRecycler(), root)
                        dismiss()
                    }
                }

                // radio
                radioGroup.children.forEach { it.isEnabled = isNew }
                val index = if (vb.tab.selectedTabPosition == 2) 1 else 0
                val radioChild = radioGroup.getChildAt(index)
                (radioChild as MaterialRadioButton).isChecked = true

                // edit text
                layDesc.editText?.setText(item?.desc)
                layDate.editText?.setText(item?.datetime)
                layMass.editText?.setText("${item?.mass ?: 0.0}")
                layPrice.editText?.setText("${item?.price ?: 0.0}")

                // switch
                switchPrice.setOnCheckedChangeListener { view, isChecked ->
                    view.text = if(isChecked) "As Income" else "As Outcome"
                }

                // date
                layDate.editText?.apply {
                    setText(du.dateTimeYmd().dropLast(3))
                    setOnFocusChangeListener { v, hasFocus ->
                        (v as TextInputEditText).clearFocus()
                        if(hasFocus) MaterialDialog(requireContext()).show {
                            cornerRadius(24f)
                            lifecycleOwner(viewLifecycleOwner)
                            dateTimePicker(
                                autoFlipToTime = true,
                                show24HoursView = true
                            ) { _, cal -> v.setText(du.dateTimeFromCal(cal).dropLast(3)) }
                        }
                    }
                }

                // spinner
                layName.apply {
                    root.hint = "Material"
                    root.isHelperTextEnabled = false
                    edit.setText(item?.name)
                    edit.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            vm.materials.map { it.name }
                        )
                    )
                    edit.doAfterTextChanged {
                        // is material exists
                        val material = vm.materials.find {
                            it.name == "${layName.root.editText?.text }"
                        }
                        layUnit.editText?.setText(if(material != null) material.unit else "")
                    }
                }
            }
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
                gravity = CENTER
                post { selectAll() }
                setBackgroundColor(
                    resources.getColor(
                        android.R.color.transparent, null
                    )
                )
            }
        }
    }

    private fun catsFilterDialog() {
        val cats = vm.getAllCats()
        val indices = vm.getCatsFilterIndices()

        fun setFilter(list: List<CharSequence>) {
            vm.setCatsFilter(list)
            initRecycler()
        }

        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Categories Filter")
            message(text = getString(R.string.msg_categories))

            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            if (vm.hasCatsFilter()) neutralButton(text = "Remove") {
                setFilter(emptyList())
            }

            listItemsMultiChoice(
                items = cats,
                initialSelection = indices
            ) { _, _, list -> setFilter(list) }
        }
    }
}