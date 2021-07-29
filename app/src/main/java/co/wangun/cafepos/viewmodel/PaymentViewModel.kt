package co.wangun.cafepos.viewmodel

import androidx.lifecycle.ViewModel
import co.wangun.cafepos.App.Companion.db
import co.wangun.cafepos.App.Companion.su
import co.wangun.cafepos.util.SessionUtils.Companion.Currency_STR
import cowanguncafepos.Payment
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("MemberVisibilityCanBePrivate")
class PaymentViewModel: ViewModel() {

    val keywordFilter by lazy { MutableStateFlow("") }
    val dbPayment by lazy { db.paymentQueries }

    fun selectAll(): List<Payment> {
        return dbPayment.selectAll().executeAsList().toMutableList().apply {
            if(keywordFilter.value.isNotBlank()) {
                retainAll {
                    val hasName = it.name.contains(keywordFilter.value, true)
                    val hasDesc = it.desc?.contains(keywordFilter.value, true) == true
                    hasName || hasDesc
                }
            }
        }
    }

    fun find(name: String): Payment? {
        return dbPayment.find(name).executeAsOneOrNull()
    }

    fun getCurrency(): String {
        return su.get(Currency_STR) as String
    }

    fun setCurrency(str: String) {
        su.set(Currency_STR, str)
    }

    fun setKeywordFilter(input: String) {
        keywordFilter.value = input
    }
}