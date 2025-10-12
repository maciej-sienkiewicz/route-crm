package pl.sienkiewiczmaciej.routecrm.vehicle

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController
import pl.sienkiewiczmaciej.routecrm.vehicle.create.CreateVehicleHandler
import pl.sienkiewiczmaciej.routecrm.vehicle.delete.DeleteVehicleCommand
import pl.sienkiewiczmaciej.routecrm.vehicle.delete.DeleteVehicleHandler
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleId
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleStatus
import pl.sienkiewiczmaciej.routecrm.vehicle.domain.VehicleType
import pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.GetVehicleHandler
import pl.sienkiewiczmaciej.routecrm.vehicle.getbyid.GetVehicleQuery
import pl.sienkiewiczmaciej.routecrm.vehicle.list.ListVehiclesHandler
import pl.sienkiewiczmaciej.routecrm.vehicle.list.ListVehiclesQuery
import pl.sienkiewiczmaciej.routecrm.vehicle.update.UpdateVehicleHandler

@RestController
@RequestMapping("/api/vehicles")
class VehicleController(
    private val createHandler: CreateVehicleHandler,
    private val listHandler: ListVehiclesHandler,
    private val getHandler: GetVehicleHandler,
    private val updateHandler: UpdateVehicleHandler,
    private val deleteHandler: DeleteVehicleHandler
) : BaseController() {

    @PostMapping
    suspend fun create(
        @Valid @RequestBody request: CreateVehicleRequest
    ): ResponseEntity<VehicleResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId)
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(VehicleResponse.from(result, command))
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false) status: VehicleStatus?,
        @RequestParam(required = false) vehicleType: VehicleType?,
        @PageableDefault(size = 20, sort = ["registrationNumber"]) pageable: Pageable
    ): Page<VehicleListResponse> {
        val principal = getPrincipal()
        val query = ListVehiclesQuery(
            companyId = principal.companyId,
            status = status,
            vehicleType = vehicleType,
            pageable = pageable
        )
        return listHandler.handle(principal, query).map { VehicleListResponse.from(it) }
    }

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: String): VehicleDetailResponse {
        val principal = getPrincipal()
        val query = GetVehicleQuery(
            companyId = principal.companyId,
            id = VehicleId.from(id)
        )
        return VehicleDetailResponse.from(getHandler.handle(principal, query))
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateVehicleRequest
    ): UpdateVehicleResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, VehicleId.from(id))
        val result = updateHandler.handle(principal, command)
        return UpdateVehicleResponse.from(result)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteVehicleCommand(
            companyId = principal.companyId,
            id = VehicleId.from(id)
        )
        deleteHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }
}