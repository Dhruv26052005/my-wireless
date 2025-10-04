package com.hybridmesh.chat.data.database

import androidx.room.*
import com.hybridmesh.chat.data.model.Message
import com.hybridmesh.chat.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE receiverId = :receiverId ORDER BY timestamp DESC")
    fun getMessagesForReceiver(receiverId: String): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE senderId = :senderId ORDER BY timestamp DESC")
    fun getMessagesFromSender(senderId: String): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE status = :status ORDER BY timestamp ASC")
    fun getMessagesByStatus(status: MessageStatus): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE status IN (:statuses) ORDER BY timestamp ASC")
    fun getMessagesByStatuses(statuses: List<MessageStatus>): Flow<List<Message>>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): Message?
    
    @Query("SELECT * FROM messages WHERE hopCount < maxHops AND status = :status ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingMessages(status: MessageStatus, limit: Int = 10): List<Message>
    
    @Query("SELECT * FROM messages WHERE receiverId = :receiverId AND status = :status")
    suspend fun getPendingMessagesForReceiver(receiverId: String, status: MessageStatus): List<Message>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)
    
    @Update
    suspend fun updateMessage(message: Message)
    
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    
    @Query("UPDATE messages SET hopCount = :hopCount, route = :route WHERE id = :messageId")
    suspend fun updateMessageHop(messageId: String, hopCount: Int, route: String)
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    @Query("DELETE FROM messages WHERE status = :status AND timestamp < :cutoffTime")
    suspend fun deleteOldMessages(status: MessageStatus, cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM messages WHERE status = :status")
    suspend fun getMessageCountByStatus(status: MessageStatus): Int
    
    @Query("SELECT * FROM messages WHERE timestamp < :cutoffTime AND status IN (:statuses)")
    suspend fun getExpiredMessages(cutoffTime: Long, statuses: List<MessageStatus>): List<Message>
}
