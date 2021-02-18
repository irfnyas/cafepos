package co.wangun.cafepos.view.fragment

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.du
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogPrinterBinding
import co.wangun.cafepos.databinding.FragmentHomeBinding
import co.wangun.cafepos.viewmodel.HomeViewModel
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import cowanguncafepos.Printer


class HomeFragment: Fragment(R.layout.fragment_home) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: HomeViewModel by viewModels()
    private val bind: FragmentHomeBinding by viewBinding()

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    // init
    //
    private fun initFun() {
        val notLoggedIn = vm.getNick().isBlank()
        if(notLoggedIn) navToLoginFragment() else {
            initView()
            initRecycler()
            initBtn()
        }
    }

    private fun initBtn() {
        bind.apply {
            btnEditAccount.setOnClickListener { createAccountDialog() }
            btnEditTables.setOnClickListener { createTablesDialog() }
            btnEditMenu.setOnClickListener { navToMenuFragment() }
            btnEditPrinter.setOnClickListener { createPrinterDialog() }
            btnOrderHistory.setOnClickListener { navToHistoryFragment() }
        }
    }

    private fun initView() {
        bind.textNick.text = vm.getNick()
    }

    private fun initRecycler() {
        // init val
        val list = IntRange(1, vm.getTablesAmount()).toList()
        val presenter = Presenter.simple(
                requireContext(), R.layout.item_table, 0
        ) { view, item: Int ->
            (view as MaterialButton).apply {
                text = "$item"
                setOnClickListener {
                    createOrderDialog(item)
                }
            }
        }

        // set adapter
        Adapter.builder(viewLifecycleOwner)
                .addSource(Source.fromList(list))
                .addPresenter(presenter)
                .into(bind.rvTables)
    }

    // Dialog
    //
    private fun createOldPassDialog() {
        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = "Check")
            title(text = "Change My Password (1/3)")
            message(text = "Please input your old password")
            input(
                    hint = "Input old password...",
                    inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            ) { _, input ->
                if(vm.isOldPassValid("$input")) {
                    createNewPassDialog(); dismiss()
                } else
                    getInputLayout().error = getString(R.string.pass_old_invalid)
            }
            getInputLayout().apply {
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
            getInputField().apply {
                gravity = Gravity.CENTER
                setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
        }
    }

    private fun createNewPassDialog() {
        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            cancelable(false)
            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = "Change")
            title(text = "Change My Password (2/3)")
            message(text = "Please input your new password")
            input(
                    hint = "Input new password...",
                    inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            ) {
                _, input -> createConfirmPassDialog("$input", this)
            }
            getInputLayout().apply {
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
            getInputField().apply {
                gravity = Gravity.CENTER
                setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
        }
    }

    private fun createConfirmPassDialog(newPass: String, newPassDialog: MaterialDialog) {
        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            cancelable(false)
            negativeButton(text = "Back") { dismiss() }
            positiveButton(text = "Confirm")
            title(text = "Change My Password (3/3)")
            message(text = "Confirm your new password")
            input(
                    hint = "Input new password again...",
                    inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            ) { _, input ->
                if ("$input" != newPass) {
                    getInputLayout().error = getString(R.string.pass_new_invalid)
                } else {
                    vm.putPass(newPass)
                    dismiss()
                    newPassDialog.dismiss()
                }
            }
            getInputLayout().apply {
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
            getInputField().apply {
                gravity = Gravity.CENTER
                setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            }
        }
    }

    private fun createAccountDialog() {
        val list = listOf("User Management", "Change My Password", "Logout")
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            negativeButton(text = "Back")
            title(text = "Edit Account")
            listItems(items = list) { _, index, _ ->
                when(index) {
                    0 -> navToAccountFragment()
                    1-> createOldPassDialog()
                    2 -> createLogoutDialog()
                }
            }
        }
    }

    private fun createLogoutDialog() {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Confirm Logout")
            message(text = "Are you sure you want to logout?")
            positiveButton(text = "Logout") { navToLoginFragment() }
            negativeButton(text = "Back")
        }
    }

    private fun createTablesDialog() {
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            negativeButton(text = "Back")
            positiveButton(text = "Confirm")
            title(text = "Edit Tables")
            message(text = "How many tables can you serve today?")
            input(
                    hint = "Input the amount...",
                    prefill = "${vm.getTablesAmount()}",
                    inputType = InputType.TYPE_CLASS_NUMBER
            ) { _, input -> putTablesAmount(input.toString().toInt()) }
            getInputField().apply {
                gravity = Gravity.CENTER
                post { selectAll() }
                setBackgroundColor(resources.getColor(
                        android.R.color.transparent, null
                ))
            }
        }
    }

    private fun createOrderDialog(num: Int) {
        // init val
        val list = vm.getTodayOrderForTable(num).sortedDescending()

        // create dialog
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Table $num (${du.getTodayDate()})")
            negativeButton(text = "Back")
            positiveButton(text = "New Order") {
                navToOrderFragment(num, du.getTime(), du.getTodayDateYmd())
            }
            listItems(items = list, waitForPositiveButton = false) { _, _, text ->
                navToOrderFragment(num, du.parseTime("$text"), du.getTodayDateYmd())
            }
            if (list.isEmpty()) {
                message(text = getString(R.string.msg_list_empty))
            }
        }
    }

    private fun createPrinterDialog() {
        // init val
        val printers = avm.getPrinters()
        val list = printers.map { "${it.name} - ${it.address}" }

        // create dialog
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            cornerRadius(24f)
            title(text = "Edit Printer")
            negativeButton(text = "Back")
            positiveButton(text = "New Printer") {
                createPrinterDetailDialog(null)
            }
            listItems(items = list, waitForPositiveButton = false) { _, _, text ->
                val name = text.split(" - ")[0]
                val printer = printers.find { it.name == name }
                createPrinterDetailDialog(printer)
                dismiss()
            }
            if (list.isEmpty()) {
                message(text = getString(R.string.msg_list_empty))
            }
        }
    }

    private fun createPrinterDetailDialog(printer: Printer?) {
        val binding = DialogPrinterBinding.inflate(LayoutInflater.from(requireContext()))
        val title = if (printer != null) "Edit ${printer.name}" else "Add New Printer"

        MaterialDialog(requireContext()).onDismiss {
            createPrinterDialog()
        }.show {
            noAutoDismiss()
            cancelable(false)
            lifecycleOwner(viewLifecycleOwner)
            customView(view = binding.root, horizontalPadding = true)
            cornerRadius(24f)
            title(text = title)
            binding.apply {
                editPrinterName.setText(printer?.name)
                editPrinterAddress.setText(printer?.address)

                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") {
                    if(isPrinterFieldValid(binding)) {
                        vm.putPrinter(
                                "${editPrinterName.text}",
                                "${editPrinterAddress.text}"
                        )
                        dismiss()
                    }
                }

                // add remove if edit
                printer?.let {
                    neutralButton(text = "Remove") {
                        vm.delPrinter(printer.id)
                        dismiss()
                    }
                }
            }
        }
    }

    // Navigation
    //
    private fun navToHistoryFragment() {
        val action = HomeFragmentDirections.actionHomeFragmentToHistoryFragment()
        findNavController().navigate(action)
    }

    private fun navToMenuFragment() {
        val action = HomeFragmentDirections.actionHomeFragmentToMenuFragment()
        findNavController().navigate(action)
    }

    private fun navToLoginFragment() {
        val action = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
        findNavController().navigate(action)
        vm.logout()
    }

    private fun navToOrderFragment(num: Int, time: String, date: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToOrderFragment(num, time, date)
        findNavController().navigate(action)
    }

    private fun navToAccountFragment() {
        //TODO
    }

    // Etc
    //
    private fun putTablesAmount(input: Int) {
        vm.putTablesAmount(input)
        initRecycler()
    }

    private fun isPrinterFieldValid(binding: DialogPrinterBinding): Boolean {
        var err = 0

        binding.apply {
            layPrinterName.error = when {
                editPrinterName.text.isNullOrBlank() -> {
                    err++; getString(R.string.edit_empty)
                }
                vm.isPrinterListed("${editPrinterName.text}") -> {
                    err++; getString(R.string.edit_duplicated)
                }
                else -> null
            }

            layPrinterAddress.error = if(editPrinterAddress.text.isNullOrBlank()) {
                err++; getString(R.string.edit_empty)
            } else ""
        }

        return err == 0
    }
}