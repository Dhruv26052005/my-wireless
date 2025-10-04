package com.hybridmesh.chat.network.routing

import com.hybridmesh.chat.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.math.abs

class RoutingEngine {
    
    private val _networkTopology = MutableStateFlow(NetworkTopology(emptyMap(), emptyList()))
    val networkTopology: StateFlow<NetworkTopology> = _networkTopology.asStateFlow()
    
    private val routingTable = mutableMapOf<String, Map<String, Route>>()
    
    fun updateTopology(devices: Map<String, Device>) {
        val connections = buildConnections(devices)
        val topology = NetworkTopology(devices, connections)
        _networkTopology.value = topology
        
        // Recalculate routing table
        recalculateRoutingTable(devices, connections)
    }
    
    fun findBestRoute(from: String, to: String): Route? {
        return routingTable[from]?.get(to)
    }
    
    fun findAlternativeRoutes(from: String, to: String, maxRoutes: Int = 3): List<Route> {
        val routes = mutableListOf<Route>()
        val bestRoute = findBestRoute(from, to)
        
        if (bestRoute != null) {
            routes.add(bestRoute)
        }
        
        // Find alternative paths using different transport types
        val alternativeRoutes = findAlternativePaths(from, to, bestRoute?.path?.map { it.deviceId } ?: emptyList())
        routes.addAll(alternativeRoutes.take(maxRoutes - 1))
        
        return routes.sortedBy { it.totalCost }
    }
    
    private fun buildConnections(devices: Map<String, Device>): List<Connection> {
        val connections = mutableListOf<Connection>()
        
        devices.values.forEach { device ->
            devices.values.forEach { otherDevice ->
                if (device.id != otherDevice.id && canConnect(device, otherDevice)) {
                    val transportType = selectBestTransport(device, otherDevice)
                    val signalStrength = calculateSignalStrength(device, otherDevice, transportType)
                    val cost = calculateConnectionCost(device, otherDevice, transportType, signalStrength)
                    
                    connections.add(
                        Connection(
                            from = device.id,
                            to = otherDevice.id,
                            transportType = transportType,
                            signalStrength = signalStrength,
                            isActive = true
                        )
                    )
                }
            }
        }
        
        return connections
    }
    
    private fun canConnect(device1: Device, device2: Device): Boolean {
        val commonTransports = device1.transportTypes.intersect(device2.transportTypes.toSet())
        return commonTransports.isNotEmpty()
    }
    
    private fun selectBestTransport(device1: Device, device2: Device): TransportType {
        val commonTransports = device1.transportTypes.intersect(device2.transportTypes.toSet())
        
        // Prioritize based on signal strength and transport characteristics
        return when {
            commonTransports.contains(TransportType.BLUETOOTH) && 
            device1.signalStrength > -60 && device2.signalStrength > -60 -> TransportType.BLUETOOTH
            
            commonTransports.contains(TransportType.WIFI_DIRECT) && 
            device1.signalStrength > -70 && device2.signalStrength > -70 -> TransportType.WIFI_DIRECT
            
            commonTransports.contains(TransportType.HOTSPOT) -> TransportType.HOTSPOT
            
            else -> commonTransports.firstOrNull() ?: TransportType.BLUETOOTH
        }
    }
    
    private fun calculateSignalStrength(device1: Device, device2: Device, transportType: TransportType): Int {
        return when (transportType) {
            TransportType.BLUETOOTH -> minOf(device1.signalStrength, device2.signalStrength)
            TransportType.WIFI_DIRECT -> (device1.signalStrength + device2.signalStrength) / 2
            TransportType.HOTSPOT -> maxOf(device1.signalStrength, device2.signalStrength)
            else -> -70
        }
    }
    
    private fun calculateConnectionCost(device1: Device, device2: Device, transportType: TransportType, signalStrength: Int): Int {
        val baseCost = when (transportType) {
            TransportType.BLUETOOTH -> 10
            TransportType.WIFI_DIRECT -> 5
            TransportType.HOTSPOT -> 3
            TransportType.INTERNET -> 1
        }
        
        val signalCost = when {
            signalStrength > -50 -> 0
            signalStrength > -60 -> 2
            signalStrength > -70 -> 5
            signalStrength > -80 -> 10
            else -> 20
        }
        
        val batteryCost = if (device1.isOnline && device2.isOnline) 0 else 5
        
        return baseCost + signalCost + batteryCost
    }
    
    private fun recalculateRoutingTable(devices: Map<String, Device>, connections: List<Connection>) {
        routingTable.clear()
        
        devices.keys.forEach { source ->
            val routes = dijkstraShortestPath(source, devices, connections)
            routingTable[source] = routes
        }
    }
    
