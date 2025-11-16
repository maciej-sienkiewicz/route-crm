// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/create/CreateDriverAbsenceContextBuilder.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.create

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.absence.domain.DriverAbsenceRepository
import pl.sienkiewiczmaciej.routecrm.driver.domain.Driver
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverRepository
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.DriverNotFoundException

data class CreateDriverAbsenceValidationContext(
    val driver: Driver,
    val absenceRepository: DriverAbsenceRepository
)

@Component
class CreateDriverAbsenceContextBuilder(
    private val driverRepository: DriverRepository,
    private val absenceRepository: DriverAbsenceRepository
) {
    suspend fun build(command: CreateDriverAbsenceCommand): CreateDriverAbsenceValidationContext = coroutineScope {
        val driverDeferred = async {
            driverRepository.findById(command.companyId, command.driverId)
                ?: throw DriverNotFoundException(command.driverId)
        }

        CreateDriverAbsenceValidationContext(
            driver = driverDeferred.await(),
            absenceRepository = absenceRepository
        )
    }
}