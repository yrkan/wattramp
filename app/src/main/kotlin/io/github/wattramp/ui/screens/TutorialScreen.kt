package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.ui.theme.*

/**
 * Tutorial/Help screen - simple scrollable page with app usage instructions.
 * Optimized for Karoo 2 (240x400) and Karoo 3 (320x480).
 */
@Composable
fun TutorialScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(horizontal = 4.dp, vertical = if (isCompact) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = stringResource(R.string.tutorial_title),
                fontSize = if (isCompact) 14.sp else 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = 2.sp
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // How to use section
            SectionHeader(stringResource(R.string.tutorial_how_to_use))
            TutorialStep("1", stringResource(R.string.tutorial_step1))
            TutorialStep("2", stringResource(R.string.tutorial_step2))
            TutorialStep("3", stringResource(R.string.tutorial_step3))
            TutorialStep("4", stringResource(R.string.tutorial_step4))

            Spacer(modifier = Modifier.height(4.dp))

            // Tests section
            SectionHeader(stringResource(R.string.tutorial_tests))
            TestInfo(
                name = stringResource(R.string.protocol_ramp),
                description = stringResource(R.string.tutorial_ramp_desc),
                color = Primary
            )
            TestInfo(
                name = stringResource(R.string.protocol_20min),
                description = stringResource(R.string.tutorial_20min_desc),
                color = Zone3
            )
            TestInfo(
                name = stringResource(R.string.protocol_8min),
                description = stringResource(R.string.tutorial_8min_desc),
                color = Zone2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Tips section
            SectionHeader(stringResource(R.string.tutorial_tips))
            TipItem(stringResource(R.string.tutorial_tip1))
            TipItem(stringResource(R.string.tutorial_tip2))
            TipItem(stringResource(R.string.tutorial_tip3))

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = Primary,
        letterSpacing = 1.sp
    )
}

@Composable
private fun TutorialStep(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurface,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun TestInfo(name: String, description: String, color: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .background(color)
        )
        Column {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = OnSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun TipItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = OnSurface,
            lineHeight = 14.sp
        )
    }
}
