package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.OrderItem
import com.example.model.SplitPart
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val orderItemListType = Types.newParameterizedType(List::class.java, OrderItem::class.java)
    private val orderItemAdapter = moshi.adapter<List<OrderItem>>(orderItemListType)

    private val splitPartListType = Types.newParameterizedType(List::class.java, SplitPart::class.java)
    private val splitPartAdapter = moshi.adapter<List<SplitPart>>(splitPartListType)

    @TypeConverter
    fun fromOrderItemList(value: List<OrderItem>?): String {
        return if (value == null) "[]" else orderItemAdapter.toJson(value)
    }

    @TypeConverter
    fun toOrderItemList(value: String?): List<OrderItem> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            orderItemAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSplitPartList(value: List<SplitPart>?): String {
        return if (value == null) "[]" else splitPartAdapter.toJson(value)
    }

    @TypeConverter
    fun toSplitPartList(value: String?): List<SplitPart> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            splitPartAdapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
