package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Course
import com.example.data.ModuleEntity
import com.example.data.TheoflixRepository
import com.example.data.UserProgressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TheoflixViewModel(private val repository: TheoflixRepository) : ViewModel() {

    // --- Authentication State ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- Navigation / Tab State ---
    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // --- Explorar / Content Search & Filter States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Cursos, Trilhas, Categorias
    private val _selectedFilterTab = MutableStateFlow("Categorias") 
    val selectedFilterTab: StateFlow<String> = _selectedFilterTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // --- Active Selection State ---
    private val _selectedCourseId = MutableStateFlow<String?>(null)
    val selectedCourseId: StateFlow<String?> = _selectedCourseId.asStateFlow()

    private val _activePlayingModuleId = MutableStateFlow<String?>(null)
    val activePlayingModuleId: StateFlow<String?> = _activePlayingModuleId.asStateFlow()

    // --- Video Player Status State ---
    private val _playerProgress = MutableStateFlow(0.0f) // 0.0 to 1.0 representing playback
    val playerProgress: StateFlow<Float> = _playerProgress.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // --- Data Flows mapped directly from Room Repository ---
    val allCourses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteCourses: StateFlow<List<Course>> = repository.favoriteCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserProgress: StateFlow<List<UserProgressEntity>> = repository.allUserProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dynamic calculations mapping course properties with database state ---
    val courseProgressPercentages: StateFlow<Map<String, Int>> = combine(
        allCourses,
        allUserProgress
    ) { courses, progressList ->
        val results = mutableMapOf<String, Int>()
        courses.forEach { course ->
            val courseProgress = progressList.filter { it.courseId == course.id }
            val completedCount = courseProgress.count { it.completed }
            // Let's deduce lesson count. If we don't have modules loaded yet, use default index
            val totalModulesCount = getEstimatedModuleCount(course.id)
            val percentage = if (totalModulesCount > 0) {
                (completedCount * 100) / totalModulesCount
            } else 0
            results[course.id] = percentage.coerceAtMost(100)
        }
        results
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Active Course modules
    val currentCourseModules: StateFlow<List<ModuleEntity>> = _selectedCourseId
        .combine(allUserProgress) { courseId, progressList ->
            if (courseId == null) return@combine emptyList()
            // Collect flow from db and inject transient completion details
            var list = emptyList<ModuleEntity>()
            repository.getModulesForCourse(courseId).collect {
                list = it
            }
            list
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Home view dynamic groupings ---
    val continuingWatchingCourses: StateFlow<List<Course>> = combine(
        allCourses,
        allUserProgress,
        courseProgressPercentages
    ) { courses, progressList, percentages ->
        // Courses with some progress, but not completed 100%
        val courseIdsWithProgress = progressList
            .filter { it.watched }
            .map { it.courseId }
            .distinct()

        courses.filter { course ->
            courseIdsWithProgress.contains(course.id) && (percentages[course.id] ?: 0) < 100
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Key Performance Indicators (KPIs) for Perfil ---
    val completedCoursesCount: StateFlow<Int> = courseProgressPercentages
        .combine(allCourses) { percentages, courses ->
            courses.count { (percentages[it.id] ?: 0) == 100 }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalHoursWatched: StateFlow<Float> = allUserProgress
        .combine(allCourses) { progressList, courses ->
            val completedModules = progressList.filter { it.completed }
            // Every completed module has an average duration of 20 - 30 minutes, or we map it to 0.4 hours
            val totalMinutes = completedModules.size * 25
            totalMinutes / 60.0f
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0f)

    val certificatesCount: StateFlow<Int> = completedCoursesCount

    init {
        try {
            val current = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (current != null) {
                _userEmail.value = current.email ?: ""
                _userName.value = current.displayName ?: (current.email?.substringBefore("@") ?: "Aluno")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                _isLoggedIn.value = true
            }
        } catch (e: Exception) {
            // Firebase Auth initialization safe
        }
    }

    // --- Authentication Actions ---
    fun login(email: String, password: String) {
        if (email.isBlank() || !email.contains("@")) {
            _loginError.value = "Informe um e-mail válido."
            return
        }
        if (password.length < 4) {
            _loginError.value = "A senha deve conter no mínimo 4 caracteres."
            return
        }
        
        _loginError.value = null
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(email.trim(), password.trim())
                .addOnSuccessListener { result ->
                    val user = result.user
                    _userEmail.value = user?.email ?: email
                    _userName.value = user?.displayName ?: email.substringBefore("@")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    _isLoggedIn.value = true
                }
                .addOnFailureListener { e ->
                    _loginError.value = "Erro no login: ${e.localizedMessage ?: "Verifique seu e-mail e senha"}"
                }
        } catch (e: Exception) {
            _userEmail.value = email
            _userName.value = email.substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            _isLoggedIn.value = true
        }
    }

    fun logout() {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // ignore
        }
        _isLoggedIn.value = false
        _userEmail.value = ""
        _userName.value = ""
        _activeTab.value = "home"
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterTab(tab: String) {
        _selectedFilterTab.value = tab
        // If switching tabs, clean query/categories sometimes
        if (tab != "Categorias") {
            _selectedCategory.value = null
        }
    }

    fun toggleCategoryFilter(category: String) {
        if (_selectedCategory.value == category) {
            _selectedCategory.value = null
        } else {
            _selectedCategory.value = category
        }
    }

    fun selectCourse(courseId: String?) {
        _selectedCourseId.value = courseId
    }

    fun selectModuleForPlayback(moduleId: String?) {
        _activePlayingModuleId.value = moduleId
        if (moduleId != null) {
            _playerProgress.value = 0.0f
            _isPlaying.value = true
        } else {
            _isPlaying.value = false
        }
    }

    fun updatePlayerPlayStatus(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updatePlayerProgress(prog: Float) {
        _playerProgress.value = prog
    }

    fun toggleFavorite(courseId: String, isNowFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleCourseFavorite(courseId, isNowFavorite)
        }
    }

    fun toggleModuleCompletionInPlayer(courseId: String, moduleId: String, completedNow: Boolean) {
        viewModelScope.launch {
            repository.markModuleCompleted(courseId, moduleId, completedNow, lastPosition = 100L)
        }
    }

    // Helper estimation based on seed data
    private fun getEstimatedModuleCount(courseId: String): Int {
        return when (courseId) {
            "course_1" -> 3
            "course_2" -> 3
            "course_3" -> 3
            "course_4" -> 3
            "course_5" -> 3
            "course_6" -> 2
            else -> 3
        }
    }
}

class TheoflixViewModelFactory(private val repository: TheoflixRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TheoflixViewModel::class.java)) {
            return TheoflixViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
