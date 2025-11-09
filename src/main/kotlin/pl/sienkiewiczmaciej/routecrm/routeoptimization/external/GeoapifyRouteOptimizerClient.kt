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

// ============================================
// REQUEST MODELS - Z NULLABLE FIELDS
// ============================================

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
    val timeWindows: List<List<Int>>? = null // NULLABLE - dla najkrótszej drogi
)

data class GeoapifyDelivery(
    val location: List<Double>,
    val duration: Int,
    @JsonProperty("time_windows")
    val timeWindows: List<List<Int>>? = null // NULLABLE - dla najkrótszej drogi
)

data class GeoapifyShipment(
    val id: String,
    val pickup: GeoapifyPickup,
    val delivery: GeoapifyDelivery,
    val amount: Int? = null,
    val priority: Int? = null, // NULLABLE - wszystkie równe dla najkrótszej drogi
    val requirements: List<String>? = null,
    val description: String? = null
)

data class GeoapifyOptimizationRequest(
    val mode: String = "drive",
    val agents: List<GeoapifyAgent>,
    val shipments: List<GeoapifyShipment>,
    val traffic: String? = "approximated",
    val type: String? = "shortest" // "shortest" = minimalizacja dystansu, "balanced" = balans czasu i dystansu
)

// ============================================
// RESPONSE MODELS
// ============================================

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

// ============================================
// CLIENT
// ============================================

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

                logger.debug("Sending optimization request to Geoapify")
                logger.debug("Request type: ${request.type}")
                logger.debug("Agents: ${request.agents.size}")
                logger.debug("Shipments: ${request.shipments.size}")

                val headers = HttpHeaders()
                headers.contentType = MediaType.APPLICATION_JSON

                val entity = HttpEntity(requestJson, headers)
                val response = restTemplate.postForObject(url, entity, String::class.java)

                logger.debug("Received optimization response from Geoapify")

                response?.let {
                    val parsedResponse = objectMapper.readValue(it, GeoapifyOptimizationResponse::class.java)

                    // Log wyników
                    logger.info("Optimization completed:")
                    parsedResponse.features.forEachIndexed { index, feature ->
                        val plan = feature.properties
                        logger.info("Route ${index + 1}: distance=${plan.distance}m, time=${plan.time}s, stops=${plan.actions.size}")
                    }

                    parsedResponse.properties.issues?.let { issues ->
                        if (!issues.unassignedShipments.isNullOrEmpty()) {
                            logger.warn("Unassigned shipments: ${issues.unassignedShipments.size}")
                        }
                    }

                    parsedResponse
                }
            } catch (e: Exception) {
                logger.error("Error calling Geoapify Route Optimizer: ${e.message}", e)
                null
            }
        }
}