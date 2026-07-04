package com.novafinance.core.domain.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Every monetary value in the app flows through this type — never a raw
 * Double or Float. Storing minor units (cents) as a Long is what makes
 * `0.10 + 0.20 == 0.30` actually true instead of a binary-float footgun,
 * which matters more here than almost anywhere else in the codebase.
 */
@JvmInline
value class Money(val minorUnits: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)
    operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)
    operator fun unaryMinus(): Money = Money(-minorUnits)
    operator fun times(factor: Int): Money = Money(minorUnits * factor)

    val isNegative: Boolean get() = minorUnits < 0
    val isPositive: Boolean get() = minorUnits > 0

    fun toMajorDouble(): Double = minorUnits / 100.0

    /** Ratio of this amount to [whole], clamped to [0, 1]. Used for progress bars. */
    fun ratioOf(whole: Money): Float {
        if (whole.minorUnits <= 0) return 0f
        return (minorUnits.toFloat() / whole.minorUnits.toFloat()).coerceIn(0f, 1f)
    }

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    companion object {
        val ZERO = Money(0)

        fun fromMajor(amount: Double): Money = Money(Math.round(amount * 100))

        fun sum(values: Iterable<Money>): Money =
            values.fold(ZERO) { acc, value -> acc + value }
    }
}

/**
 * [Currency.getInstance] does a real lookup against the ICU currency table
 * on every call. [formatted] runs once per transaction row on every list
 * recomposition, so this cache — keyed on the handful of currency codes
 * the app will ever pass in — turns a repeated table lookup into a map hit.
 * [Currency] instances are immutable and safe to share across threads.
 */
private val currencyCache = ConcurrentHashMap<String, Currency>()

private fun currencyFor(code: String): Currency =
    currencyCache.getOrPut(code) { Currency.getInstance(code) }

/**
 * [NumberFormat] instances are mutable and not thread-safe, so unlike
 * [currencyCache] this can't be a single shared instance — but formatting
 * almost always happens from Compose's main-thread recomposition path, so
 * a per-thread cache still turns away the repeated locale/pattern lookup
 * [NumberFormat.getCurrencyInstance] does internally for the common case,
 * while staying safe if a background thread ever calls this too.
 */
private val threadLocalFormatterCache =
    ThreadLocal.withInitial { mutableMapOf<Pair<Locale, String>, NumberFormat>() }

private fun formatterFor(locale: Locale, currencyCode: String): NumberFormat {
    val cache = threadLocalFormatterCache.get()
    return cache.getOrPut(locale to currencyCode) {
        NumberFormat.getCurrencyInstance(locale).apply { currency = currencyFor(currencyCode) }
    }
}

/**
 * Locale-aware currency formatting lives in a single place so every screen
 * renders amounts identically. Defaults to USD/US formatting — Nova has no
 * multi-currency support yet (see FinancialSourceRepository doc for the same note).
 */
fun Money.formatted(locale: Locale = Locale.US, currencyCode: String = "USD"): String =
    formatterFor(locale, currencyCode).format(toMajorDouble())

/** Same as [formatted] but with a leading "+" for positive amounts — for transaction rows and deltas. */
fun Money.formattedWithSign(locale: Locale = Locale.US, currencyCode: String = "USD"): String {
    val base = formatted(locale, currencyCode)
    return if (isPositive) "+$base" else base
}
