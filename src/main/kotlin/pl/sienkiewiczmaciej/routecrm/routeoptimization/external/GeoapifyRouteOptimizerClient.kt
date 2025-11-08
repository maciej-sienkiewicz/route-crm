// src/main/kotlin/pl/sienkiewiczmaciej/routecrm/routeoptimization/external/GeoapifyRouteOptimizerClient.kt
package pl.sienkiewiczmaciej.routecrm.routeoptimization.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

data class GeoapifyAgent(
    @JsonProperty("start_location")
    val startLocation: List<Double>,
    @JsonProperty("end_location")
    val endLocation: List<Double>? = null,
    @JsonProperty("time_windows")
    val timeWindows: List<List<Int>>? = null,
    @JsonProperty("delivery_capacity")
    val deliveryCapacity: Int? = null,
    val capabilities: List<String>? = null,
    val id: String? = null,
    val description: String? = null
)

data class GeoapifyPickup(
    val location: List<Double>,
    val duration: Int,
    @JsonProperty("time_windows")
    val timeWindows: List<List<Int>>? = null
)

data class GeoapifyDelivery(
    val location: List<Double>,
    val duration: Int,
    @JsonProperty("time_windows")
    val timeWindows: List<List<Int>>? = null
)

data class GeoapifyShipment(
    val id: String,
    val pickup: GeoapifyPickup,
    val delivery: GeoapifyDelivery,
    val amount: Int? = null,
    val priority: Int? = null,
    val requirements: List<String>? = null,
    val description: String? = null
)

data class GeoapifyOptimizationRequest(
    val mode: String = "drive",
    val agents: List<GeoapifyAgent>,
    val shipments: List<GeoapifyShipment>,
    val traffic: String? = "approximated",
    val type: String? = "balanced"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoapifyOptimizationResponse(
    val type: String,
    val properties: GeoapifyProperties,
    val features: List<GeoapifyFeature>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoapifyProperties(
    val mode: String,
    val issues: GeoapifyIssues? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoapifyIssues(
    @JsonProperty("unassigned_agents")
    val unassignedAgents: List<Int>? = null,
    @JsonProperty("unassigned_shipments")
    val unassignedShipments: List<Int>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoapifyFeature(
    val type: String,
    val properties: GeoapifyAgentPlan
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoapifyAgentPlan(
    @JsonProperty("agent_index")
    val agentIndex: Int,
    val distance: Int,
    val time: Int,
    @JsonProperty("start_time")
    val startTime: Int,
    @JsonProperty("end_time")
    val endTime: Int,
    val actions: List<GeoapifyAction>,
    val waypoints: List<GeoapifyWaypoint>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoapifyAction(
    val type: String,
    @JsonProperty("start_time")
    val startTime: Int,
    val duration: Int,
    @JsonProperty("shipment_index")
    val shipmentIndex: Int? = null,
    @JsonProperty("shipment_id")
    val shipmentId: String? = null,
    @JsonProperty("waypoint_index")
    val waypointIndex: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoapifyWaypoint(
    @JsonProperty("original_location")
    val originalLocation: List<Double>,
    val location: List<Double>,
    @JsonProperty("start_time")
    val startTime: Int,
    val duration: Int,
    val actions: List<GeoapifyAction>
)

@Service
class GeoapifyRouteOptimizerClient(
    @Value("\${geoapify.api.key:}")
    private val apiKey: String,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(GeoapifyRouteOptimizerClient::class.java)
    private val apiUrl = "https://api.geoapify.com/v1/routeplanner"

    suspend fun optimizeRoutes(request: GeoapifyOptimizationRequest): GeoapifyOptimizationResponse? =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                logger.error("Geoapify API key is not configured")
                return@withContext null
            }

            try {
                val url = "$apiUrl?apiKey=$apiKey"
                val requestJson = objectMapper.writeValueAsString(request)

                logger.debug("Sending optimization request to Geoapify: $requestJson")

                val headers = HttpHeaders()
                headers.contentType = MediaType.APPLICATION_JSON

                val entity = HttpEntity(requestJson, headers)
                val response = restTemplate.postForObject(url, entity, String::class.java)

                logger.debug("Received optimization response: $response")

                response?.let {
                    objectMapper.readValue(it, GeoapifyOptimizationResponse::class.java)
                }
            } catch (e: Exception) {
                logger.error("Error calling Geoapify Route Optimizer: ${e.message}", e)
                null
            }
        }
}