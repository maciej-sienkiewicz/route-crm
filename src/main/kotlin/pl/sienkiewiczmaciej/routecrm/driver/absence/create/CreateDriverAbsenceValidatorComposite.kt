// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/absence/create/CreateDriverAbsenceValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.driver.absence.create

import org.springframework.stereotype.Component
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus

@Component
class CreateDriverAbsenceValidatorComposite(
    private val contextBuilder: CreateDriverAbsenceContextBuilder,
    private val driverActiveValidator: DriverAbsenceDriverActiveValidator
) {
    suspend fun validate(command: CreateDriverAbsenceCommand): CreateDriverAbsenceValidationContext {
        val context = contextBuilder.build(command)

        driverActiveValidator.validate(context)

        return context
    }
}

@Component
class DriverAbsenceDriverActiveValidator {
    fun validate(context: CreateDriverAbsenceValidationContext) {
        require(context.driver.status == DriverStatus.ACTIVE) {
            "Cannot create absence for inactive driver ${context.driver.id.value}"
        }
    }
}