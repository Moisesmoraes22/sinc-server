package com.apksinc.monitor

import com.apksinc.monitor.domain.ColorTag
import com.apksinc.monitor.domain.HabitCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitCategoryTest {

    @Test
    fun `parses known raw categories`() {
        assertEquals(HabitCategory.AGUA, HabitCategory.fromRaw("AGUA"))
        assertEquals(HabitCategory.SONO, HabitCategory.fromRaw("SONO"))
        assertEquals(HabitCategory.SKINCARE, HabitCategory.fromRaw("SKINCARE"))
    }

    @Test
    fun `unknown category falls back to outro`() {
        assertEquals(HabitCategory.OUTRO, HabitCategory.fromRaw("desconhecido"))
    }

    @Test
    fun `unknown color tag falls back to accent`() {
        assertEquals(ColorTag.ACCENT, ColorTag.fromRaw("desconhecido"))
    }
}
