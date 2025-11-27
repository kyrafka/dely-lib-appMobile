package com.example.mobileapp.data.local.dao

import androidx.room.*
import com.example.mobileapp.data.local.entity.GeneroEntity

@Dao
interface GeneroDao {
    @Query("SELECT * FROM generos")
    suspend fun getAll(): List<GeneroEntity>

    @Query("SELECT * FROM generos WHERE idGenero = :id")
    suspend fun getById(id: Long): GeneroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(generos: List<GeneroEntity>)

    @Query("DELETE FROM generos")
    suspend fun deleteAll()
}
