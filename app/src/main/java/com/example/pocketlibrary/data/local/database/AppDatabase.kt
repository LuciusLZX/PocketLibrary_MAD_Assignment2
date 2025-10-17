package com.example.pocketlibrary.data.local.database
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pocketlibrary.data.local.dao.BookDao
import com.example.pocketlibrary.data.local.entity.BookEntity


// Main Room database for Pocket Library.
// Uses a singleton so the app only creates one database instance.
@Database( entities = [BookEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder( //  building a Room database
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_library_database" //  the on-device file name of the database.
                )
                    .build()

                INSTANCE = instance //  stores the created instance so future calls return the same one.
                instance
            }
        }
    }
}

