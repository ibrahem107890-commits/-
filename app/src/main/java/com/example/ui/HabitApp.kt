package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Habit
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitApp(
    viewModel: HabitViewModel = viewModel()
) {
    // Forcing Arabic RTL direction for native experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val selectedDate by viewModel.selectedDate.collectAsState()
        val selectedCategory by viewModel.selectedCategory.collectAsState()
        val habits by viewModel.filteredHabits.collectAsState()
        val completedIds by viewModel.selectedDateCompletions.collectAsState()
        val progress by viewModel.dailyProgress.collectAsState()

        var showAddDialog by remember { mutableStateOf(false) }
        var habitToEdit by remember { mutableStateOf<Habit?>(null) }
        var showStatsDialog by remember { mutableStateOf(false) }

        // Compute completed and total counts
        val totalCount = habits.size
        val completedCount = habits.count { completedIds.contains(it.id) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "مرحباً، صديقي 🌿",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary, // #48483B
                                        fontSize = 22.sp
                                    ),
                                    modifier = Modifier.testTag("app_title")
                                )
                                Text(
                                    text = getFullArabicDate(selectedDate),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            // Distinctive Organic Avatar from Natural Tones Spec
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer) // bg-[#E7E8D8]
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape) // border-[#D1D3C0]
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                )
                                Text(
                                    text = "🌱",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(
                            onClick = { showStatsDialog = true },
                            modifier = Modifier.testTag("stats_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "الإحصائيات",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer, // bg-[#D3E8D3]
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer, // text-[#111D0E]
                    shape = RoundedCornerShape(16.dp), // rounded-2xl
                    modifier = Modifier
                        .testTag("add_habit_fab")
                        .padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة عادة جديدة",
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Progress Dashboard Card using rich styling keys
                ProgressCard(completedCount = completedCount, totalCount = totalCount, progress = progress)

                // Date Picker Strip
                DatePickerStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.setSelectedDate(it) }
                )

                // Category Selection Chips
                CategorySelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setSelectedCategory(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Habits List (or Empty State)
                if (habits.isEmpty()) {
                    EmptyHabitState(
                        isFiltered = selectedCategory != null,
                        onClearFilter = { viewModel.setSelectedCategory(null) },
                        onAddClick = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("habits_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(habits, key = { it.id }) { habit ->
                            val isCompleted = completedIds.contains(habit.id)
                            val streaks = viewModel.getHabitStreak(habit.id)

                            HabitItemCard(
                                habit = habit,
                                isCompleted = isCompleted,
                                currentStreak = streaks.current,
                                bestStreak = streaks.best,
                                onToggleCompletion = {
                                    viewModel.toggleHabitCompletion(habit.id, selectedDate)
                                },
                                onEditClick = { habitToEdit = habit },
                                onDeleteClick = { viewModel.deleteHabit(habit) }
                            )
                        }
                    }
                }
            }
        }

        // Add Habit Dialog
        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, desc, icon, category, reminder ->
                    viewModel.addHabit(title, desc, icon, category, reminder)
                    showAddDialog = false
                }
            )
        }

        // Edit Habit Dialog
        habitToEdit?.let { habit ->
            EditHabitDialog(
                habit = habit,
                onDismiss = { habitToEdit = null },
                onSave = { updatedHabit ->
                    viewModel.editHabit(updatedHabit)
                    habitToEdit = null
                }
            )
        }

        // Stats Overview Dialog
        if (showStatsDialog) {
            StatsOverviewDialog(
                habits = habits,
                completedCount = completedIds.size,
                todayProgress = progress,
                viewModel = viewModel,
                onDismiss = { showStatsDialog = false }
            )
        }
    }
}

