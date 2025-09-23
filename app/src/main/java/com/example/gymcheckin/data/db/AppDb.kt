// AppDatabase.kt
package com.example.gymcheckin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gymcheckin.data.dao.AsistenciaDao
import com.example.gymcheckin.data.dao.ClienteDao
import com.example.gymcheckin.data.dao.PagoDao
import com.example.gymcheckin.data.entity.AsistenciaEntity
import com.example.gymcheckin.data.entity.ClienteEntity
import com.example.gymcheckin.data.entity.PagoEntity
import com.example.gymcheckin.util.Converters

@Database(
    entities = [ClienteEntity::class, AsistenciaEntity::class, PagoEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun pagoDao(): PagoDao

    companion object {
        @Volatile private var INSTANCE: AppDb? = null
        fun getInstance(ctx: Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDb::class.java,
                    "gym.db"
                )
                    .fallbackToDestructiveMigration() // <<< durante dev
                    .build().also { INSTANCE = it }
            }
    }
}
