package com.novafinance.core.domain.model

import java.time.Instant

/**
 * Any place money lives or moves through — not just a traditional bank
 * account. Renamed/expanded from the original "Account" concept (Phases
 * 1-8) specifically to drop the salary-centric assumption that every
 * financial source is a payroll-fed checking account: cash in a wallet,
 * a prepaid e-wallet, a credit card, are all first-class [FinancialSource]s
 * with the same shape, not special cases bolted onto a bank-account model.
 *
 * [id] is still the same identity every [Transaction.accountId] foreign
 * key points at — that field name predates this rename and is kept as-is
 * deliberately rather than renamed to `sourceId` everywhere, to avoid a
 * much larger blast radius through the transactions feature and its Room
 * schema for a purely cosmetic rename. See TECHNICAL_DEBT.md.
 */
data class FinancialSource(
    val id: String,
    val name: String,
    val type: FinancialSourceType,
    /** The source's own ledger balance — what it would show reconciled against real transactions. */
    val currentBalance: Money,
    /**
     * What's actually usable right now — for a credit card this is the
     * remaining credit line, not the statement balance; for a bank
     * account with a pending hold it can be lower than [currentBalance].
     * Defaults to [currentBalance] for source types where the two don't
     * meaningfully differ (cash, e-wallet).
     */
    val availableBalance: Money,
    /** ISO 4217 currency code. Nova is USD-only in practice today (see [Money.formatted]), but every source carries its own code so a future multi-currency pass has real data to work from instead of retrofitting one. */
    val currency: String = "USD",
    val notes: String? = null,
    val isActive: Boolean = true,
    /** How this source's balance gets kept up to date — see [BalanceUpdateMode]. */
    val balanceUpdateMode: BalanceUpdateMode = BalanceUpdateMode.MANUAL,
    /** Only meaningful for [FinancialSourceType.CREDIT_CARD] — null for every other type. See [creditCardUtilization]. */
    val creditLimit: Money? = null,
    /**
     * Every flag defaults to `true` except [isEmergencyReserve] — a
     * newly added source counts toward every calculation until the
     * person deliberately excludes it, per this phase's Freedom First
     * principle ("users always control what counts," not "nothing
     * counts until they opt in"). See `FINANCIAL_SOURCES_ARCHITECTURE.md`.
     */
    val includeInLiquidity: Boolean = true,
    /** A savings account might reasonably have [includeInLiquidity] = true but this = false — money that's liquid in principle but not meant for everyday spending. See 11.4's own "Savings Account" example. */
    val includeInSpendingPower: Boolean = true,
    val includeInForecast: Boolean = true,
    val includeInGoals: Boolean = true,
    val includeInAnalytics: Boolean = true,
    /** Marks this source as (part of) the person's emergency fund for [BalanceIntelligence.emergencyReserve] — a person-declared fact, never inferred from the source's name or type. */
    val isEmergencyReserve: Boolean = false,
    /** References a [SourceGroup.id], or null for "ungrouped." No foreign-key enforcement at the Room level — see `FINANCIAL_SOURCES_ARCHITECTURE.md` for why groups are a lightweight organizational label, not a referentially-enforced relationship. */
    val groupId: String? = null,
    /**
     * References a [Debt.id] — the reconciliation link Phase 11.5.1
     * introduces so a credit card can be represented once, not twice
     * with two independently-drifting balances. Null means unlinked
     * (the common case, and the only case before this phase). See
     * `effectiveLiabilityBalance` and `FINANCIAL_SOURCES_ARCHITECTURE.md`'s
     * "Ownership rules" section for what linking actually changes.
     */
    val linkedDebtId: String? = null,
    val createdAt: Instant
) {
    /** Null unless this is a credit card with a limit set — see [creditLimit]'s own doc. */
    val creditCardUtilization: CreditCardUtilization?
        get() {
            val limit = creditLimit ?: return null
            if (type != FinancialSourceType.CREDIT_CARD || limit.minorUnits <= 0) return null
            return calculateCreditCardUtilization(usedAmount = currentBalance, creditLimit = limit)
        }
}

/**
 * Deliberately broader than a bank's own account-type taxonomy. [CASH]
 * and [E_WALLET] in particular didn't fit the original salary-centric
 * [FinancialSource] model at all — they have no statement, no routing
 * number, sometimes no linked institution — but they're exactly the
 * kind of source Nova needs to represent well for the markets it's
 * actually aimed at. [INVESTMENT_ACCOUNT] (Phase 11.1) fills the one
 * gap Phase 8.5's rename left: the original "Account" model had an
 * `INVESTMENT` type that got folded into `CUSTOM` during that
 * migration (see `MIGRATION_2_3`) for lack of a real investment-specific
 * treatment at the time — it's back as a first-class type now that
 * groups (11.2) and inclusion controls (11.4) give it somewhere
 * meaningful to differ from a bank account (e.g. excluded from
 * Spending Power by default expectation, though the person still
 * decides that explicitly per source).
 */
enum class FinancialSourceType(val displayName: String, val isLiability: Boolean) {
    BANK_ACCOUNT("Bank Account", isLiability = false),
    DEBIT_CARD("Debit Card", isLiability = false),
    CREDIT_CARD("Credit Card", isLiability = true),
    CASH("Cash", isLiability = false),
    SAVINGS_ACCOUNT("Savings Account", isLiability = false),
    E_WALLET("E-Wallet", isLiability = false),
    INVESTMENT_ACCOUNT("Investment Account", isLiability = false),
    CUSTOM("Custom", isLiability = false)
}

/**
 * How a source's balance is expected to get updated. This is the
 * foundation the Hybrid Balance Model (Phase 8.5.3) is built on, and
 * exists specifically so [MANUAL] sources today don't need to change
 * shape at all once [ASSISTED] or [SMART] sources ship — a source just
 * changes which mode it's in, the field has been here from day one.
 */
enum class BalanceUpdateMode {
    /** The person types the balance in themselves. Every source today. */
    MANUAL,
    /** Nova suggests an updated balance (e.g. derived from recent transaction activity) and the person confirms or edits it before it's applied. Not implemented yet — see ROADMAP_NEXT.md. */
    ASSISTED,
    /** Balance updates arrive automatically from a real signal (SMS parsing, OCR on a statement/receipt). Not implemented yet — see ROADMAP_NEXT.md. Requires the SMS/Camera/OCR permissions the Permission Center already models. */
    SMART
}
