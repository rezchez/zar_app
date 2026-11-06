package com.zarfolio.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationsTest {
    @Test fun allRequiredPagesHaveNavigationDestinations() {
        assertEquals(setOf("home", "products", "recent", "settings", "profile"), AppDestinations.bottomTabs.toSet())
        assertTrue(AppDestinations.all.containsAll(AppDestinations.bottomTabs + AppDestinations.HOME + AppDestinations.LOGIN))
        assertTrue(AppDestinations.all.contains(AppDestinations.ALL_ETIQUETTES))
    }
    @Test fun bottomNavigationContainsFiveUniqueDestinations() {
        assertEquals(5, AppDestinations.bottomTabs.size)
        assertEquals(5, AppDestinations.bottomTabs.toSet().size)
    }
}
