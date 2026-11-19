package com.example.qrorderkitchen.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.qrorderkitchen.models.Dish
import com.example.qrorderkitchen.ui.theme.WebBorder
import com.example.qrorderkitchen.ui.theme.WebCardBg
import com.example.qrorderkitchen.ui.theme.WebCreamBg
import com.example.qrorderkitchen.ui.theme.WebGoldDark
import com.example.qrorderkitchen.ui.theme.WebGoldPrimary
import com.example.qrorderkitchen.ui.theme.WebSurfacePill
import com.example.qrorderkitchen.ui.theme.WebTextMuted
import com.example.qrorderkitchen.ui.theme.WebTextPrimary
import com.example.qrorderkitchen.utils.CurrencyUtils
import com.google.firebase.Timestamp

@Composable
fun MenuCard(
    dish: Dish,
    cartQuantity: Int,
    onAddToCart: (Dish) -> Unit,
    onRemoveFromCart: (Dish) -> Unit,
    onCardClick: (Dish) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Check if dish is locked/unavailable
    val isDishAvailable = dish.isCurrentlyAvailable

    var isPressed by remember { mutableStateOf(false) }
    val bounceScale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "bounce"
    )

    // Cỡ chữ tối ưu riêng biệt: Màn hình thẳng (Portrait) chữ to rõ nét cho điện thoại
    val titleFontSize = if (isLandscape) 13.sp else 16.sp
    val descFontSize = if (isLandscape) 11.sp else 13.sp
    val priceFontSize = if (isLandscape) 12.5.sp else 15.5.sp
    val qtyFontSize = if (isLandscape) 13.sp else 15.5.sp
    val imageSize = if (isLandscape) 62.dp else 72.dp
    val buttonSize = if (isLandscape) 28.dp else 34.dp
    val initButtonSize = if (isLandscape) 30.dp else 36.dp
    val iconSize = if (isLandscape) 13.dp else 16.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLandscape) 3.dp else 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, WebBorder, RoundedCornerShape(18.dp))
            .alpha(if (isDishAvailable) 1.0f else 0.6f)
            .clickable(enabled = isDishAvailable) { onCardClick(dish) },
        colors = CardDefaults.cardColors(containerColor = WebCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ảnh món ăn
            Box(
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE2DACB))
            ) {
                AsyncImage(
                    model = dish.img.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c" },
                    contentDescription = dish.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                if (!isDishAvailable) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Hết món",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(if (isLandscape) 18.dp else 22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 12.dp))

            // Thông tin món ăn
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dish.name,
                    color = WebTextPrimary,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (dish.desc.isNotBlank()) {
                    Text(
                        text = dish.desc,
                        color = WebTextMuted,
                        fontSize = descFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyUtils.formatVnd(dish.price),
                        color = if (isDishAvailable) WebGoldDark else Color(0xFFC5BFB3),
                        fontSize = priceFontSize,
                        fontWeight = FontWeight.Black,
                        textDecoration = if (isDishAvailable) null else TextDecoration.LineThrough
                    )
                    if (dish.options.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(0.5.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+ Topping",
                                color = Color(0xFFB45309),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Bộ nút tăng giảm số lượng / Thêm món
            if (isDishAvailable) {
                if (cartQuantity > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        // Nút giảm (-)
                        Box(
                            modifier = Modifier
                                .size(buttonSize)
                                .clip(CircleShape)
                                .background(WebSurfacePill)
                                .clickable { onRemoveFromCart(dish) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Remove",
                                tint = WebTextPrimary,
                                modifier = Modifier.size(iconSize)
                            )
                        }

                        Text(
                            text = cartQuantity.toString(),
                            color = WebTextPrimary,
                            fontSize = qtyFontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = if (isLandscape) 7.dp else 9.dp)
                        )

                        // Nút tăng (+)
                        Box(
                            modifier = Modifier
                                .scale(bounceScale)
                                .size(buttonSize)
                                .clip(CircleShape)
                                .background(WebGoldPrimary)
                                .clickable {
                                    isPressed = true
                                    onAddToCart(dish)
                                    isPressed = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                } else {
                    // Nút Thêm (+) ban đầu
                    Box(
                        modifier = Modifier
                            .scale(bounceScale)
                            .size(initButtonSize)
                            .clip(CircleShape)
                            .background(WebCreamBg)
                            .border(1.dp, Color(0xFFE2DACB), CircleShape)
                            .clickable {
                                isPressed = true
                                onAddToCart(dish)
                                isPressed = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = WebGoldDark,
                            modifier = Modifier.size(iconSize + 2.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Hết hàng",
                        color = Color(0xFFDC2626),
                        fontSize = if (isLandscape) 10.sp else 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
