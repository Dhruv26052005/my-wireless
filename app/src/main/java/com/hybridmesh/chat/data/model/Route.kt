package com.hybridmesh.chat.data.model

data class Route(
    val destination: String,
    val path: List<RouteHop>,
    val totalCost: Int,
    val estimatedDeliveryTime: Long,
    val reliability: Float // 0.0 to 1.0
)

data class RouteHop(
    val deviceId: String,
    val transportType: TransportType,
    val cost: Int, // Based on signal strength, battery, etc.
    val estimatedLatency: Long
)

data class NetworkTopology(
    val devices: Map<String, Device>,
    val connections: List<Connection>,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class Connection(
    val from: String,
    val to: String,
    val transportType: TransportType,
    val signalStrength: Int,
    val isActive: Boolean,
    val lastPing: Long = System.currentTimeMillis()
)
