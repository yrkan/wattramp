package io.github.wattramp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.ui.theme.*

/**
 * Guide tour step definition
 */
data class GuideStep(
    val screen: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val highlightColor: Color
)

/**
 * All guide tour steps
 */
@Composable
fun getGuideTourSteps(): List<GuideStep> = listOf(
    GuideStep(
        screen = "home",
        titleRes = R.string.guide_home_title,
        descriptionRes = R.string.guide_home_desc,
        icon = Icons.Default.Home,
        highlightColor = Primary
    ),
    GuideStep(
        screen = "settings",
        titleRes = R.string.guide_settings_title,
        descriptionRes = R.string.guide_settings_desc,
        icon = Icons.Default.Settings,
        highlightColor = Zone2
    ),
    GuideStep(
        screen = "history",
        titleRes = R.string.guide_history_title,
        descriptionRes = R.string.guide_history_desc,
        icon = Icons.Default.History,
        highlightColor = Zone3
    ),
    GuideStep(
        screen = "zones",
        titleRes = R.string.guide_zones_title,
        descriptionRes = R.string.guide_zones_desc,
        icon = Icons.Default.BarChart,
        highlightColor = Zone5
    ),
    GuideStep(
        screen = "running",
        titleRes = R.string.guide_running_title,
        descriptionRes = R.string.guide_running_desc,
        icon = Icons.Default.DirectionsBike,
        highlightColor = Zone4
    ),
    GuideStep(
        screen = "result",
        titleRes = R.string.guide_result_title,
        descriptionRes = R.string.guide_result_desc,
        icon = Icons.Default.EmojiEvents,
        highlightColor = Success
    )
)

const val GUIDE_TOUR_TOTAL_STEPS = 6

/**
 * Compact guide tour overlay optimized for small screens like Karoo 3
 */
@Composable
fun GuideOverlay(
    currentStep: Int,
    totalSteps: Int,
    step: GuideStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for card entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentStep) {
        isVisible = false
        kotlinx.coroutines.delay(50)
        isVisible = true
    }

    // Pulsing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onNext
            )
    ) {
        // Minimal gradient overlay - almost fully transparent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.25f)
                        )
                    )
                )
        )

        // Compact bottom card
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(250, easing = EaseOutCubic)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Surface.copy(alpha = 0.80f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Top row: Icon + Title + Step counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Animated icon with glow
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            // Glow
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .alpha(glowAlpha * 0.5f)
                                    .background(
                                        step.highlightColor.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            )
                            // Icon circle
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        step.highlightColor.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .border(
                                        1.5.dp,
                                        step.highlightColor.copy(alpha = 0.6f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    tint = step.highlightColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title
                        Text(
                            text = stringResource(step.titleRes),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            modifier = Modifier.weight(1f)
                        )

                        // Step counter pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = step.highlightColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${currentStep + 1}/$totalSteps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = step.highlightColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = stringResource(step.descriptionRes),
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Start,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(totalSteps) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(
                                        when {
                                            index <= currentStep -> step.highlightColor
                                            else -> OnSurfaceVariant.copy(alpha = 0.2f)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.guide_skip),
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Tap hint
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = OnSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = stringResource(R.string.guide_tap_hint),
                                fontSize = 10.sp,
                                color = OnSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Next button
                        Button(
                            onClick = onNext,
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = step.highlightColor,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    if (currentStep == totalSteps - 1) R.string.guide_finish
                                    else R.string.guide_next
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (currentStep == totalSteps - 1) Icons.Default.Check
                                else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