    private fun dijkstraShortestPath(source: String, devices: Map<String, Device>, connections: List<Connection>): Map<String, Route> {
        val distances = mutableMapOf<String, Int>()
        val previous = mutableMapOf<String, String>()
        val visited = mutableSetOf<String>()
        val queue = PriorityQueue<Pair<String, Int>>(compareBy { it.second })
        
        // Initialize distances
        devices.keys.forEach { deviceId ->
            distances[deviceId] = if (deviceId == source) 0 else Int.MAX_VALUE
        }
        
        queue.add(Pair(source, 0))
        
        while (queue.isNotEmpty()) {
            val (current, currentDistance) = queue.poll()
            
            if (visited.contains(current)) continue
            visited.add(current)
            
            val outgoingConnections = connections.filter { it.from == current && it.isActive }
            
            outgoingConnections.forEach { connection ->
                val neighbor = connection.to
                val edgeCost = connection.signalStrength + 100 // Convert signal to cost
                val newDistance = currentDistance + edgeCost
                
                if (newDistance < (distances[neighbor] ?: Int.MAX_VALUE)) {
                    distances[neighbor] = newDistance
                    previous[neighbor] = current
                    queue.add(Pair(neighbor, newDistance))
                }
            }
        }
        
        // Build routes
        val routes = mutableMapOf<String, Route>()
        devices.keys.forEach { destination ->
            if (destination != source && distances[destination] != Int.MAX_VALUE) {
                val path = buildPath(source, destination, previous, connections)
                if (path.isNotEmpty()) {
                    routes[destination] = Route(
                        destination = destination,
                        path = path,
                        totalCost = distances[destination] ?: 0,
                        estimatedDeliveryTime = calculateEstimatedDeliveryTime(path),
                        reliability = calculateRouteReliability(path)
                    )
                }
            }
        }
        
        return routes
    }
    
    private fun buildPath(source: String, destination: String, previous: Map<String, String>, connections: List<Connection>): List<RouteHop> {
        val path = mutableListOf<String>()
        var current = destination
        
        while (current != source && current in previous) {
            path.add(current)
            current = previous[current] ?: break
        }
        
        if (current == source) {
            path.add(source)
            path.reverse()
            
            return path.zipWithNext { from, to ->
                val connection = connections.find { it.from == from && it.to == to }
                RouteHop(
                    deviceId = to,
                    transportType = connection?.transportType ?: TransportType.BLUETOOTH,
                    cost = connection?.signalStrength ?: -70,
                    estimatedLatency = calculateLatency(connection?.transportType ?: TransportType.BLUETOOTH)
                )
            }
        }
        
        return emptyList()
    }
    
    private fun calculateLatency(transportType: TransportType): Long {
        return when (transportType) {
            TransportType.BLUETOOTH -> 100 // 100ms
            TransportType.WIFI_DIRECT -> 50 // 50ms
            TransportType.HOTSPOT -> 30 // 30ms
            TransportType.INTERNET -> 200 // 200ms
        }
    }
    
    private fun calculateEstimatedDeliveryTime(path: List<RouteHop>): Long {
        return path.sumOf { it.estimatedLatency }
    }
    
    private fun calculateRouteReliability(path: List<RouteHop>): Float {
        if (path.isEmpty()) return 0.0f
        
        val avgSignalStrength = path.map { it.cost }.average()
        val transportReliability = path.map { hop ->
            when (hop.transportType) {
                TransportType.BLUETOOTH -> 0.8f
                TransportType.WIFI_DIRECT -> 0.9f
                TransportType.HOTSPOT -> 0.95f
                TransportType.INTERNET -> 0.7f
            }
        }.average().toFloat()
        
        val signalReliability = when {
            avgSignalStrength > -50 -> 1.0f
            avgSignalStrength > -60 -> 0.9f
            avgSignalStrength > -70 -> 0.7f
            avgSignalStrength > -80 -> 0.5f
            else -> 0.3f
        }
        
        return (transportReliability * signalReliability).coerceIn(0.0f, 1.0f)
    }
    
    private fun findAlternativePaths(from: String, to: String, excludeDevices: List<String>): List<Route> {
        // Simple alternative path finding by excluding some devices from the main path
        val alternativeRoutes = mutableListOf<Route>()
        val topology = _networkTopology.value
        
        // Try to find paths that avoid the main route
        val filteredConnections = topology.connections.filter { connection ->
            !excludeDevices.contains(connection.from) && !excludeDevices.contains(connection.to)
        }
        
        // Recalculate with filtered connections
        val alternativeRoutesMap = dijkstraShortestPath(from, topology.devices, filteredConnections)
        alternativeRoutesMap[to]?.let { route ->
            alternativeRoutes.add(route)
        }
        
        return alternativeRoutes
    }
    
    fun getRoutingTable(): Map<String, Map<String, Route>> {
        return routingTable.toMap()
    }
    
    fun getNextHop(from: String, to: String): String? {
        return findBestRoute(from, to)?.path?.firstOrNull()?.deviceId
    }
}
