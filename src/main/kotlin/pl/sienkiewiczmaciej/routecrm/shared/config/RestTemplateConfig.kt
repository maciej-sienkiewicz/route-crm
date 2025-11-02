package pl.sienkiewiczmaciej.routecrm.shared.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Konfiguracja RestTemplate z odpowiednimi timeoutami dla zewnętrznych API.
 *
 * Opcjonalna - jeśli już istnieje konfiguracja RestTemplate w projekcie,
 * można pominąć ten plik lub zmodyfikować istniejącą konfigurację.
 */
@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(5))  // Timeout połączenia
            .readTimeout(Duration.ofSeconds(10))     // Timeout odczytu
            .build()
    }
}