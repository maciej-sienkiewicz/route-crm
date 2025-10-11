package pl.sienkiewiczmaciej.routecrm.schedule.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ScheduleJpaRepository : JpaRepository<ScheduleEntity, String> {

    fun findByIdAndCompanyId(id: String, companyId: String): ScheduleEntity?

    fun findByCompanyIdAndChildId(companyId: String, childId: String): List<ScheduleEntity>

    @Query("""
        SELECT COUNT(s) FROM ScheduleEntity s 
        WHERE s.companyId = :companyId 
        AND s.childId = :childId 
        AND s.active = true
    """)
    fun countActiveByCompanyIdAndChildId(companyId: String, childId: String): Int

    fun deleteByIdAndCompanyId(id: String, companyId: String)
}