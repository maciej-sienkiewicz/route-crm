package pl.sienkiewiczmaciej.routecrm.driverauth.service

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class PinGenerator {
    private val random = SecureRandom()

    fun generate6DigitPin(): String {
        val pin = random.nextInt(900000) + 100000
        return pin.toString()
    }
}