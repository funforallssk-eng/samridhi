package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.VoiceManager
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PrismGradients
import java.util.Locale

@Composable
fun ChatInputBar(
    voiceManager: VoiceManager,
    onSendMessage: (String) -> Unit,
    onSendImage: (String) -> Unit,
    onSendVoiceNote: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var showFormatToolbar by remember { mutableStateOf(false) }

    val isRecording by voiceManager.isRecording.collectAsState()
    val recordDurationSec by voiceManager.recordingDurationSeconds.collectAsState()
    val recordAmplitude by voiceManager.recordingAmplitude.collectAsState()

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendImage(uri.toString())
        }
    }

    // Audio record permission launcher
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            voiceManager.startRecording()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Rich Text Formatting Bar
            AnimatedVisibility(
                visible = showFormatToolbar && !isRecording,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FormatToolbar(
                    onApplyBold = {
                        textFieldValue = RichTextHelper.applyFormat(textFieldValue, "*", "*")
                    },
                    onApplyItalic = {
                        textFieldValue = RichTextHelper.applyFormat(textFieldValue, "_", "_")
                    },
                    onApplyUnderline = {
                        textFieldValue = RichTextHelper.applyFormat(textFieldValue, "<u>", "</u>")
                    },
                    onInsertEmoji = { emoji ->
                        val text = textFieldValue.text
                        val sel = textFieldValue.selection
                        val newText = text.substring(0, sel.start) + emoji + text.substring(sel.end)
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(sel.start + emoji.length)
                        )
                    },
                    onCloseToolbar = { showFormatToolbar = false }
                )
            }

            // Main Bar: Recording view OR Input view
            if (isRecording) {
                // Recording Active Bar
                RecordingView(
                    durationSec = recordDurationSec,
                    amplitude = recordAmplitude,
                    onCancel = { voiceManager.cancelRecording() },
                    onSend = {
                        val result = voiceManager.stopRecording()
                        if (result != null) {
                            onSendVoiceNote(result.fileUri, result.durationSec)
                        }
                    }
                )
            } else {
                // Standard Chat Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image attachment button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Share Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Rich formatting toggle
                    IconButton(
                        onClick = { showFormatToolbar = !showFormatToolbar },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Rich text formatting",
                            tint = if (showFormatToolbar) NeonPink else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Text Input field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Message or *bold*, _italic_...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_input_text_field")
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (textFieldValue.text.isNotBlank()) {
                        // Send text button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrismGradients.PrimaryBrush)
                                .clickable {
                                    val textToSend = textFieldValue.text
                                    textFieldValue = TextFieldValue("")
                                    onSendMessage(textToSend)
                                }
                                .testTag("send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Voice note record button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    if (hasAudioPermission) {
                                        voiceManager.startRecording()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                                .testTag("mic_record_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormatToolbar(
    onApplyBold: () -> Unit,
    onApplyItalic: () -> Unit,
    onApplyUnderline: () -> Unit,
    onInsertEmoji: (String) -> Unit,
    onCloseToolbar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Bold
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onApplyBold),
                    contentAlignment = Alignment.Center
                ) {
                    Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                // Italic
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onApplyItalic),
                    contentAlignment = Alignment.Center
                ) {
                    Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                // Underline
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onApplyUnderline),
                    contentAlignment = Alignment.Center
                ) {
                    Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Quick emojis
                listOf("✨", "🚀", "💡", "🔥", "👍").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onInsertEmoji(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 16.sp)
                    }
                }
            }

            IconButton(onClick = onCloseToolbar, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close formatting", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RecordingView(
    durationSec: Int,
    amplitude: Float,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val durationFormatted = remember(durationSec) {
        val m = durationSec / 60
        val s = durationSec % 60
        String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NeonPink.copy(alpha = 0.12f))
            .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red recording indicator dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(NeonPink)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Timer
        Text(
            text = durationFormatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NeonPink
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Live amplitude waveform
        Row(
            modifier = Modifier
                .weight(1f)
                .height(26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (0..14).forEach { i ->
                val barHeight = ((amplitude * 24f) * (0.4f + (i % 5) * 0.15f)).coerceIn(4f, 24f)
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(NeonPink)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Cancel button
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Cancel Recording",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Send Voice Note button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrismGradients.PrimaryBrush)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Voice Note",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
