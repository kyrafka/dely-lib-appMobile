package com.example.mobileapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mobileapp.data.local.dao.GeneroDao
import com.example.mobileapp.data.local.dao.InventarioDao
import com.example.mobileapp.data.local.dao.LibroDao
import com.example.mobileapp.data.local.entity.GeneroEntity
import com.example.mobileapp.data.local.entity.InventarioEntity
import com.example.mobileapp.data.local.entity.LibroEntity

@Database(
    entities = [LibroEntity::class, GeneroEntity::class, InventarioEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libroDao(): LibroDao
    abstract fun generoDao(): GeneroDao
    abstract fun inventarioDao(): InventarioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dely_lib_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
