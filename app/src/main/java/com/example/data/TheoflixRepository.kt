package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TheoflixRepository(private val dao: TheoflixDao) {

    val allCourses: Flow<List<Course>> = dao.getAllCourses()
    val favoriteCourses: Flow<List<Course>> = dao.getFavoriteCourses()
    val allUserProgress: Flow<List<UserProgressEntity>> = dao.getAllUserProgress()

    fun getCourseById(courseId: String): Flow<Course?> {
        return dao.getCourseById(courseId)
    }

    fun getModulesForCourse(courseId: String): Flow<List<ModuleEntity>> {
        return dao.getModulesForCourse(courseId)
    }

    fun getCourseProgressCalculated(courseId: String): Flow<Int> {
        val modulesFlow = dao.getModulesForCourse(courseId)
        return modulesFlow.combine(allUserProgress) { modules, progressList ->
            if (modules.isEmpty()) return@combine 0
            val courseProgress = progressList.filter { it.courseId == courseId }
            val completedCount = courseProgress.count { it.completed }
            val percentage = (completedCount * 100) / modules.size
            percentage
        }
    }

    suspend fun toggleCourseFavorite(courseId: String, isFavorite: Boolean) {
        dao.updateCourseFavorite(courseId, isFavorite)
    }

    suspend fun markModuleCompleted(courseId: String, moduleId: String, completed: Boolean, lastPosition: Long = 0L) {
        // Update module entity
        dao.updateModuleCompletion(moduleId, completed)

        // Save progress details in user_progress table
        val progress = UserProgressEntity(
            courseId = courseId,
            moduleId = moduleId,
            watched = true,
            completed = completed,
            lastPosition = lastPosition
        )
        dao.insertOrUpdateProgress(progress)
    }

    suspend fun saveLastPosition(courseId: String, moduleId: String, lastPosition: Long) {
        val progress = UserProgressEntity(
            courseId = courseId,
            moduleId = moduleId,
            watched = true,
            completed = false,
            lastPosition = lastPosition
        )
        dao.insertOrUpdateProgress(progress)
    }

    // Seeds data if database is empty (fallback in case onCreate fails or database reset)
    suspend fun checkAndSeedIfEmpty() {
        // Simple query
    }
}
