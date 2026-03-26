package ai.teammates.rayachat.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ai.teammates.rayachat.core.Constants
import ai.teammates.rayachat.core.models.TypeMessage

@Database(entities = [TypeMessage::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    Constants.DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /** For testing — creates an in-memory database. */
        fun createInMemory(context: Context): ChatDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
