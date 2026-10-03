package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationEntity
import com.example.data.model.UserEntity
import com.example.ui.components.AudioMessageBubble
import com.example.ui.components.ChatInputBar
import com.example.ui.components.FullscreenImageViewer
import com.example.ui.components.GradientAvatar
import com.example.ui.components.GroupDetailsDialog
import com.example.ui.components.ImageMessageBubble
import com.example.ui.components.TextMessageBubble
import com.example.ui.components.UserProfileDialog
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PrismGradients
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val conversation by viewModel.activeConversation.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val groupMembers by viewModel.activeGroupMemberUsers.collectAsState()
    val allUsers by viewModel.otherUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val typingMap by viewModel.typingStatus.collectAsState()
    val fullscreenImage by viewModel.fullscreenImageUri.collectAsState()
    val viewingProfile by viewModel.viewingUserProfile.collectAsState()
    val showGroupInfo by viewModel.showGroupInfoDialog.collectAsState()

    // Voice player state
    val playingMessageId by viewModel.voiceManager.playingMessageId.collectAsState()
    val playbackProgress by viewModel.voiceManager.playbackProgress.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Fullscreen Image Dialog
    if (fullscreenImage != null) {
        FullscreenImageViewer(
            imageUri = fullscreenImage,
            onDismiss = { viewModel.openFullscreenImage(null) }
        )
    }

    // User Profile Dialog
    if (viewingProfile != null) {
        UserProfileDialog(
            user = viewingProfile,
            isCurrentUser = viewingProfile?.id == currentUser?.id,
            onDismiss = { viewModel.showUserProfile(null) },
            onSendMessage = { user -> viewModel.startDirectChat(user) },
            onEditProfile = { viewModel.setShowEditProfileDialog(true) }
        )
    }

    // Group Details Dialog
    if (showGroupInfo && conversation != null) {
        GroupDetailsDialog(
            conversation = conversation!!,
            members = groupMembers,
            availableUsers = allUsers,
            onDismiss = { viewModel.setShowGroupInfoDialog(false) },
            onUserClick = { user -> viewModel.showUserProfile(user) },
            onAddMember = { userId -> viewModel.addMemberToActiveGroup(userId) }
        )
    }

    val currentTyping = conversation?.id?.let { typingMap[it] }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            ChatTopBar(
                conversation = conversation,
                memberCount = groupMembers.size,
                typingStatus = currentTyping,
                onBack = onNavigateBack,
                onHeaderClick = {
                    if (conversation?.isGroup == true) {
                        viewModel.setShowGroupInfoDialog(true)
                    } else {
                        // Find direct user
                        val directUser = allUsers.firstOrNull { it.id == conversation?.directUserId }
                        if (directUser != null) {
                            viewModel.showUserProfile(directUser)
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                voiceManager = viewModel.voiceManager,
                onSendMessage = { text -> viewModel.sendMessage(text) },
                onSendImage = { imageUri -> viewModel.sendImage(imageUri) },
                onSendVoiceNote = { audioUri, duration -> viewModel.sendVoiceNote(audioUri, duration) },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Topic card for groups
                if (conversation?.isGroup == true && !conversation?.description.isNullOrBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Space Topic",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = conversation?.description ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    when (msg.mediaType) {
                        "IMAGE" -> {
                            ImageMessageBubble(
                                message = msg,
                                onImageClick = { uri -> viewModel.openFullscreenImage(uri) },
                                onAvatarClick = {
                                    val user = allUsers.firstOrNull { it.id == msg.senderId }
                                    if (user != null) viewModel.showUserProfile(user)
                                }
                            )
                        }
                        "AUDIO" -> {
                            AudioMessageBubble(
                                message = msg,
                                isPlaying = playingMessageId == msg.id,
                                progress = if (playingMessageId == msg.id) playbackProgress else 0f,
                                onPlayPause = {
                                    msg.mediaUri?.let { uri ->
                                        viewModel.voiceManager.togglePlayVoice(msg.id, uri)
                                    }
                                },
                                onAvatarClick = {
                                    val user = allUsers.firstOrNull { it.id == msg.senderId }
                                    if (user != null) viewModel.showUserProfile(user)
                                }
                            )
                        }
                        else -> {
                            TextMessageBubble(
                                message = msg,
                                onAvatarClick = {
                                    val user = allUsers.firstOrNull { it.id == msg.senderId }
                                    if (user != null) viewModel.showUserProfile(user)
                                }
                            )
                        }
                    }
                }

                // Real-time typing bubble
                if (currentTyping != null) {
                    item {
                        TypingIndicatorBubble(typingText = currentTyping)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTopBar(
    conversation: ConversationEntity?,
    memberCount: Int,
    typingStatus: String?,
    onBack: () -> Unit,
    onHeaderClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("chat_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Avatar + Name + Subtitle (clickable to view space/profile details)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onHeaderClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (conversation?.isGroup == true) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrismGradients.getAvatarBrush(conversation.gradientIndex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    GradientAvatar(
                        name = conversation?.name ?: "Chat",
                        avatarUri = conversation?.avatarUri,
                        gradientIndex = conversation?.gradientIndex ?: 0,
                        size = 42.dp,
                        showStatusDot = true,
                        status = "Online"
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = conversation?.name ?: "Chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (typingStatus != null) {
                        Text(
                            text = typingStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPink,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (conversation?.isGroup == true) {
                        Text(
                            text = "$memberCount participants",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Active now",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Info action button
            IconButton(
                onClick = onHeaderClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("chat_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(typingText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonPink)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = typingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}
