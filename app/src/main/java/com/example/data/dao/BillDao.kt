package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getBillsInRange(startTime: Long, endTime: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getBillsInRangeSync(startTime: Long, endTime: Long): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE billNumber = :billNumber LIMIT 1")
    suspend fun getBillByNumber(billNumber: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("UPDATE bills SET isVoided = 1, voidReason = :reason WHERE id = :id")
    suspend fun voidBill(id: Long, reason: String)

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getTotalBillsCount(): Int
}
