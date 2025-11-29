package pl.sienkiewiczmaciej.routecrm.auth.register

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.auth.global.*
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.api.NotFoundException
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class VerifyEmailCommand(
    val token: VerificationToken,
    val companyId: CompanyId,
    val pendingData: PendingGuardianData?
)

data class VerifyEmailResult(
    val globalGuardianId: GlobalGuardianId,
    val guardianId: pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId?,
    val email: String,
    val profileCreated: Boolean
)

class VerificationTokenNotFoundException : NotFoundException("Verification token not found or expired")

@Component
class VerifyEmailHandler(
    private val emailVerificationRepository: EmailVerificationRepository,
    private val globalGuardianRepository: GlobalGuardianRepository,
    private val guardianRepository: GuardianRepository
) {
    @Transactional
    suspend fun handle(command: VerifyEmailCommand): VerifyEmailResult {
        val verification = emailVerificationRepository.findByToken(command.token)
            ?: throw VerificationTokenNotFoundException()

        require(!verification.isExpired()) { "Verification token has expired" }
        require(!verification.isVerified()) { "Email already verified" }

        val globalGuardian = globalGuardianRepository.findById(verification.globalGuardianId)
            ?: throw NotFoundException("Guardian not found")

        val verifiedGuardian = globalGuardian.verifyEmail()
        globalGuardianRepository.save(verifiedGuardian)

        val verifiedToken = verification.verify()
        emailVerificationRepository.save(verifiedToken)

        var guardianId: pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId? = null
        var profileCreated = false

        if (command.pendingData != null) {
            val existingProfile = guardianRepository.findByCompanyAndEmail(
                command.companyId,
                globalGuardian.email
            )

            if (existingProfile == null) {
                val newProfile = Guardian.create(
                    companyId = command.companyId,
                    firstName = command.pendingData.firstName,
                    lastName = command.pendingData.lastName,
                    email = globalGuardian.email,
                    phone = command.pendingData.phone,
                    address = command.pendingData.address
                )
                val saved = guardianRepository.save(newProfile)
                guardianId = saved.id
                profileCreated = true
            }
        }

        return VerifyEmailResult(
            globalGuardianId = verifiedGuardian.id,
            guardianId = guardianId,
            email = verifiedGuardian.email,
            profileCreated = profileCreated
        )
    }
}