package pl.sienkiewiczmaciej.routecrm.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface CompanyJpaRepository : JpaRepository<CompanyEntity, String>