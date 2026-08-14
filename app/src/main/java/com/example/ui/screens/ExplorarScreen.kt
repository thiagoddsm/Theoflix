package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Course
import com.example.ui.TheoflixViewModel
import com.example.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorarScreen(
    viewModel: TheoflixViewModel,
    onNavigateToCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val filterTab by viewModel.selectedFilterTab.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val courses by viewModel.allCourses.collectAsState()
    val progressPercentages by viewModel.courseProgressPercentages.collectAsState()

    // Supported categories aligning with IBM Church Levels & Tracks
    val categories = listOf(
        CategoryItem("Fundamentos", Icons.Default.Shield, Color(0xFF1D4ED8)),
        CategoryItem("Consolidação", Icons.Default.Home, Color(0xFFE11D48)),
        CategoryItem("Liderança", Icons.Default.Groups, Color(0xFFD97706)),
        CategoryItem("Alta Gestão", Icons.Default.AutoStories, Color(0xFF7C3AED)),
        CategoryItem("Obrigatório", Icons.Default.VolunteerActivism, Color(0xFF2563EB)),
        CategoryItem("Maturidade", Icons.Default.MenuBook, Color(0xFFBE123C)),
        CategoryItem("Pastoreio", Icons.Default.Campaign, Color(0xFFF59E0B))
    )

    // useDeferredValue equivalent in Jetpack Compose to optimize heavy search filters
    var deferredQuery by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(250L) // Debounce/defer filter computations by 250ms
        deferredQuery = query
    }

    // Filtered courses based on current search & active category/tabs
    val filteredCourses = remember(courses, deferredQuery, selectedCategory, filterTab) {
        courses.filter { course ->
            val matchesQuery = deferredQuery.isBlank() || 
                    course.title.contains(deferredQuery, ignoreCase = true) ||
                    course.teacher.contains(deferredQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == null || 
                    course.category.equals(selectedCategory, ignoreCase = true)

            // Filter for Tab - Trilhas can mean focused curation, categories is grid
            val matchesTab = when (filterTab) {
                "Cursos" -> true
                "Trilhas" -> course.category == "Liderança" || course.category == "GC"
                else -> true // "Categorias" shows matching categories
            }

            matchesQuery && matchesCategory && matchesTab
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Title Header
            Text(
                text = "EXPLORAR",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            // Global search bar
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("O que você deseja aprender hoje?", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            )

            // Core Top Filters / Grid Tabs (Cursos / Trilhas / Categorias)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Categorias", "Cursos", "Trilhas").forEach { tab ->
                    val isActive = filterTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) GoldPrimary else Color(0xFF141414))
                            .border(1.dp, if (isActive) GoldPrimary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.setFilterTab(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isActive) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (filterTab == "Categorias" && query.isBlank() && selectedCategory == null) {
                // Category Selection Panel grid
                Text(
                    text = "CATEGORIAS DO REINO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { viewModel.toggleCategoryFilter(category.name) }
                        )
                    }
                }
            } else {
                // Render list / grid of filtered courses
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeLabel = selectedCategory ?: "Cursos Filtrados"
                    Text(
                        text = activeLabel.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 1.sp
                    )

                    if (selectedCategory != null) {
                        Text(
                            text = "Limpar Filtro",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.clickable { viewModel.toggleCategoryFilter(selectedCategory!!) }
                        )
                    }
                }

                if (filteredCourses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum curso coincide com os filtros.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredCourses) { course ->
                            GridCourseCard(
                                course = course,
                                progress = progressPercentages[course.id] ?: 0,
                                onClick = { onNavigateToCourse(course.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun CategoryCard(
    category: CategoryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(category.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = category.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun GridCourseCard(
    course: Course,
    progress: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(course.thumbnailColor)).copy(alpha = 0.45f),
                                Color(0xFF141414)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = course.category,
                        color = GoldPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (progress > 0) {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        color = GoldPrimary,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = course.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.teacher,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.duration,
                        color = Color.Gray,
                        fontSize = 8.sp
                    )
                    if (progress > 0) {
                        Text(
                            text = "$progress%",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}
