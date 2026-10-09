package dev.andikuneiocontroll.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TvEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TvDatabase : RoomDatabase() {

    abstract fun tvDao(): TvDao

    companion object {
        private const val DB_NAME = "iocontroll_tv.db"

        @Volatile
        private var INSTANCE: TvDatabase? = null

        fun getInstance(context: Context): TvDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TvDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
