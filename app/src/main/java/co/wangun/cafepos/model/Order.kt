package co.wangun.cafepos.model

data class Order(
        var id: Int = 0,
        var table: Int? = 0,
        var date: String? = "",
        var price: Double? = 0.0,
        //var menus: List<Menu>? = emptyList()
)