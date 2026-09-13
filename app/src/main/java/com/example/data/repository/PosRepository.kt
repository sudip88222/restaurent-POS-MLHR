package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BillEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.MenuItemEntity
import com.example.data.local.OrderEntity
import com.example.data.local.TableEntity
import com.example.model.OrderItem
import com.example.model.PaymentMethod
import com.example.model.SeatingArea
import com.example.model.SplitPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PosRepository(private val db: AppDatabase) {
    private val menuItemDao = db.menuItemDao()
    private val tableDao = db.tableDao()
    private val orderDao = db.orderDao()
    private val billDao = db.billDao()
    private val expenseDao = db.expenseDao()

    // --- Menu Flow & Operations ---
    val allMenuItems: Flow<List<MenuItemEntity>> = menuItemDao.getAllMenuItems()
    val allCategories: Flow<List<String>> = menuItemDao.getAllCategories()

    suspend fun addMenuItem(item: MenuItemEntity): Long = withContext(Dispatchers.IO) {
        menuItemDao.insertMenuItem(item)
    }

    suspend fun addMenuItems(items: List<MenuItemEntity>): List<Long> = withContext(Dispatchers.IO) {
        menuItemDao.insertAll(items)
    }

    suspend fun updateMenuItem(item: MenuItemEntity) = withContext(Dispatchers.IO) {
        menuItemDao.updateMenuItem(item)
    }

    suspend fun deleteMenuItem(item: MenuItemEntity) = withContext(Dispatchers.IO) {
        menuItemDao.deleteMenuItem(item)
    }

    // --- Tables Flow & Operations ---
    val allTables: Flow<List<TableEntity>> = tableDao.getAllTables()

    suspend fun addTable(name: String, area: SeatingArea, capacity: Int = 4): Long = withContext(Dispatchers.IO) {
        tableDao.insertTable(
            TableEntity(
                name = name,
                area = area.name,
                capacity = capacity
            )
        )
    }

    suspend fun updateTable(table: TableEntity) = withContext(Dispatchers.IO) {
        tableDao.updateTable(table)
    }

    suspend fun deleteTable(table: TableEntity) = withContext(Dispatchers.IO) {
        tableDao.deleteTable(table)
    }

    // --- Active Orders ---
    val activeOrders: Flow<List<OrderEntity>> = orderDao.getActiveOrders()

    suspend fun getActiveOrderByTable(tableId: Long): OrderEntity? = withContext(Dispatchers.IO) {
        orderDao.getActiveOrderByTableId(tableId)
    }

    suspend fun getOrderById(orderId: Long): OrderEntity? = withContext(Dispatchers.IO) {
        orderDao.getOrderById(orderId)
    }

    suspend fun addOrUpdateTableOrder(
        table: TableEntity,
        itemsToAdd: List<OrderItem>,
        customerNotes: String = ""
    ): OrderEntity = withContext(Dispatchers.IO) {
        val existingOrder = orderDao.getActiveOrderByTableId(table.id)
        val updatedOrder: OrderEntity
        if (existingOrder == null) {
            val newOrder = OrderEntity(
                tableId = table.id,
                tableName = table.name,
                area = table.area,
                items = itemsToAdd,
                status = "ACTIVE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                customerNotes = customerNotes
            )
            val orderId = orderDao.insertOrder(newOrder)
            updatedOrder = newOrder.copy(id = orderId)
            tableDao.updateTableStatus(table.id, true, orderId, System.currentTimeMillis())
        } else {
            val mutableList = existingOrder.items.toMutableList()
            for (newItem in itemsToAdd) {
                val index = mutableList.indexOfFirst {
                    it.menuItemId == newItem.menuItemId && it.roundNumber == newItem.roundNumber && it.notes == newItem.notes
                }
                if (index != -1) {
                    val current = mutableList[index]
                    mutableList[index] = current.copy(quantity = current.quantity + newItem.quantity)
                } else {
                    mutableList.add(newItem)
                }
            }
            updatedOrder = existingOrder.copy(
                items = mutableList,
                updatedAt = System.currentTimeMillis(),
                customerNotes = if (customerNotes.isNotBlank()) customerNotes else existingOrder.customerNotes
            )
            orderDao.updateOrder(updatedOrder)
        }
        updatedOrder
    }

    suspend fun updateOrderItems(orderId: Long, items: List<OrderItem>) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        if (items.isEmpty()) {
            orderDao.deleteOrder(order)
            tableDao.updateTableStatus(order.tableId, false, null, null)
        } else {
            orderDao.updateOrder(order.copy(items = items, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun cancelOrder(orderId: Long) = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext
        orderDao.updateOrderStatus(orderId, "CANCELLED")
        tableDao.updateTableStatus(order.tableId, false, null, null)
    }

    // --- Bills Flow & Operations ---
    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()

    suspend fun getBillById(id: Long): BillEntity? = withContext(Dispatchers.IO) {
        billDao.getBillById(id)
    }

    suspend fun createBill(
        order: OrderEntity?,
        tableId: Long?,
        tableName: String,
        area: String,
        items: List<OrderItem>,
        taxPercent: Double = 5.0,
        discountPercent: Double = 0.0,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        splitDetails: List<SplitPart> = emptyList(),
        customerName: String = "",
        customerPhone: String = ""
    ): BillEntity = withContext(Dispatchers.IO) {
        val count = billDao.getTotalBillsCount() + 1
        val datePrefix = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        val billNumber = "B$datePrefix-${String.format(Locale.US, "%03d", count % 1000)}"

        val subtotal = items.sumOf { it.totalAmount }
        val discountAmount = (subtotal * discountPercent) / 100.0
        val afterDiscount = subtotal - discountAmount
        val taxAmount = (afterDiscount * taxPercent) / 100.0
        val finalTotal = afterDiscount + taxAmount

        val bill = BillEntity(
            billNumber = billNumber,
            orderId = order?.id,
            tableId = tableId,
            tableName = tableName,
            area = area,
            items = items,
            subtotal = subtotal,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            discountPercent = discountPercent,
            discountAmount = discountAmount,
            finalTotal = finalTotal,
            paymentMethod = paymentMethod.name,
            splitDetails = splitDetails,
            customerName = customerName,
            customerPhone = customerPhone,
            timestamp = System.currentTimeMillis(),
            isPaid = true,
            isVoided = false
        )

        val insertedId = billDao.insertBill(bill)

        // Close out the order and free the table
        if (order != null) {
            orderDao.updateOrderStatus(order.id, "BILLED")
            tableDao.updateTableStatus(order.tableId, false, null, null)
        } else if (tableId != null) {
            tableDao.updateTableStatus(tableId, false, null, null)
        }

        bill.copy(id = insertedId)
    }

    suspend fun restoreBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        billDao.insertBill(bill)
    }

    suspend fun updateBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        val subtotal = bill.items.sumOf { it.totalAmount }
        val discountAmount = (subtotal * bill.discountPercent) / 100.0
        val afterDiscount = subtotal - discountAmount
        val taxAmount = (afterDiscount * bill.taxPercent) / 100.0
        val finalTotal = afterDiscount + taxAmount

        val updated = bill.copy(
            subtotal = subtotal,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            finalTotal = finalTotal
        )
        billDao.updateBill(updated)
    }

    suspend fun voidBill(billId: Long, reason: String) = withContext(Dispatchers.IO) {
        billDao.voidBill(billId, reason)
    }

    suspend fun deleteBill(billId: Long) = withContext(Dispatchers.IO) {
        billDao.deleteBillById(billId)
    }

    // --- Expenses Flow & Operations ---
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun addExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(id: Long) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpenseById(id)
    }

    // --- Daily Sales & Reporting Queries ---
    suspend fun getDailyBills(dayTimestamp: Long): List<BillEntity> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = cal.timeInMillis - 1
        billDao.getBillsInRangeSync(startTime, endTime)
    }

    suspend fun getDailyExpenses(dayTimestamp: Long): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endTime = cal.timeInMillis - 1
        expenseDao.getExpensesInRangeSync(startTime, endTime)
    }

    // --- Seed Initial Starter Data if Empty ---
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        if (menuItemDao.getCount() == 0) {
            val starterMenu = listOf(
                MenuItemEntity(name = "Paneer Tikka", category = "Starters", price = 220.0, isVeg = true, description = "Char-grilled cottage cheese cubes with spices", imageUrl = "icon:starter"),
                MenuItemEntity(name = "Crispy Chicken Wings", category = "Starters", price = 280.0, isVeg = false, description = "Spicy peri peri glazed wings", imageUrl = "icon:chicken"),
                MenuItemEntity(name = "Garlic Butter Naan", category = "Breads", price = 60.0, isVeg = true, description = "Clay oven tandoori bread with fresh garlic", imageUrl = "icon:bread"),
                MenuItemEntity(name = "Butter Chicken", category = "Main Course", price = 340.0, isVeg = false, description = "Tender chicken in rich aromatic tomato gravy", imageUrl = "icon:chicken"),
                MenuItemEntity(name = "Dal Makhani", category = "Main Course", price = 240.0, isVeg = true, description = "Slow cooked black lentils with cream & butter", imageUrl = "icon:curry"),
                MenuItemEntity(name = "Chicken Dum Biryani", category = "Rice & Biryani", price = 320.0, isVeg = false, description = "Aromatic basmati rice layered with spiced chicken", imageUrl = "icon:rice"),
                MenuItemEntity(name = "Veg Hakka Noodles", category = "Main Course", price = 190.0, isVeg = true, description = "Wok-tossed noodles with garden vegetables", imageUrl = "icon:noodles"),
                MenuItemEntity(name = "Margherita Pizza 8\"", category = "Fast Food", price = 260.0, isVeg = true, description = "Classic mozzarella cheese and basil", imageUrl = "icon:pizza"),
                MenuItemEntity(name = "Gourmet Cheese Burger", category = "Fast Food", price = 210.0, isVeg = true, description = "Cheddar cheese patty with chipotle mayo", imageUrl = "icon:burger"),
                MenuItemEntity(name = "Cold Brew Coffee", category = "Beverages", price = 120.0, isVeg = true, description = "Smooth steeped iced coffee", imageUrl = "icon:coffee"),
                MenuItemEntity(name = "Fresh Lime Soda", category = "Beverages", price = 80.0, isVeg = true, description = "Sweet and salted refreshing soda", imageUrl = "icon:drink"),
                MenuItemEntity(name = "Chocolate Lava Cake", category = "Desserts", price = 160.0, isVeg = true, description = "Warm molten chocolate core with vanilla", imageUrl = "icon:dessert"),
                MenuItemEntity(name = "Gulab Jamun (2 pcs)", category = "Desserts", price = 90.0, isVeg = true, description = "Golden milk dumplings in rose cardamom syrup", imageUrl = "icon:dessert")
            )
            menuItemDao.insertAll(starterMenu)
        }

        if (tableDao.getCount() == 0) {
            val starterTables = listOf(
                // Hall Tables
                TableEntity(name = "Table 1", area = SeatingArea.HALL.name, capacity = 2),
                TableEntity(name = "Table 2", area = SeatingArea.HALL.name, capacity = 4),
                TableEntity(name = "Table 3", area = SeatingArea.HALL.name, capacity = 4),
                TableEntity(name = "Table 4", area = SeatingArea.HALL.name, capacity = 6),
                TableEntity(name = "Table 5", area = SeatingArea.HALL.name, capacity = 4),
                TableEntity(name = "Table 6", area = SeatingArea.HALL.name, capacity = 8),
                // Private Cabins
                TableEntity(name = "Cabin 1 (VIP)", area = SeatingArea.CABIN.name, capacity = 6),
                TableEntity(name = "Cabin 2 (Royal)", area = SeatingArea.CABIN.name, capacity = 8),
                TableEntity(name = "Cabin 3 (Family)", area = SeatingArea.CABIN.name, capacity = 10),
                // Terrace Tables
                TableEntity(name = "Terrace T1", area = SeatingArea.TERRACE.name, capacity = 4),
                TableEntity(name = "Terrace T2", area = SeatingArea.TERRACE.name, capacity = 4),
                // Takeaway Counter
                TableEntity(name = "Counter Pickup", area = SeatingArea.TAKEAWAY.name, capacity = 1)
            )
            tableDao.insertAll(starterTables)
        }
    }
}
