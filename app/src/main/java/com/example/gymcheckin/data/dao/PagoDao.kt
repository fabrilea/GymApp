package com.example.gymcheckin.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gymcheckin.data.entity.PagoEntity

@Dao
interface PagoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPago(pago: PagoEntity)

    @Query("SELECT * FROM pagos WHERE dni = :dni")
    suspend fun getPagosByDni(dni: String): List<PagoEntity>

    @Query("SELECT * FROM pagos")
    suspend fun getAllPagos(): List<PagoEntity>
}
