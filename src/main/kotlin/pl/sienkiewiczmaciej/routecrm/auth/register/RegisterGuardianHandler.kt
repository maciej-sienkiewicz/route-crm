package pl.sienkiewiczmaciej.routecrm.auth.register

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.auth.global.*
import pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

data class RegisterGuardianCommand(
    val companyId: CompanyId,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val address: Address?
)

sealed class RegisterGuardianResult {
    data class NewGuardianCreated(
        val globalGuardianId: GlobalGuardianId,
        val guardianId: pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId,
        val requiresEmailVerification: Boolean
    ) : RegisterGuardianResult()

    data class ExistingGuardianPendingVerification(
        val globalGuardianId: GlobalGuardianId,
        val email: String,
        val verificationSent: Boolean
    ) : RegisterGuardianResult()
}

@Component
class RegisterGuardianHandler(
    private val globalGuardianRepository: GlobalGuardianRepository,
    private val guardianRepository: GuardianRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val emailService: RegistrationEmailService,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    suspend fun handle(command: RegisterGuardianCommand): RegisterGuardianResult {
        val existingGlobal = globalGuardianRepository.findByEmail(command.email)

        return if (existingGlobal == null) {
            handleNewGuardian(command)
        } else {
            handleExistingGuardian(existingGlobal, command)
        }
    }

    private suspend fun handleNewGuardian(command: RegisterGuardianCommand): RegisterGuardianResult {
        val globalGuardian = GlobalGuardian.create(
            email = command.email,
            password = command.password,
            passwordEncoder = passwordEncoder,
            phone = command.phone
        )
        val savedGlobal = globalGuardianRepository.save(globalGuardian)

        val guardian = Guardian.create(
            companyId = command.companyId,
            firstName = command.firstName,
            lastName = command.lastName,
            email = command.email,
            phone = command.phone,
            address = command.address
        )
        val savedGuardian = guardianRepository.save(guardian)

        updateGuardianWithGlobalId(savedGuardian, savedGlobal.id)

        val verification = EmailVerification.create(savedGlobal.id, command.email)
        emailVerificationRepository.save(verification)

        emailService.sendVerificationEmail(
            email = command.email,
            firstName = command.firstName,
            token = verification.token,
            companyId = command.companyId
        )

        return RegisterGuardianResult.NewGuardianCreated(
            globalGuardianId = savedGlobal.id,
            guardianId = savedGuardian.id,
            requiresEmailVerification = true
        )
    }

    private suspend fun handleExistingGuardian(
        existingGlobal: GlobalGuardian,
        command: RegisterGuardianCommand
    ): RegisterGuardianResult {
        val existingProfile = guardianRepository.findByCompanyAndEmail(
            command.companyId,
            command.email
        )

        if (existingProfile != null) {
            throw IllegalArgumentException("Guardian already has an account in this company")
        }

        val verification = EmailVerification.create(existingGlobal.id, command.email)
        emailVerificationRepository.save(verification)

        emailService.sendCompanyLinkingEmail(
            email = command.email,
            token = verification.token,
            companyId = command.companyId,
            pendingData = PendingGuardianData(
                firstName = command.firstName,
                lastName = command.lastName,
                phone = command.phone,
                address = command.address
            )
        )

        return RegisterGuardianResult.ExistingGuardianPendingVerification(
            globalGuardianId = existingGlobal.id,
            email = command.email,
            verificationSent = true
        )
    }

    private suspend fun updateGuardianWithGlobalId(
        guardian: pl.sienkiewiczmaciej.routecrm.guardian.domain.Guardian,
        globalId: GlobalGuardianId
    ) {
    }
}

data class PendingGuardianData(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val address: Address?
)