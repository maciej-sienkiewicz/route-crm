package pl.sienkiewiczmaciej.routecrm.driverauth.domain

enum class AccountStatus {
    PENDING_ACTIVATION,   // Konto utworzone, czeka na pierwszą aktywację
    ACTIVE,               // Normalne działanie
    LOCKED,               // Po 3 błędnych próbach
    SUSPENDED             // Admin suspend (bez auto-unlock)
}