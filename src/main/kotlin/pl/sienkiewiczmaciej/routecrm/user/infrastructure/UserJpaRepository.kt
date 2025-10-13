package pl.sienkiewiczmaciej.routecrm.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun findByEmail(email: String): UserEntity?
    fun findByCompanyIdAndEmail(companyId: String, email: String): UserEntity?
}