package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import co.wangun.cafepos.App
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentMenuBinding
import co.wangun.cafepos.databinding.FragmentOrderBinding
import co.wangun.cafepos.databinding.ItemMenuBinding
import co.wangun.cafepos.util.SessionUtils
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import com.otaliastudios.elements.pagers.PageSizePager
import com.skydoves.powerspinner.PowerSpinnerView
import cowanguncafepos.Menu

@SuppressLint("SetTextI18n")
class MenuFragment: Fragment(R.layout.fragment_menu) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val bind: FragmentMenuBinding by viewBinding()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    private fun initFun() {
        initBtn()
        initView()
    }

    private fun initBtn() {
        bind.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnNewMenu.setOnClickListener { createMenuDialog(null) }
        }
    }

    private fun initView() {
        initRecycler()
    }

    private fun createMenuDialog(menu: Menu?) {
        val isNew = menu == null
        val titleDialog = if (isNew) "New Menu" else "Edit ${menu?.name}"
        val positiveText = if (isNew) "Add" else "Confirm"
        val categories = avm.getAllCategories()

        MaterialDialog(cxt).show {
            lifecycleOwner(viewLifecycleOwner)
            cancelable(false)
            title(text = titleDialog)
            noAutoDismiss()
            customView(R.layout.dialog_menu, scrollable = true, horizontalPadding = true)
            cornerRadius(24f)
            if (menu != null) {
                neutralButton(text = "Remove") {
                    avm.deleteMenu(menu.id)
                    dismiss()
                    initRecycler()
                }
            }
            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = positiveText) {
                val id = menu?.id ?: avm.countMenu()+1
                val cat = view.findViewById<PowerSpinnerView>(R.id.spinner_categories)
                val name = view.findViewById<TextInputEditText>(R.id.fld_name_menu)
                val desc = view.findViewById<TextInputEditText>(R.id.fld_desc_menu)
                val price = view.findViewById<TextInputEditText>(R.id.fld_price_menu)
                        .text.toString().toDouble()
                val layName = findViewById<TextInputLayout>(R.id.lay_name_menu)
                val newMenu = Menu(id, "${name.text}",
                        "${desc.text}", "${cat.text}", price
                )

                if (avm.isMenuFormValid(newMenu, isNew)) {
                    avm.postMenu(newMenu)
                    dismiss()
                    initRecycler()
                } else {
                    if(name.text.isNullOrBlank()) {
                        layName.error = "Name must not be empty"
                    }

                    if(isNew && avm.isMenuListed("$name")) {
                        layName.error = "$name is already on the menu"
                    }

                    if(cat.text.isNullOrBlank()) {
                        cat.setHintTextColor(ContextCompat.getColor(context, R.color.red_900))
                    }
                }
            }

            view.apply {
                val spinnerCats = findViewById<PowerSpinnerView>(R.id.spinner_categories)
                val btnNewCat = findViewById<FloatingActionButton>(R.id.btn_new_category)
                val fldName = findViewById<TextInputEditText>(R.id.fld_name_menu)
                val fldDesc = findViewById<TextInputEditText>(R.id.fld_desc_menu)
                val fldPrice = findViewById<TextInputEditText>(R.id.fld_price_menu)

                spinnerCats.apply {
                    lifecycleOwner = viewLifecycleOwner
                    setIsFocusable(false)
                    setItems(categories)
                    setOnSpinnerItemSelectedListener<Any> { _, _, _, _ ->
                        spinnerCats.setHintTextColor(null)
                    }
                    if(isNew) hint = "Choose category..."
                    else selectItemByIndex(categories.indexOf(menu?.category))
                }

                btnNewCat.setOnClickListener {
                    fun putCategories(input: String) {
                        spinnerCats.apply {
                            if(!categories.contains(input)) text = input
                            else selectItemByIndex(categories.indexOf(input))
                        }
                    }

                    MaterialDialog(cxt).show {
                        lifecycleOwner(viewLifecycleOwner)
                        cancelable(false)
                        negativeButton(text = "Back")
                        positiveButton(text = "Confirm")
                        title(text = "Add Category")
                        input(hint = "Input the name of new category...") {
                                _, input -> putCategories("$input")
                        }
                        getInputField().apply {
                            gravity = Gravity.CENTER
                            setBackgroundColor(
                                resources.getColor(android.R.color.transparent, null)
                            )
                        }
                    }

                }

                fldName.setText(menu?.name)
                fldDesc.setText(menu?.desc)
                fldPrice.setText("${menu?.price ?: 0.0}")
            }
        }
    }

    private fun initRecycler() {
        Adapter.builder(viewLifecycleOwner)
            .addSource(Source.fromList(avm.getAllMenus()))
            .addPresenter(Presenter.simple(cxt, R.layout.item_menu, 0)
            { view, menu: Menu ->
                view.apply {
                    // init view
                    val viewName = findViewById<AppCompatTextView>(R.id.text_name_menu)
                    val viewDesc = findViewById<AppCompatTextView>(R.id.text_desc_menu)
                    val viewPrice = findViewById<AppCompatTextView>(R.id.text_price_menu)
                    val viewEdit = findViewById<FloatingActionButton>(R.id.btn_edit_menu)

                    // set view
                    viewName.text = menu.name
                    viewDesc.text = "(${menu.category}) ${menu.desc}"
                    viewPrice.text = avm.withCurrency(menu.price ?: 0.0)
                    viewEdit.setOnClickListener { createMenuDialog(menu) }
                }
            })
            .into(bind.rvMenus)
    }
}