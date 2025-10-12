package pl.sienkiewiczmaciej.routecrm.schedule.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.Schedule
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId

@Repository
class ScheduleRepositoryImpl(
    private val jpaRepository: ScheduleJpaRepository
) : ScheduleRepository {

    override suspend fun save(schedule: Schedule): Schedule = withContext(Dispatchers.IO) {
        val entity = ScheduleEntity.fromDomain(schedule)
        jpaRepository.save(entity).toDomain()
    }

    override suspend fun findById(companyId: CompanyId, id: ScheduleId): Schedule? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findByChild(companyId: CompanyId, childId: ChildId): List<Schedule> =
        withContext(Dispatchers.IO) {
            jpaRepository.findByCompanyIdAndChildId(companyId.value, childId.value)
                .map { it.toDomain() }
        }

    override suspend fun countActiveByChild(companyId: CompanyId, childId: ChildId): Int =
        withContext(Dispatchers.IO) {
            jpaRepository.countActiveByCompanyIdAndChildId(companyId.value, childId.value)
        }

    @Transactional
    override suspend fun delete(companyId: CompanyId, id: ScheduleId) {
        withContext(Dispatchers.IO) {
            val entity = jpaRepository.findByIdAndCompanyId(id.value, companyId.value)
                ?: return@withContext
            jpaRepository.delete(entity)
        }
    }
}