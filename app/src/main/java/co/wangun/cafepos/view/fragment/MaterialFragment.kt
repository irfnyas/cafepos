package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.Gravity.CENTER
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
import co.wangun.cafepos.App.Companion.TABLE_RECIPE
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.DialogMaterialBinding
import co.wangun.cafepos.databinding.DialogRecipeBinding
import co.wangun.cafepos.databinding.FragmentMaterialBinding
import co.wangun.cafepos.databinding.ItemTableBinding
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.viewmodel.MainViewModel
import co.wangun.cafepos.viewmodel.MaterialViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.tabs.TabLayout
import com.wajahatkarim3.easyvalidation.core.view_ktx.nonEmpty
import cowanguncafepos.Material
import cowanguncafepos.Recipe
import pl.kremblewski.android.simplerecyclerviewadapter.Adapter
import pl.kremblewski.android.simplerecyclerviewadapter.adapter

@SuppressLint("SetTextI18n")
class MaterialFragment: Fragment(R.layout.fragment_material) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: MaterialViewModel by viewModels()
    private val vb: FragmentMaterialBinding by viewBinding()
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
            vb.btnFind.text = if (it.isNullOrBlank()) "All Products" else "\"$it\""
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
        listOf("Material", "Category", "Mass Unit", "Mass Value").let { str ->
            vb.rvHeader.apply { text4.visibility = if(i == 2) GONE else VISIBLE }
                .run { listOf(text1, text2, text3, text4) }
                .forEachIndexed { index, v -> v.text = str[index] }
        }
    }

    private fun initBtn() {
        vb.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnNew.setOnClickListener { materialDialog() }
            btnFind.setOnClickListener { findFilterDialog() }
            btnCats.setOnClickListener { catsFilterDialog() }
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
                    it as Material

                    bind.btnAct.setOnClickListener { _ -> materialDialog(it) }
                    listOf(it.name, it.category, it.unit, fu.toLocale(it.mass)).let { str ->
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

    private fun initRvRecipes(
        parentBind: DialogMaterialBinding,
        onlyRaw: Boolean? = true
    ) {
        val list = vm.recipesTemp.value
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
                        setOnClickListener { _ ->
                            if (isMaterialFormValid(parentBind, false))
                                recipeDialog(
                                    parentBind, it, onlyRaw,
                                    "${parentBind.layName.editText?.text?.trim()}"
                                )
                        }
                    }
                }
            }.apply { submitList(items) }

            // helper
            GravitySnapHelper(TOP).attachToRecyclerView(this)
        }
    }

    private fun materialDialog(item: Material? = null) {
        val isNew = item == null

        val rawList = vm.getAllRawMaterials()
        val catList = vm.getAllCats()
        val unitList = vm.getAllUnits()

        vm.setRecipes(
            if (item == null) mutableListOf()
            else avm.getParentRecipes(item.name)
        )

        val titleDialog = if (isNew) "New Material" else "Edit ${item?.name}"
        val bind = DialogMaterialBinding.inflate(LayoutInflater.from(requireContext()))

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
                            avm.deleteItem(item.id, TABLE_MATERIAL, item.name),
                            initRecycler(), bind.root)
                        dismiss()
                    }
                }
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") {
                    if(isMaterialFormValid(bind, isNew)) {
                        fu.runCS(
                            avm.postItem(Material(
                                item?.id ?: avm.idIncrement(TABLE_MATERIAL),
                                "${layName.editText?.text?.trim()}",
                                "${layDesc.editText?.text?.trim()}",
                                "${layCategories.root.editText?.text?.trim()}",
                                "${layUnits.root.editText?.text?.trim()}",
                                "${layMass.editText?.text}".toDouble(),
                                if (radioRaw.isChecked) 1L else 0
                            ), vm.recipesTemp.value),
                            initRecycler(),
                            root)
                        dismiss()
                    }
                }

                // radio
                radioRaw.isEnabled = isNew
                radioRefined.isEnabled = isNew && rawList.isNotEmpty()
                radioGroup.setOnCheckedChangeListener { _, _ ->
                    layRecipes.root.visibility = if(radioRaw.isChecked) GONE else VISIBLE
                    layMass.editText?.apply {
                        isEnabled = !radioRaw.isChecked
                        if(radioRaw.isChecked) {
                            setText("0.0")
                            layMass.helperText = getString(R.string.edit_limited)
                        } else layMass.isHelperTextEnabled = false
                    }
                }
                val radioChild = radioGroup.getChildAt(vb.tab.selectedTabPosition)
                (radioChild as MaterialRadioButton).isChecked = true
                if(rawList.isEmpty()) radioRaw.isChecked = true

                // edit text
                layName.apply {
                    isEnabled = isNew
                    if(isNew) helperText = getString(R.string.edit_once)
                    editText?.setText(item?.name)
                }
                layDesc.editText?.setText(item?.desc)
                layMass.editText?.setText("${item?.mass ?: 0.0}")

                // rv
                initRvRecipes(bind)
                layRecipes.apply {
                    title.text = "Material Recipes"
                    add.setOnClickListener {
                        if(isMaterialFormValid(bind, isNew)) recipeDialog(
                            bind, null, true,
                            "${layName.editText?.text?.trim()}"
                        )
                    }
                }

                // spinner
                layUnits.root.apply {
                    isEnabled = isNew
                    hint = "Mass Unit"

                    helperText = when {
                        unitList.isEmpty() -> getString(R.string.msg_list_empty)
                        isNew -> getString(R.string.edit_once)
                        else -> ""
                    }

                    editText?.setText(item?.unit)
                    layUnits.edit.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            unitList
                        )
                    )
                }

                layCategories.apply {
                    root.hint = "Category"
                    if(catList.isNotEmpty()) root.isHelperTextEnabled = false
                    edit.setText(item?.category)
                    edit.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            catList
                        )
                    )
                }
            }
        }
    }

    private fun recipeDialog(
        parentBind: DialogMaterialBinding,
        item: Recipe? = null,
        onlyRaw: Boolean? = true,
        recipeParent: String = "",
    ) {
        val isNew = item == null
        val rawList = if(onlyRaw == true) vm.getAllRawMaterials() else vm.getAllItems(null)
        val unitList = vm.getAllUnits()

        val titleDialog = if (isNew) "New Recipe" else "Edit ${item?.name}"
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

            val isListed = avm.isItemListed(
                listOf(
                    recipeParent,
                    "${bind.layEdit1.edit.text}"
                ), TABLE_RECIPE
            )

            if(isNew && isListed) {
                bind.layEdit1.root.error = getString(R.string.edit_duplicated)
                err++
            }

            return err == 0
        }


        MaterialDialog(requireContext()).show {
            noAutoDismiss()
            lifecycleOwner(viewLifecycleOwner)
            title(text = titleDialog)
            customView(view = bind.root, scrollable = true, horizontalPadding = true)
            cornerRadius(24f)

            bind.apply {
                // dialog btn
                if (item != null) {
                    neutralButton(text = "Remove") {
                        fu.runCS(
                            vm.recipesTemp.value.removeAt(
                                vm.recipesTemp.value.indexOf(item)
                            ), initRvRecipes(parentBind, onlyRaw), root)
                        dismiss()
                    }
                }
                negativeButton(text = "Back") { dismiss() }
                positiveButton(text = "Confirm") {
                    if(isFormValid()) {
                        fu.runCS(
                            vm.recipesTemp.value.add(Recipe(
                                item?.id ?: avm.idIncrement(TABLE_RECIPE),
                                "${layEdit1.edit.text.trim()}",
                                "${edit2.text}".toDouble(),
                                "${layEdit3.edit.text.trim()}",
                                recipeParent)),
                            initRvRecipes(parentBind, onlyRaw),
                            root
                        )
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
                        } catch (e: Exception) {}
                    }
                    edit.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            rawList.map {
                                it
                                "${it.name} (${it.category})"
                            }
                        )
                    )
                }

                layEdit2.apply {
                    hint = "Mass Value"
                    editText?.setText("${item?.mass ?: 0.0}")
                }

                layEdit3.apply {
                    root.hint = "Mass Unit"
                    if(unitList.isNotEmpty()) root.isHelperTextEnabled = false
                    edit.setText(item?.unit)
                    edit.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            unitList
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

    // etc
    //
    private fun isMaterialFormValid(
        bind: DialogMaterialBinding,
        isNew: Boolean
    ): Boolean {
        bind.apply {
            var err = 0

            listOf(
                layName, layMass,
                layCategories.root,
                layUnits.root
            ).forEach { v ->
                v.isErrorEnabled = false
                v.editText?.nonEmpty {
                    v.error = getString(R.string.edit_empty)
                    err++
                }
            }

            if(isNew && avm.isItemListed(listOf("${layName.editText?.text}"), TABLE_MATERIAL)) {
                layName.error = getString(R.string.edit_duplicated)
                err++
            }

            return err == 0
        }
    }
}