package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogMenuBinding
import co.wangun.cafepos.databinding.FragmentMenuBinding
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.MenuViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import cowanguncafepos.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

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

    // init
    //
    private fun initFun() {
        initBtn()
        initRecycler()
    }

    private fun initBtn() {
        bind.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnNewMenu.setOnClickListener { createMenuDialog(null) }
        }
    }

    private fun initRecycler() {
        Adapter.builder(viewLifecycleOwner)
                .addSource(Source.fromList(vm.getAllMenu()))
                .addPresenter(Presenter.simple(requireContext(), R.layout.item_menu, 0)
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

    // dialog
    //
    private fun createCategoryDialog(binding: DialogMenuBinding, categories: List<String>) {
        binding.apply {
            fun putCategories(input: String) {
                spinnerCategories.apply {
                    if (categories.contains(input)) selectItemByIndex(categories.indexOf(input))
                    else text = input.toUpperCase(Locale.ROOT)
                }
            }

            MaterialDialog(requireContext()).show {
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
                            resources.getColor(
                                    android.R.color.transparent,
                                    null
                            )
                    )
                }
            }
        }
    }

    private fun createMenuDialog(menu: Menu?) {
        val isNew = menu == null
        val titleDialog = if (isNew) "New Menu" else "Edit ${menu?.name}"
        val categories = vm.getAllCategories()
        val binding = DialogMenuBinding.inflate(LayoutInflater.from(requireContext()))

        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            cancelable(false)
            lifecycleOwner(viewLifecycleOwner)
            title(text = titleDialog)
            customView(view = binding.root, scrollable = true, horizontalPadding = true)
            cornerRadius(24f)

            binding.apply {
                // btn dialog
                if (menu != null) {
                    neutralButton(text = "Remove") {
                        vm.deleteMenu(menu.id)
                        dismiss()
                        initRecycler()
                    }
                }
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") { confirmMenu(binding, menu, it) }

                // view dialog
                editNameMenu.setText(menu?.name)
                editDescMenu.setText(menu?.desc)
                editPriceMenu.setText("${menu?.price ?: 0.0}")
                btnNewCategory.setOnClickListener {
                    createCategoryDialog(binding, categories)
                }
                spinnerCategories.apply {
                    lifecycleOwner = viewLifecycleOwner
                    setIsFocusable(false)
                    setItems(categories)
                    if(!isNew) selectItemByIndex(
                            categories.indexOf(menu?.category)
                    )
                }
            }
        }
    }

    // etc
    //
    private fun confirmMenu(binding: DialogMenuBinding, menu: Menu?, dialog: MaterialDialog) {
        binding.apply {
            val newMenu = Menu(
                    menu?.id ?: vm.countMenu(),
                    "${editNameMenu.text}",
                    "${editDescMenu.text}",
                    "${spinnerCategories.text}",
                    "${editPriceMenu.text}".toDouble()
            )

            if (isMenuFormValid(binding)) {
                vm.postMenu(newMenu)
                dialog.dismiss()
                initRecycler()
            }
        }
    }

    private fun isMenuFormValid(binding: DialogMenuBinding): Boolean {
        binding.apply {
            var err = 0

            layNameMenu.error = when {
                editNameMenu.text.isNullOrBlank() -> {
                    err++; "Name must not be empty"
                }
                vm.isMenuListed("${editNameMenu.text}") -> {
                    err++; "${editNameMenu.text} is already on the menu"
                }
                else -> null
            }

            spinnerCategories.error = if(spinnerCategories.text.isNullOrBlank()) {
                err++; "Category must not be empty"
            } else null

            return err == 0
        }
    }
}