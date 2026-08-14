package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TheoflixViewModel
import com.example.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    viewModel: TheoflixViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect active Course data dynamically
    val courses by viewModel.allCourses.collectAsState()
    val percentages by viewModel.courseProgressPercentages.collectAsState()
    
    val course = remember(courses, courseId) {
        courses.find { it.id == courseId }
    }

    if (course == null) {
        Box(
            modifier = modifier.fillMaxSize().background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            Text("Carregando curso...", color = Color.White)
        }
        return
    }

    // Load available modules / lessons
    val modules by viewModel.currentCourseModules.collectAsState()
    val progressPercent = percentages[courseId] ?: 0

    // Force reloading modules in ViewModel when this Course opens
    LaunchedEffect(courseId) {
        viewModel.selectCourse(courseId)
    }

    val headerBgColor = remember(course.thumbnailColor) {
        Color(android.graphics.Color.parseColor(course.thumbnailColor))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- CORE SCROLLABLE CONTAINER ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Course Cover Hero Header banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        headerBgColor.copy(alpha = 0.5f),
                                        Color(0xFF0A0A0A)
                                    )
                                )
                            )
                    ) {
                        // Title action bar inside cover
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleFavorite(course.id, !course.isFavorite) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = if (course.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    tint = if (course.isFavorite) GoldPrimary else Color.White
                                )
                            }
                        }

                        // Play/Continue hovering indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(GoldPrimary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = course.category.uppercase(),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Course Metadata Details block
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = course.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 30.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = "Professor: ${course.teacher}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Info KPIs strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InfoBadge(label = "Carga:", value = course.duration)
                            InfoBadge(label = "Aulas:", value = "${modules.size} episódios")
                            InfoBadge(label = "Progresso:", value = "$progressPercent%")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Linear Progress Indicators
                        if (progressPercent > 0) {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                LinearProgressIndicator(
                                    progress = progressPercent / 100f,
                                    color = GoldPrimary,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$progressPercent% Concluído deste curso",
                                    fontSize = 11.sp,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Continuation Play button
                        val firstUncompletedModule = modules.find { !it.completed } ?: modules.firstOrNull()
                        Button(
                            onClick = {
                                firstUncompletedModule?.let { 
                                    onNavigateToPlayer(it.id)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("continue_course_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (progressPercent > 0) "Continuar de onde parou" else "Iniciar o Ensino",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Description
                        Text(
                            text = "SOBRE O CURSO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Text(
                            text = course.description,
                            fontSize = 13.sp,
                            color = Color.LightGray,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Divider Module title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "EPISÓDIOS DO MÓDULO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.Layers, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Modules list iteration item
                if (modules.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Criando lições de discipulado...", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    itemsIndexed(modules) { index, mod ->
                        ModuleLessonItem(
                            index = index + 1,
                            module = mod,
                            onClick = { onNavigateToPlayer(mod.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ModuleLessonItem(
    index: Int,
    module: com.example.data.ModuleEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lecture index or completed hook status
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (module.completed) GoldPrimary else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (module.completed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Concluída",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = index.toString(),
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = module.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Duração: ${module.duration}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            // Simple visual icon based on watch states
            Icon(
                imageVector = if (module.completed) Icons.Default.PlayCircleFilled else Icons.Default.PlayArrow,
                contentDescription = "Assistir",
                tint = if (module.completed) GoldPrimary else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
