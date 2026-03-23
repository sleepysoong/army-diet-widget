package com.sleepysoong.armydiet.testutil

import com.sleepysoong.armydiet.data.local.MealDao
import com.sleepysoong.armydiet.data.local.MealEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMealDao(
    initialMeals: List<MealEntity> = emptyList()
) : MealDao {
    private val meals = linkedMapOf<String, MealEntity>()
    private val mealsFlow = MutableStateFlow(initialMeals)

    init {
        initialMeals.forEach { meals[it.date] = it }
        refreshFlow()
    }

    override suspend fun getMeal(date: String): MealEntity? = meals[date]

    override suspend fun getMealsByDates(dates: List<String>): List<MealEntity> =
        dates.mapNotNull { meals[it] }

    override suspend fun getMealsInRange(startDate: String, endDate: String): List<MealEntity> =
        meals.values.filter { it.date in startDate..endDate }.sortedBy { it.date }

    override fun getAllMealsFlow(): Flow<List<MealEntity>> = mealsFlow

    override suspend fun clearMeals() {
        meals.clear()
        refreshFlow()
    }

    override suspend fun insertMeals(meals: List<MealEntity>) {
        meals.forEach { meal -> this.meals[meal.date] = meal }
        refreshFlow()
    }

    private fun refreshFlow() {
        mealsFlow.value = meals.values.sortedBy { it.date }
    }
}
