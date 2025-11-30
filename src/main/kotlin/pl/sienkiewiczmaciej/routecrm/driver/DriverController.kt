package pl.sienkiewiczmaciej.routecrm.driver

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.driver.create.CreateDriverHandler
import pl.sienkiewiczmaciej.routecrm.driver.delete.DeleteDriverCommand
import pl.sienkiewiczmaciej.routecrm.driver.delete.DeleteDriverHandler
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverStatus
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.GetDriverHandler
import pl.sienkiewiczmaciej.routecrm.driver.getbyid.GetDriverQuery
import pl.sienkiewiczmaciej.routecrm.driver.list.ListDriversHandler
import pl.sienkiewiczmaciej.routecrm.driver.list.ListDriversQuery
import pl.sienkiewiczmaciej.routecrm.driver.update.UpdateDriverHandler
import pl.sienkiewiczmaciej.routecrm.driverauth.resetpassword.DriverResetPasswordHandler
import pl.sienkiewiczmaciej.routecrm.driverauth.resetpassword.ResetPasswordCommand
import pl.sienkiewiczmaciej.routecrm.driverauth.unlock.UnlockAccountCommand
import pl.sienkiewiczmaciej.routecrm.driverauth.unlock.UnlockAccountHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/drivers")
class DriverController(
    private val createHandler: CreateDriverHandler,
    private val listHandler: ListDriversHandler,
    private val getHandler: GetDriverHandler,
    private val updateHandler: UpdateDriverHandler,
    private val deleteHandler: DeleteDriverHandler,
    private val unlockAccountHandler: UnlockAccountHandler,
    private val driverResetPasswordHandler: DriverResetPasswordHandler
) : BaseController() {

    @PostMapping
    suspend fun create(
        @Valid @RequestBody request: CreateDriverRequest
    ): ResponseEntity<DriverResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId)
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(DriverResponse.from(result, command))
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false) status: DriverStatus?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20, sort = ["lastName", "firstName"]) pageable: Pageable
    ): Page<DriverListResponse> {
        val principal = getPrincipal()
        val query = ListDriversQuery(
            companyId = principal.companyId,
            status = status,
            search = search,
            pageable = pageable
        )
        return listHandler.handle(principal, query).map { DriverListResponse.from(it) }
    }

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: String): DriverDetailResponse {
        val principal = getPrincipal()
        val query = GetDriverQuery(
            companyId = principal.companyId,
            id = DriverId.from(id)
        )
        return DriverDetailResponse.from(getHandler.handle(principal, query))
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateDriverRequest
    ): UpdateDriverResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, DriverId.from(id))
        val result = updateHandler.handle(principal, command)
        return UpdateDriverResponse.from(result)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteDriverCommand(
            companyId = principal.companyId,
            id = DriverId.from(id)
        )
        deleteHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }

    @PostMapping("/{id}/unlock")
    suspend fun unlockAccount(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val principal = getPrincipal()
        val command = UnlockAccountCommand(
            companyId = principal.companyId,
            driverId = DriverId.from(id)
        )
        unlockAccountHandler.handle(principal, command)
        return ResponseEntity.ok(mapOf("message" to "Account unlocked successfully"))
    }

    @PostMapping("/{id}/reset-password")
    suspend fun resetPassword(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val principal = getPrincipal()
        val command = ResetPasswordCommand(
            companyId = principal.companyId,
            driverId = DriverId.from(id)
        )
        val result = driverResetPasswordHandler.handle(principal, command)
        return ResponseEntity.ok(
            mapOf(
                "message" to "Password reset successfully",
                "newPin" to result.newPin
            )
        )
    }
}