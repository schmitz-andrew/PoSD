package com.example.foodtracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.foodtracker.data.AppDatabase
import com.example.foodtracker.data.Product
import com.example.foodtracker.data.ProductDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test


class ProductDaoTest {

    private lateinit var productDao: ProductDao
    private lateinit var appDatabase: AppDatabase

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        productDao = appDatabase.productDao()
    }

    @After
    fun closeDb() {
        appDatabase.close()
    }

    private val product1 = Product(1, "Soda", 3, "30/6/25")
    private val product2 = Product(2, "Yogurt", 1, "25/5/24")

    private suspend fun addOneProductToDb() {
        productDao.insert(product1)
    }

    private suspend fun addTwoProductsToDb() {
        productDao.insert(product1)
        productDao.insert(product2)
    }

    @Test
    fun productDaoInsert_insertsItemIntoDb() = runBlocking {
        addOneProductToDb()

        val allProducts = productDao.getAllProducts().first()
        assertEquals(product1, allProducts[0])
    }

    @Test
    fun productDaoGetAllProducts_returnsAllProductsFromDb() = runBlocking {
        addTwoProductsToDb()

        val allProducts = productDao.getAllProducts().first()
        assertEquals(product2, allProducts[0])
        assertEquals(product1, allProducts[1])
    }

    @Test
    fun productDaoGetProductById_returnsProductFromDb() = runBlocking {
        addOneProductToDb()

        val product = productDao.getProductById(1).first()
        assertEquals(product1, product)
    }

    @Test
    fun productDaoGetProductById_doesNotReturnNonexistentProduct() = runBlocking {
        addOneProductToDb()

        val product = productDao.getProductById(2).first()
        assertNull(product)
    }

}
