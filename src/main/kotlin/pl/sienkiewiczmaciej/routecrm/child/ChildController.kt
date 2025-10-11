package pl.sienkiewiczmaciej.routecrm.child

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.sienkiewiczmaciej.routecrm.child.create.CreateChildHandler
import pl.sienkiewiczmaciej.routecrm.child.delete.DeleteChildCommand
import pl.sienkiewiczmaciej.routecrm.child.delete.DeleteChildHandler
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildStatus
import pl.sienkiewiczmaciej.routecrm.child.getbyid.GetChildHandler
import pl.sienkiewiczmaciej.routecrm.child.getbyid.GetChildQuery
import pl.sienkiewiczmaciej.routecrm.child.list.ListChildrenHandler
import pl.sienkiewiczmaciej.routecrm.child.list.ListChildrenQuery
import pl.sienkiewiczmaciej.routecrm.child.update.UpdateChildHandler
import pl.sienkiewiczmaciej.routecrm.shared.api.BaseController

@RestController
@RequestMapping("/api/children")
class ChildController(
    private val createHandler: CreateChildHandler,
    private val listHandler: ListChildrenHandler,
    private val getHandler: GetChildHandler,
    private val updateHandler: UpdateChildHandler,
    private val deleteHandler: DeleteChildHandler
) : BaseController() {

    @PostMapping
    suspend fun create(
        @Valid @RequestBody request: CreateChildRequest
    ): ResponseEntity<ChildResponse> {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId)
        val result = createHandler.handle(principal, command)
        return ResponseEntity.status(CREATED).body(ChildResponse.from(result, principal.companyId))
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false) status: ChildStatus?,
        @PageableDefault(size = 20, sort = ["lastName", "firstName"]) pageable: Pageable
    ): Page<ChildListResponse> {
        val principal = getPrincipal()
        val query = ListChildrenQuery(
            companyId = principal.companyId,
            status = status,
            pageable = pageable,
            guardianId = principal.guardianId
        )
        return listHandler.handle(principal, query).map { ChildListResponse.from(it) }
    }

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: String): ChildDetailResponse {
        val principal = getPrincipal()
        val query = GetChildQuery(
            companyId = principal.companyId,
            id = ChildId.from(id)
        )
        return ChildDetailResponse.from(getHandler.handle(principal, query))
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateChildRequest
    ): UpdateChildResponse {
        val principal = getPrincipal()
        val command = request.toCommand(principal.companyId, ChildId.from(id))
        val result = updateHandler.handle(principal, command)
        return UpdateChildResponse.from(result)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val principal = getPrincipal()
        val command = DeleteChildCommand(
            companyId = principal.companyId,
            id = ChildId.from(id)
        )
        deleteHandler.handle(principal, command)
        return ResponseEntity.status(NO_CONTENT).build()
    }
}