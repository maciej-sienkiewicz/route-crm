// routeseries/addchild/AddChildValidatorComposite.kt
package pl.sienkiewiczmaciej.routecrm.routeseries.addchild

import org.springframework.stereotype.Component

@Component
class AddChildValidatorComposite(
    private val contextBuilder: AddChildValidationContextBuilder,
    private val seriesStatusValidator: AddChildSeriesStatusValidator,
    private val scheduleOwnershipValidator: AddChildScheduleOwnershipValidator,
    private val childStatusValidator: AddChildStatusValidator,
    private val stopOrderValidator: AddChildStopOrderValidator,
    private val effectiveDatesValidator: AddChildEffectiveDatesValidator
) {
    suspend fun validate(command: AddChildToRouteSeriesCommand): AddChildValidationContext {
        stopOrderValidator.validate(command)

        val context = contextBuilder.build(command)

        seriesStatusValidator.validate(context)
        scheduleOwnershipValidator.validate(context, command)
        childStatusValidator.validate(context)
        effectiveDatesValidator.validate(command, context)

        return context
    }
}