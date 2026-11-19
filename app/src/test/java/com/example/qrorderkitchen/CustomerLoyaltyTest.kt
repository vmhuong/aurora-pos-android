package com.example.qrorderkitchen

import com.example.qrorderkitchen.models.Customer
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerLoyaltyTest {

    @Test
    fun testEarnRate_memberTier() {
        val customer = Customer(tier = "member")
        assertEquals(0.03, customer.getEarnRate(), 0.001)
        assertEquals("Member (Tích 3%)", customer.getTierDisplayName())
    }

    @Test
    fun testEarnRate_silverTier() {
        val customer = Customer(tier = "silver")
        assertEquals(0.05, customer.getEarnRate(), 0.001)
        assertEquals("Bạc (Tích 5%)", customer.getTierDisplayName())

        val customerBac = Customer(tier = "bac")
        assertEquals(0.05, customerBac.getEarnRate(), 0.001)
    }

    @Test
    fun testEarnRate_goldTier() {
        val customer = Customer(tier = "gold")
        assertEquals(0.08, customer.getEarnRate(), 0.001)
        assertEquals("Vàng (Tích 8%)", customer.getTierDisplayName())

        val customerVang = Customer(tier = "vang")
        assertEquals(0.08, customerVang.getEarnRate(), 0.001)
    }

    @Test
    fun testEarnRate_platinumTier() {
        val customer = Customer(tier = "platinum")
        assertEquals(0.10, customer.getEarnRate(), 0.001)
        assertEquals("Bạch Kim (Tích 10%)", customer.getTierDisplayName())

        val customerBachKim = Customer(tier = "bach_kim")
        assertEquals(0.10, customerBachKim.getEarnRate(), 0.001)
    }

    @Test
    fun testEarnRate_vipTier() {
        val customer = Customer(tier = "vip")
        assertEquals(0.13, customer.getEarnRate(), 0.001)
        assertEquals("VIP (Tích 13%)", customer.getTierDisplayName())
    }

    @Test
    fun testPointsCalculation_memberSpend() {
        val customer = Customer(tier = "member", points = 100.0)
        val billAmount = 500000.0
        val earnedPoints = (billAmount * customer.getEarnRate()).toLong()
        assertEquals(15000L, earnedPoints)
    }

    @Test
    fun testPointsRedemption_cannotExceedBill() {
        val customerPoints = 300000.0
        val billAmount = 200000.0
        val maxPointsUsable = minOf(customerPoints, billAmount)
        assertEquals(200000.0, maxPointsUsable, 0.01)
        val remainingPay = billAmount - maxPointsUsable
        assertEquals(0.0, remainingPay, 0.01)
    }
}
