package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Category
import com.example.data.Expense
import com.example.data.ExpenseWithCategory
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseWithCategory?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "WiseWallet",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (selectedTab == 1) {
                        IconButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.testTag("action_add_category")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Expenses") },
                    label = { Text("Expenses") },
                    modifier = Modifier.testTag("nav_tab_expenses")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Categories") },
                    label = { Text("Categories") },
                    modifier = Modifier.testTag("nav_tab_categories")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    modifier = Modifier.testTag("nav_tab_charts")
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    modifier = Modifier
                        .testTag("fab_add_expense")
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> ExpensesTabScreen(
                    expenses = expenses,
                    categories = categories,
                    onEditExpense = { expenseToEdit = it },
                    onDeleteExpense = { viewModel.deleteExpense(it.expense) }
                )
                1 -> CategoriesTabScreen(
                    categories = categories,
                    expenses = expenses,
                    onDeleteCategory = { viewModel.deleteCategory(it) }
                )
                2 -> AnalyticsTabScreen(
                    expenses = expenses
                )
            }
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        ExpenseFormDialog(
            categories = categories,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { description, amount, categoryId, dateMillis ->
                viewModel.addExpense(description, amount, categoryId, dateMillis)
                showAddExpenseDialog = false
            }
        )
    }

    // Edit Expense Dialog
    if (expenseToEdit != null) {
        ExpenseFormDialog(
            categories = categories,
            expenseToEdit = expenseToEdit,
            onDismiss = { expenseToEdit = null },
            onSave = { description, amount, categoryId, dateMillis ->
                val updatedExpense = expenseToEdit!!.expense.copy(
                    description = description,
                    amount = amount,
                    categoryId = categoryId,
                    dateMillis = dateMillis
                )
                viewModel.updateExpense(updatedExpense)
                expenseToEdit = null
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        CategoryFormDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, colorHex, iconName ->
                viewModel.addCategory(name, colorHex, iconName)
                showAddCategoryDialog = false
            }
        )
    }
}

// Map stored icon strings to Material icons
fun getCategoryIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.ShoppingCart
        "shopping" -> Icons.Default.ShoppingCart
        "travel" -> Icons.Default.LocationOn
        "bill" -> Icons.Default.Warning
        "play" -> Icons.Default.Star
        "home" -> Icons.Default.Home
        else -> Icons.Default.Info
    }
}

// Convert Hex string securely to Color object
fun parseColorHex(hex: String, fallback: Color = Color.Gray): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

// Format double value safely to Local currency representation
fun formatCurrency(amount: Double): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.format(amount)
    } catch (e: Exception) {
        // Fallback with custom symbol if locale format fails
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        "₹" + format.format(amount)
    }
}

// List of predefined colors for categories
val DESIGN_COLORS = listOf(
    "#FF5722", // Deep Orange
    "#E91E63", // Hot Pink
    "#9C27B0", // Royal Purple
    "#2196F3", // Blue Accent
    "#00BCD4", // Cyan Accent
    "#4CAF50", // Mint Green
    "#FFC107", // Sunny Gold
    "#795548", // Wood Brown
    "#607D8B", // Slate Blue
    "#FF3D00"  // Neo Red
)

// List of predefined icons key-label representation
val DESIGN_ICONS = listOf(
    Pair("restaurant", "Food & Drinks"),
    Pair("shopping", "Shopping"),
    Pair("travel", "Transportation"),
    Pair("bill", "Utilities & Bills"),
    Pair("play", "Leisure & Fun"),
    Pair("home", "Household"),
    Pair("info", "General")
)

// ==========================================
// 1. EXPENSES TAB VIEW
// ==========================================
@Composable
fun ExpensesTabScreen(
    expenses: List<ExpenseWithCategory>,
    categories: List<Category>,
    onEditExpense: (ExpenseWithCategory) -> Unit,
    onDeleteExpense: (ExpenseWithCategory) -> Unit
) {
    var selectedFilterCategoryId by remember { mutableStateOf<Int?>(null) }

    val filteredExpenses = if (selectedFilterCategoryId == null) {
        expenses
    } else {
        expenses.filter { it.expense.categoryId == selectedFilterCategoryId }
    }

    val totalSpent = expenses.sumOf { it.expense.amount }
    val filteredSpent = filteredExpenses.sumOf { it.expense.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expenses_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Total Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(totalSpent),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    if (selectedFilterCategoryId != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Text(
                                text = "Filtered Spend: ${formatCurrency(filteredSpent)}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Category Selection Row (Filter)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Filter Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterCategoryId == null,
                            onClick = { selectedFilterCategoryId = null },
                            label = { Text("All") },
                            modifier = Modifier.testTag("filter_all")
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedFilterCategoryId == category.id,
                            onClick = { selectedFilterCategoryId = category.id },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parseColorHex(category.colorHex))
                                )
                            },
                            modifier = Modifier.testTag("filter_category_${category.id}")
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Empty History",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No expenses found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(filteredExpenses) { item ->
                ExpenseRowItem(
                    item = item,
                    onClick = { onEditExpense(item) },
                    onDelete = { onDeleteExpense(item) }
                )
            }
        }
    }
}

