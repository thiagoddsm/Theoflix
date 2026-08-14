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
        syncUserProfileProgress()
    }

    /**
     * Sincroniza em tempo real os cursos do OikoApp (tanto da coleção theoflix_courses
     * quanto do documento config/theoflix).
     */
    suspend fun ensureOfficialCourses() {
        try {
            TheoflixDatabase.seedDatabase(dao)
        } catch (e: Exception) {
            Log.e("TheoflixRepo", "Erro no seed: ${e.message}")
        }
    }

    fun startRealtimeSync() {
        // 1. Limpa o banco local e semeia os 6 cursos oficiais imediatamente
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.clearAllCourses()
                dao.clearAllModules()
                TheoflixDatabase.seedDatabase(dao)
                Log.d("TheoflixRepo", "Banco Room atualizado com os 6 cursos oficiais!")
            } catch (e: Exception) {
                Log.e("TheoflixRepo", "Erro ao semear banco local: ${e.message}")
            }
        }

        // 2. Listener para a coleção `theoflix_courses` (onde o TheoFlix Manager do OikoApp salva)
        try {
            firestore.collection("theoflix_courses").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TheoflixRepo", "Erro ao escutar theoflix_courses: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val coursesList = mutableListOf<Course>()
                        val modulesList = mutableListOf<ModuleEntity>()

                        for (doc in snapshot.documents) {
                            val data = doc.data ?: continue
                            val courseId = doc.id
                            val title = data["title"] as? String ?: "Curso TheoFlix"
                            val desc = (data["desc"] as? String) ?: (data["description"] as? String) ?: ""
                            val category = (data["type"] as? String) ?: (data["category"] as? String) ?: "Doutrina"
                            val teacher = (data["teacher"] as? String) ?: (data["instructor"] as? String) ?: "IBM"
                            val duration = data["duration"] as? String ?: "4h"
                            val color = (data["image"] as? String) ?: (data["color"] as? String) ?: "#1D4ED8"

                            coursesList.add(
                                Course(
                                    id = courseId,
                                    title = title,
                                    description = desc,
                                    thumbnailColor = color,
                                    category = category,
                                    teacher = teacher,
                                    duration = duration,
                                    isFavorite = false
                                )
                            )

                            val rawEpisodes = data["episodes"] as? List<Map<String, Any>> ?: emptyList()
                            rawEpisodes.forEachIndexed { index, epMap ->
                                val epId = epMap["id"] as? String ?: "${courseId}_${index + 1}"
                                val epTitle = epMap["title"] as? String ?: "Aula ${index + 1}"
                                val epDuration = epMap["duration"] as? String ?: "30 min"
                                val youtubeId = epMap["youtubeId"] as? String ?: epMap["videoId"] as? String ?: ""
                                val videoUrl = if (youtubeId.isNotBlank()) {
                                    if (youtubeId.startsWith("http")) youtubeId
                                    else "https://www.youtube.com/watch?v=$youtubeId"
                                } else {
                                    epMap["videoUrl"] as? String ?: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                                }

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
                        Log.d("TheoflixRepo", "Sincronizados ${coursesList.size} cursos de theoflix_courses!")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TheoflixRepo", "Erro ao iniciar listener de theoflix_courses: ${e.message}")
        }

        // 3. Listener para o documento `config/theoflix`
        try {
            firestore.document("config/theoflix").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TheoflixRepo", "Erro ao escutar config/theoflix: ${error.message}")
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
                                val desc = (cMap["desc"] as? String) ?: (cMap["description"] as? String) ?: ""
                                val color = (cMap["image"] as? String) ?: (cMap["color"] as? String) ?: "#1D4ED8"
                                val category = (cMap["type"] as? String) ?: (cMap["category"] as? String) ?: "Doutrina"
                                val teacher = (cMap["teacher"] as? String) ?: "IBM"
                                val duration = cMap["duration"] as? String ?: "4h"

                                coursesList.add(
                                    Course(
                                        id = courseId,
                                        title = title,
                                        description = desc,
                                        thumbnailColor = color,
                                        category = category,
                                        teacher = teacher,
                                        duration = duration,
                                        isFavorite = false
                                    )
                                )

                                val episodes = cMap["episodes"] as? List<Map<String, Any>> ?: emptyList()
                                episodes.forEachIndexed { index, epMap ->
                                    val epId = epMap["id"] as? String ?: "${courseId}_${index + 1}"
                                    val epTitle = epMap["title"] as? String ?: "Aula ${index + 1}"
                                    val epDuration = epMap["duration"] as? String ?: "30 min"
                                    val youtubeId = epMap["youtubeId"] as? String ?: epMap["videoId"] as? String ?: ""
                                    val videoUrl = if (youtubeId.isNotBlank()) {
                                        if (youtubeId.startsWith("http")) youtubeId
                                        else "https://www.youtube.com/watch?v=$youtubeId"
                                    } else {
                                        epMap["videoUrl"] as? String ?: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                                    }

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
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TheoflixRepo", "Erro no listener de config/theoflix: ${e.message}")
        }
    }

    /**
     * Sincroniza o progresso do usuário cadastrado no OikoApp (Firestore -> Room)
     */
    fun syncUserProfileProgress() {
        val currentUser = auth.currentUser ?: return
        try {
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                    val journey = snapshot.get("journey") as? Map<String, Any> ?: return@addSnapshotListener
                    val theoflixProgress = journey["theoflixProgress"] as? Map<String, Any> ?: emptyMap()
                    val theoflixModules = journey["theoflixModules"] as? Map<String, Any> ?: emptyMap()

                    CoroutineScope(Dispatchers.IO).launch {
                        // Mapeia os módulos assistidos
                        theoflixModules.forEach { (modId, isDone) ->
                            if (isDone == true) {
                                dao.updateModuleCompletion(modId, true)
                            }
                        }

                        // Mapeia progresso detalhado por curso
                        theoflixProgress.forEach { (courseId, epsMap) ->
                            if (epsMap is Map<*, *>) {
                                epsMap.forEach { (epKey, status) ->
                                    val isCompleted = status == true || status == "completed" || status == "done"
                                    val modId = "${courseId}_$epKey"
                                    dao.updateModuleCompletion(modId, isCompleted)
                                    dao.insertOrUpdateProgress(
                                        UserProgressEntity(
                                            courseId = courseId,
                                            moduleId = modId,
                                            watched = true,
                                            completed = isCompleted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("TheoflixRepo", "Erro ao sincronizar progresso do usuário: ${e.message}")
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
                                    "theoflixModules" to mapOf(moduleId to true),
                                    "theoflixProgress" to mapOf(courseId to mapOf(moduleId to true))
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
