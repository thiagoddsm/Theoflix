package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.TheoflixViewModel
import com.example.ui.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    viewModel: TheoflixViewModel,
    onNavigateToCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val completedCount by viewModel.completedCoursesCount.collectAsState()
    val hoursWatched by viewModel.totalHoursWatched.collectAsState()
    val certificatesCount by viewModel.certificatesCount.collectAsState()
    val faveCourses by viewModel.favoriteCourses.collectAsState()
    val allCourses by viewModel.allCourses.collectAsState()
    val percentages by viewModel.courseProgressPercentages.collectAsState()

    var showCertificateDialogForCourseTitle by remember { mutableStateOf<String?>(null) }

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
            // Screen title
            Text(
                text = "MEU PERFIL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Member detail header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Letter avatar
                        val initial = userName.firstOrNull()?.uppercase() ?: "T"
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = userEmail,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MEMBRO PARCEIRO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // KPIs dynamic strip container
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiCard(
                            label = "Concluídos",
                            value = completedCount.toString(),
                            icon = Icons.Default.DoneAll,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Horas",
                            value = String.format("%.1f", hoursWatched),
                            icon = Icons.Default.AccessTime,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            label = "Certificados",
                            value = certificatesCount.toString(),
                            icon = Icons.Default.CardMembership,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // CERTIFICADOS DISPONÍVEIS
                item {
                    Text(
                        text = "MEUS CERTIFICADOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    val courses100percent = allCourses.filter { (percentages[it.id] ?: 0) == 100 }

                    if (courses100percent.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141414))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Conclua qualquer curso para gerar seu certificado de formação.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            courses100percent.forEach { course ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1D1B18))
                                        .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .clickable { showCertificateDialogForCourseTitle = course.title }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(course.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Toque para abrir certificado oficial", color = Color.Gray, fontSize = 10.sp)
                                    }
                                    Icon(Icons.Default.RemoveRedEye, contentDescription = "Ver", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // FAVORITADOS
                item {
                    Text(
                        text = "MEUS FAVORITADOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    if (faveCourses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF141414))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhum curso favoritado no momento.", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            faveCourses.forEach { course ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF141414))
                                        .clickable { onNavigateToCourse(course.id) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(android.graphics.Color.parseColor(course.thumbnailColor)).copy(alpha = 0.3f))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(course.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(course.category, color = Color.Gray, fontSize = 10.sp)
                                    }
                                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Abrir", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // CONFIGURAÇÕES E OUTROS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141414))
                            .padding(6.dp)
                    ) {
                        ProfileOptionRow(
                            icon = Icons.Default.Settings,
                            label = "Configurações da Conta",
                            onClick = {}
                        )
                        ProfileOptionRow(
                            icon = Icons.Default.Security,
                            label = "Privacidade e Segurança",
                            onClick = {}
                        )
                        ProfileOptionRow(
                            icon = Icons.Default.DownloadForOffline,
                            label = "Gerenciar Downloads (V2)",
                            onClick = {}
                        )
                    }
                }

                // LOGOUT BUTTON
                item {
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.2f),
                            contentColor = Color.Red
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "Sair")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sair do THEOFLIX",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // --- PRESTIGIOUS GOLD VISUAL CERTIFICATE DIALOG DIALOGUE ---
        if (showCertificateDialogForCourseTitle != null) {
            Dialog(onDismissRequest = { showCertificateDialogForCourseTitle = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(2.dp, GoldPrimary, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "THEOFLIX",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.SansSerif,
                            color = GoldPrimary,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "MINISTÉRIO DE ENSINO E DISCIPULADO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "CERTIFICADO DE CONCLUSÃO",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Certificamos para os devidos fins ministeriais e espirituais que o membro",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = userName.uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "concluiu com aproveitamento de 100% o currículo de capacitação ministerial do curso",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "\"${showCertificateDialogForCourseTitle ?: ""}\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 6.dp))

                        Text(
                            text = "Credenciado com Oiko Core de Discipulado",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showCertificateDialogForCourseTitle = null },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fechar Certificado", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
            Column {
                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ProfileOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}
