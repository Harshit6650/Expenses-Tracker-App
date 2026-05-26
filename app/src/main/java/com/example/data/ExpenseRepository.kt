package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<ExpenseWithCategory>> = expenseDao.getAllExpensesWithCategory()
    val allCategories: Flow<List<Category>> = expenseDao.getAllCategories()

    suspend fun insertExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        expenseDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        expenseDao.deleteCategory(category)
    }

    suspend fun seedDefaultCategoriesIfEmpty() = withContext(Dispatchers.IO) {
        val currentCategories = expenseDao.getAllCategories().first()
        if (currentCategories.isEmpty()) {
            val defaults = listOf(
                Category(name = "Food & Drinks", colorHex = "#FF5722", iconName = "restaurant"), // Deep orange
                Category(name = "Shopping", colorHex = "#E91E63", iconName = "shopping"),     // Pink
                Category(name = "Transportation", colorHex = "#2196F3", iconName = "travel"), // Blue
                Category(name = "Bills & Utilities", colorHex = "#9C27B0", iconName = "bill"), // Purple
                Category(name = "Entertainment", colorHex = "#4CAF50", iconName = "play"),    // Green
                Category(name = "Others", colorHex = "#9E9E9E", iconName = "info")          // Grey
            )
            for (category in defaults) {
                expenseDao.insertCategory(category)
            }
        }
    }
}
