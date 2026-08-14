package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TheoflixViewModel
import com.example.ui.theme.GoldPrimary
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: TheoflixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var email by remember { mutableStateFlowOf("") }
    var password by remember { mutableStateFlowOf("") }
    var passwordVisible by remember { mutableStateFlowOf(false) }

    val loginError by viewModel.loginError.collectAsState()

    // OAuth 2.0 Web Client ID from official google-services.json
    val webClientId = "989586605112-pl638euilgmaki5i1e5cuna6le80nu27.apps.googleusercontent.com"

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(idToken)
            } else {
                viewModel.setLoginError("Token do Google não retornado. Verifique a configuração do SHA-1 no Firebase.")
            }
        } catch (e: ApiException) {
            viewModel.setLoginError("Erro Google Sign-In (código ${e.statusCode}): ${e.localizedMessage}")
        } catch (e: Exception) {
            viewModel.setLoginError("Erro ao autenticar com Google: ${e.message}")
        }
    }

    // Smooth gradient backgrounds matching streaming visual aesthetics
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF141414),
            Color(0xFF0A0A0A)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo column
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "THEOFLIX",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-1.5).sp,
                color = GoldPrimary,
                modifier = Modifier
                    .testTag("theoflix_logo")
                    .padding(bottom = 6.dp)
            )

            Text(
                text = "Plataforma Cristã de Ensino e Discipulado Celular",
                fontSize = 12.sp,
                color = Color.LightGray.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Badge de Versão Oficial
            Surface(
                color = GoldPrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "v2.0.0 • IBM OFICIAL (6 CURSOS)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = GoldPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Login inputs container card style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF161616))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Acesse sua conta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail do OikoApp") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color.Gray
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input")
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val desc = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                        
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = desc, tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Error field
                if (loginError != null) {
                    Text(
                        text = loginError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Entrar Button
                Button(
                    onClick = { viewModel.login(email, password) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_button"),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    )
                ) {
                    Text(
                        text = "Entrar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Acesse utilizando seu e-mail e senha ou sua conta Google do OikoApp.",
                    fontSize = 12.sp,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Login Integration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.1f)
                )
                Text(
                    text = "OU ENTRAR COM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Official Google login button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        // Sign out first so Google always displays the account chooser dialog
                        client.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(client.signInIntent)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continuar com o Google",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Support flow for compose mutable state declaration in older gradle/room dependencies
private fun <T> mutableStateFlowOf(value: T): MutableState<T> = mutableStateOf(value)
