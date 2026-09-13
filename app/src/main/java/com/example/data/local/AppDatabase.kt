package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.BillDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.MenuItemDao
import com.example.data.dao.OrderDao
import com.example.data.dao.TableDao

@Database(
    entities = [
        MenuItemEntity::class,
        TableEntity::class,
        OrderEntity::class,
        BillEntity::class,
        ExpenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun tableDao(): TableDao
    abstract fun orderDao(): OrderDao
    abstract fun billDao(): BillDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant_pos.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
