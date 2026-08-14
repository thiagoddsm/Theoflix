package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Course
import com.example.ui.TheoflixViewModel
import com.example.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TheoflixViewModel,
    onNavigateToCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.allCourses.collectAsState()
    val favorites by viewModel.favoriteCourses.collectAsState()
    val continuingWatching by viewModel.continuingWatchingCourses.collectAsState()
    val progressPercentages by viewModel.courseProgressPercentages.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    val scrollState = rememberScrollState()

    // Filter courses based on top search bar queries
    val filteredCourses = remember(courses, query) {
        if (query.isBlank()) {
            courses
        } else {
            courses.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true) ||
                        it.teacher.contains(query, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                    Text(
                        text = "THEOFLIX",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = (-1).sp,
                        color = GoldPrimary,
                        modifier = Modifier.clickable { viewModel.setSearchQuery("") }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile initials avatar
                    val initials = if (userName.length >= 2) userName.take(2).uppercase() else "TF"
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .clickable { viewModel.setTab("perfil") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0A0A0A)
                        )
                    }
                }
            }

            // --- SEARCH BOX ---
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Pesquisar cursos, trilhas, professores...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedBorderColor = GoldPrimary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            )

            // Dynamic grid layout or horizontal strips
            if (query.isNotBlank()) {
                // Search result view
                Text(
                    text = "Resultados para \"$query\"",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                if (filteredCourses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum curso encontrado.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 80.dp)
                    ) {
                        filteredCourses.forEach { course ->
                            CourseSearchItem(
                                course = course,
                                progress = progressPercentages[course.id] ?: 0,
                                onClick = { onNavigateToCourse(course.id) }
                            )
                        }
                    }
                }
            } else {
                // Primary Dashboard Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp)
                ) {
                    // --- HERO BANNER (Top featured or Continue Watching) ---
                    HeroBannerSection(
                        continuingWatching = continuingWatching,
                        defaultCourse = courses.firstOrNull(),
                        progressPercentages = progressPercentages,
                        onNavigate = onNavigateToCourse
                    )

                    // --- SECTION 1: Continuar Assistindo (Condicional) ---
                    if (continuingWatching.isNotEmpty()) {
                        StreamRowSection(
                            title = "Continuar Assistindo",
                            courses = continuingWatching,
                            progressPercentages = progressPercentages,
                            onCourseClick = onNavigateToCourse,
                            viewModel = viewModel
                        )
                    }

                    // --- SECTION 2: Trilhas Recomendadas (Seed) ---
                    val trilhas = courses.filter { it.category == "Liderança" || it.category == "GC" }
                    StreamRowSection(
                        title = "Trilhas Recomendadas",
                        courses = trilhas,
                        progressPercentages = progressPercentages,
                        onCourseClick = onNavigateToCourse,
                        viewModel = viewModel
                    )

                    // --- SECTION 3: Escola de Líderes ---
                    val escolaLideres = courses.filter { it.category == "Liderança" }
                    if (escolaLideres.isNotEmpty()) {
                        StreamRowSection(
                            title = "Escola de Líderes",
                            courses = escolaLideres,
                            progressPercentages = progressPercentages,
                            onCourseClick = onNavigateToCourse,
                            viewModel = viewModel
                        )
                    }

                    // --- SECTION 4: Discipulado ---
                    val discipulado = courses.filter { it.category == "Discipulado" }
                    if (discipulado.isNotEmpty()) {
                        StreamRowSection(
                            title = "Discipulado",
                            courses = discipulado,
                            progressPercentages = progressPercentages,
                            onCourseClick = onNavigateToCourse,
                            viewModel = viewModel
                        )
                    }

                    // --- SECTION 5: Família ---
                    val familia = courses.filter { it.category == "Família" }
                    if (familia.isNotEmpty()) {
                        StreamRowSection(
                            title = "Família",
                            courses = familia,
                            progressPercentages = progressPercentages,
                            onCourseClick = onNavigateToCourse,
                            viewModel = viewModel
                        )
                    }

                    // --- SECTION 6: Teologia ---
                    val teologia = courses.filter { it.category == "Teologia" }
                    if (teologia.isNotEmpty()) {
                        StreamRowSection(
                            title = "Teologia",
                            courses = teologia,
                            progressPercentages = progressPercentages,
                            onCourseClick = onNavigateToCourse,
                            viewModel = viewModel
                        )
                    }

                    // --- SECTION 7: Cursos Recentes ---
                    StreamRowSection(
                        title = "Cursos Recentes",
                        courses = courses.takeLast(3),
                        progressPercentages = progressPercentages,
                        onCourseClick = onNavigateToCourse,
                        viewModel = viewModel
                    )

                    // --- SECTION 8: Favoritos cellulaires ---
                    if (favorites.isNotEmpty()) {
                        StreamRowSection(
                            title = "Meus Favoritos",
                            courses = favorites,
                            progressPercentages = progressPercentages,
                            onCourseClick = onNavigateToCourse,
                            viewModel = viewModel
                        )
                    } else {
                        // Encourage favorites seeding if list empty
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF121212))
                                .border(1.dp, Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Ainda sem favoritos salvos", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Toque no ícone de favorito no detalhe dos cursos.", color = Color.Gray.copy(alpha = 0.6f), fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroBannerSection(
    continuingWatching: List<Course>,
    defaultCourse: Course?,
    progressPercentages: Map<String, Int>,
    onNavigate: (String) -> Unit
) {
    // If user has a continuing watch course, feature it, otherwise show default first Course
    val featuredCourse = continuingWatching.firstOrNull() ?: defaultCourse ?: return
    val progress = progressPercentages[featuredCourse.id] ?: 0

    val overlayBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.95f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { onNavigate(featuredCourse.id) },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1B18)) // Fallback dark container
        ) {
            // Simulated gradient wallpaper instead of raw file image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFC5A059).copy(alpha = 0.45f),
                                Color(0xFF141414)
                            )
                        )
                    )
            )

            // Color block pattern accentuating high contrast visual depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )

            // Content Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (progress > 0) "CONTINUAR" else "PRIMEIRO DESTAQUE",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = featuredCourse.category.uppercase(),
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = featuredCourse.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Aulas com ${featuredCourse.teacher} • ${featuredCourse.duration}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (progress > 0) {
                    // Stream progress bar indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            LinearProgressIndicator(
                                progress = progress / 100f,
                                color = GoldPrimary,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$progress% concluído • 12 min restantes",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onNavigate(featuredCourse.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Excelente capacitação de liderança para você iniciar hoje.",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                        )

                        Button(
                            onClick = { onNavigate(featuredCourse.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Começar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamRowSection(
    title: String,
    courses: List<Course>,
    progressPercentages: Map<String, Int>,
    onCourseClick: (String) -> Unit,
    viewModel: TheoflixViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = GoldPrimary
            )
            
            Text(
                text = "Ver todos",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.clickable { /* future expand action */ }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(courses) { course ->
                val progress = progressPercentages[course.id] ?: 0
                StreamCourseCard(
                    course = course,
                    progress = progress,
                    onClick = { onCourseClick(course.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(course.id, !course.isFavorite) }
                )
            }
        }
    }
}

@Composable
fun StreamCourseCard(
    course: Course,
    progress: Int,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column {
            // Visual simulated cover thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(course.thumbnailColor)).copy(alpha = 0.5f),
                                Color(0xFF141414)
                            )
                        )
                    )
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = course.category,
                        color = GoldPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Favorite icon toggle
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = if (course.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (course.isFavorite) GoldPrimary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (progress > 0) {
                    // Small progress slide indicator
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

            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = course.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = course.teacher,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.duration,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )

                    if (progress > 0) {
                        Text(
                            text = "$progress%",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseSearchItem(
    course: Course,
    progress: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(course.thumbnailColor)).copy(alpha = 0.7f),
                                Color(0xFF1D1B18)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = course.category.take(2).uppercase(),
                    color = GoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = course.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${course.teacher} • ${course.duration}",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (progress > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            color = GoldPrimary,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .width(60.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$progress% concluído",
                            fontSize = 9.sp,
                            color = GoldPrimary
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Ver detalhe",
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
