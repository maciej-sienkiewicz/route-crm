package pl.sienkiewiczmaciej.routecrm.guardian.domain

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.auth.global.GlobalGuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianJpaRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Service
class GuardianService(
    private val guardianJpaRepository: GuardianJpaRepository
) {
    suspend fun linkGuardianToGlobal(
        guardianId: GuardianId,
        globalGuardianId: GlobalGuardianId
    ) {
        val entity = guardianJpaRepository.findById(guardianId.value).orElse(null)
            ?: throw IllegalArgumentException("Guardian not found")

        val updated = pl.sienkiewiczmaciej.routecrm.guardian.infrastructure.GuardianEntity(
            id = entity.id,
            companyId = entity.companyId,
            globalGuardianId = globalGuardianId.value,
            firstName = entity.firstName,
            lastName = entity.lastName,
            email = entity.email,
            phone = entity.phone,
            address = entity.address
        )

        guardianJpaRepository.save(updated)
    }

    suspend fun findByGlobalGuardianId(
        companyId: CompanyId,
        globalGuardianId: GlobalGuardianId
    ): Guardian? {
        return guardianJpaRepository.findByCompanyIdAndGlobalGuardianId(
            companyId.value,
            globalGuardianId.value
        )?.toDomain()
    }
}