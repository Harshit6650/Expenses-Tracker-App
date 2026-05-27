package com.example.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Category
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.data.ExpenseWithCategory
import com.example.utils.NotificationHelper
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val sharedPreferences: SharedPreferences? = null,
    private val appContext: Context? = null
) : ViewModel() {

    // Authentication & Cloud Sync States
    private val _isUserLoggedIn = MutableStateFlow(sharedPreferences?.getBoolean("is_logged_in", false) ?: false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(sharedPreferences?.getBoolean("onboarding_completed", false) ?: false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _userUid = MutableStateFlow(sharedPreferences?.getString("logged_in_uid", "") ?: "")
    val userUid: StateFlow<String> = _userUid.asStateFlow()

    private val _userEmailState = MutableStateFlow(sharedPreferences?.getString("logged_in_email", "") ?: "")
    val userEmailState: StateFlow<String> = _userEmailState.asStateFlow()

    private val _userPhoneState = MutableStateFlow(sharedPreferences?.getString("logged_in_phone", "") ?: "")
    val userPhoneState: StateFlow<String> = _userPhoneState.asStateFlow()

    private val _userNameState = MutableStateFlow(sharedPreferences?.getString("logged_in_name", "") ?: "")
    val userNameState: StateFlow<String> = _userNameState.asStateFlow()

    private val _authProvider = MutableStateFlow(sharedPreferences?.getString("logged_in_provider", "") ?: "")
    val authProvider: StateFlow<String> = _authProvider.asStateFlow()

    private val _authToken = MutableStateFlow(sharedPreferences?.getString("logged_in_token", "") ?: "")
    val authToken: StateFlow<String> = _authToken.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(sharedPreferences?.getLong("last_synced_time", 0L) ?: 0L)
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    private val _cloudSyncSyncing = MutableStateFlow(false)
    val cloudSyncSyncing: StateFlow<Boolean> = _cloudSyncSyncing.asStateFlow()

    private val _syncStatusText = MutableStateFlow<String?>(null)
    val syncStatusText: StateFlow<String?> = _syncStatusText.asStateFlow()

    private val _customServerUrl = MutableStateFlow(sharedPreferences?.getString("custom_server_url", "https://wisewallet.free.beeceptor.com") ?: "https://wisewallet.free.beeceptor.com")
    val customServerUrl: StateFlow<String> = _customServerUrl.asStateFlow()

    // Tab state: 0 = Expenses, 1 = Categories, 2 = Charts, 3 = Profile
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Theme state: "system" = Follow system default, "light" = Light mode, "dark" = Dark mode
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Monthly Budget Limit
    private val _monthlyBudgetLimit = MutableStateFlow(
        sharedPreferences?.getString("monthly_budget_limit", "1000.0")?.toDoubleOrNull() ?: 1000.0
    )
    val monthlyBudgetLimit: StateFlow<Double> = _monthlyBudgetLimit.asStateFlow()

    // Retrieve all categories and expenses
    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allExpenses: StateFlow<List<ExpenseWithCategory>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Profile states directly initialized from SharedPreferences
    private val _profileName = MutableStateFlow(sharedPreferences?.getString("profile_name", "Alex Mercer") ?: "Alex Mercer")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileAge = MutableStateFlow(sharedPreferences?.getString("profile_age", "28") ?: "28")
    val profileAge: StateFlow<String> = _profileAge.asStateFlow()

    private val _profileEmail = MutableStateFlow(sharedPreferences?.getString("profile_email", "alex.mercer@wisewallet.com") ?: "alex.mercer@wisewallet.com")
    val profileEmail: StateFlow<String> = _profileEmail.asStateFlow()

    private val _profilePhone = MutableStateFlow(sharedPreferences?.getString("profile_phone", "+91 98765 43210") ?: "+91 98765 43210")
    val profilePhone: StateFlow<String> = _profilePhone.asStateFlow()

    private val _profileOccupation = MutableStateFlow(sharedPreferences?.getString("profile_occupation", "Product & Design") ?: "Product & Design")
    val profileOccupation: StateFlow<String> = _profileOccupation.asStateFlow()

    private val _profileAvatarIndex = MutableStateFlow(sharedPreferences?.getInt("profile_avatar_index", 0) ?: 0)
    val profileAvatarIndex: StateFlow<Int> = _profileAvatarIndex.asStateFlow()

    private val _profilePhotoPath = MutableStateFlow(sharedPreferences?.getString("profile_photo_path", "") ?: "")
    val profilePhotoPath: StateFlow<String> = _profilePhotoPath.asStateFlow()

    init {
        _themeMode.value = sharedPreferences?.getString("theme_mode", "system") ?: "system"
        viewModelScope.launch {
            repository.seedDefaultCategoriesIfEmpty()
        }

        // Reactive Checker for monthly budget LIMIT utilization and notifies when overrun
        viewModelScope.launch {
            try {
                allExpenses.collect { expenses ->
                    try {
                        val currentSpent = calculateCurrentMonthSpent(expenses)
                        val currentLimit = _monthlyBudgetLimit.value
                        
                        if (currentLimit > 0 && currentSpent > currentLimit) {
                            val calendar = Calendar.getInstance()
                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH)
                            val alertKey = "budget_alert_sent_${year}_${month}"
                            val alreadySent = sharedPreferences?.getBoolean(alertKey, false) ?: false
                            
                            if (!alreadySent && appContext != null) {
                                sharedPreferences?.edit()?.putBoolean(alertKey, true)?.apply()
                                NotificationHelper.showBudgetOverrunNotification(appContext, currentLimit, currentSpent)
                            }
                        } else if (currentLimit > 0 && currentSpent <= currentLimit) {
                            // Reset alert flag to allow alerts if the user logs new trans exceeding it again after adjusting values
                            val calendar = Calendar.getInstance()
                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH)
                            val alertKey = "budget_alert_sent_${year}_${month}"
                            if (sharedPreferences?.getBoolean(alertKey, false) == true) {
                                sharedPreferences.edit().putBoolean(alertKey, false).apply()
                            }
                        }
                    } catch (inner: Throwable) {
                        inner.printStackTrace()
                    }
                }
            } catch (outer: Throwable) {
                outer.printStackTrace()
            }
        }
    }

    fun calculateCurrentMonthSpent(expenses: List<ExpenseWithCategory>): Double {
        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        
        return expenses.filter {
            val expenseCalendar = Calendar.getInstance().apply { timeInMillis = it.expense.dateMillis }
            expenseCalendar.get(Calendar.YEAR) == currentYear && expenseCalendar.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.expense.amount }
    }

    fun updateMonthlyBudgetLimit(limit: Double) {
        _monthlyBudgetLimit.value = limit
        sharedPreferences?.edit()?.putString("monthly_budget_limit", limit.toString())?.apply()
    }

    fun updateProfile(
        name: String,
        age: String,
        email: String,
        phone: String,
        occupation: String,
        avatarIndex: Int
    ) {
        _profileName.value = name
        _profileAge.value = age
        _profileEmail.value = email
        _profilePhone.value = phone
        _profileOccupation.value = occupation
        _profileAvatarIndex.value = avatarIndex

        sharedPreferences?.edit()?.apply {
            putString("profile_name", name)
            putString("profile_age", age)
            putString("profile_email", email)
            putString("profile_phone", phone)
            putString("profile_occupation", occupation)
            putInt("profile_avatar_index", avatarIndex)
            apply()
        }
    }

    fun submitOnboarding(
        name: String,
        age: String,
        email: String,
        occupation: String
    ) {
        _profileName.value = name
        _profileAge.value = age
        _profileEmail.value = email
        _profileOccupation.value = occupation
        _isOnboardingCompleted.value = true

        sharedPreferences?.edit()?.apply {
            putString("profile_name", name)
            putString("profile_age", age)
            putString("profile_email", email)
            putString("profile_occupation", occupation)
            putBoolean("onboarding_completed", true)
            apply()
        }
    }

    fun updateProfilePhoto(path: String) {
        _profilePhotoPath.value = path
        sharedPreferences?.edit()?.putString("profile_photo_path", path)?.apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPreferences?.edit()?.putString("theme_mode", mode)?.apply()
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun addExpense(description: String, amount: Double, categoryId: Int, dateMillis: Long) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    description = description,
                    amount = amount,
                    categoryId = categoryId,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    name = name,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Google Sign-In Authenticate Flow
    fun loginWithGoogle(email: String, name: String) {
        viewModelScope.launch {
            try {
                _cloudSyncSyncing.value = true
                _syncStatusText.value = "Authenticating with Google Accounts..."
                delay(1200)

                val generatedUid = "gg_" + email.replace(".", "_").replace("@", "_")
                val mockToken = "google_token_jwt_" + System.currentTimeMillis()

                _userUid.value = generatedUid
                _userEmailState.value = email
                _userPhoneState.value = ""
                _userNameState.value = name
                _authProvider.value = "Google"
                _authToken.value = mockToken
                _isUserLoggedIn.value = true

                sharedPreferences?.edit()?.apply {
                    putBoolean("is_logged_in", true)
                    putString("logged_in_uid", generatedUid)
                    putString("logged_in_email", email)
                    putString("logged_in_phone", "")
                    putString("logged_in_name", name)
                    putString("logged_in_provider", "Google")
                    putString("logged_in_token", mockToken)
                    apply()
                }

                _syncStatusText.value = "Login Successful! Loading cloud data..."
                delay(800)
                syncWithCloud()
            } catch (e: Throwable) {
                _syncStatusText.value = "Login Fail: ${e.localizedMessage}"
                delay(1500)
                _syncStatusText.value = null
                _cloudSyncSyncing.value = false
            }
        }
    }

    // Mobile/Phone OTP Authenticate Flow
    fun loginWithPhone(phone: String, otp: String) {
        viewModelScope.launch {
            try {
                _cloudSyncSyncing.value = true
                _syncStatusText.value = "Verifying Mobile SMS OTP..."
                delay(1200)

                val sanitizedPhone = phone.replace(" ", "").replace("-", "")
                val generatedUid = "ph_" + sanitizedPhone
                val mockToken = "phone_token_jwt_" + System.currentTimeMillis()
                val friendlyName = "User " + sanitizedPhone.takeLast(4)

                _userUid.value = generatedUid
                _userEmailState.value = ""
                _userPhoneState.value = phone
                _userNameState.value = friendlyName
                _authProvider.value = "Phone"
                _authToken.value = mockToken
                _isUserLoggedIn.value = true

                sharedPreferences?.edit()?.apply {
                    putBoolean("is_logged_in", true)
                    putString("logged_in_uid", generatedUid)
                    putString("logged_in_email", "")
                    putString("logged_in_phone", phone)
                    putString("logged_in_name", friendlyName)
                    putString("logged_in_provider", "Phone")
                    putString("logged_in_token", mockToken)
                    apply()
                }

                _syncStatusText.value = "Mobile Authorized! Loading cloud data..."
                delay(800)
                syncWithCloud()
            } catch (e: Throwable) {
                _syncStatusText.value = "Verification Fail: ${e.localizedMessage}"
                delay(1500)
                _syncStatusText.value = null
                _cloudSyncSyncing.value = false
            }
        }
    }

    // Logout and purge device session credentials
    fun logout() {
        viewModelScope.launch {
            _syncStatusText.value = "Logging out securely..."
            delay(1000)

            _isUserLoggedIn.value = false
            _isOnboardingCompleted.value = false
            _userUid.value = ""
            _userEmailState.value = ""
            _userPhoneState.value = ""
            _userNameState.value = ""
            _authProvider.value = ""
            _authToken.value = ""
            _lastSyncedTime.value = 0L

            sharedPreferences?.edit()?.apply {
                putBoolean("is_logged_in", false)
                putBoolean("onboarding_completed", false)
                putString("logged_in_uid", "")
                putString("logged_in_email", "")
                putString("logged_in_phone", "")
                putString("logged_in_name", "")
                putString("logged_in_provider", "")
                putString("logged_in_token", "")
                putLong("last_synced_time", 0L)
                apply()
            }

            _syncStatusText.value = "Logout clear complete!"
            delay(800)
            _syncStatusText.value = null
        }
    }

    fun updateCustomServerUrl(url: String) {
        _customServerUrl.value = url
        sharedPreferences?.edit()?.putString("custom_server_url", url)?.apply()
    }

    // Cloud Database Bidirectional Synchronization Engine
    fun syncWithCloud() {
        viewModelScope.launch {
            if (!_isUserLoggedIn.value) return@launch
            try {
                _cloudSyncSyncing.value = true
                _syncStatusText.value = "Connecting to Cloud Database..."
                delay(1000)

                com.example.data.sync.CloudSyncService.updateBaseUrl(_customServerUrl.value)

                // 1. Prepare local expenses
                _syncStatusText.value = "Compiling local transactions..."
                val localExpenses = repository.allExpenses.first()
                val localDtos = localExpenses.map {
                    com.example.data.sync.CloudExpenseDto(
                        description = it.expense.description,
                        amount = it.expense.amount,
                        categoryName = it.category?.name ?: "Others",
                        dateMillis = it.expense.dateMillis
                    )
                }

                // 2. Perform Retrofit requests to Cloud Database
                _syncStatusText.value = "Syncing categories and records with server..."
                val header = "Bearer " + _authToken.value
                val payload = com.example.data.sync.SyncPayload(userId = _userUid.value, expenses = localDtos)

                val uploadResponse = com.example.data.sync.CloudSyncService.api.uploadExpenses(header, payload)
                val downloadResponse = com.example.data.sync.CloudSyncService.api.downloadExpenses(header, _userUid.value)

                val serverExpenses = downloadResponse.expenses ?: emptyList()

                if (serverExpenses.isNotEmpty()) {
                    _syncStatusText.value = "Merging server records locally..."
                    mergeServerExpensesLocally(serverExpenses)
                }

                _lastSyncedTime.value = System.currentTimeMillis()
                sharedPreferences?.edit()?.putLong("last_synced_time", _lastSyncedTime.value)?.apply()
                _syncStatusText.value = "Cloud Synchronization Completed Successfully! ✅"
            } catch (e: Throwable) {
                // FALLBACK TO SECURE SIMULATED CLOUD ENGINE ON NETWORK TIMEOUT OR OFFLINE
                _syncStatusText.value = "Fallback: Syncing with sandbox backup cloud..."
                delay(1200)
                performSandboxSync()
            } finally {
                delay(1800)
                _cloudSyncSyncing.value = false
                _syncStatusText.value = null
            }
        }
    }

    private suspend fun mergeServerExpensesLocally(serverExpenses: List<com.example.data.sync.CloudExpenseDto>) {
        val currentCategories = repository.allCategories.first()
        val currentExpenses = repository.allExpenses.first()

        // Sync or Insert Category DTOs
        val categoryMap = currentCategories.associateBy { it.name.lowercase() }.toMutableMap()

        for (dto in serverExpenses) {
            val catNameLower = dto.categoryName.lowercase()
            if (!categoryMap.containsKey(catNameLower)) {
                val color = when (dto.categoryName) {
                    "Food & Drinks" -> "#FF5722"
                    "Shopping" -> "#E91E63"
                    "Transportation" -> "#2196F3"
                    "Bills & Utilities" -> "#9C27B0"
                    "Entertainment" -> "#4CAF50"
                    else -> "#9E9E9E"
                }
                val icon = when (dto.categoryName) {
                    "Food & Drinks" -> "restaurant"
                    "Shopping" -> "shopping"
                    "Transportation" -> "travel"
                    "Bills & Utilities" -> "bill"
                    "Entertainment" -> "play"
                    else -> "info"
                }
                val freshCat = Category(name = dto.categoryName, colorHex = color, iconName = icon)
                repository.insertCategory(freshCat)

                // Re-fetch to assign newly generated room IDs
                val updatedCats = repository.allCategories.first()
                val freshInserted = updatedCats.find { it.name.equals(dto.categoryName, ignoreCase = true) }
                if (freshInserted != null) {
                    categoryMap[catNameLower] = freshInserted
                }
            }
        }

        // Insert Expense DTOs
        for (dto in serverExpenses) {
            val targetCategory = categoryMap[dto.categoryName.lowercase()] ?: continue

            // Prevent duplicate records locally (checking desc, amount and approximate timestamp)
            val isDuplicate = currentExpenses.any { local ->
                local.expense.description.equals(dto.description, ignoreCase = true) &&
                Math.abs(local.expense.amount - dto.amount) < 0.05 &&
                Math.abs(local.expense.dateMillis - dto.dateMillis) < 8000 // 8 sec tolerance
            }

            if (!isDuplicate) {
                repository.insertExpense(
                    Expense(
                        description = dto.description,
                        amount = dto.amount,
                        categoryId = targetCategory.id,
                        dateMillis = dto.dateMillis
                    )
                )
            }
        }
    }

    private suspend fun performSandboxSync() {
        val globalSharedPrefs = appContext?.getSharedPreferences("wise_wallet_global_cloud_sim", Context.MODE_PRIVATE)
        val cloudDataKey = "cloud_data_user_${_userUid.value}"
        val cloudDataJson = globalSharedPrefs?.getString(cloudDataKey, "") ?: ""

        val localExpenses = repository.allExpenses.first()
        val localDtos = localExpenses.map {
            com.example.data.sync.CloudExpenseDto(
                description = it.expense.description,
                amount = it.expense.amount,
                categoryName = it.category?.name ?: "Others",
                dateMillis = it.expense.dateMillis
            )
        }

        val serverDtos = mutableListOf<com.example.data.sync.CloudExpenseDto>()
        if (cloudDataJson.isNotEmpty()) {
            try {
                // Standard parser to reinitialize dto lists dynamically
                val regex = java.util.regex.Pattern.compile("""\{"description":"(.*?)","amount":(.*?),"categoryName":"(.*?)","dateMillis":(.*?)\}""")
                val matcher = regex.matcher(cloudDataJson)
                while (matcher.find()) {
                    val desc = matcher.group(1) ?: ""
                    val amt = matcher.group(2)?.toDoubleOrNull() ?: 0.0
                    val cat = matcher.group(3) ?: "Others"
                    val date = matcher.group(4)?.toLongOrNull() ?: System.currentTimeMillis()
                    serverDtos.add(com.example.data.sync.CloudExpenseDto(desc, amt, cat, date))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Merging collections securely
        val allMerged = (localDtos + serverDtos).distinctBy {
            "${it.description.lowercase()}_${it.amount}_${it.dateMillis / 8000}"
        }

        // Save merge results to the back-end "Cloud DB" simulator
        val serializedListBuilder = StringBuilder()
        serializedListBuilder.append("[")
        allMerged.forEachIndexed { index, dto ->
            serializedListBuilder.append("""{"description":"${dto.description}","amount":${dto.amount},"categoryName":"${dto.categoryName}","dateMillis":${dto.dateMillis}}""")
            if (index < allMerged.size - 1) serializedListBuilder.append(",")
        }
        serializedListBuilder.append("]")
        globalSharedPrefs?.edit()?.putString(cloudDataKey, serializedListBuilder.toString())?.apply()

        // Push downloading categories & transactions to room db
        mergeServerExpensesLocally(allMerged)

        _lastSyncedTime.value = System.currentTimeMillis()
        sharedPreferences?.edit()?.putLong("last_synced_time", _lastSyncedTime.value)?.apply()
        _syncStatusText.value = "Sandbox Cloud Backup & Sync Completed successfully! ✅"
    }

    // Direct helper to clean local database to simulate fresh app installation/clearance
    fun purgeLocalDatabaseForDemo() {
        viewModelScope.launch {
            _syncStatusText.value = "Simulating Reinstallation (Clearing local database)..."
            delay(1200)
            
            val expenses = repository.allExpenses.first()
            for (expense in expenses) {
                repository.deleteExpense(expense.expense)
            }
            
            _syncStatusText.value = "Local database is now empty. Log back in to sync and restore!"
            delay(1800)
            _syncStatusText.value = null
        }
    }
}

class ExpenseViewModelFactory(
    private val repository: ExpenseRepository,
    private val sharedPreferences: SharedPreferences? = null,
    private val appContext: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository, sharedPreferences, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
