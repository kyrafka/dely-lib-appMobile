package com.example.mobileapp.data.local.dao

import androidx.room.*
import com.example.mobileapp.data.local.entity.InventarioEntity

@Dao
interface InventarioDao {
    @Query("SELECT * FROM inventario")
    suspend fun getAll(): List<InventarioEntity>

    @Query("SELECT * FROM inventario WHERE idInventario = :id")
    suspend fun getById(id: Long): InventarioEntity?

    @Query("SELECT * FROM inventario WHERE idLibro = :libroId")
    suspend fun getByLibroId(libroId: Long): InventarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inventario: List<InventarioEntity>)

    @Query("DELETE FROM inventario")
    suspend fun deleteAll()
}
