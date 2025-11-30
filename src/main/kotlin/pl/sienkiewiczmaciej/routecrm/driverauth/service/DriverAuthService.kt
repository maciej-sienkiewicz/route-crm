package pl.sienkiewiczmaciej.routecrm.driverauth.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentials
import pl.sienkiewiczmaciej.routecrm.driverauth.domain.DriverCredentialsRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant
import java.time.temporal.ChronoUnit

data class DriverActivationResult(
    val driverId: DriverId,
    val activationPin: String,
    val expiresAt: Instant
)

@Service
class DriverAuthService(
    private val credentialsRepository: DriverCredentialsRepository,
    private val pinGenerator: PinGenerator,
    private val passwordEncoder: PasswordEncoder
) {
    suspend fun createCredentialsForNewDriver(
        companyId: CompanyId,
        driverId: DriverId,
        phoneNumber: String
    ): DriverActivationResult {
        if (credentialsRepository.existsByPhoneNumber(companyId, phoneNumber)) {
            throw IllegalArgumentException("Credentials already exist for phone number $phoneNumber")
        }

        val activationPin = pinGenerator.generate6DigitPin()
        val hashedPin = passwordEncoder.encode(activationPin)

        val credentials = DriverCredentials.createForNewDriver(
            driverId = driverId,
            companyId = companyId,
            phoneNumber = phoneNumber,
            activationPin = activationPin,
            hashedPin = hashedPin
        )

        credentialsRepository.save(credentials)

        return DriverActivationResult(
            driverId = driverId,
            activationPin = activationPin,
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
        )
    }

    suspend fun unlockAccount(companyId: CompanyId, driverId: DriverId) {
        val credentials = credentialsRepository.findByDriverId(companyId, driverId)
            ?: throw DriverCredentialsNotFoundException(driverId)

        val unlocked = credentials.unlock()
        credentialsRepository.save(unlocked)
    }

    suspend fun suspendAccount(companyId: CompanyId, driverId: DriverId) {
        val credentials = credentialsRepository.findByDriverId(companyId, driverId)
            ?: throw DriverCredentialsNotFoundException(driverId)

        val suspended = credentials.suspend()
        credentialsRepository.save(suspended)
    }

    suspend fun unsuspendAccount(companyId: CompanyId, driverId: DriverId) {
        val credentials = credentialsRepository.findByDriverId(companyId, driverId)
            ?: throw DriverCredentialsNotFoundException(driverId)

        val unsuspended = credentials.unsuspend()
        credentialsRepository.save(unsuspended)
    }

    suspend fun resetPassword(companyId: CompanyId, driverId: DriverId): String {
        val credentials = credentialsRepository.findByDriverId(companyId, driverId)
            ?: throw DriverCredentialsNotFoundException(driverId)

        val newPin = pinGenerator.generate6DigitPin()
        val hashedPin = passwordEncoder.encode(newPin)

        val reset = credentials.resetPassword(newPin, hashedPin)
        credentialsRepository.save(reset)

        return newPin
    }

    suspend fun deleteCredentials(companyId: CompanyId, driverId: DriverId) {
        val credentials = credentialsRepository.findByDriverId(companyId, driverId)
            ?: return

        credentialsRepository.delete(credentials.id)
    }
}

class DriverCredentialsNotFoundException(driverId: DriverId) :
    RuntimeException("Driver credentials not found for driver ${driverId.value}")