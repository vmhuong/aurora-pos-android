package com.example.qrorderkitchen.ui.customer

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.qrorderkitchen.utils.CurrencyUtils
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.qrorderkitchen.ui.theme.WebBorder
import com.example.qrorderkitchen.ui.theme.WebCardBg
import com.example.qrorderkitchen.ui.theme.WebCreamBg
import com.example.qrorderkitchen.ui.theme.WebGoldDark
import com.example.qrorderkitchen.ui.theme.WebGoldPrimary
import com.example.qrorderkitchen.ui.theme.WebInputBg
import com.example.qrorderkitchen.ui.theme.WebSurfacePill
import com.example.qrorderkitchen.ui.theme.WebTextMuted
import com.example.qrorderkitchen.ui.theme.WebTextPrimary
import com.example.qrorderkitchen.ui.theme.WebTextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AuroraViewModel
import com.example.qrorderkitchen.ui.viewmodel.CustomerTab

@Composable
fun CustomerCartScreen(viewModel: AuroraViewModel) {
    val context = LocalContext.current
    val cartMap by viewModel.cartItems.collectAsState()
    val tableId by viewModel.currentTableId.collectAsState()
    val isSubmitting by viewModel.isSubmittingOrder.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val cartList = remember(cartMap) { cartMap.values.toList() }
    val totalAmount = remember(cartList) { cartList.sumOf { it.quantity * (if (it.unitPrice > 0.0) it.unitPrice else it.dish.price) } }
    val totalItems = remember(cartList) { cartList.sumOf { it.quantity } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WebCreamBg)
    ) {
        if (cartList.isEmpty()) {
            // Elegant Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 72.dp else 80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEAE3D5).copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Empty Cart",
                            tint = Color(0xFFC5BFB3),
                            modifier = Modifier.size(if (isLandscape) 36.dp else 42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Giỏ hàng của bạn đang trống",
                        color = WebTextPrimary,
                        fontSize = if (isLandscape) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Chưa có món nào được thêm vào giỏ",
                        color = WebTextMuted,
                        fontSize = if (isLandscape) 12.sp else 14.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.setCustomerTab(CustomerTab.MENU) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WebGoldPrimary),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = if (isLandscape) 10.dp else 12.dp)
                    ) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Khám phá Thực đơn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 13.sp else 15.sp)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar matching Web
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (isLandscape) 10.dp else 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = WebGoldPrimary,
                            modifier = Modifier.size(if (isLandscape) 18.dp else 22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Giỏ hàng của bạn",
                            color = WebTextPrimary,
                            fontSize = if (isLandscape) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(WebSurfacePill)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$totalItems món",
                                color = WebTextSecondary,
                                fontSize = if (isLandscape) 11.sp else 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Clear All Text Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.clearCart() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(if (isLandscape) 14.dp else 16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Xóa tất cả",
                            color = Color(0xFFEF4444),
                            fontSize = if (isLandscape) 11.5.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Cart Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 10.dp)
                ) {
                    items(cartList, key = { it.cartItemKey.ifBlank { it.dish.id } }) { cartItem ->
                        val effectiveUnitPrice = if (cartItem.unitPrice > 0.0) cartItem.unitPrice else cartItem.dish.price
                        val itemKey = cartItem.cartItemKey.ifBlank { cartItem.dish.id }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, WebBorder, RoundedCornerShape(18.dp)),
                            colors = CardDefaults.cardColors(containerColor = WebCardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(if (isLandscape) 10.dp else 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Image
                                    AsyncImage(
                                        model = cartItem.dish.img.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c" },
                                        contentDescription = cartItem.dish.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(if (isLandscape) 54.dp else 66.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE2DACB))
                                    )

                                    Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 12.dp))

                                    // Name & Unit Price
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cartItem.dish.name,
                                            color = WebTextPrimary,
                                            fontSize = if (isLandscape) 13.5.sp else 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        if (cartItem.optionsSummary.isNotBlank()) {
                                            val optLines = if (cartItem.optionsSummary.contains("\n")) cartItem.optionsSummary.split("\n") else cartItem.optionsSummary.split(", ")
                                            optLines.filter { it.isNotBlank() }.forEach { optLine ->
                                                val cleanLine = optLine.trim().removePrefix("+").removePrefix("•").trim()
                                                Text(
                                                    text = "• $cleanLine",
                                                    color = Color(0xFFB45309),
                                                    fontSize = if (isLandscape) 11.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = CurrencyUtils.formatVnd(effectiveUnitPrice),
                                            color = WebGoldDark,
                                            fontSize = if (isLandscape) 12.sp else 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    // Subtotal & Quantity Adjuster
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = CurrencyUtils.formatVnd(effectiveUnitPrice * cartItem.quantity),
                                            color = WebTextPrimary,
                                            fontSize = if (isLandscape) 13.sp else 15.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            val btnSize = if (isLandscape) 28.dp else 34.dp
                                            val btnIconSize = if (isLandscape) 13.dp else 16.dp

                                            // Minus (or Trash if qty == 1)
                                            Box(
                                                modifier = Modifier
                                                    .size(btnSize)
                                                    .clip(CircleShape)
                                                    .background(WebSurfacePill)
                                                    .clickable { viewModel.updateCartItemQuantity(itemKey, -1) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                                    contentDescription = "Minus",
                                                    tint = if (cartItem.quantity == 1) Color(0xFFEF4444) else WebTextPrimary,
                                                    modifier = Modifier.size(btnIconSize)
                                                )
                                            }

                                            Text(
                                                text = cartItem.quantity.toString(),
                                                color = WebTextPrimary,
                                                fontSize = if (isLandscape) 13.sp else 15.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = if (isLandscape) 4.dp else 6.dp)
                                            )

                                            // Plus
                                            Box(
                                                modifier = Modifier
                                                    .size(btnSize)
                                                    .clip(CircleShape)
                                                    .background(WebGoldPrimary)
                                                    .clickable { viewModel.updateCartItemQuantity(itemKey, 1) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Plus",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(btnIconSize)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 10.dp))

                                // Inline Note Input
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(WebInputBg)
                                        .border(1.dp, Color(0xFFE2DACB), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = if (isLandscape) 6.dp else 8.dp)
                                ) {
                                    if (cartItem.note.isBlank()) {
                                        Text(
                                            text = "Ghi chú món (VD: ít đá, không cay...)",
                                            color = Color(0xFFC5BFB3),
                                            fontSize = if (isLandscape) 11.sp else 13.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = cartItem.note,
                                        onValueChange = { newNote ->
                                            viewModel.updateCartItemNote(itemKey, newNote)
                                        },
                                        textStyle = TextStyle(
                                            color = WebTextPrimary,
                                            fontSize = if (isLandscape) 11.5.sp else 13.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary & Submit Button Footer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .border(1.dp, WebBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    colors = CardDefaults.cardColors(containerColor = WebCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = if (isLandscape) 14.dp else 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tổng cộng (Bàn $tableId)",
                                    color = WebTextSecondary,
                                    fontSize = if (isLandscape) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = CurrencyUtils.formatVnd(totalAmount),
                                    color = WebGoldDark,
                                    fontSize = if (isLandscape) 20.sp else 23.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(WebCreamBg)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$totalItems món",
                                    color = WebTextSecondary,
                                    fontSize = if (isLandscape) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 14.dp))

                        Button(
                            onClick = {
                                if (!isSubmitting) {
                                    viewModel.submitCustomerOrder(context)
                                }
                            },
                            enabled = !isSubmitting && cartList.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 48.dp else 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WebGoldPrimary,
                                disabledContainerColor = WebGoldPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(if (isLandscape) 18.dp else 22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ĐANG GỬI ĐƠN...",
                                    color = Color.White,
                                    fontSize = if (isLandscape) 14.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Submit",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isLandscape) 16.dp else 19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "XÁC NHẬN GỬI ĐƠN BẾP",
                                    color = Color.White,
                                    fontSize = if (isLandscape) 14.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
