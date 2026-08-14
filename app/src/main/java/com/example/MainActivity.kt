package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.example.data.TheoflixDatabase
import com.example.data.TheoflixDao
import com.example.data.TheoflixRepository
import com.example.ui.TheoflixViewModel
import com.example.ui.TheoflixViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TheoflixTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local instance initialization of Room SQLite Database and Seed actions
        val database = TheoflixDatabase.getDatabase(this)
        val repository = TheoflixRepository(database.theoflixDao())
        
        // Manual seeding fallback to guarantee database contents on first launch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure starting items are populated
                TheoflixDatabase.seedDatabase(database.theoflixDao())
            } catch (e: Exception) {
                // Already matching keys or populated
            }
        }

        val viewModelFactory = TheoflixViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[TheoflixViewModel::class.java]

        setContent {
            TheoflixTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                
                // Root switcher mapping authentication sessions
                Crossfade(targetState = isLoggedIn, label = "Authentication") { loggedIn ->
                    if (loggedIn) {
                        TheoflixAppScaffold(viewModel = viewModel)
                    } else {
                        LoginScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TheoflixAppScaffold(viewModel: TheoflixViewModel) {
    val navController = rememberNavController()
    val activeTab by viewModel.activeTab.collectAsState()
    val favorites by viewModel.favoriteCourses.collectAsState()
    val context = LocalContext.current

    // Load on mount: equivalent of useEffect(() => {}, []) with try/catch
    LaunchedEffect(Unit) {
        try {
            val sharedPrefs = context.getSharedPreferences("TheoflixPrefs", android.content.Context.MODE_PRIVATE)
            val myListStr = sharedPrefs.getString("theoflix_mylist", null) as? String
            if (myListStr != null) {
                val savedIds = myListStr.split(",").filter { it.isNotBlank() }
                for (id in savedIds) {
                    viewModel.toggleFavorite(id, true)
                }
            }

            // Recovery of last opened lesson/course (Optional requirement)
            val lastCourseId = sharedPrefs.getString("last_course_id", null) as? String
            val lastModuleId = sharedPrefs.getString("last_module_id", null) as? String
            if (lastCourseId != null) {
                viewModel.selectCourse(lastCourseId)
            }
            if (lastModuleId != null) {
                viewModel.selectModuleForPlayback(lastModuleId)
                navController.navigate("player/$lastModuleId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Automatically synchronize database favorites directly to SharedPreferences key "theoflix_mylist"
    LaunchedEffect(favorites) {
        try {
            val sharedPrefs = context.getSharedPreferences("TheoflixPrefs", android.content.Context.MODE_PRIVATE)
            val compiledIds = favorites.map { it.id }.joinToString(",")
            val editor = sharedPrefs.edit()
            editor.putString("theoflix_mylist", compiledIds)
            editor.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Control navigation in and out of nested screens
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Conditionally show custom bottom bar only on core content tabs
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route ?: "main"
            
            if (currentRoute == "main") {
                TheoflixBottomBar(
                    activeTab = activeTab,
                    onTabSelect = { tab ->
                        viewModel.setTab(tab)
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main tabs screen containing the switcher logic
            composable("main") {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeTab) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToCourse = { courseId ->
                                navController.navigate("detail/$courseId")
                            }
                        )
                        "explorar" -> ExplorarScreen(
                            viewModel = viewModel,
                            onNavigateToCourse = { courseId ->
                                navController.navigate("detail/$courseId")
                            }
                        )
                        "favoritos" -> FavoriteTabScreen(
                            viewModel = viewModel,
                            onNavigateToCourse = { courseId ->
                                navController.navigate("detail/$courseId")
                            }
                        )
                        "perfil" -> PerfilScreen(
                            viewModel = viewModel,
                            onNavigateToCourse = { courseId ->
                                navController.navigate("detail/$courseId")
                            }
                        )
                    }
                }
            }

            // Detailed course view route screen
            composable(
                route = "detail/{courseId}",
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
                CourseDetailScreen(
                    courseId = courseId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPlayer = { moduleId ->
                        navController.navigate("player/$moduleId")
                    }
                )
            }

            // Video simulation playback player route
            composable(
                route = "player/{moduleId}",
                arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: ""
                PlayerScreen(
                    moduleId = moduleId,
                    viewModel = viewModel,
                    onBackToCourse = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun TheoflixBottomBar(
    activeTab: String,
    onTabSelect: (String) -> Unit
) {
    // Elegant Dark Bottom Navigation customized styling matching Tailwind specs
    NavigationBar(
        containerColor = Color(0xFF0F0F0F),
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
            .height(72.dp)
            .testTag("theoflix_bottom_bar")
    ) {
        val tabList = listOf(
            TabDefinition("home", "Início", Icons.Default.Home),
            TabDefinition("explorar", "Explorar", Icons.Default.Explore),
            TabDefinition("favoritos", "Favoritos", Icons.Default.Favorite),
            TabDefinition("perfil", "Perfil", Icons.Default.Person)
        )

        tabList.forEach { tab ->
            val isActive = activeTab == tab.id
            NavigationBarItem(
                selected = isActive,
                onClick = { onTabSelect(tab.id) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoldPrimary,
                    selectedTextColor = GoldPrimary,
                    indicatorColor = GoldPrimary.copy(alpha = 0.15f),
                    unselectedIconColor = Color.White.copy(alpha = 0.4f),
                    unselectedTextColor = Color.White.copy(alpha = 0.4f)
                )
            )
        }
    }
}

data class TabDefinition(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun FavoriteTabScreen(
    viewModel: TheoflixViewModel,
    onNavigateToCourse: (String) -> Unit
) {
    val favorites by viewModel.favoriteCourses.collectAsState()
    val percentages by viewModel.courseProgressPercentages.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = "MEUS FAVORITOS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum favorito favoritado ainda.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    favorites.forEach { course ->
                        CourseSearchItem(
                            course = course,
                            progress = percentages[course.id] ?: 0,
                            onClick = { onNavigateToCourse(course.id) }
                        )
                    }
                }
            }
        }
    }
}
