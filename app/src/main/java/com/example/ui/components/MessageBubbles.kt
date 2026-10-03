package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MessageEntity
import com.example.ui.theme.NeonPink
import com.example.ui.theme.PrismGradients
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TextMessageBubble(
    message: MessageEntity,
    onAvatarClick: () -> Unit
) {
    val isMe = message.isSentByMe
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            GradientAvatar(
                name = message.senderName,
                avatarUri = message.senderAvatarUri,
                gradientIndex = message.senderGradientIndex,
                size = 32.dp,
                onClick = onAvatarClick
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            if (!isMe) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            val bubbleShape = if (isMe) {
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
            }
            val bgModifier = if (isMe) {
                Modifier
                    .background(PrismGradients.SentMessageBrush, bubbleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), bubbleShape)
            } else {
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, bubbleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, bubbleShape)
            }

            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .then(bgModifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = RichTextHelper.parseRichText(message.text),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            // Message metadata (time + delivery status)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = if (message.status == "READ") Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = message.status,
                        tint = if (message.status == "READ") NeonPink else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ImageMessageBubble(
    message: MessageEntity,
    onImageClick: (String) -> Unit,
    onAvatarClick: () -> Unit
) {
    val isMe = message.isSentByMe
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            GradientAvatar(
                name = message.senderName,
                avatarUri = message.senderAvatarUri,
                gradientIndex = message.senderGradientIndex,
                size = 32.dp,
                onClick = onAvatarClick
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            val cardBorder = if (isMe) {
                Modifier.border(1.5.dp, PrismGradients.PrimaryBrush, RoundedCornerShape(18.dp))
            } else {
                Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .then(cardBorder)
                    .clickable { message.mediaUri?.let { onImageClick(it) } }
                    .testTag("image_message_card")
            ) {
                Column {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Shared image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    )

                    if (message.text.isNotBlank()) {
                        Text(
                            text = RichTextHelper.parseRichText(message.text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Delivered",
                        tint = NeonPink,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioMessageBubble(
    message: MessageEntity,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val isMe = message.isSentByMe
    val durationText = remember(message.audioDurationSec) {
        val total = if (message.audioDurationSec > 0) message.audioDurationSec else 6
        val mins = total / 60
        val secs = total % 60
        String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            GradientAvatar(
                name = message.senderName,
                avatarUri = message.senderAvatarUri,
                gradientIndex = message.senderGradientIndex,
                size = 32.dp,
                onClick = onAvatarClick
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            val audioShape = RoundedCornerShape(18.dp)
            val audioBgModifier = if (isMe) {
                Modifier
                    .background(PrismGradients.SentAudioBrush, audioShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), audioShape)
            } else {
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, audioShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, audioShape)
            }

            Box(
                modifier = Modifier
                    .clip(audioShape)
                    .then(audioBgModifier)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("audio_message_bubble")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isMe) Color.White else MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isMe) NeonPink else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Soundwave waveform visualizer
                        WaveformVisualizer(isPlaying = isPlaying, isMe = isMe)

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress line
                        LinearProgressIndicator(
                            progress = { if (isPlaying) progress else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.primary,
                            trackColor = if (isMe) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Voice Note",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Delivered",
                        tint = NeonPink,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 18
    val waveHeights = remember { listOf(6, 14, 18, 10, 22, 16, 26, 12, 20, 8, 24, 18, 12, 20, 16, 10, 14, 8) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0 until barCount).forEach { index ->
            val heightBase = waveHeights[index % waveHeights.size]
            val animatedHeight = remember { Animatable(heightBase.toFloat()) }

            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    animatedHeight.animateTo(
                        targetValue = ((index * 7) % 20 + 8).toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 300 + (index * 30), easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                } else {
                    animatedHeight.snapTo(heightBase.toFloat())
                }
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight.value.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isMe) Color.White.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
            )
        }
    }
}
