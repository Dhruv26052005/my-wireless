package com.hybridmesh.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hybridmesh.chat.data.model.Device
import com.hybridmesh.chat.data.model.TransportType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Device name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Online status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (device.isOnline) Color.Green else Color.Red,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // MAC Address
            Text(
                text = device.macAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transport types and signal strength
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transport type icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    device.transportTypes.forEach { transportType ->
                        Icon(
                            imageVector = when (transportType) {
                                TransportType.BLUETOOTH -> Icons.Default.Bluetooth
                                TransportType.WIFI_DIRECT -> Icons.Default.Wifi
                                else -> Icons.Default.Wifi
                            },
                            contentDescription = transportType.name,
                            modifier = Modifier.size(16.dp),
                            tint = when (transportType) {
                                TransportType.BLUETOOTH -> Color.Blue
                                TransportType.WIFI_DIRECT -> Color.Green
                                else -> Color.Gray
                            }
                        )
                    }
                }
                
                // Signal strength
                SignalStrengthIndicator(signalStrength = device.signalStrength)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Last seen
            Text(
                text = "Last seen: ${formatLastSeen(device.lastSeen)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignalStrengthIndicator(
    signalStrength: Int,
    modifier: Modifier = Modifier
) {
    val (color, text) = when {
        signalStrength > -50 -> Color.Green to "Excellent"
        signalStrength > -60 -> Color.Green to "Good"
        signalStrength > -70 -> Color.Orange to "Fair"
        signalStrength > -80 -> Color.Orange to "Weak"
        else -> Color.Red to "Poor"
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun formatLastSeen(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
