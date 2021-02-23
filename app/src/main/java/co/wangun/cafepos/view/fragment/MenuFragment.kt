package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
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
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
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

    // init
    //
    private fun initFun() {
        initBtn()
        initView()
    }

    private fun initView() {
        initRecycler(vm.getAllMenu())
        initNameFilter()
        initCatsFilter()
    }

    private fun initNameFilter() {
        bind.btnFind.text = "All Menus"
    }

    private fun initCatsFilter() {
        bind.btnCats.text = "All Categories"
    }

    private fun initBtn() {
        bind.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnNew.setOnClickListener { createMenuDialog(null) }
            btnFind.setOnClickListener { createFindDialog() }
            btnCats.setOnClickListener { createCatsDialog() }
        }
    }

    private fun initRecycler(list: List<Menu>) {
        // init val
        val source = Source.fromList(list)
        val presenter = Presenter.simple(requireContext(), R.layout.item_menu, 0)
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
        }

        // build adapter
        Adapter.builder(viewLifecycleOwner)
                .addSource(source)
                .addPresenter(presenter)
                .into(bind.rvMenus)

        // if list empty
        bind.layEmpty.root.visibility = if (list.isEmpty()) VISIBLE else GONE
    }

    // dialog
    //
    private fun createNewCatDialog(binding: DialogMenuBinding, categories: List<String>) {
        binding.apply {
            fun putCategories(input: String) {
                spinnerCategories.apply {
                    if (categories.contains(input)) {
                        selectItemByIndex(categories.indexOf(input))
                    } else text = input
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
                // dialog btn
                if (menu != null) {
                    neutralButton(text = "Remove") {
                        vm.deleteMenu(menu.id)
                        dismiss()
                        initView()
                    }
                }
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") { confirmMenu(binding, menu, it, isNew) }

                // view dialog
                editNameMenu.setText(menu?.name)
                editDescMenu.setText(menu?.desc)
                editPriceMenu.setText("${menu?.price ?: 0.0}")
                btnNewCategory.setOnClickListener {
                    createNewCatDialog(binding, categories)
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

    private fun createFindDialog() {
        // init val
        val isAlreadyFiltered = vm.nameFilter.value.isNotBlank()
        val preFill = if(isAlreadyFiltered) vm.nameFilter.value else ""

        // build dialog
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Name Filter")
            cornerRadius(24f)
            cancelable(false)

            // input
            input(hint = "Input the name...", prefill = preFill) {
                _, input -> filterByName("$input")
            }

            // input field style
            getInputField().apply {
                gravity = Gravity.CENTER
                post { selectAll() }
                setBackgroundColor(resources.getColor(
                        android.R.color.transparent, null
                ))
            }

            // neutral btn
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            if(isAlreadyFiltered) {
                neutralButton(text = "Remove") {
                    filterByName("")
                }
            }
        }
    }

    private fun createCatsDialog() {
        // init val
        val cats = vm.getAllCategories()
        val indices = vm.getCatsFilterIndices()

        // create dialog
        MaterialDialog(requireContext()).show {
            // dialog general
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Categories Filter")
            message(text = "The menus that are within this selected categories will be displayed")

            // dialog list
            listItemsMultiChoice(
                    items = cats,
                    initialSelection = indices
            ) { _, _, list -> filterByCats(list) }

            // dialog btn
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            if(vm.hasCatsFilter()) {
                neutralButton(text = "Remove") {
                    filterByCats(vm.getAllCategories())
                }
            }
        }
    }

    // etc
    //
    private fun confirmMenu(
            binding: DialogMenuBinding, menu: Menu?,
            dialog: MaterialDialog, isNew: Boolean
    ) {
        binding.apply {
            val newMenu = Menu(
                    menu?.id ?: vm.countMenu(),
                    "${editNameMenu.text}",
                    "${editDescMenu.text}",
                    "${spinnerCategories.text}",
                    "${editPriceMenu.text}".toDouble()
            )

            if (isMenuFormValid(binding, isNew)) {
                vm.postMenu(newMenu)
                dialog.dismiss()
                initRecycler(vm.getLastMenus())
            }
        }
    }

    private fun isMenuFormValid(binding: DialogMenuBinding, isNew: Boolean): Boolean {
        binding.apply {
            var err = 0

            layNameMenu.error = when {
                editNameMenu.text.isNullOrBlank() -> {
                    err++; getString(R.string.edit_empty)
                }
                isNew && vm.isMenuListed("${editNameMenu.text}") -> {
                    err++; getString(R.string.edit_duplicated)
                }
                else -> null
            }

            spinnerCategories.error = if(spinnerCategories.text.isNullOrBlank()) {
                err++; getString(R.string.edit_empty)
            } else null

            return err == 0
        }
    }

    private fun filterByName(input: String) {
        // set filter
        vm.setNameFilter(input)

        // set view
        if(input.isNotBlank()) bind.btnFind.text = "\"$input\""
        else initNameFilter()

        // refresh list
        initRecycler(vm.getLastMenus())
    }

    private fun filterByCats(list: List<CharSequence>) {
        // set filter
        vm.setCatsFilter(list)

        // set view
        if(list.isNotEmpty() && vm.hasCatsFilter()) {
            bind.btnCats.text = when (list.size) {
                1 -> "${list[0]}"
                2 -> "${list[0]} and ${list[1]}"
                else -> "${list[0]}, ${list[1]},\nand ${list.size - 2} others"
            }
        } else initCatsFilter()

        // refresh list
        initRecycler(vm.getLastMenus())
    }
}