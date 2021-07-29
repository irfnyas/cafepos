package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.TABLE_PAYMENT
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogPaymentBinding
import co.wangun.cafepos.databinding.FragmentPaymentBinding
import co.wangun.cafepos.databinding.ItemCardBinding
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.PaymentViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.wajahatkarim3.easyvalidation.core.view_ktx.nonEmpty
import cowanguncafepos.Payment
import pl.kremblewski.android.simplerecyclerviewadapter.adapter

@SuppressLint("SetTextI18n")
class PaymentFragment: Fragment(R.layout.fragment_payment) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: PaymentViewModel by viewModels()
    private val vb: FragmentPaymentBinding by viewBinding()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    private fun initFun() {
        initBtn()
        initRecycler()
        initView()
    }

    private fun initView() {
        vm.keywordFilter.asLiveData().observe(viewLifecycleOwner) {
            vb.btnFind.text = if(it.isNullOrBlank()) "All Payments" else "\"$it\""
        }
    }

    private fun initBtn() {
        vb.btnBack.setOnClickListener { findNavController().popBackStack() }
        vb.btnNew.setOnClickListener { itemDialog(null) }
        vb.btnFind.setOnClickListener { findFilterDialog() }
        vb.btnCurrency.setOnClickListener { currencyDialog() }
    }

    private fun initRecycler() {
        val list = vm.selectAll()
        val items = list.mapIndexed { index, item ->
            FunUtils.Items(index, item)
        }

        vb.rvMain.apply {
            adapter = adapter {
                register { bind: ItemCardBinding, item: FunUtils.Items, _ ->
                    val it = item.item as Payment
                    bind.textName.text = it.name

                    bind.textDesc.apply {
                        text = it.desc
                        minLines = 1
                        maxLines = 1
                    }

                    bind.btnEdit.apply {
                        visibility = if(it.id != 1L) VISIBLE else INVISIBLE
                        setOnClickListener { _ -> itemDialog(it) }
                    }
                }
            }.apply { submitList(items) }

            // helper
            GravitySnapHelper(Gravity.TOP).attachToRecyclerView(this)
            vb.layEmpty.root.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun itemDialog(item: Payment?) {
        val isNew = item == null
        val titleDialog = if(isNew) "New Payment" else "Edit ${item?.name}"
        val bind = DialogPaymentBinding.inflate(LayoutInflater.from(requireContext()))

        fun isFormValid(): Boolean {
            bind.apply {
                var err = 0

                listOf(layName).forEach { v ->
                    v.isErrorEnabled = false
                    v.editText?.nonEmpty {
                        v.error = getString(R.string.edit_empty)
                        err++
                    }
                }

                if(isNew && avm.isItemListed(listOf("${editName.text}"), TABLE_PAYMENT)) {
                    layName.error = getString(R.string.edit_duplicated)
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
            customView(
                view = bind.root,
                scrollable = true,
                horizontalPadding = true
            )

            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = "Confirm") {
                if(isFormValid()) {
                    fu.runCS(avm.postItem(Payment(
                        item?.id ?: avm.idIncrement(TABLE_PAYMENT),
                        "${bind.editName.text?.trim()?.take(99)}",
                        "${bind.editDesc.text?.trim()}",
                        null
                    )), initRecycler(), bind.root)
                    dismiss()
                }
            }
            if(!isNew) neutralButton(text = "Remove") {
                fu.runCS(
                    avm.deleteItem(item?.id ?: -1, TABLE_PAYMENT),
                    initRecycler(), bind.root)
                dismiss()
            }

            bind.apply {
                root.requestFocus()
                layName.apply {
                    isEnabled = isNew
                    editText?.setText(item?.name)
                    if(isNew) helperText = getString(R.string.edit_once)
                }
                editDesc.setText(item?.desc)
            }
        }
    }

    private fun currencyDialog() {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            title(text = "Edit Currency")
            message(text = "What currency symbol do you want to use?")
            input(
                hint = "Input the currency symbol...",
                prefill = vm.getCurrency()
            ) { _, input -> vm.setCurrency("$input") }
            getInputField().apply {
                gravity = CENTER
                post { selectAll() }
                setBackgroundColor(resources.getColor(
                    android.R.color.transparent, null
                ))
            }
        }
    }

    private fun findFilterDialog() {
        val isAlreadyFiltered = vm.keywordFilter.value.isNotBlank()
        val preFill = if(isAlreadyFiltered) vm.keywordFilter.value else ""

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
            if(isAlreadyFiltered) neutralButton(text = "Remove") { setFilter("") }


            input(
                hint = "Input the keyword you are looking for...",
                prefill = preFill
            ) { _, input -> setFilter("$input") }
            getInputField().apply {
                gravity = CENTER
                post { selectAll() }
                setBackgroundColor(resources.getColor(
                    android.R.color.transparent, null
                ))
            }
        }
    }
}