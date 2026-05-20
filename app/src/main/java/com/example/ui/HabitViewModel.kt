package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.HabitCompletion
import com.example.data.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository

    // State of selected date (default: today)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Selected category filter (null means "الكل" - All)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao())

        // Seed initial habits if the database is empty
        viewModelScope.launch {
            repository.allHabits.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedInitialHabits()
                }
            }
        }
    }

    // Expose all active habits
    val habits: StateFlow<List<Habit>> = repository.allActiveHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose all completions
    val completions: StateFlow<List<HabitCompletion>> = repository.allCompletions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived State: filtered habits based on selected category and date
    val filteredHabits: StateFlow<List<Habit>> = combine(habits, _selectedCategory) { habitList, category ->
        if (category == null) {
            habitList
        } else {
            habitList.filter { it.category == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter completions for the selected date
    val selectedDateCompletions: StateFlow<Set<Int>> = combine(completions, _selectedDate) { completionList, date ->
        val dateStr = formatDate(date)
        completionList.filter { it.date == dateStr }.map { it.habitId }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Dynamic metrics for the selected date
    val dailyProgress: StateFlow<Float> = combine(filteredHabits, selectedDateCompletions) { habits, completedIds ->
        if (habits.isEmpty()) 0f
        else {
            val count = habits.count { completedIds.contains(it.id) }
            count.toFloat() / habits.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Get streak for a specific habit
    fun getHabitStreak(habitId: Int): HabitStreak {
        val allComps = completions.value.filter { it.habitId == habitId }
        if (allComps.isEmpty()) {
            return HabitStreak(current = 0, best = 0)
        }

        val completedDatesSet = allComps.mapNotNull {
            try {
                LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                null
            }
        }.toSet()

        if (completedDatesSet.isEmpty()) {
            return HabitStreak(current = 0, best = 0)
        }

        // 1. Calculate Current Streak
        var currentStreak = 0
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Streak requires completion either today or yesterday to remain active
        if (completedDatesSet.contains(today) || completedDatesSet.contains(yesterday)) {
            var dateToCheck = if (completedDatesSet.contains(today)) today else yesterday
            while (completedDatesSet.contains(dateToCheck)) {
                currentStreak++
                dateToCheck = dateToCheck.minusDays(1)
            }
        }

        // 2. Calculate Best Streak
        var bestStreak = 0
        var currentBestVal = 0
        val sortedDates = completedDatesSet.sorted()

        var lastDate: LocalDate? = null
        for (date in sortedDates) {
            if (lastDate == null) {
                currentBestVal = 1
            } else {
                val daysDiff = ChronoUnit.DAYS.between(lastDate, date)
                if (daysDiff == 1L) {
                    currentBestVal++
                } else if (daysDiff > 1L) {
                    bestStreak = maxOf(bestStreak, currentBestVal)
                    currentBestVal = 1
                }
            }
            lastDate = date
        }
        bestStreak = maxOf(bestStreak, currentBestVal)

        return HabitStreak(current = currentStreak, best = bestStreak)
    }

    // Public actions
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleHabitCompletion(habitId: Int, date: LocalDate) {
        viewModelScope.launch {
            val dateStr = formatDate(date)
            val isCurrentlyCompleted = selectedDateCompletions.value.contains(habitId)
            repository.toggleCompletion(habitId, dateStr, !isCurrentlyCompleted)
        }
    }

    fun addHabit(title: String, description: String, icon: String, category: String, reminderTime: String?) {
        viewModelScope.launch {
            repository.insertHabit(
                Habit(
                    title = title,
                    description = description,
                    icon = icon,
                    category = category,
                    reminderTime = reminderTime
                )
            )
        }
    }

    fun editHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    private suspend fun seedInitialHabits() {
        val initialHabits = listOf(
            Habit(title = "شرب كمية كافية من الماء", description = "شرب 8 كؤوس من الماء يومياً لترطيب الجسم", icon = "💧", category = "صحة رياضية"),
            Habit(title = "الورد اليومي من القرآن والتأمل", description = "قراءة حزب أو بضع صفحات وتأملها بهدوء", icon = "📖", category = "عبادة"),
            Habit(title = "تمارين رياضية وإطالات", description = "ممارسة تمارين خفيفة لمدة 15 إلى 30 دقيقة", icon = "🏃‍♂️", category = "صحة رياضية"),
            Habit(title = "ترتيب المهام اليومية", description = "تنظيم أولويات اليوم وتحديد 3 مهام أساسية للمستقبل", icon = "📝", category = "تنظيم وعمل"),
            Habit(title = "قراءة كتاب تثقيفي", description = "قراءة 10 صفحات يومياً لتنمية المعرفة العامة", icon = "💡", category = "تطوير الذات")
        )
        initialHabits.forEach { repository.insertHabit(it) }
    }

    private fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

data class HabitStreak(
    val current: Int,
    val best: Int
)
