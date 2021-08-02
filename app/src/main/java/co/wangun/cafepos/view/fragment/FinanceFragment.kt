package co.wangun.cafepos.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity.CENTER
import android.view.Gravity.TOP
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.viewbinding.library.fragment.viewBinding
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import co.wangun.cafepos.App.Companion.fu
import co.wangun.cafepos.R
import co.wangun.cafepos.databinding.FragmentFinanceBinding
import co.wangun.cafepos.databinding.ItemTableBinding
import co.wangun.cafepos.util.FunUtils
import co.wangun.cafepos.viewmodel.FinanceViewModel
import co.wangun.cafepos.viewmodel.MainViewModel
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.google.android.material.tabs.TabLayout
import pl.kremblewski.android.simplerecyclerviewadapter.Adapter
import pl.kremblewski.android.simplerecyclerviewadapter.adapter

@SuppressLint("SetTextI18n")
class FinanceFragment : Fragment(R.layout.fragment_finance) {

    private val TAG by lazy { javaClass.simpleName }
    private val avm: MainViewModel by activityViewModels()
    private val vm: FinanceViewModel by viewModels()
    private val vb: FragmentFinanceBinding by viewBinding()
    private lateinit var mainAdapter: Adapter

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        initFun()
    }

    // init
    //
    private fun initFun() {
        vm.initData()
        initBtn()
        initTab()
    }

    private fun initTab() {
        // init
        initView(0)

        // listener
        vb.tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                initView(tab?.position ?: 0)
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

        vm.footerStr.asLiveData().observe(viewLifecycleOwner) {
            vb.rvFooter.apply {
                root.visibility = if (vb.tab.selectedTabPosition == 2) GONE else VISIBLE
                run { listOf(text1, text2, text3, text4) }
                    .forEachIndexed { index, v -> v.text = it[index] }
            }
        }
    }

    private fun initHeaderList(i: Int) {
        when (i) {
            0 -> listOf("Product", "Sold", "Revenue", "Profit")
            1 -> listOf("Product", "Price", "Cost", "% COGS")
            2 -> listOf("Material", "Cost", "Mass", "Cost / Mass")
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
                    it as FinanceViewModel.FinanceItem

                    bind.btnAct.apply {
                        setOnClickListener { _ -> summaryDialog(it) }
                        setImageResource(R.drawable.ic_baseline_chevron_right_24)
                    }

                    when (vb.tab.selectedTabPosition) {
                        0 -> listOf(
                            "(${it.category}) ${it.name}",
                            fu.toLocale(it.sold.toDouble()),
                            fu.toLocale(it.revenue),
                            fu.toLocale(it.profit)
                        )
                        1 -> listOf(
                            "(${it.category}) ${it.name}",
                            fu.toLocale(it.revenue),
                            fu.toLocale(it.cost),
                            "${fu.toLocale(it.cost / it.price * 100)}%"
                        )
                        2 -> listOf(
                            "(${it.category}) ${it.name}",
                            fu.toLocale(it.cost),
                            fu.toLocale(vm.massSum(it)),
                            fu.toLocale(it.cost / vm.massSum(it))
                        )
                        else -> listOf("?", "?", "?", "?")
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
    private fun summaryDialog(it: FinanceViewModel.FinanceItem) {
        Toast.makeText(requireContext(), "Detail of ${it.name} summary", Toast.LENGTH_SHORT).show()
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