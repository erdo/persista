package co.early.persista

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

class TestState {

    @Serializable
    data class DashboardState(
        val dashboardId: Int,
        val userName: String,
        val drivers: List<Driver> = emptyList(),
        val error: Error? = null,
        @Transient val isUpdating: Boolean = false
    )

    @Serializable
    data class Driver(
        val driverId: Int,
        val driverName: String,
        val powerLevel: Int,
        val lat: Int,
        val long: Int
    )

    @Serializable
    enum class Error {
        UNKNOWN,
        SECURITY,
        IO
    }

    data class SomethingElseState(
        val someId: Int,
        val someUserName: String,
    )
}