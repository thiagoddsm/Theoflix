package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TheoflixRepository(private val dao: TheoflixDao) {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    val allCourses: Flow<List<Course>> = dao.getAllCourses()
    val favoriteCourses: Flow<List<Course>> = dao.getFavoriteCourses()
    val allUserProgress: Flow<List<UserProgressEntity>> = dao.getAllUserProgress()

    init {
        // Inicia sincronização em tempo real com o Firestore do OikoApp
        startRealtimeSync()
    }

    /**
     * Sincroniza em tempo real os cursos cadastrados no OikoApp (Firestore config/theoflix)
     * para o banco local Room do Theoflix Android.
     */
    fun startRealtimeSync() {
        try {
            firestore.document("config/theoflix").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TheoflixRepo", "Erro ao sincronizar config/theoflix: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val coursesRaw = snapshot.get("courses") as? List<Map<String, Any>> ?: emptyList()
                    if (coursesRaw.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val coursesList = mutableListOf<Course>()
                            val modulesList = mutableListOf<ModuleEntity>()

                            coursesRaw.forEach { cMap ->
                                val courseId = cMap["id"] as? String ?: return@forEach
                                val title = cMap["title"] as? String ?: "Curso Teológico"
                                val desc = cMap["desc"] as? String ?: ""
                                val color = cMap["color"] as? String ?: "blue"
                                val category = cMap["type"] as? String ?: "Doutrina"
                                val teacher = cMap["teacher"] as? String ?: "Pastoral"

                                val episodes = cMap["episodes"] as? List<Map<String, Any>> ?: emptyList()
                                val totalDurationMinutes = episodes.size * 30
                                val durationStr = if (totalDurationMinutes > 60) "${totalDurationMinutes / 60}h ${totalDurationMinutes % 60}m" else "${totalDurationMinutes}m"

                                coursesList.add(
                                    Course(
                                        id = courseId,
                                        title = title,
                                        description = desc,
                                        thumbnailColor = color,
                                        category = category,
                                        teacher = teacher,
                                        duration = durationStr,
                                        isFavorite = false
                                    )
                                )

                                episodes.forEachIndexed { index, epMap ->
                                    val epId = epMap["id"] as? String ?: "ep_${index + 1}"
                                    val epTitle = epMap["title"] as? String ?: "Aula ${index + 1}"
                                    val epDuration = epMap["duration"] as? String ?: "25 min"
                                    val videoUrl = epMap["videoUrl"] as? String ?: ""

                                    modulesList.add(
                                        ModuleEntity(
                                            id = epId,
                                            courseId = courseId,
                                            title = epTitle,
                                            duration = epDuration,
                                            videoUrl = videoUrl,
                                            completed = false
                                        )
                                    )
                                }
                            }

                            if (coursesList.isNotEmpty()) {
                                dao.insertCourses(coursesList)
                            }
                            if (modulesList.isNotEmpty()) {
                                dao.insertModules(modulesList)
                            }
                            Log.d("TheoflixRepo", "Sincronizados ${coursesList.size} cursos e ${modulesList.size} aulas do OikoApp!")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TheoflixRepo", "Falha na inicialização do Firestore listener: ${e.message}")
        }
    }

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
            (completedCount * 100) / modules.size
        }
    }

    suspend fun toggleCourseFavorite(courseId: String, isFavorite: Boolean) {
        dao.updateCourseFavorite(courseId, isFavorite)
    }

    suspend fun markModuleCompleted(courseId: String, moduleId: String, completed: Boolean, lastPosition: Long = 0L) {
        // 1. Atualiza Room Local
        dao.updateModuleCompletion(moduleId, completed)

        val progress = UserProgressEntity(
            courseId = courseId,
            moduleId = moduleId,
            watched = true,
            completed = completed,
            lastPosition = lastPosition
        )
        dao.insertOrUpdateProgress(progress)

        // 2. Envia progresso para o Firestore do OikoApp em tempo real
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                val watchData = mapOf(
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: currentUser.email ?: "Aluno"),
                    "courseId" to courseId,
                    "moduleId" to moduleId,
                    "completed" to completed,
                    "lastPositionMs" to lastPosition,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                // Registra no histórico de visualização do OikoApp
                firestore.collection("teaching_watch_history")
                    .document("${currentUser.uid}_${courseId}_${moduleId}")
                    .set(watchData, SetOptions.merge())

                // Atualiza o documento de jornada do usuário
                if (completed) {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .set(
                            mapOf(
                                "journey" to mapOf(
                                    "theoflixModules" to mapOf(moduleId to true)
                                )
                            ),
                            SetOptions.merge()
                        )
                }
            } catch (e: Exception) {
                Log.e("TheoflixRepo", "Erro ao salvar progresso no Firestore: ${e.message}")
            }
        }
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

    suspend fun checkAndSeedIfEmpty() {
        startRealtimeSync()
    }
}
