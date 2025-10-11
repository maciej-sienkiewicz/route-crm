package pl.sienkiewiczmaciej.routecrm.schedule.domain

import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

interface ScheduleRepository {
    suspend fun save(schedule: Schedule): Schedule
    suspend fun findById(companyId: CompanyId, id: ScheduleId): Schedule?
    suspend fun findByChild(companyId: CompanyId, childId: ChildId): List<Schedule>
    suspend fun countActiveByChild(companyId: CompanyId, childId: ChildId): Int
    suspend fun delete(companyId: CompanyId, id: ScheduleId)
}