// Today Progress Card Component - styled precisely like bg-[#E7E8D8] rounded-[32px]
@Composable
fun ProgressCard(completedCount: Int, totalCount: Int, progress: Float) {
    val progressPercent = (progress * 100).toInt()
    Card(
        shape = RoundedCornerShape(32.dp), // Matches rounded-[32px]
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer // Warm container color #E7E8D8
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("progress_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "إنجاز اليوم",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary // #48483B
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatToArabicNumerals(progressPercent)}٪",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 38.sp,
                        color = MaterialTheme.colorScheme.onSurface // #1C1C17
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (totalCount > 0) {
                        "تم إكمال ${formatToArabicNumerals(completedCount)} من أصل ${formatToArabicNumerals(totalCount)} عادات"
                    } else {
                        "ابدأ بإضافة عاداتك اليوم لتبدأ التتبع!"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.secondary, // Muted green-olive #7D815F
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.outline, // Track tint #D1D3C0
                )
                Text(
                    text = "${formatToArabicNumerals(progressPercent)}٪",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// Weekly Horizontal Date Picker Scroll Stripe
@Composable
fun DatePickerStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dateList = remember {
        (0..10).map { today.minusDays(5 - it.toLong()) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        dateList.forEach { date ->
            val isSelected = date == selectedDate
            val isDayToday = date == today
            val formatterDay = formatToArabicNumerals(date.dayOfMonth)
            val dayName = getDayNameArabic(date)

            Card(
                onClick = { onDateSelected(date) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer // bg-[#E7E8D8] style
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected || isDayToday) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else if (isDayToday) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier
                    .width(62.dp)
                    .height(84.dp)
                    .testTag("date_card_${date.dayOfMonth}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Text(
                        text = formatterDay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Tiny indicator dot
                    if (isDayToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}

// Category selection row elements
@Composable
fun CategorySelector(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    val categories = listOf("الكل", "صحة رياضية", "عبادة", "تطوير الذات", "تنظيم وعمل")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { catName ->
            val isSelected = (catName == "الكل" && selectedCategory == null) || (selectedCategory == catName)
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (catName == "الكل") {
                        onCategorySelected(null)
                    } else {
                        onCategorySelected(catName)
                    }
                },
                label = { Text(catName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("category_chip_${catName}")
            )
        }
    }
}

// Custom Habit Item Card - White with border border-[#E2E2D5] and active completion state bg-[#D3E8D3]
@Composable
fun HabitItemCard(
    habit: Habit,
    isCompleted: Boolean,
    currentStreak: Int,
    bestStreak: Int,
    onToggleCompletion: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp), // rounded-2xl
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant // border-[#E2E2D5]
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.background.copy(alpha = 0.5f) // Subtle tint of background color
            } else {
                MaterialTheme.colorScheme.surface // bg-white
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleCompletion() } // Quick toggle list element tap
            .testTag("habit_card_${habit.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circle with Habit Icon Emoji - rounded-xl bg-[#F4F4EB]
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) // bg-[#F4F4EB] styled neatly
                ) {
                    Text(text = habit.icon, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Titles & Subtitles
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // More options dropdown/button
                        Box {
                            IconButton(
                                onClick = { showMenu = !showMenu },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "خيارات العادة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تعديل العادة") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onEditClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("حذف النهائي", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }

                    if (habit.description.isNotEmpty()) {
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Circular customized checklist box - size w-8 h-8, rounded-full, border-2 border-[#D1D3C0]
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp) // w-8
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.secondaryContainer // bg-[#D3E8D3]
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline, // border-[#D1D3C0]
                            shape = CircleShape
                        )
                        .clickable { onToggleCompletion() }
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "تمت بنجاح",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer, // text-[#324D32] / text-[#111D0E]
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak metrics
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥 ", fontSize = 14.sp)
                        Text(
                            text = "السلسلة: ${formatToArabicNumerals(currentStreak)} أيام",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (currentStreak > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆 ", fontSize = 14.sp)
                        Text(
                            text = "أفضلها: ${formatToArabicNumerals(bestStreak)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                // Reminder Pill
                if (habit.reminderTime != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = habit.reminderTime,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// Add Habit Dialog Modal Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, icon: String, category: String, reminder: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("صحة رياضية") }
    var selectedIcon by remember { mutableStateOf("🎯") }
    var reminderTime by remember { mutableStateOf("") }

    val categories = listOf("صحة رياضية", "عبادة", "تطوير الذات", "تنظيم وعمل")
    val icons = listOf("🎯", "💧", "📖", "🏃‍♂️", "📝", "🧘", "🍏", "💡", "⏰", "💼", "🚲", "🧠", "🥬", "🛌")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_habit_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "بناء عادة جديدة 🚀",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = Alignment.Center.let { TextAlign.Right }
                    )
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان العادة (مثال: شرب الكولاجين)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_habit_title"),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        )
                    )
                }

                // Description Input
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("وصف أو خطة التنفيذ") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_habit_desc")
                    )
                }

                // Category selection
                item {
                    Text(
                        text = "التصنيف:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Icon chooser
                item {
                    Text(
                        text = "اختر أيقونة العادة:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.forEach { icon ->
                            val isSelected = selectedIcon == icon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedIcon = icon }
                            ) {
                                Text(
                                    text = icon,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }

                // Reminder Optional input
                item {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("وقت التذكير (اختياري: مثلاً 08:30 ص)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Actions buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                if (title.trim().isNotEmpty()) {
                                    onSave(
                                        title.trim(),
                                        description.trim(),
                                        selectedIcon,
                                        selectedCategory,
                                        if (reminderTime.trim().isEmpty()) null else reminderTime.trim()
                                    )
                                }
                            },
                            enabled = title.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("save_habit_button")
                        ) {
                            Text("حفظ العادة")
                        }
                    }
                }
            }
        }
    }
}

// Edit Habit Dialog Modal
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit
) {
    var title by remember { mutableStateOf(habit.title) }
    var description by remember { mutableStateOf(habit.description) }
    var selectedCategory by remember { mutableStateOf(habit.category) }
    var selectedIcon by remember { mutableStateOf(habit.icon) }
    var reminderTime by remember { mutableStateOf(habit.reminderTime ?: "") }

    val categories = listOf("صحة رياضية", "عبادة", "تطوير الذات", "تنظيم وعمل")
    val icons = listOf("🎯", "💧", "📖", "🏃‍♂️", "📝", "🧘", "🍏", "💡", "⏰", "💼", "🚲", "🧠", "🥬", "🛌")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "تعديل هذه العادة 💫",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان العادة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("الوصف") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "التصنيف:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "اختر أيقونة العادة:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.forEach { icon ->
                            val isSelected = selectedIcon == icon
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedIcon = icon }
                            ) {
                                Text(
                                    text = icon,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("وقت التذكير") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                if (title.trim().isNotEmpty()) {
                                    onSave(
                                        habit.copy(
                                            title = title.trim(),
                                            description = description.trim(),
                                            icon = selectedIcon,
                                            category = selectedCategory,
                                            reminderTime = if (reminderTime.trim().isEmpty()) null else reminderTime.trim()
                                        )
                                    )
                                }
                            },
                            enabled = title.trim().isNotEmpty(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("تحديث")
                        }
                    }
                }
            }
        }
    }
}

