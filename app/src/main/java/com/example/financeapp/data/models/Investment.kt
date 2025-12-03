package com.example.financeapp.data.models

import com.google.firebase.firestore.PropertyName

data class Investment(
    val id: String = "",
    val name: String = "",
    val type: InvestmentType = InvestmentType.STOCK,

    // Các property chính cho tính toán
    @PropertyName("investedAmount")
    val investedAmount: Long = 0,

    @PropertyName("currentValue")
    val currentValue: Long = 0,

    @PropertyName("profitLoss")
    val profitLoss: Long = 0,

    val startDate: Long = System.currentTimeMillis(),
    val userId: String = "",

    // Các property để tương thích với code cũ
    @PropertyName("symbol")
    val symbol: String = "",

    @PropertyName("quantity")
    val quantity: Int = 0,

    @PropertyName("purchasePrice")
    val purchasePrice: Long = 0,

    @PropertyName("currentPrice")
    val currentPrice: Long = 0,

    @PropertyName("notes")
    val notes: String = "",

    @PropertyName("category")
    val category: String = "",

    @PropertyName("color")
    val color: Int = 0,

    @PropertyName("icon")
    val icon: Int = 0
) {
    constructor() : this(
        "", "", InvestmentType.STOCK, 0, 0, 0, System.currentTimeMillis(), "",
        "", 0, 0, 0, "", "", 0, 0
    )

    // Helper properties
    val profit: Long get() = currentValue - investedAmount
    val profitPercentage: Double get() =
        if (investedAmount > 0) (profit.toDouble() / investedAmount) * 100 else 0.0
}

enum class InvestmentType {
    STOCK,
    BOND,
    MUTUAL_FUND,
    REAL_ESTATE,
    CRYPTO,
    GOLD,
    OTHER,

    // Các giá trị để tương thích
    @PropertyName("STOCKS")
    STOCKS,

    @PropertyName("CRYPTOCURRENCY")
    CRYPTOCURRENCY,

    @PropertyName("SAVINGS")
    SAVINGS
}