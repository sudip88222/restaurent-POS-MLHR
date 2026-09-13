package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE status = 'ACTIVE' ORDER BY updatedAt DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveOrderByTableId(tableId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun observeOrderById(orderId: Long): Flow<OrderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String)
}
