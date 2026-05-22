package com.example.unigearmanager

import org.junit.Test
import org.junit.Assert.*
import com.example.unigearmanager.UniWearRepository
import com.example.unigearmanager.StudentOrder

class ExampleUnitTest {
    @Test
    fun claimOrder_requiresPaymentAndReadyStatus() {
        val item = UniWearRepository.addItem(
            emoji = "🧪",
            name = "Test Release Item",
            category = "Test Gear",
            price = 100.0,
            stock = 2,
            unit = "pcs",
            audience = "Test students"
        )
        val order = requireNotNull(UniWearRepository.placeOrder("Test Student", item, 1))

        assertFalse(UniWearRepository.claimOrder(order.id))
        assertEquals("Collect payment before release.", UniWearRepository.claimBlockReason(order))

        assertEquals(100.0, UniWearRepository.markBalancePaid(order.id), 0.0)
        assertFalse(UniWearRepository.claimOrder(order.id))
        assertEquals("Mark ready for pickup before release.", UniWearRepository.claimBlockReason(order))

        assertTrue(UniWearRepository.advanceStatus(order.id))
        assertTrue(UniWearRepository.claimOrder(order.id))
        assertTrue(order.claimed)
        assertEquals("Claimed", order.status)
    }
}

