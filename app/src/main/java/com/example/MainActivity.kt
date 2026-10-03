package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.PrismChatTheme
import com.example.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by chatViewModel.isDarkMode.collectAsState()
            val currentUser by chatViewModel.currentUser.collectAsState()

            PrismChatTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = currentUser != null,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "AuthOrAppTransition"
                    ) { isAuthenticated ->
                        if (!isAuthenticated) {
                            AuthScreen(
                                onLoginWithPassword = { identifier, password, onResult ->
                                    chatViewModel.loginWithPassword(identifier, password, onResult)
                                },
                                onRegister = { displayName, username, email, password, avatarUri, onResult ->
                                    chatViewModel.registerUser(displayName, username, email, password, avatarUri, onResult)
                                },
                                onLoginWithGoogle = { googleEmail, displayName, onResult ->
                                    chatViewModel.loginWithGoogle(googleEmail, displayName, onResult)
                                }
                            )
                        } else {
                            AppNavigation(viewModel = chatViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: ChatViewModel) {
    val activeConversationId by viewModel.activeConversationId.collectAsState()

    AnimatedContent(
        targetState = activeConversationId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { convId ->
        if (convId == null) {
            HomeScreen(
                viewModel = viewModel,
                onOpenConversation = { id ->
                    viewModel.selectConversation(id)
                }
            )
        } else {
            ChatDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.selectConversation(null)
                }
            )
        }
    }
}
