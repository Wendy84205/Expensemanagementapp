package com.example.financeapp

// Extension function để convert TransactionData sang Transaction
fun TransactionData.toTransaction(): Transaction {
    return Transaction(
        id = this.id,
        date = this.date,
        dayOfWeek = this.dayOfWeek,
        category = this.category,
        amount = this.amount,
        isIncome = this.isIncome,
        group = this.group,
        wallet = this.wallet
    )
}