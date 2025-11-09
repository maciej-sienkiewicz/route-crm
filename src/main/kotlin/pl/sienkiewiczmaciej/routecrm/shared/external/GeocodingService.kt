package pl.sienkiewiczmaciej.routecrm.shared.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address

data class GeocodingResult(
    val latitude: Double,
    val longitude: Double
)

@JsonIgnoreProperties(ignoreUnknown = true)  // ← WAŻNE: Ignoruj nieznane pola z API
data class GoogleGeocodingResponse(
    val results: List<GoogleGeocodingResult>,
    val status: String,
    @JsonProperty("error_message")  // ← DODANE: Obsługa pola error_message
    val errorMessage: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleGeocodingResult(
    val geometry: GoogleGeometry
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleGeometry(
    val location: GoogleLocation
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleLocation(
    val lat: Double,
    val lng: Double
)

@Service
class GeocodingService(
    private val apiKey: String = "AIzaSyAr0qHze3moiMPHo-cwv171b8luH-anyXA",
    private val restTemplate: RestTemplate = RestTemplate()
) {
    private val logger = LoggerFactory.getLogger(GeocodingService::class.java)

    suspend fun geocodeAddress(address: Address): GeocodingResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            logger.warn("Google Maps API key is not configured")
            return@withContext null
        }

        try {
            val formattedAddress = formatAddress(address)
            val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$formattedAddress&key=$apiKey"

            logger.debug("Geocoding address: $formattedAddress")

            val response = restTemplate.getForObject<GoogleGeocodingResponse>(url)

            when (response.status) {
                "OK" -> {
                    if (response.results.isNotEmpty()) {
                        val location = response.results[0].geometry.location
                        logger.debug("Geocoding successful: lat=${location.lat}, lng=${location.lng}")
                        GeocodingResult(
                            latitude = location.lat,
                            longitude = location.lng
                        )
                    } else {
                        logger.warn("Geocoding returned OK but no results")
                        null
                    }
                }
                "ZERO_RESULTS" -> {
                    logger.warn("Geocoding found no results for address: $formattedAddress")
                    null
                }
                "OVER_QUERY_LIMIT" -> {
                    logger.error("Google API quota exceeded!")
                    null
                }
                "REQUEST_DENIED" -> {
                    logger.error("Google API request denied. Error: ${response.errorMessage}")
                    null
                }
                "INVALID_REQUEST" -> {
                    logger.warn("Invalid geocoding request for address: $formattedAddress. Error: ${response.errorMessage}")
                    null
                }
                else -> {
                    logger.warn("Geocoding failed with status: ${response.status}. Error: ${response.errorMessage}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error geocoding address: ${e.message}", e)
            null
        }
    }

    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()

        // Ulica i numer domu
        parts.add("${address.street} ${address.houseNumber}")

        // Numer mieszkania (opcjonalnie)
        address.apartmentNumber?.let {
            parts.add(it)
        }

        // Kod pocztowy i miasto
        parts.add("${address.postalCode} ${address.city}")

        // Kraj (zakładam Polskę)
        parts.add("Poland")

        return parts.joinToString(", ").replace(" ", "+")
    }
}