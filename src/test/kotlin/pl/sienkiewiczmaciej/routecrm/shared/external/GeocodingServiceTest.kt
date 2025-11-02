package pl.sienkiewiczmaciej.routecrm.shared.external

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.web.client.RestTemplate
import pl.sienkiewiczmaciej.routecrm.shared.domain.Address

class GeocodingServiceTest {

    @Test
    fun `should return null when API key is not configured`() = runTest {
        // given
        val restTemplate = mockk<RestTemplate>()
        val service = GeocodingService(apiKey = "", restTemplate = restTemplate)
        val address = Address(
            street = "Marszałkowska",
            houseNumber = "1",
            apartmentNumber = null,
            postalCode = "00-001",
            city = "Warszawa"
        )

        // when
        val result = service.geocodeAddress(address)

        // then
        assertNull(result)
    }

    @Test
    fun `should geocode address successfully`() = runTest {
        // given
        val restTemplate = mockk<RestTemplate>()
        val service = GeocodingService(apiKey = "test-key", restTemplate = restTemplate)

        val mockResponse = GoogleGeocodingResponse(
            results = listOf(
                GoogleGeocodingResult(
                    geometry = GoogleGeometry(
                        location = GoogleLocation(
                            lat = 52.2297,
                            lng = 21.0122
                        )
                    )
                )
            ),
            status = "OK"
        )

        every {
            restTemplate.getForObject(any<String>(), GoogleGeocodingResponse::class.java)
        } returns mockResponse

        val address = Address(
            street = "Marszałkowska",
            houseNumber = "1",
            apartmentNumber = null,
            postalCode = "00-001",
            city = "Warszawa"
        )

        // when
        val result = service.geocodeAddress(address)

        // then
        assertNotNull(result)
        assertEquals(52.2297, result?.latitude)
        assertEquals(21.0122, result?.longitude)
    }

    @Test
    fun `should return null when geocoding fails`() = runTest {
        // given
        val restTemplate = mockk<RestTemplate>()
        val service = GeocodingService(apiKey = "test-key", restTemplate = restTemplate)

        val mockResponse = GoogleGeocodingResponse(
            results = emptyList(),
            status = "ZERO_RESULTS"
        )

        every {
            restTemplate.getForObject(any<String>(), GoogleGeocodingResponse::class.java)
        } returns mockResponse

        val address = Address(
            street = "Nonexistent Street",
            houseNumber = "999999",
            apartmentNumber = null,
            postalCode = "99-999",
            city = "Nowhere"
        )

        // when
        val result = service.geocodeAddress(address)

        // then
        assertNull(result)
    }

    @Test
    fun `should handle exception gracefully`() = runTest {
        // given
        val restTemplate = mockk<RestTemplate>()
        val service = GeocodingService(apiKey = "test-key", restTemplate = restTemplate)

        every {
            restTemplate.getForObject(any<String>(), GoogleGeocodingResponse::class.java)
        } throws RuntimeException("Network error")

        val address = Address(
            street = "Marszałkowska",
            houseNumber = "1",
            apartmentNumber = null,
            postalCode = "00-001",
            city = "Warszawa"
        )

        // when
        val result = service.geocodeAddress(address)

        // then
        assertNull(result)
    }
}