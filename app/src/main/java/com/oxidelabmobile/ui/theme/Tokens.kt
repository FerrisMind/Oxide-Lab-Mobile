package com.oxidelabmobile.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.unit.dp

// Spacing tokens (8dp grid system)
object Spacing {
    val Micro = 4.dp      // Для тесно связанных элементов
    val Small = 8.dp      // Стандартные отступы
    val Medium = 16.dp    // Основные отступы между блоками
    val Large = 24.dp     // Для разделения крупных секций
    val ExtraLarge = 32.dp // Для создания "дыхания" в интерфейсе
    val Huge = 48.dp      // Для больших разделителей
}

// Motion tokens
object Motion {
    val Quick = 100       // Мгновенная обратная связь
    val Standard = 250     // Переходы между состояниями
    val Emphasized = 400  // Важные изменения интерфейса
    
    // Easing curves
    val StandardEasing: Easing = FastOutSlowInEasing
    val EmphasizedEasing: Easing = FastOutSlowInEasing
    val DeceleratedEasing: Easing = FastOutSlowInEasing
    val LinearEasingCurve: Easing = LinearEasing
}

// Component dimensions
object Dimensions {
    // Touch targets (minimum 48dp for accessibility)
    val TouchTargetMin = 48.dp
    
    // Button heights
    val ButtonHeight = 56.dp
    val FABSize = 56.dp
    val ExtendedFABHeight = 56.dp
    
    // Input field
    val InputMinHeight = 56.dp
    val InputMaxHeight = 120.dp
    
    // Header
    val HeaderHeight = 48.dp
    
    // Progress bar
    val ProgressBarHeight = 4.dp
    
    // Card radius
    val CardRadius = 16.dp
    
    // Logo size
    val LogoSize = 128.dp
}

// Status indicators
object Status {
    val DotSize = 8.dp
    val StatusIconSize = 24.dp
}