// Stats Dialog Modal Box
@Composable
fun StatsOverviewDialog(
    habits: List<Habit>,
    completedCount: Int,
    todayProgress: Float,
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .testTag("stats_dialog_container")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إحصائيات الإنجاز 📊",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats rows with custom numbers
                StatItem(label = "جمالي العادات النشطة", value = formatToArabicNumerals(habits.size))
                StatItem(label = "معدل إنجاز اليوم", value = "${formatToArabicNumerals((todayProgress * 100).toInt())}%")

                // Compute overall best streak among all active habits
                val maxStreakHabit = habits.maxByOrNull { viewModel.getHabitStreak(it.id).best }
                val overallBestStreak = maxStreakHabit?.let { viewModel.getHabitStreak(it.id).best } ?: 0
                val bestHabitTitle = maxStreakHabit?.title ?: "لا يوجد بعد"

                StatItem(label = "أفضل سلسلة إنجاز شاملة", value = "${formatToArabicNumerals(overallBestStreak)} دقيقة/أيام")
                if (overallBestStreak > 0) {
                    Text(
                        text = "حققتها في عادة: $bestHabitTitle",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق الإحصائيات")
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
        )
    }
}

// Empty state placeholder
@Composable
fun EmptyHabitState(
    isFiltered: Boolean,
    onClearFilter: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌿",
            fontSize = 56.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFiltered) "لا توجد عادات تحت هذا التصنيف!" else "ابدأ رحلتك الأولى اليوم!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFiltered) "امسح الفلتر لرؤية جميع عاداتك أو أنشئ واحدة جديدة خاصة بهذا التصنيف." else "لم تسجل أي عادات بعد. اضغط على الزر بالأسفل وصمم روتينك المثالي.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isFiltered) {
            Button(
                onClick = onClearFilter,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("إظهار كل العادات")
            }
        } else {
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("إنشاء عادتي الأولى ➕")
            }
        }
    }
}

// Helper to format integers to dynamic Arabic Eastern Numerals
fun formatToArabicNumerals(number: Int): String {
    val numStr = number.toString()
    val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val builder = StringBuilder()
    for (char in numStr) {
        if (char.isDigit()) {
            builder.append(arabicChars[char - '0'])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}

// Full dynamic Arabic date helper matching styling: "الأحد، ١٥ أكتوبر" style
fun getFullArabicDate(date: LocalDate): String {
    val dayName = getDayNameArabic(date)
    val dayOfMonth = date.dayOfMonth
    val monthName = when (date.monthValue) {
        1 -> "يناير"
        2 -> "فبراير"
        3 -> "مارس"
        4 -> "أبريل"
        5 -> "مايو"
        6 -> "يونيو"
        7 -> "يوليو"
        8 -> "أغسطس"
        9 -> "سبتمبر"
        10 -> "أكتوبر"
        11 -> "نوفمبر"
        12 -> "ديسمبر"
        else -> ""
    }
    return "$dayName، ${formatToArabicNumerals(dayOfMonth)} $monthName"
}

// Motivation generator
fun getMotivationMessage(progress: Float): String {
    return when {
        progress == 0f -> "بداية موفقة لتنظيم يومك! خطوة بخطوة..."
        progress <= 0.35f -> "ممتاز! استمر في التقدم وتحقيق العادات."
        progress <= 0.7f -> "رائع جداً! أنت الآن في منتصف الطريق ليوم عظيم."
        progress < 1.0f -> "أقرب مما تظن! عادة واحدة تفصلك عن اليوم المثالي."
        else -> "إنجاز أسطوري! يوم كامل من العادات الناجحة 🌟🥇"
    }
}

// Arabic Day names mapper
fun getDayNameArabic(date: java.time.LocalDate): String {
    return when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "الاثنين"
        java.time.DayOfWeek.TUESDAY -> "الثلاثاء"
        java.time.DayOfWeek.WEDNESDAY -> "الأربعاء"
        java.time.DayOfWeek.THURSDAY -> "الخميس"
        java.time.DayOfWeek.FRIDAY -> "الجمعة"
        java.time.DayOfWeek.SATURDAY -> "السبت"
        java.time.DayOfWeek.SUNDAY -> "الأحد"
    }
}
