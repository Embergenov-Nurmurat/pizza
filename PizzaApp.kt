package com.example.pizzaorder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Screen { MENU, CART, CHECKOUT, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizzaApp(vm: OrderViewModel = viewModel()) {
    val cart by vm.cart.collectAsStateWithLifecycle()
    val orderNumber by vm.orderNumber.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.MENU) }

    BackHandler(enabled = screen == Screen.CART || screen == Screen.CHECKOUT) {
        screen = if (screen == Screen.CHECKOUT) Screen.CART else Screen.MENU
    }

    val title = when (screen) {
        Screen.MENU -> "🍕 Пиццерия"
        Screen.CART -> "Корзина"
        Screen.CHECKOUT -> "Оформление заказа"
        Screen.SUCCESS -> "Заказ принят"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (screen == Screen.CART || screen == Screen.CHECKOUT) {
                        IconButton(onClick = {
                            screen = if (screen == Screen.CHECKOUT) Screen.CART else Screen.MENU
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (screen == Screen.MENU) {
                        IconButton(onClick = { screen = Screen.CART }) {
                            BadgedBox(badge = {
                                val count = cart.sumOf { it.quantity }
                                if (count > 0) Badge { Text(count.toString()) }
                            }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Корзина")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.MENU -> MenuScreen(onAdd = vm::addToCart)
                Screen.CART -> CartScreen(
                    items = cart,
                    onChangeQuantity = vm::changeQuantity,
                    onRemove = vm::remove,
                    onCheckout = { screen = Screen.CHECKOUT }
                )
                Screen.CHECKOUT -> CheckoutScreen(
                    total = cart.sumOf { it.total },
                    onConfirm = { name, phone, address, payment ->
                        vm.placeOrder(name, phone, address, payment)
                        screen = Screen.SUCCESS
                    }
                )
                Screen.SUCCESS -> SuccessScreen(
                    orderNumber = orderNumber,
                    onDone = {
                        vm.finishOrder()
                        screen = Screen.MENU
                    }
                )
            }
        }
    }
}

/* ───────────────────────── Меню ───────────────────────── */

@Composable
private fun MenuScreen(onAdd: (Pizza, PizzaSize, Set<Topping>) -> Unit) {
    var selected by remember { mutableStateOf<Pizza?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Menu.pizzas, key = { it.id }) { pizza ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { selected = pizza }
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pizza.emoji, fontSize = 40.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pizza.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            pizza.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "от ${pizza.basePrice.formatPrice()}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    selected?.let { pizza ->
        PizzaDialog(
            pizza = pizza,
            onDismiss = { selected = null },
            onAdd = { size, toppings ->
                onAdd(pizza, size, toppings)
                selected = null
            }
        )
    }
}

@Composable
private fun PizzaDialog(
    pizza: Pizza,
    onDismiss: () -> Unit,
    onAdd: (PizzaSize, Set<Topping>) -> Unit
) {
    var size by remember { mutableStateOf(PizzaSize.MEDIUM) }
    var toppings by remember { mutableStateOf(setOf<Topping>()) }
    val price = pizza.basePrice * size.multiplier + toppings.sumOf { it.price }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${pizza.emoji} ${pizza.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Размер", fontWeight = FontWeight.Bold)
                PizzaSize.entries.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = size == s, role = Role.RadioButton) { size = s },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = size == s, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(s.label, Modifier.padding(vertical = 8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Добавки", fontWeight = FontWeight.Bold)
                Menu.toppings.forEach { t ->
                    val checked = t in toppings
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { toppings = if (checked) toppings - t else toppings + t },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(t.name, Modifier.weight(1f).padding(vertical = 8.dp))
                        Text("+${t.price.formatPrice()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(size, toppings) }) {
                Text("Добавить · ${price.formatPrice()}")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

/* ───────────────────────── Корзина ───────────────────────── */

@Composable
private fun CartScreen(
    items: List<CartItem>,
    onChangeQuantity: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onCheckout: () -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Корзина пуста", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${item.pizza.name}, ${item.size.label}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (item.toppings.isNotEmpty()) {
                                    Text(
                                        item.toppings.joinToString { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onRemove(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onChangeQuantity(item.id, -1) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) { Text("−") }
                            Text(
                                item.quantity.toString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedButton(
                                onClick = { onChangeQuantity(item.id, 1) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) { Text("+") }
                            Spacer(Modifier.weight(1f))
                            Text(item.total.formatPrice(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row {
                    Text("Итого", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        items.sumOf { it.total }.formatPrice(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth()) {
                    Text("Оформить заказ")
                }
            }
        }
    }
}

/* ───────────────────────── Оформление ───────────────────────── */

@Composable
private fun CheckoutScreen(
    total: Double,
    onConfirm: (name: String, phone: String, address: String, payment: PaymentMethod) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var payment by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val nameError = name.isBlank()
    val phoneError = phone.count { it.isDigit() } < 9
    val addressError = address.isBlank()
    val showErrors = submitted

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Имя") },
            isError = showErrors && nameError,
            supportingText = { if (showErrors && nameError) Text("Введите имя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Телефон") },
            isError = showErrors && phoneError,
            supportingText = { if (showErrors && phoneError) Text("Введите корректный номер") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Адрес доставки") },
            isError = showErrors && addressError,
            supportingText = { if (showErrors && addressError) Text("Введите адрес") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Способ оплаты", fontWeight = FontWeight.Bold)
        PaymentMethod.entries.forEach { method ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = payment == method, role = Role.RadioButton) {
                        payment = method
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = payment == method, onClick = null)
                Spacer(Modifier.width(8.dp))
                Text(method.label, Modifier.padding(vertical = 8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                submitted = true
                if (!nameError && !phoneError && !addressError) {
                    onConfirm(name.trim(), phone.trim(), address.trim(), payment)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Заказать · ${total.formatPrice()}")
        }
    }
}

/* ───────────────────────── Успех ───────────────────────── */

@Composable
private fun SuccessScreen(orderNumber: Int?, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✅", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Заказ №${orderNumber ?: "—"} принят!",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text("Курьер привезёт пиццу в течение 40 минут.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Вернуться в меню") }
    }
}
