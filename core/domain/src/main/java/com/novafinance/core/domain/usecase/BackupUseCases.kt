package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.parseBackupPayload
import com.novafinance.core.domain.model.toJson
import com.novafinance.core.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Both of these finally give [NovaUseCase] (the single-shot base class
 * that sat unused since Phase 1 — every other use case in this codebase
 * needed [NovaFlowUseCase] instead) a real reason to exist: export and
 * import are exactly the "one-shot domain operation" shape it was built
 * for, and its built-in try/catch-to-NovaResult.Error wrapping is what
 * turns [com.novafinance.core.domain.model.InvalidBackupException] into
 * something a ViewModel can branch on without its own try/catch.
 */
class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    dispatcher: CoroutineDispatcher
) : NovaUseCase<Unit, String>(dispatcher) {
    override suspend fun execute(params: Unit): String = backupRepository.buildBackupPayload().toJson()
}

class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    dispatcher: CoroutineDispatcher
) : NovaUseCase<String, Unit>(dispatcher) {
    override suspend fun execute(params: String) {
        val payload = parseBackupPayload(params)
        backupRepository.restoreFromPayload(payload)
    }
}
