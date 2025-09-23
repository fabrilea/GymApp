
package com.example.gymcheckin.data.dao

import androidx.room.*
import com.example.gymcheckin.data.entity.ClienteEntity

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes WHERE dni = :dni LIMIT 1")
    suspend fun findByDni(dni: String): ClienteEntity?

    @Query("SELECT * FROM clientes")
    suspend fun getAll(): List<ClienteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vararg c: ClienteEntity)

    @Query("DELETE FROM clientes") suspend fun clearAll()
}