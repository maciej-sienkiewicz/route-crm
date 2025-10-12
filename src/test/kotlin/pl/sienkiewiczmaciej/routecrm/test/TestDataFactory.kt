package pl.sienkiewiczmaciej.routecrm.test

import java.time.LocalDate

object TestDataFactory {

    fun guardianRequest(
        firstName: String = "Jan",
        lastName: String = "Kowalski",
        email: String = "jan.kowalski${System.currentTimeMillis()}@example.com",
        phone: String = "+48123456789",
        alternatePhone: String? = null,
        communicationPreference: String = "SMS"
    ) = mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "alternatePhone" to alternatePhone,
        "address" to mapOf(
            "street" to "ul. Marszałkowska",
            "houseNumber" to "10",
            "apartmentNumber" to "5",
            "postalCode" to "00-001",
            "city" to "Warszawa"
        ),
        "communicationPreference" to communicationPreference
    )

    fun childRequest(
        firstName: String = "Anna",
        lastName: String = "Kowalska",
        birthDate: String = "2015-03-15",
        disability: List<String> = listOf("INTELLECTUAL", "PHYSICAL"),
        guardianId: String? = null,
        notes: String? = null
    ) = mapOf(
        "child" to mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "birthDate" to birthDate,
            "disability" to disability,
            "transportNeeds" to mapOf(
                "wheelchair" to false,
                "specialSeat" to true,
                "safetyBelt" to true
            ),
            "notes" to notes
        ),
        "guardian" to if (guardianId != null) {
            mapOf(
                "existingId" to guardianId,
                "firstName" to null,
                "lastName" to null,
                "email" to null,
                "phone" to null,
                "relationship" to null
            )
        } else {
            mapOf(
                "existingId" to null,
                "firstName" to "Maria",
                "lastName" to "Kowalska",
                "email" to "maria${System.currentTimeMillis()}@example.com",
                "phone" to "+48987654321",
                "relationship" to "PARENT"
            )
        }
    )

    fun driverUpdateRequest(
        firstName: String = "Jan",
        lastName: String = "Nowak",
        email: String = "jan.nowak${System.currentTimeMillis()}@example.com",
        phone: String = "+48123456789",
        status: String = "ACTIVE"
    ) = mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "address" to mapOf(
            "street" to "ul. Kwiatowa",
            "houseNumber" to "20",
            "apartmentNumber" to "15",
            "postalCode" to "02-520",
            "city" to "Warszawa"
        ),
        "status" to status,
        "drivingLicense" to mapOf(
            "licenseNumber" to "ABC${System.currentTimeMillis()}",
            "categories" to listOf("B", "D"),
            "validUntil" to LocalDate.now().plusYears(5).toString()
        ),
        "medicalCertificate" to mapOf(
            "validUntil" to LocalDate.now().plusYears(2).toString(),
            "issueDate" to LocalDate.now().minusDays(1).toString()
        )
    )

    fun scheduleRequest(
        name: String = "Do szkoły",
        days: List<String> = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
        pickupTime: String = "07:30",
        dropoffTime: String = "08:15"
    ) = mapOf(
        "name" to name,
        "days" to days,
        "pickupTime" to pickupTime,
        "pickupAddress" to mapOf(
            "label" to "Dom",
            "street" to "ul. Słoneczna",
            "houseNumber" to "15",
            "apartmentNumber" to "5",
            "postalCode" to "00-001",
            "city" to "Warszawa"
        ),
        "dropoffTime" to dropoffTime,
        "dropoffAddress" to mapOf(
            "label" to "Szkoła",
            "street" to "ul. Piękna",
            "houseNumber" to "5",
            "postalCode" to "00-123",
            "city" to "Warszawa"
        ),
        "specialInstructions" to "Dzwonek przy furtce"
    )

    fun scheduleExceptionRequest(
        exceptionDate: String = LocalDate.now().plusDays(1).toString(),
        notes: String? = "Dziecko chore"
    ) = mapOf(
        "exceptionDate" to exceptionDate,
        "notes" to notes
    )

    fun driverRequest(
        firstName: String = "Jan",
        lastName: String = "Nowak",
        email: String = "jan.nowak${System.currentTimeMillis()}@example.com",
        phone: String = "+48123456789",
        dateOfBirth: String = "1985-05-15"
    ) = mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "dateOfBirth" to dateOfBirth,
        "address" to mapOf(
            "street" to "ul. Kwiatowa",
            "houseNumber" to "20",
            "apartmentNumber" to "15",
            "postalCode" to "02-520",
            "city" to "Warszawa"
        ),
        "drivingLicense" to mapOf(
            "licenseNumber" to "ABC${System.currentTimeMillis()}",
            "categories" to listOf("B", "D"),
            "validUntil" to LocalDate.now().plusYears(5).toString()
        ),
        "medicalCertificate" to mapOf(
            "validUntil" to LocalDate.now().plusYears(2).toString(),
            "issueDate" to LocalDate.now().minusDays(1).toString()
        )
    )
}