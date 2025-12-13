package io.github.wattramp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.ui.theme.*

@Composable
fun ProtocolCard(
    protocol: ProtocolType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, description, duration) = when (protocol) {
        ProtocolType.RAMP -> listOf(
            Icons.Default.TrendingUp,
            "Ramp Test",
            "Progressive power increase until failure",
            "~20 min"
        )
        ProtocolType.TWENTY_MINUTE -> listOf(
            Icons.Default.Timer,
            "20-Minute Test",
            "Classic FTP protocol with max effort",
            "~60 min"
        )
        ProtocolType.EIGHT_MINUTE -> listOf(
            Icons.Default.Replay,
            "8-Minute Test",
            "Two max efforts with recovery",
            "~50 min"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.2f) else Surface
        ),
        border = if (isSelected) BorderStroke(2.dp, Primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = null,
                tint = if (isSelected) Primary else OnSurface,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title as String,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Primary else OnSurface
                )
                Text(
                    text = description as String,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface.copy(alpha = 0.7f)
                )
            }

            Text(
                text = duration as String,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ProtocolInfoCard(
    protocol: ProtocolType,
    modifier: Modifier = Modifier
) {
    val info = when (protocol) {
        ProtocolType.RAMP -> listOf(
            "How it works:" to "Power increases by 20W every minute until you can't maintain the target.",
            "FTP Calculation:" to "Max 1-minute power x 0.75",
            "Best for:" to "Quick tests, beginners, tracking progress"
        )
        ProtocolType.TWENTY_MINUTE -> listOf(
            "How it works:" to "After warmup and blow-out, maintain maximum sustainable power for 20 minutes.",
            "FTP Calculation:" to "20-min average power x 0.95",
            "Best for:" to "Most accurate FTP, experienced cyclists"
        )
        ProtocolType.EIGHT_MINUTE -> listOf(
            "How it works:" to "Two 8-minute max efforts with 10 min recovery between.",
            "FTP Calculation:" to "Average of both efforts x 0.90",
            "Best for:" to "Alternative to 20-min, shorter max efforts"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            info.forEach { (label, value) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
