package ai.teammates.rayachat.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ai.teammates.rayachat.core.models.TypeMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY rowid ASC")
    fun observeAll(): Flow<List<TypeMessage>>

    @Query("SELECT * FROM messages ORDER BY rowid ASC")
    suspend fun getAll(): List<TypeMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: TypeMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<TypeMessage>)

    @Query(
        """
        DELETE FROM messages WHERE id NOT IN (
            SELECT id FROM messages ORDER BY rowid DESC LIMIT :limit
        )
        """
    )
    suspend fun trimToLatest(limit: Int = 500)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int

    @Query("SELECT * FROM messages ORDER BY rowid DESC LIMIT 1")
    suspend fun getLastMessage(): TypeMessage?
}
