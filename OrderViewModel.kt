package com.example.pizzaorder

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OrderViewModel : ViewModel() {

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _orderNumber = MutableStateFlow<Int?>(null)
    val orderNumber: StateFlow<Int?> = _orderNumber.asStateFlow()

    private var nextItemId = 1L
    private var nextOrderNumber = 1001

    /** Добавляет пиццу в корзину. Одинаковые позиции (пицца + размер + добавки) объединяются. */
    fun addToCart(pizza: Pizza, size: PizzaSize, toppings: Set<Topping>) {
        _cart.update { items ->
            val existing = items.find {
                it.pizza.id == pizza.id &&
                    it.size == size &&
                    it.toppings.toSet() == toppings
            }
            if (existing != null) {
                items.map { if (it.id == existing.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                items + CartItem(nextItemId++, pizza, size, toppings.toList(), 1)
            }
        }
    }

    /** Меняет количество; при нуле позиция удаляется. */
    fun changeQuantity(itemId: Long, delta: Int) {
        _cart.update { items ->
            items.mapNotNull {
                if (it.id != itemId) it
                else {
                    val q = it.quantity + delta
                    if (q <= 0) null else it.copy(quantity = q)
                }
            }
        }
    }

    fun remove(itemId: Long) {
        _cart.update { items -> items.filterNot { it.id == itemId } }
    }

    fun cartTotal(): Double = _cart.value.sumOf { it.total }

    /**
     * Оформляет заказ. Здесь можно отправить данные на сервер
     * (Retrofit / Ktor) — сейчас заказ просто «принимается» локально.
     */
    fun placeOrder(name: String, phone: String, address: String, payment: PaymentMethod) {
        if (_cart.value.isEmpty()) return
        _orderNumber.value = nextOrderNumber++
        _cart.value = emptyList()
    }

    fun finishOrder() {
        _orderNumber.value = null
    }
}
