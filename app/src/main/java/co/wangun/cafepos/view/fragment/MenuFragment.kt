package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.cxt
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentMenuBinding
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.MenuViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import com.skydoves.powerspinner.PowerSpinnerView
import cowanguncafepos.Menu

@SuppressLint("SetTextI18n")
class MenuFragment: Fragment(R.layout.fragment_menu) {

    private val TAG: String by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: MenuViewModel by viewModels()
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
        val categories = vm.getAllCategories()

        MaterialDialog(cxt).show {
            noAutoDismiss()
            cancelable(false)
            lifecycleOwner(viewLifecycleOwner)
            title(text = titleDialog)
            customView(R.layout.dialog_menu, scrollable = true, horizontalPadding = true)
            cornerRadius(24f)

            val spinnerCats = view.findViewById<PowerSpinnerView>(R.id.spinner_categories)
            val btnNewCat = view.findViewById<FloatingActionButton>(R.id.btn_new_category)
            val editName = view.findViewById<TextInputEditText>(R.id.edit_name_menu)
            val editDesc = view.findViewById<TextInputEditText>(R.id.edit_desc_menu)
            val editPrice = view.findViewById<TextInputEditText>(R.id.edit_price_menu)
            val layName = view.findViewById<TextInputLayout>(R.id.lay_name_menu)

            if (menu != null) {
                neutralButton(text = "Remove") {
                    vm.deleteMenu(menu.id)
                    dismiss()
                    initRecycler()
                }
            }
            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = positiveText) {
                val id = menu?.id ?: vm.countMenu()+1
                val price = editPrice.text.toString().toDouble()
                val newMenu = Menu(id, "${editName.text}", "${editDesc.text}",
                    "${spinnerCats.text}", price)

                if (vm.isMenuFormValid(newMenu, isNew)) {
                    vm.postMenu(newMenu)
                    dismiss()
                    initRecycler()
                } else {
                    if(editName.text.isNullOrBlank()) {
                        layName.error = "Name must not be empty"
                    }

                    if(isNew && vm.isMenuListed("$editName")) {
                        layName.error = "$editName is already on the menu"
                    }

                    if(spinnerCats.text.isNullOrBlank()) {
                        spinnerCats.setHintTextColor(
                            ContextCompat.getColor(context, R.color.red_900)
                        )
                    }
                }
            }

            view.apply {
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
                        cornerRadius(24f)
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

                editName.setText(menu?.name)
                editDesc.setText(menu?.desc)
                editPrice.setText("${menu?.price ?: 0.0}")
            }
        }
    }

    private fun initRecycler() {
        Adapter.builder(viewLifecycleOwner)
            .addSource(Source.fromList(vm.getAllMenu()))
            .addPresenter(Presenter.simple(cxt, R.layout.item_menu, 0)
            { view, item: Menu ->
                view.apply {
                    // init view
                    val viewName = findViewById<AppCompatTextView>(R.id.text_name_menu)
                    val viewDesc = findViewById<AppCompatTextView>(R.id.text_desc_menu)
                    val viewPrice = findViewById<AppCompatTextView>(R.id.text_price_menu)
                    val viewEdit = findViewById<FloatingActionButton>(R.id.btn_edit_menu)

                    // set view
                    viewName.text = item.name
                    viewDesc.text = "(${item.category}) ${item.desc}"
                    viewPrice.text = avm.withCurrency(item.price ?: 0.0)
                    viewEdit.setOnClickListener { createMenuDialog(item) }
                }
            })
            .into(bind.rvMenus)
    }
}