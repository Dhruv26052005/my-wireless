package com.hybridmesh.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hybridmesh.chat.data.model.Message
import com.hybridmesh.chat.data.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Message bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                            bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isFromCurrentUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Message metadata
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Status indicator
                MessageStatusIndicator(status = message.status)
                
                // Hop count indicator
                if (message.hopCount > 0) {
                    Text(
                        text = "Hops: ${message.hopCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageStatusIndicator(status: MessageStatus) {
    val (color, text) = when (status) {
        MessageStatus.PENDING -> Color.Gray to "⏳"
        MessageStatus.SENT -> Color.Blue to "✓"
        MessageStatus.DELIVERED -> Color.Green to "✓✓"
        MessageStatus.FAILED -> Color.Red to "✗"
        MessageStatus.EXPIRED -> Color.Orange to "⏰"
    }
    
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp
    
    return when {
        diff < 60000 -> "Just now" // Less than 1 minute
        diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
        diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
    }
}
