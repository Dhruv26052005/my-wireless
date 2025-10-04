package com.hybridmesh.chat.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hybridmesh.chat.data.model.DeviceCapability
import com.hybridmesh.chat.data.model.TransportType

class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromTransportTypeList(value: List<TransportType>): String {
        return value.joinToString(",") { it.name }
    }
    
    @TypeConverter
    fun toTransportTypeList(value: String): List<TransportType> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { TransportType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromDeviceCapabilityList(value: List<DeviceCapability>): String {
        return value.joinToString(",") { it.name }
    }
    
    @TypeConverter
    fun toDeviceCapabilityList(value: String): List<DeviceCapability> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { DeviceCapability.valueOf(it) }
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