@Composable
fun ExpenseRowItem(
    item: ExpenseWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = parseColorHex(item.category?.colorHex ?: "#9E9E9E")
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("expense_row_${item.expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category accent indicator vertical bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Icon background container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.category?.iconName ?: "info"),
                    contentDescription = item.category?.name ?: "Expense Icon",
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Body Columns
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category?.name ?: "Uncategorized",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(item.expense.dateMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Spend amount
            Text(
                text = formatCurrency(item.expense.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Direct Delete trigger
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_expense_${item.expense.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ==========================================
// 2. CATEGORIES TAB VIEW
// ==========================================
@Composable
fun CategoriesTabScreen(
    categories: List<Category>,
    expenses: List<ExpenseWithCategory>,
    onDeleteCategory: (Category) -> Unit
) {
    // Map expenses by Category ID for dynamic summaries
    val categoryStats = remember(expenses) {
        expenses.groupBy { it.expense.categoryId }.mapValues { entry ->
            val total = entry.value.sumOf { it.expense.amount }
            val count = entry.value.size
            Pair(count, total)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("categories_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Expense Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (categories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No Categories available. Match, update or create custom.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(categories) { category ->
                val stats = categoryStats[category.id] ?: Pair(0, 0.0)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_card_${category.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored visual dot
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(category.colorHex).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = category.name,
                                tint = parseColorHex(category.colorHex),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.first} Transactions  •  Total Spent: ${formatCurrency(stats.second)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Prevent deleting default critical category index to avoid dangling tables
                        val canDelete = stats.first == 0
                        if (canDelete) {
                            IconButton(
                                onClick = { onDeleteCategory(category) },
                                modifier = Modifier.testTag("delete_category_${category.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Category",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            // Locked badge for in-use categories
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Active database category",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ANALYTICS (MONTHLY TOTAL CHART) VIEW
// ==========================================
@Composable
fun AnalyticsTabScreen(
    expenses: List<ExpenseWithCategory>
) {
    val monthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    // Aggregate monthly statistics
    val monthlyData = remember(expenses) {
        val groupedByMonth = expenses.groupBy {
            monthFormat.format(Date(it.expense.dateMillis))
        }

        groupedByMonth.map { (monthLabel, monthlyList) ->
            val totalSpent = monthlyList.sumOf { it.expense.amount }
            Pair(monthLabel, totalSpent)
        }.sortedWith { a, b ->
            try {
                val dateA = monthFormat.parse(a.first) ?: Date(0)
                val dateB = monthFormat.parse(b.first) ?: Date(0)
                dateA.compareTo(dateB)
            } catch (e: Exception) {
                0
            }
        }
    }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val maxAmount = remember(monthlyData) {
        monthlyData.maxOfOrNull { it.second } ?: 1.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Spending Trends",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Monthly total expenses across cycles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (monthlyData.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Enter expenses to calculate trend curves",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        } else {
            // Chart Container
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Monthly Spend Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Interactive Graph Canvas
                        val barColor = MaterialTheme.colorScheme.primary
                        val barSelectedColor = MaterialTheme.colorScheme.error
                        val gridLineColor = MaterialTheme.colorScheme.outlineVariant

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { /* Tap tracking on positions if expanded */ }
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Draw horizontal grid lines
                                val gridLineCount = 4
                                for (i in 0..gridLineCount) {
                                    val y = (canvasHeight / gridLineCount) * i
                                    drawLine(
                                        color = gridLineColor,
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Draw vertical Bars
                                val barCount = monthlyData.size
                                val spaceBetween = 24.dp.toPx()
                                val totalSpacingWidth = spaceBetween * (barCount + 1)
                                val availableWidthForBars = canvasWidth - totalSpacingWidth
                                val barWidth = if (barCount > 0) availableWidthForBars / barCount else 1f

                                monthlyData.forEachIndexed { index, pair ->
                                    val amount = pair.second
                                    val percentHeight = (amount / maxAmount).toFloat()
                                    val finalBarHeight = canvasHeight * percentHeight * 0.85f // Avoid touching the ceiling

                                    val x = spaceBetween + (index * (barWidth + spaceBetween))
                                    val y = canvasHeight - finalBarHeight

                                    // Render dynamic gradients
                                    val paintColor = if (selectedBarIndex == index) barSelectedColor else barColor

                                    drawRoundRect(
                                        color = paintColor,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, finalBarHeight),
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Labels aligned with column spacing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            monthlyData.forEachIndexed { index, pair ->
                                val isSelected = selectedBarIndex == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            selectedBarIndex = if (isSelected) null else index
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pair.first,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Month description detail summary
            item {
                AnimatedVisibility(
                    visible = selectedBarIndex != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    selectedBarIndex?.let { index ->
                        if (index in monthlyData.indices) {
                            val data = monthlyData[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chart_monthly_detail"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            data.first,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Aggregated Spending Cycle",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        formatCurrency(data.second),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Breakdown List section
            item {
                Text(
                    "Historical Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(monthlyData.asReversed()) { data ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            data.first,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            formatCurrency(data.second),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// FORM DIALOGS (EXPENSE)
// ==========================================
@Composable
fun ExpenseFormDialog(
    categories: List<Category>,
    expenseToEdit: ExpenseWithCategory? = null,
    onDismiss: () -> Unit,
    onSave: (description: String, amount: Double, categoryId: Int, dateMillis: Long) -> Unit
) {
    var description by remember { mutableStateOf(expenseToEdit?.expense?.description ?: "") }
    var amountText by remember { mutableStateOf(expenseToEdit?.expense?.amount?.toString() ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(
            expenseToEdit?.expense?.categoryId ?: categories.firstOrNull()?.id ?: 0
        )
    }
    var dateMillis by remember { mutableStateOf(expenseToEdit?.expense?.dateMillis ?: System.currentTimeMillis()) }

    var descriptionError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("expense_form_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (expenseToEdit == null) "Add Expense" else "Edit Expense",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = false
                    },
                    label = { Text("Description") },
                    isError = descriptionError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_description")
                )

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount")
                )

                // Date Selector Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Instant offset triggers: lets user pick either today, or yesterday to avoid complex dialog trees
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            "Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            dateFormat.format(Date(dateMillis)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Date Offset buttons directly embedded inside Dialog to keep it clean and fast!
                    TextButton(onClick = { dateMillis = System.currentTimeMillis() }) {
                        Text("Today")
                    }
                }

                // Category Selection Picker
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategoryId == category.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        parseColorHex(category.colorHex)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { selectedCategoryId = category.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("select_category_chip_${category.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = category.name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (description.isBlank()) descriptionError = true
                            if (amount == null || amount <= 0.0) amountError = true

                            if (description.isNotBlank() && amount != null && amount > 0.0) {
                                onSave(description, amount, selectedCategoryId, dateMillis)
                            }
                        },
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ==========================================
// FORM DIALOGS (CATEGORY)
// ==========================================
@Composable
fun CategoryFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(DESIGN_COLORS.first()) }
    var selectedIconIndex by remember { mutableStateOf(0) }

    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("category_form_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Category Name") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_category_name")
                )

                // Select visual Color circle
                Text(
                    "Category Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DESIGN_COLORS.take(5).forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(colorHex))
                                .clickable { selectedColor = colorHex }
                                .testTag("color_item_$colorHex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DESIGN_COLORS.drop(5).forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(colorHex))
                                .clickable { selectedColor = colorHex }
                                .testTag("color_item_$colorHex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Select representation icon key
                Text(
                    "Category Icon representation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DESIGN_ICONS.size) { index ->
                        val item = DESIGN_ICONS[index]
                        val isSelected = selectedIconIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        parseColorHex(selectedColor)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { selectedIconIndex = index }
                                .padding(12.dp)
                                .testTag("icon_option_${item.first}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(item.first),
                                contentDescription = item.second,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                            } else {
                                onSave(name, selectedColor, DESIGN_ICONS[selectedIconIndex].first)
                            }
                        },
                        modifier = Modifier.testTag("save_category_button")
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
