package pl.sienkiewiczmaciej.routecrm.scheduleexception.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Repository
import pl.sienkiewiczmaciej.routecrm.child.domain.ChildId
import pl.sienkiewiczmaciej.routecrm.schedule.domain.ScheduleId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleException
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionId
import pl.sienkiewiczmaciej.routecrm.scheduleexception.domain.ScheduleExceptionRepository
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import java.time.LocalDate

@Repository
class ScheduleExceptionRepositoryImpl(
    private val jpaRepository: ScheduleExceptionJpaRepository
) : ScheduleExceptionRepository {

    override suspend fun save(exception: ScheduleException): ScheduleException =
        withContext(Dispatchers.IO) {
            val entity = ScheduleExceptionEntity.fromDomain(exception)
            jpaRepository.save(entity).toDomain()
        }

    override suspend fun findById(companyId: CompanyId, id: ScheduleExceptionId): ScheduleException? =
        withContext(Dispatchers.IO) {
            jpaRepository.findByIdAndCompanyId(id.value, companyId.value)?.toDomain()
        }

    override suspend fun findBySchedule(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        from: LocalDate?,
        to: LocalDate?
    ): List<ScheduleException> = withContext(Dispatchers.IO) {
        jpaRepository.findByScheduleIdAndDateRange(
            companyId.value,
            scheduleId.value,
            from,
            to
        ).map { it.toDomain() }
    }

    override suspend fun findByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate?,
        to: LocalDate?
    ): List<ScheduleException> = withContext(Dispatchers.IO) {
        jpaRepository.findByChildIdAndDateRange(
            companyId.value,
            childId.value,
            from,
            to
        ).map { it.toDomain() }
    }

    override suspend fun existsByScheduleAndDate(
        companyId: CompanyId,
        scheduleId: ScheduleId,
        date: LocalDate
    ): Boolean = withContext(Dispatchers.IO) {
        jpaRepository.existsByCompanyIdAndScheduleIdAndExceptionDate(
            companyId.value,
            scheduleId.value,
            date
        )
    }

    override suspend fun countByChild(
        companyId: CompanyId,
        childId: ChildId,
        from: LocalDate,
        to: LocalDate
    ): Int = withContext(Dispatchers.IO) {
        jpaRepository.countByChildIdAndDateRange(
            companyId.value,
            childId.value,
            from,
            to
        )
    }

    override suspend fun delete(companyId: CompanyId, id: ScheduleExceptionId) {
        withContext(Dispatchers.IO) {
            jpaRepository.deleteByIdAndCompanyId(id.value, companyId.value)
        }
    }
}