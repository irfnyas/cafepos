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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.TABLE_MATERIAL
import co.wangun.cafepos.App.Companion.TABLE_PRODUCT
import co.wangun.cafepos.App.Companion.TABLE_RECIPE
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogProductBinding
import co.wangun.cafepos.databinding.DialogRecipeBinding
import co.wangun.cafepos.databinding.FragmentProductBinding
import co.wangun.cafepos.databinding.ItemCardBinding
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.ProductViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.wajahatkarim3.easyvalidation.core.view_ktx.nonEmpty
import cowanguncafepos.Material
import cowanguncafepos.Product
import cowanguncafepos.Recipe
import pl.kremblewski.android.simplerecyclerviewadapter.Adapter
import pl.kremblewski.android.simplerecyclerviewadapter.adapter

@SuppressLint("SetTextI18n")
class ProductFragment: Fragment(R.layout.fragment_product) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: ProductViewModel by viewModels()
    private val vb: FragmentProductBinding by viewBinding()
    private lateinit var mainAdapter: Adapter

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
        initRecycler(false)
        vm.keywordFilter.asLiveData().observe(viewLifecycleOwner) {
            vb.btnFind.text = if (it.isNullOrBlank()) "All Products" else "\"$it\""
        }
    }

    private fun initBtn() {
        vb.apply {
            btnNew.setOnClickListener { productDialog(null) }
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnFind.setOnClickListener { findFilterDialog() }
            btnCats.setOnClickListener { catsFilterDialog() }
        }
    }

    private fun initRecycler(submitOnly: Boolean = true) {
        // init list
        val list = vm.selectAll(TABLE_PRODUCT)
        val items = list.mapIndexed { index, item ->
            FunUtils.Items(index, item)
        }

        if(!submitOnly) {
            // init adapter
            mainAdapter = adapter {
                register { bind: ItemCardBinding, item: FunUtils.Items, _ ->
                    val it = item.item as Product
                    val recipes = avm.getParentRecipes(it.name).size

                    bind.textName.text = it.name
                    bind.textPrice.text = fu.toLocale(it.price ?: 0.0, true)
                    bind.textDesc.text = "Code: ${it.code}" +
                            "\nCategory: ${it.category}" +
                            "\nRecipes: $recipes" +
                            "\nDescription: ${it.desc}"
                    bind.btnEdit.setOnClickListener { _ -> optionsDialog(it) }
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

    /*
    private fun initRvRecipes(parentBind: DialogProductBinding) {
        val list = avm.getParentRecipes("${parentBind.editName.text}")
        val items = list.mapIndexed { index, item ->
            FunUtils.Items(index, item)
        }

        parentBind.layRecipes.rv.apply {
            adapter = adapter {
                register { bind: ItemTableBinding, item: FunUtils.Items, position: Int ->
                    val it = item.item as Recipe
                    val isLast = position == list.lastIndex

                    bind.text1.text = "${it.name}"
                    bind.text2.text = "${it.mass ?: 0.0}"
                    bind.text2.gravity = Gravity.END
                    bind.text3.text = "${it.unit}"
                    bind.text4.visibility = GONE
                    bind.separator.root.visibility = if(isLast) GONE else VISIBLE
                    bind.btnAct.apply {
                        setImageResource(R.drawable.ic_baseline_edit_24)
                        setOnClickListener { _ -> recipeDialog(parentBind, it) }
                    }
                }
            }.apply { submitList(items) }

            // helper
            GravitySnapHelper(Gravity.TOP).attachToRecyclerView(this)
        }
    }
     */


    // dialog
    //
    private fun optionsDialog(product: Product) {
        val items = listOf("Product Detail", "Product Recipes")
        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = "Edit ${product.name}")
            cornerRadius(24f)
            listItems(items = items) { _, index, _ ->
                when (index) {
                    0 -> productDialog(product)
                    1 -> recipesDialog(product)
                }
            }
        }
    }

    private fun productDialog(product: Product?) {
        val isNew = product == null
        val categories = vm.getAllCats()
        val titleDialog = if (isNew) "New Product" else "Edit ${product?.name}"
        val bind = DialogProductBinding.inflate(LayoutInflater.from(requireContext()))

        fun isFormValid(): Boolean {
            bind.apply {
                var err = 0

                listOf(
                    spinnerCategories.root,
                    layCode, layName, layPrice
                ).forEach { v ->
                    v.isErrorEnabled = false
                    v.editText?.nonEmpty {
                        v.error = getString(R.string.edit_empty)
                        err++
                    }
                }

                listOf(layName, layCode).forEach {
                    if (isNew && vm.isProductListed("${it.editText?.text}")) {
                        it.error = getString(R.string.edit_duplicated)
                        err++
                    }
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

            bind.apply {
                if (product != null) {
                    neutralButton(text = "Remove") {
                        fu.runCS(
                            avm.deleteItem(product.id, TABLE_PRODUCT),
                            initRecycler(), bind.root
                        )
                        dismiss()
                    }
                }
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") {
                    if (isFormValid()) {
                        fu.runCS(avm.postItem(Product(
                            product?.id ?: avm.idIncrement(TABLE_PRODUCT),
                            "${editCode.text?.trim()}",
                            "${editName.text?.trim()}",
                            "${editDesc.text?.trim()}",
                            "${spinnerCategories.edit.text?.trim()}",
                            "${editPrice.text}".toDouble())
                        ), initRecycler(), bind.root
                        )
                        dismiss()
                    }
                }

                bind.root.requestFocus()
                layCode.apply {
                    editText?.setText(product?.code)
                    helperText = getString(R.string.edit_once)
                    isEnabled = isNew
                    isHelperTextEnabled = isNew
                }

                layName.apply {
                    editText?.setText(product?.name)
                    helperText = getString(R.string.edit_once)
                    isEnabled = isNew
                    isHelperTextEnabled = isNew
                }

                editDesc.setText(product?.desc)
                editPrice.setText("${product?.price ?: 0.0}")

                spinnerCategories.apply {
                    root.hint = "Category"
                    if (categories.isNotEmpty()) root.isHelperTextEnabled = false
                    edit.setText(product?.category)
                    edit.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            categories
                        )
                    )
                }
            }
        }
    }

    private fun recipesDialog(product: Product) {
        val materials = vm.selectAll(TABLE_MATERIAL)
        val recipes = avm.getParentRecipes(product.name)
        val items = recipes.map { "${it.name} (${it.mass} ${it.unit})" }

        val titleDialog = "Edit ${product.name} recipes"
        val msgDialog = when {
            materials.isEmpty() -> getString(R.string.msg_material_empty)
            recipes.isEmpty() -> getString(R.string.msg_list_empty)
            else -> ""
        }

        MaterialDialog(requireContext()).show {
            lifecycleOwner(viewLifecycleOwner)
            title(text = titleDialog)
            if (msgDialog.isNotBlank()) message(text = msgDialog)
            cornerRadius(24f)

            negativeButton(text = "Back")
            if (materials.isNotEmpty()) positiveButton(text = "New Recipe") {
                recipeDialog(null, product)
                dismiss()
            }

            listItems(items = items, waitForPositiveButton = false) { _, index, _ ->
                recipeDialog(recipes[index], product)
                dismiss()
            }
        }
    }

    private fun recipeDialog(item: Recipe?, parent: Product) {
        val isNew = item == null
        val materials = vm.selectAll(TABLE_MATERIAL)

        val titleDialog =
            if (isNew) "New Recipe for ${parent.name}"
            else "Edit ${item?.name} recipe for ${parent.name}"

        val bind = DialogRecipeBinding.inflate(LayoutInflater.from(requireContext()))

        fun isFormValid(): Boolean {
            var err = 0
            listOf(
                bind.layEdit1.root,
                bind.layEdit2,
                bind.layEdit3.root
            ).forEach { v ->
                v.isErrorEnabled = false
                v.editText?.nonEmpty {
                    v.error = getString(R.string.edit_empty)
                    err++
                }
            }

            if (isNew && vm.isRecipeListed("${bind.layEdit1.edit.text}")) {
                bind.layEdit1.root.error = getString(R.string.edit_duplicated)
                err++
            }

            return err == 0
        }

        MaterialDialog(requireContext())
            .onDismiss { recipesDialog(parent) }
            .show {
                noAutoDismiss()
                lifecycleOwner(viewLifecycleOwner)
                title(text = titleDialog)
                cornerRadius(24f)
                customView(
                    view = bind.root,
                    scrollable = true,
                    horizontalPadding = true
                )

                bind.apply {
                    if (item != null) {
                        neutralButton(text = "Remove") {
                            fu.runCS(
                                avm.deleteItem(item.id, TABLE_RECIPE),
                                initRecycler(), bind.root
                            )
                            dismiss()
                        }
                    }
                    negativeButton(text = "Back") { dismiss() }
                    positiveButton(text = "Confirm") {
                        if (isFormValid()) {
                            fu.runCS(avm.postItem(Recipe(
                                item?.id ?: avm.idIncrement(TABLE_RECIPE),
                                "${layEdit1.edit.text}".trim(),
                                "${edit2.text}".toDouble(),
                                "${layEdit3.edit.text}".trim(),
                                parent.name
                            )), initRecycler(), bind.root)
                            dismiss()
                        }
                    }

                    // edit text
                    layEdit1.apply {
                        root.hint = "Name"
                        root.helperText = getString(R.string.edit_chose)
                        edit.isEnabled = false
                        edit.setText(item?.name)
                        edit.doAfterTextChanged {
                            try {
                                val split = it?.split(" (")
                                val name = split?.get(split.lastIndex - 1)
                                edit.setText(name)
                            } catch (e: Exception) {
                            }
                        }
                        edit.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                materials.map {
                                    it as Material
                                    "${it.name} (${it.category})"
                                }
                            )
                        )
                    }

                    bind.layEdit2.apply {
                        hint = "Mass Value"
                        editText?.setText("${item?.mass ?: 0.0}")
                    }

                    bind.layEdit3.apply {
                        root.hint = "Mass Unit"
                        if (materials.isNotEmpty()) root.isHelperTextEnabled = false
                        edit.setText(item?.unit)
                        edit.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                materials.map {
                                    it as Material
                                    "${it.unit}"
                                }.distinct()
                            )
                        )
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
                gravity = Gravity.CENTER
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
        val cats = avm.getAllCategories(TABLE_PRODUCT)
        val indices = vm.getCatsFilterIndices()

        fun setFilter(list: List<CharSequence>) {
            vm.setCatsFilter(list)
            vb.btnCats.text = if (!vm.hasCatsFilter()) "All Categories" else {
                when (list.size) {
                    1 -> "${list[0]}"
                    2 -> "${list[0]} and ${list[1]}"
                    else -> "${list[0]} and ${list.size - 1} others"
                }
            }
            // refresh rv
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