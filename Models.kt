package com.example.pizzaorder

import java.util.Locale

enum class PizzaSize(val label: String, val multiplier: Double) {
    SMALL("25 см", 1.0),
    MEDIUM("30 см", 1.3),
    LARGE("35 см", 1.6)
}

data class Pizza(
    val id: Int,
    val name: String,
    val description: String,
    val basePrice: Double,
    val emoji: String
)

data class Topping(
    val id: Int,
    val name: String,
    val price: Double
)

data class CartItem(
    val id: Long,
    val pizza: Pizza,
    val size: PizzaSize,
    val toppings: List<Topping>,
    val quantity: Int
) {
    val unitPrice: Double
        get() = pizza.basePrice * size.multiplier + toppings.sumOf { it.price }

    val total: Double
        get() = unitPrice * quantity
}

enum class PaymentMethod(val label: String) {
    CASH("Наличными курьеру"),
    CARD("Картой курьеру")
}

object Menu {
    val pizzas = listOf(
        Pizza(1, "Маргарита", "Томатный соус, моцарелла, базилик", 45_000.0, "🍕"),
        Pizza(2, "Пепперони", "Пикантная пепперони, моцарелла, томатный соус", 58_000.0, "🌶️"),
        Pizza(3, "Четыре сыра", "Моцарелла, горгонзола, пармезан, чеддер", 65_000.0, "🧀"),
        Pizza(4, "Гавайская", "Курица, ананасы, моцарелла", 55_000.0, "🍍"),
        Pizza(5, "Мясная", "Ветчина, колбаски, бекон, говядина, моцарелла", 72_000.0, "🥓"),
        Pizza(6, "Вегетарианская", "Грибы, перец, томаты, оливки, лук", 52_000.0, "🥬")
    )

    val toppings = listOf(
        Topping(1, "Двойной сыр", 8_000.0),
        Topping(2, "Грибы", 6_000.0),
        Topping(3, "Оливки", 5_000.0),
        Topping(4, "Бекон", 10_000.0),
        Topping(5, "Халапеньо", 5_000.0)
    )
}

fun Double.formatPrice(): String =
    String.format(Locale.US, "%,.0f", this).replace(',', ' ') + " сум"
