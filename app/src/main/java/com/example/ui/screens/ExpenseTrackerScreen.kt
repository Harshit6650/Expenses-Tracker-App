package com.example.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.Category
import com.example.data.Expense
import com.example.data.ExpenseWithCategory
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.NotificationHelper
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

    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val profilePhotoPath by viewModel.profilePhotoPath.collectAsStateWithLifecycle()
    val profileAvatarIndex by viewModel.profileAvatarIndex.collectAsStateWithLifecycle()
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()

    var isSplashFinished by remember { mutableStateOf(false) }

    if (!isSplashFinished) {
        SplashAnimationScreen(onFinished = { isSplashFinished = true })
    } else if (!isUserLoggedIn) {
        AuthLandingScreen(viewModel = viewModel, onLoginCompleted = {})
    } else if (!isOnboardingCompleted) {
        val googleEmail by viewModel.userEmailState.collectAsStateWithLifecycle()
        val googleName by viewModel.userNameState.collectAsStateWithLifecycle()
        OnboardingFlowScreen(
            initialName = googleName,
            initialEmail = googleEmail,
            onSubmit = { name, age, email, occupation ->
                viewModel.submitOnboarding(name, age, email, occupation)
            }
        )
    } else {
        var showAddExpenseDialog by remember { mutableStateOf(false) }
        var showAddCategoryDialog by remember { mutableStateOf(false) }
        var expenseToEdit by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    
    // Dynamic Deletion Protection Safeguards
    var expenseToDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    
    // Theme menu controller State
    var showThemeMenu by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

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
                    if (selectedTab == 0) {
                        IconButton(
                            onClick = { showAddExpenseDialog = true },
                            modifier = Modifier.testTag("fab_add_expense")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Expense")
                        }
                    }
                    if (selectedTab == 1) {
                        IconButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.testTag("action_add_category")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category")
                        }
                    }
                    
                    Box {
                        IconButton(
                            onClick = { showThemeMenu = true },
                            modifier = Modifier.testTag("action_theme_toggle")
                        ) {
                            val themeIcon = when (themeMode) {
                                "light" -> Icons.Default.Star
                                "dark" -> Icons.Default.Info
                                else -> Icons.Default.Settings
                            }
                            Icon(themeIcon, contentDescription = "Switch Theme")
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Light Mode") },
                                onClick = {
                                    viewModel.setThemeMode("light")
                                    showThemeMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Dark Mode") },
                                onClick = {
                                    viewModel.setThemeMode("dark")
                                    showThemeMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("System Default") },
                                onClick = {
                                    viewModel.setThemeMode("system")
                                    showThemeMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
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
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = {
                        ProfileAvatar(
                            avatarIndex = profileAvatarIndex,
                            name = profileName,
                            photoPath = profilePhotoPath,
                            size = 26.dp
                        )
                    },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_tab_profile")
                )
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
                    onDeleteExpense = { expenseToDelete = it },
                    viewModel = viewModel
                )
                1 -> CategoriesTabScreen(
                    categories = categories,
                    expenses = expenses,
                    onDeleteCategory = { categoryToDelete = it }
                )
                2 -> AnalyticsTabScreen(
                    expenses = expenses
                )
                3 -> ProfileTabScreen(
                    viewModel = viewModel
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

    // Safe Deletion Confirmation Dialog for Expenses (Accidental Protection)
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete expense record?") },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${expenseToDelete!!.expense.description}\" of ${formatCurrency(expenseToDelete!!.expense.amount)}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expenseToDelete!!.expense)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Safe Deletion Confirmation Dialog for Categories
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete category?") },
            text = {
                Text(
                    text = "Are you sure you want to delete the category \"${categoryToDelete!!.name}\"? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete!!)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
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

// Format double value safely to Local currency representation in INR (₹)
fun formatCurrency(amount: Double): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
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

// Elegant custom color-coordinated chip override
@Composable
fun SoftCategoryChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    categoryHex: String? = null,
    modifier: Modifier = Modifier
) {
    val themeColor = if (categoryHex != null) parseColorHex(categoryHex) else MaterialTheme.colorScheme.primary
    val containerBg = if (selected) themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val fontColor = if (selected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
    val borderCol = if (selected) themeColor else Color.Transparent

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = CircleShape,
        color = containerBg,
        contentColor = fontColor,
        border = if (selected) BorderStroke(1.5.dp, borderCol) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (categoryHex != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(themeColor)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ==========================================
// 1. EXPENSES TAB VIEW
// ==========================================
@Composable
fun ExpensesTabScreen(
    expenses: List<ExpenseWithCategory>,
    categories: List<Category>,
    onEditExpense: (ExpenseWithCategory) -> Unit,
    onDeleteExpense: (ExpenseWithCategory) -> Unit,
    viewModel: com.example.ui.viewmodel.ExpenseViewModel
) {
    var selectedFilterCategoryId by remember { mutableStateOf<Int?>(null) }

    val filteredExpenses = if (selectedFilterCategoryId == null) {
        expenses
    } else {
        expenses.filter { it.expense.categoryId == selectedFilterCategoryId }
    }

    val totalSpent = expenses.sumOf { it.expense.amount }
    val filteredSpent = filteredExpenses.sumOf { it.expense.amount }
    
    // Live statistics processing
    val maxExpense = if (expenses.isNotEmpty()) expenses.maxOf { it.expense.amount } else 0.0
    val averageSpend = if (expenses.isNotEmpty()) totalSpent / expenses.size else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("expenses_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card (Premium Fintech Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                val cardPrimary = MaterialTheme.colorScheme.primary
                val cardSecondary = MaterialTheme.colorScheme.secondary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // High modern visual canvas gradient using our primary & secondary schemes
                            val colors = listOf(cardPrimary, cardSecondary)
                            val brush = Brush.linearGradient(
                                colors = colors,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                            drawRect(brush = brush)
                            
                            // Glowing visual ambient highlights
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = size.width * 0.5f,
                                center = Offset(size.width, 0f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = size.width * 0.35f,
                                center = Offset(0f, size.height)
                            )
                        }
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WISEWALLET MEMBER",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp, 20.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.linearGradient(colors = listOf(Color(0xFFFBC02D), Color(0xFFF9A825))))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Total Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(totalSpent),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    
                    if (selectedFilterCategoryId != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Text(
                                text = "Filtered Spend: ${formatCurrency(filteredSpent)}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Monthly Budget Limit Card (Material 3 Adaptive Progress layout)
        item {
            val budgetLimit by viewModel.monthlyBudgetLimit.collectAsState()
            val currentMonthSpent = viewModel.calculateCurrentMonthSpent(expenses)
            var showBudgetDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_limit_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Budget",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Budget Limit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showBudgetDialog = true },
                            modifier = Modifier.size(36.dp).testTag("edit_budget_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Budget",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val percent = if (budgetLimit > 0.0) {
                        (currentMonthSpent / budgetLimit).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val isOverBudget = budgetLimit > 0.0 && currentMonthSpent > budgetLimit

                    // Beautiful linear progress indicating spend ratio
                    val progressColor = if (isOverBudget) {
                        MaterialTheme.colorScheme.error
                    } else if (percent > 0.85f) {
                        Color(0xFFF9A825) // Warning yellow
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    LinearProgressIndicator(
                        progress = percent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spent: ${formatCurrency(currentMonthSpent)} of ${formatCurrency(budgetLimit)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isOverBudget) {
                            val overrunAmt = currentMonthSpent - budgetLimit
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "Over: ${formatCurrency(overrunAmt)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        } else {
                            val remainingAmt = maxOf(0.0, budgetLimit - currentMonthSpent)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "Left: ${formatCurrency(remainingAmt)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (showBudgetDialog) {
                BudgetLimitDialog(
                    currentLimit = budgetLimit,
                    onDismissRequest = { showBudgetDialog = false },
                    onSave = { newLimit ->
                        viewModel.updateMonthlyBudgetLimit(newLimit)
                    }
                )
            }
        }

        // Horizontal Quick Statistics Board
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Max spend micro-insight
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Max Purchase",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatCurrency(maxExpense),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Average spend micro-insight
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Avg Purchase",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatCurrency(averageSpend),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Count purchase item
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Records",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${expenses.size} items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Category Selection Row (Filter)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Filter Spending Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SoftCategoryChip(
                            selected = selectedFilterCategoryId == null,
                            onClick = { selectedFilterCategoryId = null },
                            label = "All List",
                            modifier = Modifier.testTag("filter_all")
                        )
                    }
                    items(categories) { category ->
                        SoftCategoryChip(
                            selected = selectedFilterCategoryId == category.id,
                            onClick = { selectedFilterCategoryId = category.id },
                            label = category.name,
                            categoryHex = category.colorHex,
                            modifier = Modifier.testTag("filter_category_${category.id}")
                        )
                    }
                }
            }
        }

        // Transaction History Title Header
        item {
            Text(
                text = "Transaction Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No transactions in this filter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Record an expense by clicking the '+' button below,\nor pick a different category option.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
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
    val categoryColor = parseColorHex(item.category?.colorHex ?: "#949494")
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("expense_row_${item.expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category accent indicator vertical bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Icon background container with border outline
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f))
                    .drawBehind {
                        drawCircle(
                            color = categoryColor.copy(alpha = 0.25f),
                            radius = size.minDimension / 2f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.category?.iconName ?: "info"),
                    contentDescription = item.category?.name ?: "Expense Icon",
                    tint = categoryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details and values in structured vertical and horizontal layout
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // First horizontal line: Description and Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatCurrency(item.expense.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                }

                // Second horizontal line: Category (with capsule background) and Date, plus Delete Button at end
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category and Date wrapped in Row
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Small elegant capsule badge for category name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(categoryColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.category?.name ?: "Uncategorized",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Separator Dot
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Date Text
                        Text(
                            text = dateFormat.format(Date(item.expense.dateMillis)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Small compact delete icon at the right end
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_expense_${item.expense.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete transaction",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Expense Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Define custom themes and track specific compartments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        "No categories configured. Tap add above to create.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(categories) { category ->
                val stats = categoryStats[category.id] ?: Pair(0, 0.0)
                val catColor = parseColorHex(category.colorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_card_${category.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
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
                                .background(catColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = category.name,
                                tint = catColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.first} Transactions  •  Total Spent: ${formatCurrency(stats.second)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        // Protect deletion of categories holding transactions to avoid dangling keys
                        val canDelete = stats.first == 0
                        if (canDelete) {
                            IconButton(
                                onClick = { onDeleteCategory(category) },
                                modifier = Modifier.testTag("delete_category_${category.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Category",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Locked badge for active labels
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Active database category",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
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
                "Aggregate monthly spending cycles and trend curves.",
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
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
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
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Enter transactions to view spending trend graphs.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        } else {
            // Chart Container Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            "Monthly Spend Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Click directly on any bar below to view details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Interactive Graph Canvas with Pointer Detect Tap
                        val barColor = MaterialTheme.colorScheme.primary
                        val barSelectedColor = MaterialTheme.colorScheme.error
                        val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(monthlyData) {
                                        detectTapGestures { offset ->
                                            val canvasWidth = size.width
                                            val barCount = monthlyData.size
                                            if (barCount > 0) {
                                                val spaceBetween = 24.dp.toPx()
                                                val totalSpacingWidth = spaceBetween * (barCount + 1)
                                                val availableWidthForBars = canvasWidth - totalSpacingWidth
                                                val barWidth = maxOf(1f, availableWidthForBars / barCount)

                                                var tappedIndex: Int? = null
                                                for (index in 0 until barCount) {
                                                    val left = spaceBetween + (index * (barWidth + spaceBetween))
                                                    val right = left + barWidth
                                                    if (offset.x >= left && offset.x <= right) {
                                                        tappedIndex = index
                                                        break
                                                    }
                                                }
                                                if (tappedIndex != null) {
                                                    selectedBarIndex = if (selectedBarIndex == tappedIndex) null else tappedIndex
                                                }
                                            }
                                        }
                                    }
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
                                val barWidth = if (barCount > 0) maxOf(1f, availableWidthForBars / barCount) else 1f

                                monthlyData.forEachIndexed { index, pair ->
                                    val amount = pair.second
                                    val percentHeight = (amount / maxAmount).toFloat()
                                    val finalBarHeight = maxOf(0f, canvasHeight * percentHeight * 0.85f) // Avoid ceiling touching

                                    val x = spaceBetween + (index * (barWidth + spaceBetween))
                                    val y = canvasHeight - finalBarHeight

                                    val gradient = if (selectedBarIndex == index) {
                                        Brush.verticalGradient(
                                            colors = listOf(barSelectedColor, barSelectedColor.copy(alpha = 0.5f))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(barColor, barColor.copy(alpha = 0.4f))
                                        )
                                    }

                                    drawRoundRect(
                                        brush = gradient,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, finalBarHeight),
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Label selections placed under bottom axes coordinates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            monthlyData.forEachIndexed { index, pair ->
                                val isSelected = selectedBarIndex == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            selectedBarIndex = if (isSelected) null else index
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pair.first,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Month description detail summary selection drawer
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
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
                                            "Aggregated Spending Volume",
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

            // Breakdown List section title
            item {
                Text(
                    "Historical Cycles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(monthlyData.asReversed()) { data ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
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

    val context = LocalContext.current

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
                Column(modifier = Modifier.fillMaxWidth()) {
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
                    if (descriptionError) {
                        Text(
                            text = "Please enter a valid description",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Amount Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            amountError = false
                        },
                        label = { Text("Amount (INR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError,
                        singleLine = true,
                        leadingIcon = { Text("₹") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amount")
                    )
                    if (amountError) {
                        Text(
                            text = "Please enter an amount greater than 0",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Date Selector Indicator (Responsive standard Date Picker Dialog)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
                            val activityContext = context.findActivity() ?: context
                            DatePickerDialog(
                                activityContext,
                                { _, year, month, dayOfMonth ->
                                    val selected = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    dateMillis = selected.timeInMillis
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                                "Transaction Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                dateFormat.format(Date(dateMillis)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = { dateMillis = System.currentTimeMillis() },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Today", fontWeight = FontWeight.Bold)
                        }
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
                        val catColor = parseColorHex(category.colorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        catColor
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

                Column(modifier = Modifier.fillMaxWidth()) {
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
                    if (nameError) {
                        Text(
                            text = "Please enter a category name",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Color swatch grid
                Text(
                    "Category Color Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val colorsPerRow = 5
                    val rows = DESIGN_COLORS.chunked(colorsPerRow)
                    rows.forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowColors.forEach { colorHex ->
                                val isSelected = selectedColor == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(parseColorHex(colorHex))
                                        .clickable { selectedColor = colorHex }
                                        .testTag("color_item_$colorHex")
                                        .shadow(
                                            elevation = if (isSelected) 4.dp else 0.dp,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Select representation icon key
                Text(
                    "Select Category Icon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
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
                                .padding(14.dp)
                                .testTag("icon_option_${item.first}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(item.first),
                                contentDescription = item.second,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
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

private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.filesDir, "profile_photo.jpg")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ==========================================
// 4. PROFILE & SETTINGS TAB VIEW
// ==========================================
@Composable
fun ProfileTabScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profileAge by viewModel.profileAge.collectAsStateWithLifecycle()
    val profileEmail by viewModel.profileEmail.collectAsStateWithLifecycle()
    val profilePhone by viewModel.profilePhone.collectAsStateWithLifecycle()
    val profileOccupation by viewModel.profileOccupation.collectAsStateWithLifecycle()
    val profileAvatarIndex by viewModel.profileAvatarIndex.collectAsStateWithLifecycle()
    val profilePhotoPath by viewModel.profilePhotoPath.collectAsStateWithLifecycle()
    
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val savedPath = saveImageToInternalStorage(context, it)
                if (savedPath != null) {
                    viewModel.updateProfilePhoto(savedPath)
                }
            }
        }
    }

    var showPhotoOptionDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    // Temp edit fields state
    var editName by remember(profileName) { mutableStateOf(profileName) }
    var editAge by remember(profileAge) { mutableStateOf(profileAge) }
    var editEmail by remember(profileEmail) { mutableStateOf(profileEmail) }
    var editPhone by remember(profilePhone) { mutableStateOf(profilePhone) }
    var editOccupation by remember(profileOccupation) { mutableStateOf(profileOccupation) }
    var editAvatarIndex by remember(profileAvatarIndex) { mutableStateOf(profileAvatarIndex) }

    // Field errors
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }

    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showExportConfigDialog by remember { mutableStateOf(false) }
    var tempExportCsvContent by remember { mutableStateOf("") }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null && tempExportCsvContent.isNotEmpty()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(tempExportCsvContent.toByteArray())
                }
                android.widget.Toast.makeText(context, "Export saved successfully to device!", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error saving file: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    if (showPhotoOptionDialog) {
        Dialog(onDismissRequest = { showPhotoOptionDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Upload Profile Photo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Select an image from your device's gallery to customize your profile picture.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Local Device Picker Action
                    Button(
                        onClick = {
                            try {
                                photoLauncher.launch("image/*")
                                showPhotoOptionDialog = false
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "No image picker available on device.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose from Gallery")
                    }

                    if (profilePhotoPath.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.updateProfilePhoto("")
                                showPhotoOptionDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Remove Current Photo")
                        }
                    }

                    TextButton(
                        onClick = { showPhotoOptionDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    if (showExportConfigDialog) {
        Dialog(onDismissRequest = { showExportConfigDialog = false }) {
            var selectedCategoryTmp by remember { mutableStateOf<Category?>(null) }
            var dateConstraintPreset by remember { mutableStateOf("all_time") }
            var startMillisTmp by remember { mutableStateOf<Long?>(null) }
            var endMillisTmp by remember { mutableStateOf<Long?>(null) }
            var minAmountTmp by remember { mutableStateOf("") }
            var maxAmountTmp by remember { mutableStateOf("") }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = "Export Options",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Filter and download transactions to CSV format compatible with Google Sheets & Excel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 1. SELECT CATEGORY CHIP ROW
                    item {
                        Text(
                            text = "Filter by Category",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                CustomChoiceChip(
                                    selected = selectedCategoryTmp == null,
                                    onClick = { selectedCategoryTmp = null },
                                    label = "All Categories"
                                )
                            }
                            items(categories) { cat ->
                                CustomChoiceChip(
                                    selected = selectedCategoryTmp?.id == cat.id,
                                    onClick = { selectedCategoryTmp = cat },
                                    label = cat.name
                                )
                            }
                        }
                    }

                    // 2. DATE PRESET CHIP ROW
                    item {
                        Text(
                            text = "Filter by Timeframe",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val timePresets = listOf(
                                "all_time" to "All Time",
                                "this_month" to "This Month",
                                "last_month" to "Last Month",
                                "this_year" to "This Year",
                                "custom" to "Custom"
                            )
                            items(timePresets) { (presetKey, presetName) ->
                                CustomChoiceChip(
                                    selected = dateConstraintPreset == presetKey,
                                    onClick = { 
                                        dateConstraintPreset = presetKey
                                        if (presetKey != "custom") {
                                            startMillisTmp = null
                                            endMillisTmp = null
                                        } else {
                                            val c = Calendar.getInstance()
                                            endMillisTmp = c.timeInMillis
                                            c.add(Calendar.DAY_OF_YEAR, -30)
                                            startMillisTmp = c.timeInMillis
                                        }
                                    },
                                    label = presetName
                                )
                            }
                        }
                    }

                    // 3. CUSTOM DATE SELECTORS (only if 'customTime' is active)
                    if (dateConstraintPreset == "custom") {
                        item {
                            val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Start date
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val currentVal = startMillisTmp ?: System.currentTimeMillis()
                                            val c = Calendar.getInstance().apply { timeInMillis = currentVal }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val selected = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                        set(Calendar.HOUR_OF_DAY, 0)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }
                                                    startMillisTmp = selected.timeInMillis
                                                },
                                                c.get(Calendar.YEAR),
                                                c.get(Calendar.MONTH),
                                                c.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Start Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = startMillisTmp?.let { sdf.format(Date(it)) } ?: "Select Date",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // End date
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val currentVal = endMillisTmp ?: System.currentTimeMillis()
                                            val c = Calendar.getInstance().apply { timeInMillis = currentVal }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val selected = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 59)
                                                        set(Calendar.MILLISECOND, 999)
                                                    }
                                                    endMillisTmp = selected.timeInMillis
                                                },
                                                c.get(Calendar.YEAR),
                                                c.get(Calendar.MONTH),
                                                c.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("End Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = endMillisTmp?.let { sdf.format(Date(it)) } ?: "Select Date",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. AMOUNT EXTRA FILTERS
                    item {
                        Text(
                            text = "Filter by Amount Range (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = minAmountTmp,
                                onValueChange = { minAmountTmp = it },
                                label = { Text("Min Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxAmountTmp,
                                onValueChange = { maxAmountTmp = it },
                                label = { Text("Max Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // LIVE COUNTER OF FILTERED ITEMS
                    item {
                        val currentFilteredList = expenses.filter { item ->
                            val expense = item.expense
                            val matchesCategory = selectedCategoryTmp == null || expense.categoryId == selectedCategoryTmp?.id
                            
                            val matchesDate = when (dateConstraintPreset) {
                                "this_month" -> {
                                    val cal = Calendar.getInstance()
                                    val currentYear = cal.get(Calendar.YEAR)
                                    val currentMonth = cal.get(Calendar.MONTH)
                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                    expCal.get(Calendar.YEAR) == currentYear && expCal.get(Calendar.MONTH) == currentMonth
                                }
                                "last_month" -> {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, -1)
                                    val lastYear = cal.get(Calendar.YEAR)
                                    val lastMonth = cal.get(Calendar.MONTH)
                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                    expCal.get(Calendar.YEAR) == lastYear && expCal.get(Calendar.MONTH) == lastMonth
                                }
                                "this_year" -> {
                                    val cal = Calendar.getInstance()
                                    val currentYear = cal.get(Calendar.YEAR)
                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                    expCal.get(Calendar.YEAR) == currentYear
                                }
                                "custom" -> {
                                    val startsOk = startMillisTmp == null || expense.dateMillis >= startMillisTmp!!
                                    val endsOk = endMillisTmp == null || expense.dateMillis <= endMillisTmp!!
                                    startsOk && endsOk
                                }
                                else -> true // all_time
                            }
                            
                            val minAmt = minAmountTmp.toDoubleOrNull()
                            val maxAmt = maxAmountTmp.toDoubleOrNull()
                            val matchesMin = minAmt == null || expense.amount >= minAmt
                            val matchesMax = maxAmt == null || expense.amount <= maxAmt
                            
                            matchesCategory && matchesDate && matchesMin && matchesMax
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Filtered Records", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${currentFilteredList.size} of ${expenses.size}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }

                    // 5. EXPORT ACTIONS
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val currentFilteredList = expenses.filter { item ->
                                        val expense = item.expense
                                        val matchesCategory = selectedCategoryTmp == null || expense.categoryId == selectedCategoryTmp?.id
                                        
                                        val matchesDate = when (dateConstraintPreset) {
                                            "this_month" -> {
                                                val cal = Calendar.getInstance()
                                                val currentYear = cal.get(Calendar.YEAR)
                                                val currentMonth = cal.get(Calendar.MONTH)
                                                val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                expCal.get(Calendar.YEAR) == currentYear && expCal.get(Calendar.MONTH) == currentMonth
                                            }
                                            "last_month" -> {
                                                val cal = Calendar.getInstance()
                                                cal.add(Calendar.MONTH, -1)
                                                val lastYear = cal.get(Calendar.YEAR)
                                                val lastMonth = cal.get(Calendar.MONTH)
                                                val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                expCal.get(Calendar.YEAR) == lastYear && expCal.get(Calendar.MONTH) == lastMonth
                                            }
                                            "this_year" -> {
                                                val cal = Calendar.getInstance()
                                                val currentYear = cal.get(Calendar.YEAR)
                                                val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                expCal.get(Calendar.YEAR) == currentYear
                                            }
                                            "custom" -> {
                                                val startsOk = startMillisTmp == null || expense.dateMillis >= startMillisTmp!!
                                                val endsOk = endMillisTmp == null || expense.dateMillis <= endMillisTmp!!
                                                startsOk && endsOk
                                            }
                                            else -> true // all_time
                                        }
                                        
                                        val minAmt = minAmountTmp.toDoubleOrNull()
                                        val maxAmt = maxAmountTmp.toDoubleOrNull()
                                        val matchesMin = minAmt == null || expense.amount >= minAmt
                                        val matchesMax = maxAmt == null || expense.amount <= maxAmt
                                        
                                        matchesCategory && matchesDate && matchesMin && matchesMax
                                    }

                                    if (currentFilteredList.isEmpty()) {
                                        android.widget.Toast.makeText(context, "No transactions found matching active filters.", android.widget.Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val headerRow = "Transaction ID,Date,Category,Description,Amount\n"
                                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val records = currentFilteredList.map { item ->
                                        val exp = item.expense
                                        val catName = item.category?.name ?: "Unknown"
                                        val formattedDate = df.format(Date(exp.dateMillis))
                                        
                                        "${exp.id},${formattedDate.escapeCsv()},${catName.escapeCsv()},${exp.description.escapeCsv()},${exp.amount}"
                                    }.joinToString("\n")
                                    
                                    tempExportCsvContent = headerRow + records
                                    try {
                                        exportDocumentLauncher.launch("wisewallet_export_${System.currentTimeMillis()}.csv")
                                        showExportConfigDialog = false
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed to launch device file saver: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("action_export_save_file"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Document (.CSV)")
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val currentFilteredList = expenses.filter { item ->
                                            val expense = item.expense
                                            val matchesCategory = selectedCategoryTmp == null || expense.categoryId == selectedCategoryTmp?.id
                                            
                                            val matchesDate = when (dateConstraintPreset) {
                                                "this_month" -> {
                                                    val cal = Calendar.getInstance()
                                                    val currentYear = cal.get(Calendar.YEAR)
                                                    val currentMonth = cal.get(Calendar.MONTH)
                                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                    expCal.get(Calendar.YEAR) == currentYear && expCal.get(Calendar.MONTH) == currentMonth
                                                }
                                                "last_month" -> {
                                                    val cal = Calendar.getInstance()
                                                    val m = cal.add(Calendar.MONTH, -1)
                                                    val lastYear = cal.get(Calendar.YEAR)
                                                    val lastMonth = cal.get(Calendar.MONTH)
                                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                    expCal.get(Calendar.YEAR) == lastYear && expCal.get(Calendar.MONTH) == lastMonth
                                                }
                                                "this_year" -> {
                                                    val cal = Calendar.getInstance()
                                                    val currentYear = cal.get(Calendar.YEAR)
                                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                    expCal.get(Calendar.YEAR) == currentYear
                                                }
                                                "custom" -> {
                                                    val startsOk = startMillisTmp == null || expense.dateMillis >= startMillisTmp!!
                                                    val endsOk = endMillisTmp == null || expense.dateMillis <= endMillisTmp!!
                                                    startsOk && endsOk
                                                }
                                                else -> true // all_time
                                            }
                                            
                                            val minAmt = minAmountTmp.toDoubleOrNull()
                                            val maxAmt = maxAmountTmp.toDoubleOrNull()
                                            val matchesMin = minAmt == null || expense.amount >= minAmt
                                            val matchesMax = maxAmt == null || expense.amount <= maxAmt
                                            
                                            matchesCategory && matchesDate && matchesMin && matchesMax
                                        }

                                        if (currentFilteredList.isEmpty()) {
                                            android.widget.Toast.makeText(context, "No transactions to share.", android.widget.Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }

                                        val headerRow = "Transaction ID,Date,Category,Description,Amount\n"
                                        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        val records = currentFilteredList.map { item ->
                                            val exp = item.expense
                                            val catName = item.category?.name ?: "Unknown"
                                            val formattedDate = df.format(Date(exp.dateMillis))
                                            
                                            "${exp.id},${formattedDate.escapeCsv()},${catName.escapeCsv()},${exp.description.escapeCsv()},${exp.amount}"
                                        }.joinToString("\n")
                                        
                                        val fullCsvText = headerRow + records

                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "WiseWallet Transaction Export")
                                            putExtra(android.content.Intent.EXTRA_TEXT, fullCsvText)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share CSV text via..."))
                                        showExportConfigDialog = false
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("action_export_share"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Text", style = MaterialTheme.typography.bodySmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val currentFilteredList = expenses.filter { item ->
                                            val expense = item.expense
                                            val matchesCategory = selectedCategoryTmp == null || expense.categoryId == selectedCategoryTmp?.id
                                            
                                            val matchesDate = when (dateConstraintPreset) {
                                                "this_month" -> {
                                                    val cal = Calendar.getInstance()
                                                    val currentYear = cal.get(Calendar.YEAR)
                                                    val currentMonth = cal.get(Calendar.MONTH)
                                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                    expCal.get(Calendar.YEAR) == currentYear && expCal.get(Calendar.MONTH) == currentMonth
                                                }
                                                "last_month" -> {
                                                    val cal = Calendar.getInstance()
                                                    cal.add(Calendar.MONTH, -1)
                                                    val lastYear = cal.get(Calendar.YEAR)
                                                    val lastMonth = cal.get(Calendar.MONTH)
                                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                    expCal.get(Calendar.YEAR) == lastYear && expCal.get(Calendar.MONTH) == lastMonth
                                                }
                                                "this_year" -> {
                                                    val cal = Calendar.getInstance()
                                                    val currentYear = cal.get(Calendar.YEAR)
                                                    val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
                                                    expCal.get(Calendar.YEAR) == currentYear
                                                }
                                                "custom" -> {
                                                    val startsOk = startMillisTmp == null || expense.dateMillis >= startMillisTmp!!
                                                    val endsOk = endMillisTmp == null || expense.dateMillis <= endMillisTmp!!
                                                    startsOk && endsOk
                                                }
                                                else -> true // all_time
                                            }
                                            
                                            val minAmt = minAmountTmp.toDoubleOrNull()
                                            val maxAmt = maxAmountTmp.toDoubleOrNull()
                                            val matchesMin = minAmt == null || expense.amount >= minAmt
                                            val matchesMax = maxAmt == null || expense.amount <= maxAmt
                                            
                                            matchesCategory && matchesDate && matchesMin && matchesMax
                                        }

                                        if (currentFilteredList.isEmpty()) {
                                            android.widget.Toast.makeText(context, "No transactions to copy.", android.widget.Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }

                                        val headerRow = "Transaction ID,Date,Category,Description,Amount\n"
                                        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        val records = currentFilteredList.map { item ->
                                            val exp = item.expense
                                            val catName = item.category?.name ?: "Unknown"
                                            val formattedDate = df.format(Date(exp.dateMillis))
                                            
                                            "${exp.id},${formattedDate.escapeCsv()},${catName.escapeCsv()},${exp.description.escapeCsv()},${exp.amount}"
                                        }.joinToString("\n")
                                        
                                        val fullCsvText = headerRow + records

                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("WiseWallet Export", fullCsvText)
                                        clipboard.setPrimaryClip(clip)
                                        
                                        android.widget.Toast.makeText(context, "Copied CSV to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                        showExportConfigDialog = false
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("action_export_copy"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy CSV", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            TextButton(
                                onClick = { showExportConfigDialog = false },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Box
                    ProfileAvatar(
                        avatarIndex = if (isEditing) editAvatarIndex else profileAvatarIndex,
                        name = if (isEditing) editName else profileName,
                        photoPath = profilePhotoPath,
                        size = 96.dp,
                        onClick = {
                            showPhotoOptionDialog = true
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!isEditing) {
                        Text(
                            text = profileName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (profileOccupation.isNotBlank()) profileOccupation else "WiseWallet Member",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Choose Profile Avatar Theme",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Horizontal selection of gradients
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0..5) {
                                val isSelected = editAvatarIndex == i
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                when (i) {
                                                    0 -> listOf(Color(0xFF3F51B5), Color(0xFF2196F3))
                                                    1 -> listOf(Color(0xFF009688), Color(0xFF4CAF50))
                                                    2 -> listOf(Color(0xFF9C27B0), Color(0xFFE91E63))
                                                    3 -> listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                                    4 -> listOf(Color(0xFF607D8B), Color(0xFF455A64))
                                                    else -> listOf(Color(0xFFFFC107), Color(0xFFFF8F00))
                                                }
                                            )
                                        )
                                        .clickable { editAvatarIndex = i }
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected Theme",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isEditing) {
            // VIEW PROFILE INFO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileDetailRow(icon = Icons.Default.Person, title = "Age", value = "$profileAge years")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ProfileDetailRow(icon = Icons.Default.Email, title = "Email Address", value = profileEmail)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ProfileDetailRow(icon = Icons.Default.Phone, title = "Phone Number", value = profilePhone)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ProfileDetailRow(icon = Icons.Default.AccountBox, title = "Occupation", value = profileOccupation)
                    }
                }
            }
            
            // QUICK THEMES CONTROL CARD - DRAG & SELECT INTERACTIVE BOX CAPSULE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("theme_slider_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val themeIcon = when (themeMode) {
                                "light" -> Icons.Default.Star
                                "dark" -> Icons.Default.Info
                                else -> Icons.Default.Settings
                            }
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "App Theme Control",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Slide or drag the thumb to quickly alternate between standard Light, carbon Dark, or System Default look.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        var sliderValue by remember(themeMode) {
                            mutableStateOf(
                                when (themeMode) {
                                    "light" -> 1f
                                    "dark" -> 2f
                                    else -> 0f
                                }
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                androidx.compose.material3.Slider(
                                    value = sliderValue,
                                    onValueChange = { newValue ->
                                        sliderValue = newValue
                                        val rounded = (newValue + 0.5f).toInt()
                                        val targetMode = when (rounded) {
                                            1 -> "light"
                                            2 -> "dark"
                                            else -> "system"
                                        }
                                        if (themeMode != targetMode) {
                                            viewModel.setThemeMode(targetMode)
                                        }
                                    },
                                    valueRange = 0f..2f,
                                    steps = 1,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("theme_slider")
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val currentSnap = (sliderValue + 0.5f).toInt()
                                    listOf(
                                        "System" to 0,
                                        "Light" to 1,
                                        "Dark" to 2
                                    ).forEach { (label, step) ->
                                        val active = currentSnap == step
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (active) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            },
                                            modifier = Modifier.weight(1f),
                                            textAlign = when (step) {
                                                0 -> androidx.compose.ui.text.style.TextAlign.Start
                                                2 -> androidx.compose.ui.text.style.TextAlign.End
                                                else -> androidx.compose.ui.text.style.TextAlign.Center
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ACTIONS: EDIT & SHARE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Native Share Sheet
                    Button(
                        onClick = {
                            val formatted = """
                                --- WiseWallet Personal Profile ---
                                👤 Name: $profileName
                                🎂 Age: $profileAge years old
                                📧 Email: $profileEmail
                                📱 Phone: $profilePhone
                                💼 Occupation: $profileOccupation
                                ----------------------------------
                                Sent from WiseWallet Expense & Finance Manager
                            """.trimIndent()
                            
                            // Copy to clipboard
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("WiseWallet Profile", formatted)
                            clipboard.setPrimaryClip(clip)
                            
                            android.widget.Toast.makeText(context, "Profile details copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()

                            // Share sheet
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "WiseWallet Profile - $profileName")
                                putExtra(android.content.Intent.EXTRA_TEXT, formatted)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Profile with..."))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("action_share_profile"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Profile")
                    }

                    // Tweak details triggering edit view
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("action_edit_profile"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Details")
                    }
                }
            }

            // BEAUTIFUL CLOUD SECURITY SYNC CARD
            item {
                val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
                val userUid by viewModel.userUid.collectAsStateWithLifecycle()
                val userEmail by viewModel.userEmailState.collectAsStateWithLifecycle()
                val userPhone by viewModel.userPhoneState.collectAsStateWithLifecycle()
                val userName by viewModel.userNameState.collectAsStateWithLifecycle()
                val authProvider by viewModel.authProvider.collectAsStateWithLifecycle()
                val lastSyncedTime by viewModel.lastSyncedTime.collectAsStateWithLifecycle()
                val syncSyncing by viewModel.cloudSyncSyncing.collectAsStateWithLifecycle()
                val syncStatusText by viewModel.syncStatusText.collectAsStateWithLifecycle()
                val customServerUrl by viewModel.customServerUrl.collectAsStateWithLifecycle()

                var authTabState by remember { mutableStateOf(0) } // 0 = Google, 1 = Phone
                var googleEmailInput by remember { mutableStateOf("") }
                var googleNameInput by remember { mutableStateOf("") }
                var phoneInput by remember { mutableStateOf("") }
                var phoneOtpInput by remember { mutableStateOf("") }
                
                var showServerConfig by remember { mutableStateOf(false) }
                var customUrlInput by remember { mutableStateOf(customServerUrl) }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("cloud_sync_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUserLoggedIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                         else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isUserLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isUserLoggedIn) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (isUserLoggedIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WiseWallet Cloud Sync",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isUserLoggedIn) "🟢 Active Account: Connected" else "⚪ Offline Mode: Data stored locally",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUserLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Sync Activity / Live Processing Overlays
                        if (syncSyncing) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = syncStatusText ?: "Syncing database data...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (!isUserLoggedIn) {
                            // NOT LOGGED IN USER INTERFACE
                            Text(
                                text = "Secure your transaction history in our cloud database. Log in via Google or Mobile OTP to dynamically upload, merge, and recover your statistical data on any device or after application reinstallations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            // Tabs selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (authTabState == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { authTabState = 0 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Google Account", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (authTabState == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { authTabState = 1 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Mobile Number", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (authTabState == 0) {
                                // GOOGLE AUTH PANEL
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = googleEmailInput,
                                        onValueChange = { googleEmailInput = it },
                                        label = { Text("Google Account Email") },
                                        placeholder = { Text("username@gmail.com") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_google_email")
                                    )
                                    OutlinedTextField(
                                        value = googleNameInput,
                                        onValueChange = { googleNameInput = it },
                                        label = { Text("Your Display Name") },
                                        placeholder = { Text("Harshit Kumar") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_google_name")
                                    )
                                    
                                    // Helper preset button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Quick testing details:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(
                                            onClick = {
                                                googleEmailInput = "harshitkumar581@gmail.com"
                                                googleNameInput = "Harshit Kumar"
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Fill demo credentials", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val mail = googleEmailInput.trim()
                                            val name = if (googleNameInput.isNotBlank()) googleNameInput.trim() else "WiseWallet User"
                                            if (mail.contains("@")) {
                                                viewModel.loginWithGoogle(mail, name)
                                            } else {
                                                android.widget.Toast.makeText(context, "Please enter a valid Google email address.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("button_login_google")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Authorize & Sync with Google")
                                    }
                                }
                            } else {
                                // MOBILE OTP PANEL
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { phoneInput = it },
                                        label = { Text("Mobile Number (with Country Code)") },
                                        placeholder = { Text("+91 98765 43210") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_phone_number")
                                    )
                                    OutlinedTextField(
                                        value = phoneOtpInput,
                                        onValueChange = { phoneOtpInput = it },
                                        label = { Text("6-Digit Verification OTP") },
                                        placeholder = { Text("------") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_phone_otp")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Quick testing details:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(
                                            onClick = {
                                                phoneInput = "+91 99999 88888"
                                                phoneOtpInput = "123456"
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Fill demo credentials", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val phone = phoneInput.trim()
                                            val otp = phoneOtpInput.trim()
                                            if (phone.length >= 8 && otp.length == 6) {
                                                viewModel.loginWithPhone(phone, otp)
                                            } else {
                                                android.widget.Toast.makeText(context, "Enter a valid mobile (>=8 chars) and 6-digit OTP code.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("button_login_phone")
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verify SMS OTP & Authenticate")
                                    }
                                }
                            }
                        } else {
                            // REGISTERED & LOGGED IN USER INTERFACE
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "ACCOUNT SESS_TOKEN DETAILS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Display Name: $userName",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (userEmail.isNotEmpty()) {
                                            Text(
                                                text = "Google Email: $userEmail",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (userPhone.isNotEmpty()) {
                                            Text(
                                                text = "Linked Mobile: $userPhone",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "Cloud Server UID: $userUid",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Verification Provider: Signed in via ${authProvider}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Last synced: " + if (lastSyncedTime == 0L) "Never Synced"
                                                   else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastSyncedTime)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Interactive Synchronize Panel
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.syncWithCloud() },
                                        enabled = !syncSyncing,
                                        modifier = Modifier.weight(1.2f).height(48.dp).testTag("action_execute_sync"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync Now", style = MaterialTheme.typography.labelMedium)
                                    }

                                    OutlinedButton(
                                        onClick = { 
                                            // 1. Purge Room local data
                                            viewModel.purgeLocalDatabaseForDemo()
                                        },
                                        enabled = !syncSyncing,
                                        modifier = Modifier.weight(1.0f).height(48.dp).testTag("action_purge_demo"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Simulate Reinstall", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                // Informational hint on how to test
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("How to Test Cloud Data Reinstallation:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(
                                            text = "1. Record several expenses locally in the primary Expenses screen.\n2. Tap 'Sync Now' to save them securely in your cloud profile.\n3. Tap 'Simulate Reinstall' to wipe all local data from the phone database.\n4. Tap 'Sync Now' (or log out and sign back in) to watch every transaction instantly download back to your phone! 🪄",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }

                                // Collapsible Config Options
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { showServerConfig = !showServerConfig },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Cloud Sync Network Settings",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = if (showServerConfig) Icons.Default.Clear else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (showServerConfig) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Modify your remote REST server url (e.g. Supabase REST API, Node.js connection, Spring Boot or Firebase Web endpoints). Retrofit requests are delivered dynamically to this base endpoint.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedTextField(
                                            value = customUrlInput,
                                            onValueChange = {
                                                customUrlInput = it
                                                viewModel.updateCustomServerUrl(it)
                                            },
                                            label = { Text("Base Server Endpoint URL") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("custom_sync_url_field")
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    customUrlInput = "https://wisewallet.free.beeceptor.com"
                                                    viewModel.updateCustomServerUrl("https://wisewallet.free.beeceptor.com")
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Reset to Demo API", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Log out Button
                                TextButton(
                                    onClick = { viewModel.logout() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth().testTag("button_execute_logout")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Logout Current Cloud Session")
                                }
                            }
                        }
                    }
                }
            }

            // EXPORT DATA CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("export_data_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Export Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Download filtered transaction lists matching custom date ranges and categories. Formatted perfectly for Google Sheets and Microsoft Excel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Button(
                            onClick = { showExportConfigDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("button_open_export"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configure & Export")
                        }
                    }
                }
            }
        } else {
            // EDIT PROFILE DETAILS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Edit Profile Info",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = editName,
                            onValueChange = {
                                editName = it
                                nameError = false
                            },
                            label = { Text("Full Name") },
                            isError = nameError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { editAge = it },
                            label = { Text("Age") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = {
                                editEmail = it
                                emailError = false
                            },
                            label = { Text("Email Address") },
                            isError = emailError,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editOccupation,
                            onValueChange = { editOccupation = it },
                            label = { Text("Occupation") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Save / Cancel Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    if (editName.isBlank()) nameError = true
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(editEmail).matches() && editEmail.isNotBlank()) {
                                        emailError = true
                                    }
                                    
                                    if (!nameError && !emailError) {
                                        viewModel.updateProfile(
                                            name = editName,
                                            age = editAge,
                                            email = editEmail,
                                            phone = editPhone,
                                            occupation = editOccupation,
                                            avatarIndex = editAvatarIndex
                                        )
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAvatar(
    avatarIndex: Int,
    name: String,
    photoPath: String = "",
    size: androidx.compose.ui.unit.Dp = 80.dp,
    onClick: (() -> Unit)? = null
) {
    val initials = name.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
    
    val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF2196F3))), // Indigo-Blue
        Brush.linearGradient(listOf(Color(0xFF009688), Color(0xFF4CAF50))), // Teal-Green
        Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63))), // Purple-Pink
        Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFFF5722))), // Orange-DeepOrange
        Brush.linearGradient(listOf(Color(0xFF607D8B), Color(0xFF455A64))), // SlateGradients
        Brush.linearGradient(listOf(Color(0xFFFFC107), Color(0xFFFF8F00)))  // Amber-Gold
    )
    
    val brush = gradients.getOrElse(avatarIndex) { gradients[0] }
    
    val isWebUrl = photoPath.startsWith("http://") || photoPath.startsWith("https://")
    var fileExists by remember(photoPath) { mutableStateOf(false) }
    LaunchedEffect(photoPath) {
        if (photoPath.isNotEmpty() && !isWebUrl) {
            withContext(Dispatchers.IO) {
                try {
                    fileExists = java.io.File(photoPath).exists()
                } catch (e: Exception) {
                    fileExists = false
                }
            }
        } else {
            fileExists = false
        }
    }
    
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(brush)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (photoPath.isNotEmpty() && (isWebUrl || fileExists)) {
                AsyncImage(
                    model = photoPath,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = if (initials.isNotEmpty()) initials else "?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = if (size < 30.dp) 11.sp else if (size < 50.dp) 15.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
        
        if (onClick != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onClick() }
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload Photo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                if (value.isNotBlank()) value else "Not Specified",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BudgetLimitDialog(
    currentLimit: Double,
    onDismissRequest: () -> Unit,
    onSave: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf(currentLimit.toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Set Monthly Budget")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Define a general spending boundary. You will receive real-time, high-priority notifications if your cumulative monthly expenses exceed this threshold.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        if (it.toDoubleOrNull() != null && it.toDouble() >= 0.0) {
                            errorText = null
                        }
                    },
                    label = { Text("Budget Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorText != null,
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input")
                )
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doubleVal = textValue.toDoubleOrNull()
                    if (doubleVal != null && doubleVal >= 0.0) {
                        onSave(doubleVal)
                        onDismissRequest()
                    } else {
                        errorText = "Please enter a valid amount >= 0"
                    }
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun CustomChoiceChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun String.escapeCsv(): String {
    if (this.contains(",") || this.contains("\"") || this.contains("\n") || this.contains("\r")) {
        return "\"" + this.replace("\"", "\"\"") + "\""
    }
    return this
}

@Composable
fun SplashAnimationScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "LogoAlpha"
    )
    val rotation by animateFloatAsState(
        targetValue = if (startAnimation) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "LogoRotation"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Modern Logo Emblem
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha,
                        rotationZ = rotation
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Text Title
            Text(
                text = "WiseWallet",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.graphicsLayer(alpha = alpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Intelligence in Expense Tracking",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.graphicsLayer(alpha = alpha)
            )
        }
    }
}

@Composable
fun GoogleLogo() {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw creative segments representing Google logo branding
            drawArc(
                color = Color(0xFFEA4335), // Google Red
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = Color(0xFFFBBC05), // Google Yellow
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = Color(0xFF34A853), // Google Green
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            drawArc(
                color = Color(0xFF4285F4), // Google Blue
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(width / 2f, height / 2f),
                end = Offset(width - 1.dp.toPx(), height / 2f),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { 
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ) 
            },
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
fun OnboardingFlowScreen(
    initialName: String = "",
    initialEmail: String = "",
    onSubmit: (name: String, age: String, email: String, occupation: String) -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    
    var nameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var occupationInput by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    androidx.compose.runtime.LaunchedEffect(initialName) {
        if (initialName.isNotEmpty() && nameInput.isEmpty()) {
            nameInput = initialName
        }
    }

    androidx.compose.runtime.LaunchedEffect(initialEmail) {
        if (initialEmail.isNotEmpty() && emailInput.isEmpty()) {
            emailInput = initialEmail
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .drawBehind {
                if (size.width > 0f && size.height > 0f) {
                    // Circular Ambient Glow with safe bounds
                    val safeRadius = maxOf(1f, size.width)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x0F34A853),
                                Color.Transparent
                            ),
                            center = Offset(size.width, 0f),
                            radius = safeRadius
                        ),
                        radius = safeRadius,
                        center = Offset(size.width, 0f)
                    )
                }
            }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // High-fidelity progress header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CREATE PROFILE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = when (currentStep) {
                        1 -> "What should we call you?"
                        2 -> "Tell us your age"
                        3 -> "What's your email id?"
                        else -> "And your occupation?"
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Modern Segmented Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        i == currentStep -> MaterialTheme.colorScheme.primary
                                        i < currentStep -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Your full name",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "We will use this to address you in notification screens and profiles.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ModernTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = "Full Name",
                                    placeholder = "e.g. Harshit Kumar",
                                    leadingIcon = Icons.Default.Person,
                                    testTag = "onboarding_name"
                                )
                            }
                        }
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "Age details",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Required to calculate smart, age-appropriate guidelines and thresholds under limits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ModernTextField(
                                    value = ageInput,
                                    onValueChange = { ageInput = it },
                                    label = "Your Age",
                                    placeholder = "e.g. 24",
                                    leadingIcon = Icons.Default.Info,
                                    testTag = "onboarding_age",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        3 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "Email id",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Enables encrypted clouds synchronization and keeps analytics safe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ModernTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = "Email Address",
                                    placeholder = "e.g. harshitkumar581@gmail.com",
                                    leadingIcon = Icons.Default.Email,
                                    testTag = "onboarding_email",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                )
                            }
                        }
                        4 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "Current Occupation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Let us know your profession so we can recommend customized default budget bounds.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                ModernTextField(
                                    value = occupationInput,
                                    onValueChange = { occupationInput = it },
                                    label = "Profession / Job",
                                    placeholder = "e.g. Software Engineer",
                                    leadingIcon = Icons.Default.Star,
                                    testTag = "onboarding_occupation"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { 
                                    focusManager.clearFocus()
                                    currentStep-- 
                                },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                when (currentStep) {
                                    1 -> if (nameInput.isNotBlank()) currentStep++
                                    2 -> if (ageInput.isNotBlank()) currentStep++
                                    3 -> if (emailInput.isNotBlank() && emailInput.contains("@")) currentStep++
                                    4 -> {
                                        if (occupationInput.isNotBlank()) {
                                            onSubmit(
                                                nameInput.trim(),
                                                ageInput.trim(),
                                                emailInput.trim(),
                                                occupationInput.trim()
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("onboarding_next_button"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = when (currentStep) {
                                1 -> nameInput.isNotBlank()
                                2 -> ageInput.isNotBlank()
                                3 -> emailInput.isNotBlank() && emailInput.contains("@")
                                4 -> occupationInput.isNotBlank()
                                else -> false
                            }
                        ) {
                            Text(if (currentStep == 4) "Start Journey" else "Next")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (currentStep == 4) Icons.Default.Check else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthLandingScreen(
    viewModel: ExpenseViewModel,
    onLoginCompleted: () -> Unit
) {
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val syncSyncing by viewModel.cloudSyncSyncing.collectAsStateWithLifecycle()
    val syncStatusText by viewModel.syncStatusText.collectAsStateWithLifecycle()

    var authMode by remember { mutableStateOf(0) } // 0 = Options, 1 = Google Form, 2 = Phone Form
    var googleEmailInput by remember { mutableStateOf("") }
    var googleNameInput by remember { mutableStateOf("") }
    
    var phoneInput by remember { mutableStateOf("") }
    var phoneOtpInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            onLoginCompleted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .drawBehind {
                if (size.width > 0f && size.height > 0f) {
                    // Large subtle radial glow pattern for elegant, premium visuals
                    val safeRadius = maxOf(1f, size.width * 1.2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x0C4285F4),
                                Color.Transparent
                            ),
                            center = Offset(0f, size.height * 0.1f),
                            radius = safeRadius
                        ),
                        radius = safeRadius,
                        center = Offset(0f, size.height * 0.1f)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // App branding headers
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
                
                Text(
                    text = "WiseWallet",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Smart, secure budget & modern transactions tracker",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Authentication states and options
            if (syncSyncing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = syncStatusText ?: "Processing...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                when (authMode) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // High Fidelity Google Sign In Button
                            Card(
                                onClick = { authMode = 1 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .testTag("select_google_signup"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GoogleLogo()
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Continue with Google",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.2.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // OTP Button
                            Card(
                                onClick = { authMode = 2 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .testTag("select_phone_signup"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Continue with OTP",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loginWithGoogle("guest.user@wisewallet.com", "Guest User")
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Skip & Start anonymously (Offline mode)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    1 -> {
                        // Custom Modern Google Registration Mode
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GoogleLogo()
                                    Text(
                                        text = "Google Account Info",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                ModernTextField(
                                    value = googleEmailInput,
                                    onValueChange = { googleEmailInput = it },
                                    label = "Google Email ID",
                                    placeholder = "harshitkumar581@gmail.com",
                                    leadingIcon = Icons.Default.Email,
                                    testTag = "signup_google_email"
                                )

                                ModernTextField(
                                    value = googleNameInput,
                                    onValueChange = { googleNameInput = it },
                                    label = "Display Name",
                                    placeholder = "e.g. Harshit Kumar",
                                    leadingIcon = Icons.Default.Person,
                                    testTag = "signup_google_name"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Premium minimal chip selector
                                    SuggestionChip(
                                        onClick = { 
                                            googleEmailInput = "harshitkumar581@gmail.com"
                                            googleNameInput = "Harshit Kumar"
                                        },
                                        label = { Text("Use demo account", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { 
                                            focusManager.clearFocus()
                                            authMode = 0 
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Back")
                                    }
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            val mail = googleEmailInput.trim()
                                            val name = if (googleNameInput.isNotBlank()) googleNameInput.trim() else "WiseWallet User"
                                            if (mail.contains("@")) {
                                                viewModel.loginWithGoogle(mail, name)
                                            } else {
                                                android.widget.Toast.makeText(context, "Please enter a valid Google email.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1.5f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Register")
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Phone Auth Form
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Mobile OTP Login",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                ModernTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = "Phone Number",
                                    placeholder = "+91 99999 88888",
                                    leadingIcon = Icons.Default.Phone,
                                    testTag = "signup_phone"
                                )

                                ModernTextField(
                                    value = phoneOtpInput,
                                    onValueChange = { phoneOtpInput = it },
                                    label = "Verification OTP code",
                                    placeholder = "123456",
                                    leadingIcon = Icons.Default.Lock,
                                    testTag = "signup_phone_otp",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = { 
                                            phoneInput = "+91 99999 88888"
                                            phoneOtpInput = "123456"
                                        },
                                        label = { Text("Use Demo OTP", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { 
                                            focusManager.clearFocus()
                                            authMode = 0 
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Back")
                                    }
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            val ph = phoneInput.trim()
                                            val otp = phoneOtpInput.trim()
                                            if (ph.length >= 8 && otp.length == 6) {
                                                viewModel.loginWithPhone(ph, otp)
                                            } else {
                                                android.widget.Toast.makeText(context, "Enter a valid phone size & 6-digit OTP.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1.5f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Verify")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

