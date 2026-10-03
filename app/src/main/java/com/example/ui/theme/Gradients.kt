package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object PrismGradients {
    // Primary Vibrant Prism
    val PrimaryPrism = listOf(NeonPink, NeonPurple, ElectricBlue)
    val PrimaryBrush = Brush.linearGradient(
        colors = PrimaryPrism,
        start = Offset.Zero,
        end = Offset(400f, 400f)
    )

    // Cyber Cyan
    val CyberCyan = listOf(BrightCyan, SkyBlue, ElectricBlue)
    val CyberCyanBrush = Brush.linearGradient(
        colors = CyberCyan,
        start = Offset.Zero,
        end = Offset(400f, 400f)
    )

    // Sunset Flare
    val SunsetFlare = listOf(SunsetOrange, WarmAmber, Color(0xFFFF416C))
    val SunsetBrush = Brush.linearGradient(
        colors = SunsetFlare,
        start = Offset.Zero,
        end = Offset(400f, 400f)
    )

    // Emerald Aurora
    val Aurora = listOf(EmeraldGreen, MintCyan, SkyBlue)
    val AuroraBrush = Brush.linearGradient(
        colors = Aurora,
        start = Offset.Zero,
        end = Offset(400f, 400f)
    )

    // Deep Cosmic Purple (Dark Header / Glass)
    val CosmicDark = listOf(
        Color(0xFF2E1065),
        Color(0xFF1E1B4B),
        Color(0xFF0F172A)
    )
    val CosmicDarkBrush = Brush.linearGradient(
        colors = CosmicDark,
        start = Offset.Zero,
        end = Offset(600f, 600f)
    )

    // Sent message bubble gradient
    val SentMessageBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF8B5CF6),
            Color(0xFFEC4899),
            Color(0xFFF43F5E)
        )
    )

    // Sent audio bubble gradient
    val SentAudioBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF6366F1),
            Color(0xFF8B5CF6),
            Color(0xFFA855F7)
        )
    )

    // List of avatar gradient pairs
    val AvatarGradientPresets = listOf(
        listOf(Color(0xFFFF007A), Color(0xFF7928CA)),
        listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)),
        listOf(Color(0xFFFF5858), Color(0xFFF09819)),
        listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
        listOf(Color(0xFFF857A6), Color(0xFFFF5858)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFFFA709A), Color(0xFFFEE140))
    )

    fun getAvatarGradient(index: Int): List<Color> {
        val safeIndex = kotlin.math.abs(index) % AvatarGradientPresets.size
        return AvatarGradientPresets[safeIndex]
    }

    fun getAvatarBrush(index: Int): Brush {
        val colors = getAvatarGradient(index)
        return Brush.linearGradient(colors = colors)
    }
}
