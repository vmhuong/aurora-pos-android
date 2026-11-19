package com.example.qrorderkitchen.ui.theme

import androidx.compose.ui.graphics.Color

// Bảng màu sang trọng đồng bộ chuẩn Web Aurora Lounge (index.html)
val WebCreamBg = Color(0xFFF4EFE6)       // Nền kem ngà ấm áp đặc trưng của Web
val WebTextPrimary = Color(0xFF2C2621)   // Chữ màu cà phê rang đậm
val WebTextSecondary = Color(0xFF7C7367) // Chữ phụ màu nâu xám
val WebTextMuted = Color(0xFF9C9387)     // Chữ mờ placeholder
val WebGoldPrimary = Color(0xFFD4AF37)   // Vàng kim hoàng gia Imperial Gold
val WebGoldDark = Color(0xFFB89728)      // Vàng đậm hiển thị tiền tệ
val WebSurfacePill = Color(0xFFEAE3D5)   // Nền chip lọc & ô search
val WebBorder = Color(0xFFEAE3D5)        // Đường viền thẻ card đồng bộ
val WebCardBg = Color(0xFFFFFFFF)        // Thẻ màu trắng mượt mà
val WebInputBg = Color(0xFFF9F6F0)       // Nền ô nhập ghi chú món

// Nền và Bề mặt (Background & Surfaces)
val LightBgWarm = Color(0xFFF8F5F0)       // Nền chính toàn app: Màu kem sáng ấm áp, dịu mắt
val LightSurface = Color(0xFFFFFFFF)     // Bề mặt thẻ Card / Dialog: Màu trắng tinh khôi
val LightSurfaceSubtle = Color(0xFFF1ECE1)// Nền phụ / Ô input: Màu be nhạt
val LightBorder = Color(0xFFE5DECF)      // Đường viền thẻ: Màu be xám thanh lịch
val LightBorderStrong = Color(0xFFD3C8B4)// Viền đậm ngăn cách

// Màu thương hiệu Aurora (Brand Accents)
val AuroraGold = Color(0xFFB89728)       // Vàng kim đậm (dễ nhìn trên nền sáng hơn vàng chói)
val AuroraGoldHover = Color(0xFF9E801E)  // Vàng đậm khi nhấn nút
val AuroraGoldSoft = Color(0xFFFBF4DC)   // Nền vàng kem nhạt cho các tag highlight

// Chữ và Biểu tượng (Typography & Icons - Độ tương phản cao)
val TextPrimary = Color(0xFF1E1B18)      // Chữ chính: Đen than đậm, sắc nét tuyệt đối
val TextSecondary = Color(0xFF6B6357)    // Chữ phụ / Mô tả: Nâu xám trung tính
val TextMuted = Color(0xFF9E9587)        // Chữ phụ mờ / Placeholder: Xám be

// Màu trạng thái bàn & đơn hàng trên NỀN SÁNG (Pastel tương phản cao)
// ⚪ Bàn trống: Trắng viền xám tro
val TableEmptyBg = Color(0xFFFFFFFF)
val TableEmptyBorder = Color(0xFFE2E8F0)
val TableEmptyText = Color(0xFF64748B)

// 🟡 Chờ duyệt (Pending): Vàng kem rực rỡ
val StatusPendingBg = Color(0xFFFFFBEB)
val StatusPendingBorder = Color(0xFFF59E0B)
val StatusPendingText = Color(0xFFB45309)

// 🔵 Đang nấu (Cooking): Xanh biển dịu mát
val StatusCookingBg = Color(0xFFEFF6FF)
val StatusCookingBorder = Color(0xFF3B82F6)
val StatusCookingText = Color(0xFF1D4ED8)

// 🟢 Đang ăn (Dining / Done): Xanh ngọc tươi sáng
val StatusDoneBg = Color(0xFFECFDF5)
val StatusDoneBorder = Color(0xFF10B981)
val StatusDoneText = Color(0xFF047857)

// 🟣 Tính tiền (Checkout): Tím phấn nổi bật
val StatusCheckoutBg = Color(0xFFFAF5FF)
val StatusCheckoutBorder = Color(0xFFA855F7)
val StatusCheckoutText = Color(0xFF7E22CE)

// 🔴 Cần phục vụ (Assistance): Đỏ hồng báo động
val StatusAlertBg = Color(0xFFFEF2F2)
val StatusAlertBorder = Color(0xFFEF4444)
val StatusAlertText = Color(0xFFB91C1C)

// Legacy aliases for theme compatibility
val BackgroundDark = LightBgWarm
val GoldPrimary = AuroraGold
val GoldDark = AuroraGoldHover
val CardLight = LightSurface
val BackgroundLight = LightBgWarm
val TextPrimaryLight = TextPrimary
val TextPrimaryDark = TextPrimary
val StatusPending = StatusPendingBorder
val StatusCooking = StatusCookingBorder
val StatusEating = StatusDoneBorder
val StatusCheckoutStart = StatusCheckoutBorder
val StatusCheckoutEnd = StatusCheckoutText
val StatusAssistance = StatusAlertBorder
