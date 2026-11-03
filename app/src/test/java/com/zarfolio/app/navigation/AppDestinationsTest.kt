package com.zarfolio.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationsTest {
    @Test fun allRequiredPagesHaveNavigationDestinations() {
        assertEquals(setOf("products", "camera", "recent", "connection"), AppDestinations.bottomTabs.toSet())
        assertTrue(AppDestinations.all.containsAll(AppDestinations.bottomTabs + AppDestinations.HOME + AppDestinations.LOGIN))
    }
    @Test fun bottomNavigationContainsFourUniqueDestinations() {
        assertEquals(4, AppDestinations.bottomTabs.size)
        assertEquals(4, AppDestinations.bottomTabs.toSet().size)
    }
}
