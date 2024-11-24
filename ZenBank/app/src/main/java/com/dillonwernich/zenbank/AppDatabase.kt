//Dillon Wernich	ST10061721	OPSC7312
package com.dillonwernich.zenbank

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Database class for Room, specifying entities and version
@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Abstract method to get the UserDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null // Singleton instance of the database

        // Method to get the database instance
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Build the database instance if it's not already created
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zenbank_database" // Database name
                ).build()
                INSTANCE = instance // Save the instance
                instance // Return the created instance
            }
        }
    }
}
