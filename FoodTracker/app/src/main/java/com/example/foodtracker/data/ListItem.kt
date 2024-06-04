package com.example.foodtracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity


@Entity(tableName = "list_item", primaryKeys = ["product_id", "list_id"])
data class ListItem(
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "list_id") val listId: Int
)
