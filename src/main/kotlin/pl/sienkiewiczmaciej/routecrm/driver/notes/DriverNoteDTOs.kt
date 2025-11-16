// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/driver/notes/DriverNoteDTOs.kt
package pl.sienkiewiczmaciej.routecrm.driver.notes

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pl.sienkiewiczmaciej.routecrm.driver.domain.DriverId
import pl.sienkiewiczmaciej.routecrm.driver.notes.create.CreateDriverNoteCommand
import pl.sienkiewiczmaciej.routecrm.driver.notes.create.CreateDriverNoteResult
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteCategory
import pl.sienkiewiczmaciej.routecrm.driver.notes.domain.DriverNoteId
import pl.sienkiewiczmaciej.routecrm.driver.notes.list.DriverNoteListItem
import pl.sienkiewiczmaciej.routecrm.driver.notes.update.UpdateDriverNoteCommand
import pl.sienkiewiczmaciej.routecrm.driver.notes.update.UpdateDriverNoteResult
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.Instant

data class CreateDriverNoteRequest(
    @field:NotNull(message = "Category is required")
    val category: DriverNoteCategory,

    @field:NotBlank(message = "Content is required")
    @field:Size(max = 5000)
    val content: String
) {
    fun toCommand(companyId: CompanyId, driverId: DriverId) = CreateDriverNoteCommand(
        companyId = companyId,
        driverId = driverId,
        category = category,
        content = content
    )
}

data class UpdateDriverNoteRequest(
    @field:NotNull(message = "Category is required")
    val category: DriverNoteCategory,

    @field:NotBlank(message = "Content is required")
    @field:Size(max = 5000)
    val content: String
) {
    fun toCommand(companyId: CompanyId, id: DriverNoteId) = UpdateDriverNoteCommand(
        companyId = companyId,
        id = id,
        category = category,
        content = content
    )
}

data class DriverNoteResponse(
    val id: String,
    val category: DriverNoteCategory,
    val content: String,
    val createdByName: String,
    val createdAt: Instant
) {
    companion object {
        fun from(result: CreateDriverNoteResult) = DriverNoteResponse(
            id = result.id.value,
            category = result.category,
            content = result.content,
            createdByName = result.createdByName,
            createdAt = result.createdAt
        )

        fun from(item: DriverNoteListItem) = DriverNoteResponse(
            id = item.id.value,
            category = item.category,
            content = item.content,
            createdByName = item.createdByName,
            createdAt = item.createdAt
        )

        fun fromUpdate(result: UpdateDriverNoteResult) = DriverNoteResponse(
            id = result.id.value,
            category = result.category,
            content = result.content,
            createdByName = "System",
            createdAt = Instant.now()
        )
    }
}

data class DriverNoteListResponse(
    val notes: List<DriverNoteResponse>
) {
    companion object {
        fun from(notes: List<DriverNoteListItem>) = DriverNoteListResponse(
            notes = notes.map { DriverNoteResponse.from(it) }
        )
    }
}