package pl.sienkiewiczmaciej.routecrm.scheduleexception.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

interface ScheduleExceptionRepository {
    suspend fun save(exception: ScheduleException): ScheduleException
    suspend fun findById(companyId: CompanyId, id: ScheduleExceptionId): ScheduleException?
    suspend fun findBySchedule(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        from: LocalDate?,
        to: LocalDate?
    ): List<ScheduleException>
    suspend fun findByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate?,
        to: LocalDate?
    ): List<ScheduleException>
    suspend fun existsByScheduleAndDate(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        date: LocalDate
    ): Boolean
    suspend fun countByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate,
        to: LocalDate
    ): Int
    suspend fun delete(companyId: CompanyId, id: ScheduleExceptionId)
}