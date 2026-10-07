package com.example.skilldiscovery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import com.example.skilldiscovery.R

private val ManropeFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val Manrope = GoogleFont("Manrope")

private val ManropeFontFamily = FontFamily(
    Font(googleFont = Manrope, fontProvider = ManropeFontProvider),
    Font(googleFont = Manrope, fontProvider = ManropeFontProvider, weight = FontWeight.Medium),
    Font(googleFont = Manrope, fontProvider = ManropeFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = Manrope, fontProvider = ManropeFontProvider, weight = FontWeight.Bold)
)

private fun manropeStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight
): TextStyle = TextStyle(
    fontFamily = ManropeFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

val Typography = Typography(
    displayLarge = manropeStyle(57, 64, FontWeight.SemiBold),
    displayMedium = manropeStyle(45, 52, FontWeight.SemiBold),
    displaySmall = manropeStyle(36, 44, FontWeight.SemiBold),
    headlineLarge = manropeStyle(32, 40, FontWeight.SemiBold),
    headlineMedium = manropeStyle(28, 36, FontWeight.SemiBold),
    headlineSmall = manropeStyle(24, 32, FontWeight.SemiBold),
    titleLarge = manropeStyle(22, 28, FontWeight.SemiBold),
    titleMedium = manropeStyle(16, 24, FontWeight.Medium),
    titleSmall = manropeStyle(14, 20, FontWeight.Medium),
    bodyLarge = manropeStyle(16, 24, FontWeight.Normal),
    bodyMedium = manropeStyle(14, 20, FontWeight.Normal),
    bodySmall = manropeStyle(12, 16, FontWeight.Normal),
    labelLarge = manropeStyle(14, 20, FontWeight.Medium),
    labelMedium = manropeStyle(12, 16, FontWeight.Medium),
    labelSmall = manropeStyle(11, 16, FontWeight.Medium)
)
