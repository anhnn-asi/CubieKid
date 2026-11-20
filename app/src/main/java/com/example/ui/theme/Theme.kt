package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CubieLightColorScheme = lightColorScheme(
    primary = RubikBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlueSurface,
    onPrimaryContainer = RubikBlueDark,
    secondary = ToyOrange,
    onSecondary = Color.White,
    secondaryContainer = SoftAmberSurface,
    onSecondaryContainer = ToyOrangeDark,
    tertiary = ToyGreen,
    onTertiary = Color.White,
    tertiaryContainer = SoftGreenSurface,
    onTertiaryContainer = ToyGreenDark,
    background = CreamBackground,
    onBackground = InkPrimary,
    surface = WarmSurface,
    onSurface = InkPrimary,
    surfaceVariant = SoftBlueSurface,
    onSurfaceVariant = InkSecondary,
    outline = OutlineSoft,
    error = ToyRed,
    onError = Color.White,
    errorContainer = SoftCoralSurface,
    onErrorContainer = ToyRed
)

val CubieShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun CubieKidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CubieLightColorScheme,
        typography = Typography,
        shapes = CubieShapes,
        content = content
    )
}
