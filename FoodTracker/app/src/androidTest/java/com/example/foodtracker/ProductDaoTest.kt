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

    private val product1 = Product(1, "Soda", 3, "2025-06-30")
    private val product2 = Product(2, "Yogurt", 1, "2024-05-22", true)
    private val product3 = Product(3, "Salad", 1, null, true)
    private val product4 = Product(4, "Sauce", 2, "2025-08-05")

    private suspend fun addOneProductToDb() {
        productDao.insert(product1)
    }

    private suspend fun addFourProductsToDb() {
        listOf(product1, product2, product3, product4).forEach { productDao.insert(it) }
    }

    @Test
    fun productDaoInsert_insertsItemIntoDb() = runBlocking {
        addOneProductToDb()

        val allProducts = productDao.getAllProducts().first()
        assertEquals(product1, allProducts[0])
    }

    @Test
    fun productDaoGetAllProducts_returnsAllProductsFromDb() = runBlocking {
        addFourProductsToDb()

        val allProducts = productDao.getAllProducts().first()
        assertEquals(product2, allProducts[0])
        assertEquals(product1, allProducts[1])
        assertEquals(product4, allProducts[2])
        assertEquals(product3, allProducts[3])
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

    @Test
    fun productDaoGetProductsAtHome_ReturnsOnlyAtHomeProducts() = runBlocking {
        addFourProductsToDb()

        val homeProducts = productDao.getProductsAtHome().first()

        assertEquals(2, homeProducts.size)
        assertEquals(product1, homeProducts[0])
        assertEquals(product4, homeProducts[1])
    }

    @Test
    fun productDaoGetProductsInCart_ReturnsOnlyProductsInCart() = runBlocking {
        addFourProductsToDb()

        val cartProducts = productDao.getProductsInCart().first()

        assertEquals(2, cartProducts.size)
        assertEquals(product3, cartProducts[0])
        assertEquals(product2, cartProducts[1])
    }

}
