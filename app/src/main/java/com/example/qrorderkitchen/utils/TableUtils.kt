package com.example.qrorderkitchen.utils

object TableUtils {
    /**
     * Chuẩn hóa mã bàn sạch (Ví dụ: "Bàn A1", "Table A1", "ban a1", "A1", "table_b2" -> "A1", "B2")
     */
    fun cleanTableCode(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim()
            .replace(Regex("^(Table_|Table|B[àaáảãạ]n|ban|bàn)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .uppercase()
    }

    /**
     * So sánh 2 mã bàn xem có cùng là một bàn không (bỏ qua tiền tố "Bàn", "Table", khoảng trắng, chữ hoa/thường)
     */
    fun isSameTable(table1: String?, table2: String?): Boolean {
        val c1 = cleanTableCode(table1)
        val c2 = cleanTableCode(table2)
        return c1.isNotBlank() && c1 == c2
    }
}
