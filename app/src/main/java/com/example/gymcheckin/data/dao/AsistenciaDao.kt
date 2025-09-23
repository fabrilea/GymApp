
package com.example.gymcheckin.data.dao

import androidx.room.*
import com.example.gymcheckin.data.entity.AsistenciaEntity

@Dao
interface AsistenciaDao {
    @Insert
    suspend fun insertAll(list: List<AsistenciaEntity>)

    @Insert
    suspend fun insert(vararg a: AsistenciaEntity)

    @Query("SELECT * FROM asistencias")
    suspend fun getAll(): List<AsistenciaEntity>

    @Query("""
    SELECT COUNT(*) FROM asistencias
    WHERE dniCliente = :dni AND fecha BETWEEN :startDate AND :endDate
""")
    suspend fun countInWeek(dni: String, startDate: Long, endDate: Long): Int


    @Query("DELETE FROM asistencias") suspend fun clearAll()
}