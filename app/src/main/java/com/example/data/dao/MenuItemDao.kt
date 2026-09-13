package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY category ASC, name ASC")
    fun getAllMenuItems(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getMenuItemById(id: Long): MenuItemEntity?

    @Query("SELECT DISTINCT category FROM menu_items ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MenuItemEntity>): List<Long>

    @Update
    suspend fun updateMenuItem(item: MenuItemEntity)

    @Delete
    suspend fun deleteMenuItem(item: MenuItemEntity)

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun getCount(): Int
}
