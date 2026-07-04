package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {

    @Test
    fun `addition and subtraction operate on minor units`() {
        val a = Money.fromMajor(10.10)
        val b = Money.fromMajor(0.20)

        // The classic 0.10 + 0.20 == 0.30 floating-point footgun — this
        // only passes because Money stores integer minor units, never a
        // raw Double.
        assertThat((a + b).minorUnits).isEqualTo(1030L)
        assertThat((a - b).minorUnits).isEqualTo(990L)
    }

    @Test
    fun `fromMajor rounds to the nearest minor unit`() {
        assertThat(Money.fromMajor(19.995).minorUnits).isEqualTo(2000L)
        assertThat(Money.fromMajor(19.994).minorUnits).isEqualTo(1999L)
    }

    @Test
    fun `isPositive and isNegative are mutually exclusive around zero`() {
        assertThat(Money.ZERO.isPositive).isFalse()
        assertThat(Money.ZERO.isNegative).isFalse()
        assertThat(Money(100).isPositive).isTrue()
        assertThat(Money(-100).isNegative).isTrue()
    }

    @Test
    fun `ratioOf clamps to the 0 to 1 range used by progress bars`() {
        val whole = Money.fromMajor(100.0)

        assertThat(Money.fromMajor(50.0).ratioOf(whole)).isEqualTo(0.5f)
        assertThat(Money.fromMajor(150.0).ratioOf(whole)).isEqualTo(1f)
        assertThat(Money.fromMajor(-10.0).ratioOf(whole)).isEqualTo(0f)
    }

    @Test
    fun `ratioOf against a zero or negative whole returns zero rather than dividing by zero`() {
        assertThat(Money.fromMajor(50.0).ratioOf(Money.ZERO)).isEqualTo(0f)
        assertThat(Money.fromMajor(50.0).ratioOf(Money(-100))).isEqualTo(0f)
    }

    @Test
    fun `sum folds an empty list to zero`() {
        assertThat(Money.sum(emptyList()).minorUnits).isEqualTo(0L)
    }

    @Test
    fun `sum adds every value in the iterable`() {
        val values = listOf(Money.fromMajor(10.0), Money.fromMajor(5.5), Money.fromMajor(-2.0))
        assertThat(Money.sum(values)).isEqualTo(Money.fromMajor(13.5))
    }

    @Test
    fun `formatted renders as USD currency`() {
        assertThat(Money.fromMajor(1234.5).formatted()).isEqualTo("$1,234.50")
    }

    @Test
    fun `formattedWithSign prefixes positive amounts and leaves negative amounts alone`() {
        assertThat(Money.fromMajor(10.0).formattedWithSign()).isEqualTo("+$10.00")
        assertThat(Money.fromMajor(-10.0).formattedWithSign()).isEqualTo("-$10.00")
        assertThat(Money.ZERO.formattedWithSign()).isEqualTo("$0.00")
    }
}
