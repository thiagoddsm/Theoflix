package com.example.ui.screens

import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.ui.TheoflixViewModel
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.delay

class TheoflixPlayerController(
    val webView: WebView?,
    val isReady: Boolean,
    val destroy: () -> Unit
)

@Composable
fun useTheoflixPlayer(
    videoUrl: String,
    onEnded: () -> Unit
): TheoflixPlayerController {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }

    // Safe lazy initialization of WebView to avoid memory leaks
    val webView = remember {
        WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun onVideoEnded() {
                    post { onEnded() }
                }

                @JavascriptInterface
                fun onPlayerReady() {
                    post { isReady = true }
                }
            }, "TheoflixAndroid")
        }
    }

    // Load or switch videos when URL changes, preserving correct cleanup
    DisposableEffect(videoUrl) {
        val isYouTube = videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be") || videoUrl.length == 11
        val videoId = if (isYouTube) {
            if (videoUrl.contains("v=")) videoUrl.substringAfter("v=").substringBefore("&")
            else if (videoUrl.contains("youtu.be/")) videoUrl.substringAfter("youtu.be/")
            else videoUrl
        } else ""

        val htmlContent = if (isYouTube) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                    #player { width: 100%; height: 100%; }
                </style>
                <script src="https://www.youtube.com/iframe_api"></script>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var player;
                    function initPlayer() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: '$videoId',
                            playerVars: {
                                'autoplay': 1,
                                'controls': 1,
                                'rel': 0,
                                'showinfo': 0,
                                'modestbranding': 1,
                                'playsinline': 1
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange
                            }
                        });
                    }

                    // Compatible with other parts of the app, does not overwrite global namespace unsafely
                    if (window.onYouTubeIframeAPIReady) {
                        var oldCallback = window.onYouTubeIframeAPIReady;
                        window.onYouTubeIframeAPIReady = function() {
                            try { oldCallback(); } catch(e){}
                            initPlayer();
                        };
                    } else {
                        window.onYouTubeIframeAPIReady = function() {
                            initPlayer();
                        };
                    }

                    if (window.YT && window.YT.Player) {
                        initPlayer();
                    }

                    function onPlayerReady(event) {
                        TheoflixAndroid.onPlayerReady();
                    }

                    function onPlayerStateChange(event) {
                        if (event.data === YT.PlayerState.ENDED) {
                            TheoflixAndroid.onVideoEnded();
                        }
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                    video { width: 100%; height: 100%; object-fit: contain; }
                </style>
            </head>
            <body>
                <video id="videoPlayer" src="$videoUrl" autoplay controls playsinline></video>
                <script>
                    var video = document.getElementById('videoPlayer');
                    video.addEventListener('ended', function() {
                        TheoflixAndroid.onVideoEnded();
                    });
                    video.addEventListener('canplay', function() {
                        TheoflixAndroid.onPlayerReady();
                    });
                </script>
            </body>
            </html>
            """.trimIndent()
        }

        webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)

        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
        }
    }

    // Permanent garbage collection cleanup on unmount
    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
    }

    return remember(webView, isReady) {
        TheoflixPlayerController(webView, isReady, {
            webView.stopLoading()
            webView.destroy()
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    moduleId: String,
    viewModel: TheoflixViewModel,
    onBackToCourse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.allCourses.collectAsState()
    val modules by viewModel.currentCourseModules.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressFlow by viewModel.playerProgress.collectAsState()

    // Find active module detail safely
    val activeModule = remember(modules, moduleId) {
        modules.find { it.id == moduleId }
    }

    val activeCourse = remember(courses, activeModule) {
        if (activeModule != null) {
            courses.find { it.id == activeModule.courseId }
        } else null
    }

    if (activeModule == null || activeCourse == null) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Verificando credenciais de streaming...", color = Color.White)
        }
        return
    }

    // --- QUIZ & TEMP NOTES STATES ---
    var showQuiz by remember { mutableStateOf(false) }
    var quizSubmitted by remember { mutableStateOf(false) }
    var quizAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var quizScore by remember { mutableStateOf(0) }
    var isQuizApproved by remember { mutableStateOf(false) }
    var lessonNotes by remember { mutableStateOf("") }

    // --- COMPACT FLOW MODULE LAMBDAS (Declared first to avoid forward-reference compilation errors) ---
    val resetQuiz = {
        quizSubmitted = false
        quizAnswers = emptyMap()
        quizScore = 0
        isQuizApproved = false
    }

    val completeEpisode = {
        viewModel.toggleModuleCompletionInPlayer(activeCourse.id, activeModule.id, true)
        showQuiz = false
        resetQuiz()
    }

    val submitQuiz = {
        var score = 0
        if (quizAnswers[0] == 1) score += 34
        if (quizAnswers[1] == 0) score += 33
        if (quizAnswers[2] == 1) score += 33

        quizScore = score
        quizSubmitted = true
        if (score >= 67) {
            isQuizApproved = true
            completeEpisode()
        } else {
            isQuizApproved = false
        }
    }

    val openQuizIfNeeded = {
        if (!activeModule.completed) {
            viewModel.updatePlayerPlayStatus(false)
            showQuiz = true
        } else {
            completeEpisode()
        }
    }

    // Set up LocalStorage caching of last opened lesson/course
    val context = LocalContext.current
    LaunchedEffect(moduleId) {
        try {
            val sharedPrefs = context.getSharedPreferences("TheoflixPrefs", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            editor.putString("last_course_id", activeCourse.id)
            editor.putString("last_module_id", activeModule.id)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Force play mode and seed initial states
    LaunchedEffect(moduleId) {
        viewModel.selectModuleForPlayback(moduleId)
    }

    // --- STATE RECOVERY & RESET TRANSITION ---
    // Reset all temporary fields when changing pages to prevent leaks!
    LaunchedEffect(moduleId) {
        showQuiz = false
        quizSubmitted = false
        quizAnswers = emptyMap()
        quizScore = 0
        isQuizApproved = false
        lessonNotes = ""
    }

    // Custom YouTube/HTML5 Player handler integration with memory-leak cleanup
    val playerController = useTheoflixPlayer(
        videoUrl = activeModule.videoUrl,
        onEnded = {
            // Trigger flow when video ends
            openQuizIfNeeded()
        }
    )

    // Interactive simulated video timeline ticker (synced progress)
    var currentProgress by remember { mutableStateOf(progressFlow) }
    LaunchedEffect(isPlaying, moduleId) {
        if (isPlaying) {
            while (true) {
                delay(1000L)
                val nextProg = (currentProgress + 0.04f).coerceAtMost(1.0f)
                currentProgress = nextProg
                viewModel.updatePlayerProgress(nextProg)
                if (nextProg >= 1.0f) {
                    openQuizIfNeeded()
                    break
                }
            }
        }
    }

    LaunchedEffect(progressFlow) {
        currentProgress = progressFlow
    }

    // Indices computations
    val currentIndex = recuerIndex(modules, activeModule.id)
    val hasPrev = currentIndex > 0
    val hasNext = currentIndex < modules.size - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070707))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- HEADER ACTION ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToCourse) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Sair do Player", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ASSISTINDO AGORA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = activeCourse.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- THEATER VIDEO BOX ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .testTag("video_player_box"),
                contentAlignment = Alignment.Center
            ) {
                // If the player controller has WebView ready, show it
                if (playerController.webView != null) {
                    AndroidView(
                        factory = { playerController.webView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Black,
                                        Color(0xFF161616),
                                        Color(android.graphics.Color.parseColor(activeCourse.thumbnailColor)).copy(alpha = 0.15f),
                                        Color.Black
                                    )
                                )
                            )
                    )
                }

                if (!playerController.isReady) {
                    // Overlay loader
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // Controllers progress seek & sliders metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentSeconds = (currentProgress * 25 * 60).toInt()
                    Text(
                        text = formatTime(currentSeconds),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = activeModule.duration,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Slider(
                    value = currentProgress,
                    onValueChange = {
                        currentProgress = it
                        viewModel.updatePlayerProgress(it)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = GoldPrimary,
                        activeTrackColor = GoldPrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                )
            }

            // --- CONTROLLERS AND DETAILS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeModule.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        )
                        Text(
                            text = "Por ${activeCourse.teacher}",
                            color = GoldPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Complete/Conclude action buttons trigger
                    Button(
                        onClick = { openQuizIfNeeded() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeModule.completed) Color(0xFF1B3B2B) else Color(0xFF141414),
                            contentColor = if (activeModule.completed) Color(0xFF4ADE80) else Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (activeModule.completed) Color(0xFF4ADE80).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (activeModule.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeModule.completed) "Concluido" else "Concluir",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- ANOTAÇÕES DO DISCIPULO (Notes with State preservation) ---
                Text(
                    text = "MINHAS ANOTAÇÕES DO ENSINO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = lessonNotes,
                    onValueChange = { lessonNotes = it },
                    placeholder = { Text("Anote aqui os insights espirituais, versículos e reflexões de discipulado...", color = Color.Gray, fontSize = 12.sp) },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF101010),
                        unfocusedContainerColor = Color(0xFF101010),
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "DESCRIÇÃO DA AULA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Aprofunde-se nos princípios eternos trazidos nesta lição. Busque aplicar no seu discipulado individual, família ou nos Grupos de Crescimento celulares da sua igreja. Recomendamos meditação e anotação complementar.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous layout
                    OutlinedButton(
                        onClick = {
                            if (hasPrev) {
                                val prevModId = modules[currentIndex - 1].id
                                viewModel.selectModuleForPlayback(prevModId)
                            }
                        },
                        enabled = hasPrev,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.Gray
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Anterior", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Next layout
                    Button(
                        onClick = {
                            if (hasNext) {
                                val nextModId = modules[currentIndex + 1].id
                                viewModel.selectModuleForPlayback(nextModId)
                            }
                        },
                        enabled = hasNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasNext) Color.White else Color(0xFF222222),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Próxima Aula", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // --- MINI THEOLOGICAL ASSESSMENT QUIZ DIALOG ---
    if (showQuiz) {
        Dialog(onDismissRequest = { showQuiz = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "AVALIAÇÃO DE CRESCIMENTO",
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "Módulo: ${activeModule.title}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Question 1
                    Text(
                        text = "1. Qual deve ser o principal alicerce do ministro de crescimento celular?",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    listOf("Autopromoção egoica", "Pastoreio relacional amoroso e bíblico").forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { quizAnswers = quizAnswers + (0 to index) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = quizAnswers[0] == index,
                                onClick = { quizAnswers = quizAnswers + (0 to index) },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = option, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Question 2
                    Text(
                        text = "2. Como se dá a multiplicação saudável e abençoada das células?",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    listOf("Formando discípulos e novos líderes espirituais", "Dividindo os membros arbitrariamente sem preparo").forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { quizAnswers = quizAnswers + (1 to index) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = quizAnswers[1] == index,
                                onClick = { quizAnswers = quizAnswers + (1 to index) },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = option, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Question 3
                    Text(
                        text = "3. Qual o papel do caráter na liderança aprovada teologicamente?",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    listOf("É secundário frente às habilidades oratórias", "Essencial para gerar frutos reais e autoridade espiritual").forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { quizAnswers = quizAnswers + (2 to index) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = quizAnswers[2] == index,
                                onClick = { quizAnswers = quizAnswers + (2 to index) },
                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = option, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit actions & scores feedback
                    if (quizSubmitted) {
                        if (isQuizApproved) {
                            Text(
                                text = "Aprovado com Sucesso! Score: $quizScore/100",
                                color = Color(0xFF4ADE80),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        } else {
                            Text(
                                text = "Não atingiu o score mínimo de 70%. Seu score: $quizScore/100. Tente novamente!",
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showQuiz = false }) {
                            Text("Fechar", color = Color.Gray, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { submitQuiz() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            enabled = quizAnswers.size == 3
                        ) {
                            Text("Enviar Respostas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

private fun recuerIndex(modules: List<com.example.data.ModuleEntity>, termId: String): Int {
    return modules.indexOfFirst { it.id == termId }.coerceAtLeast(0)
}
