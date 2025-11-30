package pl.sienkiewiczmaciej.routecrm.driverauth.domain

import java.util.*

@JvmInline
value class DriverCredentialsId(val value: String) {
    companion object {
        fun generate() = DriverCredentialsId("DCRED-${UUID.randomUUID()}")
        fun from(value: String) = DriverCredentialsId(value)
    }
}