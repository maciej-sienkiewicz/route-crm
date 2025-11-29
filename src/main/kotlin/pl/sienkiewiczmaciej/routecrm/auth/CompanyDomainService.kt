package pl.sienkiewiczmaciej.routecrm.auth

import org.springframework.stereotype.Service
import pl.sienkiewiczmaciej.routecrm.shared.domain.CompanyId
import pl.sienkiewiczmaciej.routecrm.user.infrastructure.CompanyJpaRepository

@Service
class CompanyDomainService(
    private val companyRepository: CompanyJpaRepository
) {
    fun getCompanyIdFromHost(host: String): CompanyId {
        val companies = companyRepository.findAll()
        return if (companies.isNotEmpty()) {
            CompanyId.from(companies.first().id)
        } else {
            CompanyId.generate()
        }
    }
}