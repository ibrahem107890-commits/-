package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {

    val allActiveHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletion>> = habitDao.getAllCompletions()

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        // Delete completions first due to foreign keys if they are not cascaded
        // But cascading on delete is defined on our ForeignKey, so deleting the habit will auto-delete completions!
        habitDao.deleteHabit(habit)
    }

    suspend fun toggleCompletion(habitId: Int, date: String, isCompleted: Boolean) {
        if (isCompleted) {
            habitDao.insertCompletion(HabitCompletion(habitId = habitId, date = date))
        } else {
            habitDao.deleteCompletion(habitId, date)
        }
    }
}
