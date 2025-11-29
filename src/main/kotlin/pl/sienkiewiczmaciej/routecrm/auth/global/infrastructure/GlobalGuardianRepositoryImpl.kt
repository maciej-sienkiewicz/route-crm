package pl.sienkiewiczmaciej.routecrm.auth.global.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.auth.global.*

@Repository
class GlobalGuardianRepositoryImpl(
    private val jpaRepository: GlobalGuardianJpaRepository
) : GlobalGuardianRepository {

    override suspend fun save(guardian: GlobalGuardian): GlobalGuardian = withContext(Dispatchers.IO) {
        val entity = GlobalGuardianEntity.fromDomain(guardian)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(id: GlobalGuardianId): GlobalGuardian? = withContext(Dispatchers.IO) {
        jpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override suspend fun findByEmail(email: String): GlobalGuardian? = withContext(Dispatchers.IO) {
        jpaRepository.findByEmail(email.lowercase().trim())?.toDomain()
    }

    override suspend fun existsByEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByEmail(email.lowercase().trim())
    }

    override suspend fun delete(id: GlobalGuardianId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteById(id.value)
        }
    }
}

@Repository
class EmailVerificationRepositoryImpl(
    private val jpaRepository: EmailVerificationJpaRepository
) : EmailVerificationRepository {

    override suspend fun save(verification: EmailVerification): EmailVerification =
        withContext(Dispatchers.IO) {
            val entity = EmailVerificationEntity.fromDomain(verification)
            jpaRepository.save(entity).toDomain()
        }

    override suspend fun findByToken(token: VerificationToken): EmailVerification? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByToken(token.value)?.toDomain()
        }

    override suspend fun findActiveByGuardian(guardianId: GlobalGuardianId): EmailVerification? =
        withContext(Dispatchers.IO) {
            jpaRepository.findActiveByGuardian(guardianId.value)?.toDomain()
        }

    override suspend fun deleteByGuardian(guardianId: GlobalGuardianId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByGlobalGuardianId(guardianId.value)
        }
    }
}

@Repository
class PasswordResetTokenRepositoryImpl(
    private val jpaRepository: PasswordResetTokenJpaRepository
) : PasswordResetTokenRepository {

    override suspend fun save(token: PasswordResetToken): PasswordResetToken =
        withContext(Dispatchers.IO) {
            val entity = PasswordResetTokenEntity.fromDomain(token)
            jpaRepository.save(entity).toDomain()
        }

    override suspend fun findByToken(token: VerificationToken): PasswordResetToken? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByToken(token.value)?.toDomain()
        }

    override suspend fun findActiveByGuardian(guardianId: GlobalGuardianId): PasswordResetToken? =
        withContext(Dispatchers.IO) {
            jpaRepository.findActiveByGuardian(guardianId.value)?.toDomain()
        }

    override suspend fun deleteByGuardian(guardianId: GlobalGuardianId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByGlobalGuardianId(guardianId.value)
        }
    }
}