package com.example.foodtracker.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface ListDao {
    
    // TODO select needed methods for lists
//    @Query("SELECT * FROM list WHERE id = :id")
//    fun getListById(id: Int): Flow<ProductList>
//
//    @Query("SELECT * FROM list ORDER BY name ASC")
//    fun getAllLists(): Flow<List<ProductList>>
}
