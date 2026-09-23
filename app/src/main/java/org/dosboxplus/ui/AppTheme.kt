package org.dosboxplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppColors = darkColorScheme(
    primary = Color(0xff80e0ce), onPrimary = Color(0xff00382f),
    primaryContainer = Color(0xff164c43), onPrimaryContainer = Color(0xffbcf4e7),
    secondary = Color(0xffa9c7fa), onSecondary = Color(0xff123059),
    secondaryContainer = Color(0xff263c5a), onSecondaryContainer = Color(0xffd7e6ff),
    tertiary = Color(0xfff4ca8b), onTertiary = Color(0xff422d09),
    background = Color(0xff10151e), onBackground = Color(0xffedf1f7),
    surface = Color(0xff151c27), onSurface = Color(0xffedf1f7),
    surfaceVariant = Color(0xff273241), onSurfaceVariant = Color(0xffbdc9d8),
    outline = Color(0xff8494a6), outlineVariant = Color(0xff39485b),
    error = Color(0xffffb4ab), onError = Color(0xff690005)
)

@Composable fun DosboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        typography = Typography(
            headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
            headlineMedium = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
            titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        ), content = content
    )
}

@Composable fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable fun GuideStep(number: String, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(number, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun WelcomeArtwork(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().background(
        Brush.linearGradient(listOf(Color(0xff193d45), Color(0xff233454))), RoundedCornerShape(24.dp)
    ).padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(color = Color(0xff0d1721), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xff617b91))) {
            Column(Modifier.widthIn(max = 300.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("DOS / PLAY", color = Color(0xffa9c7fa), style = MaterialTheme.typography.labelMedium, letterSpacing = 3.sp)
                Text("C:\\> Sei pronto?", color = Color(0xff80e0ce), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                Text("I tuoi classici, un tocco alla volta.", color = Color(0xffd7e6ff), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
