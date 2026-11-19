package com.example.qrorderkitchen

import com.example.qrorderkitchen.utils.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun testFormat_standardAmount() {
        assertEquals("50.000đ", CurrencyUtils.format(50000))
        assertEquals("124.000đ", CurrencyUtils.format(124000))
    }

    @Test
    fun testFormat_zeroAmount() {
        assertEquals("0đ", CurrencyUtils.format(0))
    }

    @Test
    fun testFormat_largeAmount() {
        assertEquals("13.924.134đ", CurrencyUtils.format(13924134))
    }

    @Test
    fun testFormatVnd_standardAmount() {
        assertEquals("50.000 VNĐ", CurrencyUtils.formatVnd(50000))
        assertEquals("223.000 VNĐ", CurrencyUtils.formatVnd(223000))
    }

    @Test
    fun testFormatPlain_numberOnly() {
        assertEquals("89.000", CurrencyUtils.formatPlain(89000))
        assertEquals("0", CurrencyUtils.formatPlain(0))
    }
}
