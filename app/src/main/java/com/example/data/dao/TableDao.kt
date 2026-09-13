package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    @Query("SELECT * FROM tables ORDER BY area ASC, name ASC")
    fun getAllTables(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE area = :area ORDER BY name ASC")
    fun getTablesByArea(area: String): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getTableById(id: Long): TableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tables: List<TableEntity>)

    @Update
    suspend fun updateTable(table: TableEntity)

    @Delete
    suspend fun deleteTable(table: TableEntity)

    @Query("UPDATE tables SET isOccupied = :isOccupied, currentOrderId = :orderId, occupiedSince = :occupiedSince WHERE id = :tableId")
    suspend fun updateTableStatus(tableId: Long, isOccupied: Boolean, orderId: Long?, occupiedSince: Long?)

    @Query("SELECT COUNT(*) FROM tables")
    suspend fun getCount(): Int
}
