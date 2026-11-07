package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.KeyboardLanguage
import com.example.model.KeyboardStateBus
import com.example.model.WinKeyboardLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify WinKey Classic HE EN setup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WinKey Classic HE-EN", appName)

        // Verify Israeli Standard SI-1452 mapping on top letter row (T -> א, E -> ק, R -> ר)
        val tKey = WinKeyboardLayouts.topLetterRow.first { it.id == "t" }
        assertEquals("א", tKey.hePrimary)
        assertEquals("t", tKey.enPrimary)

        // Verify long-press 0 trigger updates state bus
        KeyboardStateBus.loadFromPrefs(context)
        val beforeCount = KeyboardStateBus.state.value.totalZeroLongPressCount
        KeyboardStateBus.notifyZeroKeyLongPressTriggered(context, "Test 0 Long Press")
        assertTrue(KeyboardStateBus.state.value.isOverlayKeyboardVisible)
        assertEquals(beforeCount + 1, KeyboardStateBus.state.value.totalZeroLongPressCount)

        // Verify Alt+Shift language toggle
        val initialLang = KeyboardStateBus.state.value.language
        KeyboardStateBus.toggleLanguage(context)
        val toggledLang = KeyboardStateBus.state.value.language
        assertTrue(initialLang != toggledLang)
        assertTrue(toggledLang == KeyboardLanguage.ENGLISH || toggledLang == KeyboardLanguage.HEBREW)
    }
}
