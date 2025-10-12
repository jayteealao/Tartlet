package io.yumemi.something

import kotlin.test.Test
import kotlin.test.assertEquals

class FibiTest {

    @Test
    fun `test 3rd element`() {
        assertEquals(1 + 2, generateFibi().take(3).last())
    }
}
