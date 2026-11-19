package com.example.qrorderkitchen

import com.example.qrorderkitchen.models.Voucher
import org.junit.Assert.*
import org.junit.Test

class VoucherCalculationTest {

    @Test
    fun testFixedVoucher_meetsMinBill() {
        val voucher = Voucher(
            code = "GIAM30K",
            type = "fixed",
            value = 30000.0,
            min_bill = 100000.0,
            status = "available"
        )
        val bill = 150000.0
        assertTrue("Bill exceeds min_bill", bill >= voucher.min_bill)
        val discount = voucher.value
        val finalBill = maxOf(0.0, bill - discount)
        assertEquals(30000.0, discount, 0.01)
        assertEquals(120000.0, finalBill, 0.01)
    }

    @Test
    fun testFixedVoucher_belowMinBill() {
        val voucher = Voucher(
            code = "GIAM30K",
            type = "fixed",
            value = 30000.0,
            min_bill = 100000.0,
            status = "available"
        )
        val bill = 80000.0
        assertFalse("Bill should be rejected when below min_bill", bill >= voucher.min_bill)
    }

    @Test
    fun testPercentVoucher_withMaxDiscountCap() {
        val voucher = Voucher(
            code = "SALE20",
            type = "percent",
            value = 20.0, // 20%
            max_discount = 50000.0,
            min_bill = 100000.0,
            status = "available"
        )
        val bill = 500000.0 // 20% of 500k = 100k > max_discount 50k
        val rawDiscount = bill * (voucher.value / 100.0)
        val effectiveDiscount = if (voucher.max_discount > 0) minOf(rawDiscount, voucher.max_discount) else rawDiscount
        assertEquals(50000.0, effectiveDiscount, 0.01)
        val finalBill = bill - effectiveDiscount
        assertEquals(450000.0, finalBill, 0.01)
    }

    @Test
    fun testVoucher_discountExceedsBill_noNegativeBill() {
        val voucher = Voucher(
            code = "SUPER100K",
            type = "fixed",
            value = 100000.0,
            min_bill = 50000.0,
            status = "available"
        )
        val bill = 70000.0
        val effectiveDiscount = minOf(bill, voucher.value)
        val finalBill = maxOf(0.0, bill - effectiveDiscount)
        assertEquals(70000.0, effectiveDiscount, 0.01)
        assertEquals(0.0, finalBill, 0.01)
    }
}
