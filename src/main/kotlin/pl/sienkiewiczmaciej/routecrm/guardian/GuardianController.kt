package pl.sienkiewiczmaciej.routecrm.guardian

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.guardian.create.CreateGuardianHandler
import pl.sienkiewiczmaciej.routecrm.guardian.domain.GuardianId
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GetGuardianHandler
import pl.sienkiewiczmaciej.routecrm.guardian.getbyid.GetGuardianQuery
import pl.sienkiewiczmaciej.routecrm.guardian.list.ListGuardiansHandler
import pl.sienkiewiczmaciej.routecrm.guardian.list.ListGuardiansQuery
import pl.sienkiewiczmaciej.routecrm.guardian.update.UpdateGuardianHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/guardians")
class GuardianController(
    private val createHandler: CreateGuardianHandler,
    private val listHandler: ListGuardiansHandler,
    private val getHandler: GetGuardianHandler,
    private val updateHandler: UpdateGuardianHandler
) : BaseController() {

    @PostMapping
    suspend fun create(
        @Valid @RequestBody request: CreateGuardianRequest
    ): ResponseEntity<GuardianResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId)
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(GuardianResponse.from(result))
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20, sort = ["lastName", "firstName"]) pageable: Pageable
    ): Page<GuardianListResponse> {
        val principal = getPrincipal()
        val query = ListGuardiansQuery(
            companyId = principal.companyId,
            search = search,
            pageable = pageable
        )
        return listHandler.handle(principal, query).map { GuardianListResponse.from(it) }
    }

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: String): GuardianDetailResponse {
        val principal = getPrincipal()
        val query = GetGuardianQuery(
            companyId = principal.companyId,
            id = GuardianId.from(id)
        )
        return GuardianDetailResponse.from(getHandler.handle(principal, query))
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateGuardianRequest
    ): UpdateGuardianResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, GuardianId.from(id))
        val result = updateHandler.handle(principal, command)
        return UpdateGuardianResponse.from(result)
    }
}