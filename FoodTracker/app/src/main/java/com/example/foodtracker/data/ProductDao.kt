package com.example.foodtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface ProductDao {

    @Query("SELECT * FROM product WHERE id = :id")
    fun getProductById(id: Int): Flow<Product>

    @Query("SELECT * FROM product ORDER BY expiry_date is NULL, expiry_date ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE in_cart = 0 ORDER BY expiry_date ASC")
    fun getProductsAtHome(): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE in_cart = 1 ORDER BY name ASC")
    fun getProductsInCart(): Flow<List<Product>>

    // TODO: how to deal with adding multiples? maybe at higher level i.e. do helper funs to update instead?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: Product): Long

    @Delete
    suspend fun remove(product: Product)

    @Update
    suspend fun update(product: Product)

}
