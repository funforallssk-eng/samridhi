package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ConversationEntity
import com.example.data.model.UserEntity
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.FindUsersDialog
import com.example.ui.components.FirebaseSyncDialog
import com.example.ui.components.GoogleAuthDialog
import com.example.ui.components.GradientAvatar
import com.example.ui.components.RichTextHelper
import com.example.ui.components.UserProfileDialog
import androidx.compose.material.icons.filled.Security
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PrismGradients
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenConversation: (String) -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val otherUsers by viewModel.otherUsers.collectAsState()
    val directConversations by viewModel.directConversations.collectAsState()
    val groupConversations by viewModel.groupConversations.collectAsState()
    val selectedTab by viewModel.selectedHomeTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val viewingProfile by viewModel.viewingUserProfile.collectAsState()
    val showEditProfile by viewModel.showEditProfileDialog.collectAsState()
    val showCreateGroup by viewModel.showCreateGroupDialog.collectAsState()
    val showGoogleAuth by viewModel.showGoogleAuthDialog.collectAsState()
    val showFindUsers by viewModel.showFindUsersDialog.collectAsState()
    val showFirebaseSync by viewModel.showFirebaseSyncDialog.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val incomingNotification by viewModel.incomingNotification.collectAsState()

    var showNewChatDialog by remember { mutableStateOf(false) }

    // Dialogs
    if (showFirebaseSync) {
        FirebaseSyncDialog(
            syncStatus = syncStatus,
            onDismiss = { viewModel.setShowFirebaseSyncDialog(false) },
            onConfigure = { projId, key ->
                viewModel.configureFirebase(projId, key)
            }
        )
    }

    if (showFindUsers) {
        FindUsersDialog(
            allUsers = otherUsers,
            onDismiss = { viewModel.setShowFindUsersDialog(false) },
            onStartChat = { user ->
                viewModel.startDirectChat(user)
            },
            onViewProfile = { user ->
                viewModel.showUserProfile(user)
            }
        )
    }

    if (showGoogleAuth) {
        GoogleAuthDialog(
            currentUser = currentUser,
            onDismiss = { viewModel.setShowGoogleAuthDialog(false) },
            onUpdateGoogleAuth = { isEnabled, googleEmail ->
                viewModel.updateGoogleAuth(isEnabled, googleEmail)
            }
        )
    }

    if (viewingProfile != null) {
        UserProfileDialog(
            user = viewingProfile,
            isCurrentUser = viewingProfile?.id == currentUser?.id,
            onDismiss = { viewModel.showUserProfile(null) },
            onSendMessage = { user -> viewModel.startDirectChat(user) },
            onEditProfile = { viewModel.setShowEditProfileDialog(true) },
            onOpenGoogleAuth = { viewModel.setShowGoogleAuthDialog(true) },
            onLogout = { viewModel.logout() }
        )
    }

    if (showEditProfile) {
        EditProfileDialog(
            currentUser = currentUser,
            onDismiss = { viewModel.setShowEditProfileDialog(false) },
            onSave = { username, displayName, bio, avatarUri, status ->
                viewModel.updateProfile(username, displayName, bio, avatarUri, status)
            }
        )
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            availableUsers = otherUsers,
            onDismiss = { viewModel.setShowCreateGroupDialog(false) },
            onCreateGroup = { name, desc, members, gradientIdx ->
                viewModel.createGroup(name, desc, members, gradientIdx)
            }
        )
    }

    // New Direct Chat Contact Picker Dialog
    if (showNewChatDialog) {
        NewChatContactDialog(
            contacts = otherUsers,
            onDismiss = { showNewChatDialog = false },
            onSelectContact = { user ->
                showNewChatDialog = false
                viewModel.startDirectChat(user)
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        showNewChatDialog = true
                    } else {
                        viewModel.setShowCreateGroupDialog(true)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("home_fab")
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.Chat else Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "New Chat" else "New Space"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Bar
            HomeHeader(
                currentUser = currentUser,
                isDarkMode = isDarkMode,
                syncStatus = syncStatus,
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onProfileClick = { viewModel.showUserProfile(currentUser) },
                onGoogleAuthClick = { viewModel.setShowGoogleAuthDialog(true) },
                onCloudSyncClick = { viewModel.setShowFirebaseSyncDialog(true) }
            )

            // Live Cross-Device Push Message Banner
            AnimatedVisibility(visible = incomingNotification != null) {
                incomingNotification?.let { (sender, text) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { viewModel.clearIncomingNotification() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Message from $sender", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text(text, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { viewModel.clearIncomingNotification() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search messages, people, spaces...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar")
                )
            }

            // Quick Find People Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setShowFindUsersDialog(true) }
                        .testTag("open_find_users_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Find people by @username or email",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Find & Chat",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tabs: Direct Chats (DMs) vs Spaces (Groups)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonPink,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setHomeTab(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Direct Chats", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setHomeTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Spaces & Groups", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Conversations List
            val conversations = if (selectedTab == 0) directConversations else groupConversations
            val filtered = conversations.filter {
                searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.Chat else Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 0) "No direct messages yet" else "No spaces yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedTab == 0) "Start a chat with team members using the + button" else "Create a collaboration space for your group",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = {
                                viewModel.selectConversation(conv.id)
                                onOpenConversation(conv.id)
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    currentUser: UserEntity?,
    isDarkMode: Boolean,
    syncStatus: com.example.data.sync.CloudSyncStatus? = null,
    onToggleDarkMode: () -> Unit,
    onProfileClick: () -> Unit,
    onGoogleAuthClick: () -> Unit,
    onCloudSyncClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App brand with gradient accent
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrismGradients.PrimaryBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Samridhi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (currentUser != null) "@${currentUser.username}" else "Real-time spaces",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Actions: Cloud Sync, Google Authenticator 2FA, Dark/Light Mode toggle & User Avatar
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Cloud Sync Status button
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onCloudSyncClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("cloud_sync_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Real-Time Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                val isConnected = syncStatus?.state == com.example.data.sync.SyncConnectionState.CONNECTED
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF10B981) else Color(0xFF2196F3))
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Google Authenticator Shield button
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                    onClick = onGoogleAuthClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("google_auth_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Google Authenticator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (currentUser?.isGoogleAuthEnabled == true) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("dark_mode_toggle")
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme mode",
                    tint = if (isDarkMode) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (currentUser != null) {
                GradientAvatar(
                    name = currentUser.displayName,
                    avatarUri = currentUser.avatarUri,
                    gradientIndex = currentUser.gradientIndex,
                    size = 40.dp,
                    showStatusDot = true,
                    status = currentUser.status,
                    onClick = onProfileClick,
                    modifier = Modifier.testTag("current_user_avatar_button")
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val cleanLastMessage = remember(conversation.lastMessage) {
        RichTextHelper.stripFormatting(conversation.lastMessage)
    }

    val timeFormatted = remember(conversation.lastMessageTime) {
        val now = System.currentTimeMillis()
        val diff = now - conversation.lastMessageTime
        val oneDay = 1000 * 60 * 60 * 24
        if (diff < oneDay) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(conversation.lastMessageTime))
        } else {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(conversation.lastMessageTime))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_item_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (conversation.isGroup) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(PrismGradients.getAvatarBrush(conversation.gradientIndex)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        } else {
            GradientAvatar(
                name = conversation.name,
                avatarUri = conversation.avatarUri,
                gradientIndex = conversation.gradientIndex,
                size = 50.dp,
                showStatusDot = true,
                status = "Online"
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (conversation.unreadCount > 0) NeonPink else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cleanLastMessage.ifEmpty { "Start a conversation" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(PrismGradients.PrimaryBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatContactDialog(
    contacts: List<UserEntity>,
    onDismiss: () -> Unit,
    onSelectContact: (UserEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Start Direct Chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn {
                    items(contacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectContact(contact) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GradientAvatar(
                                name = contact.displayName,
                                avatarUri = contact.avatarUri,
                                gradientIndex = contact.gradientIndex,
                                size = 42.dp,
                                showStatusDot = true,
                                status = contact.status
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = contact.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = contact.status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
