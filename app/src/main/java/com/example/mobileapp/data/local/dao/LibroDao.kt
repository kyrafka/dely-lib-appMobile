package com.example.mobileapp.data.local.dao

import androidx.room.*
import com.example.mobileapp.data.local.entity.LibroEntity

@Dao
interface LibroDao {
    @Query("SELECT * FROM libros")
    suspend fun getAll(): List<LibroEntity>

    @Query("SELECT * FROM libros WHERE idLibro = :id")
    suspend fun getById(id: Long): LibroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(libros: List<LibroEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(libro: LibroEntity)

    @Query("DELETE FROM libros WHERE idLibro = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM libros")
    suspend fun deleteAll()
}
