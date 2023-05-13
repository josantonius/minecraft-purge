package dev.josantonius.minecraft.purge

import java.util.*

class PurgeStatus() {
    private var currentStatus: String = "inactive"

    fun announcing() {
        currentStatus = "announcing"
    }

    fun started() {
        currentStatus = "started"
    }

    fun ended() {
        currentStatus = "inactive"
    }

    fun isAnnouncing(): Boolean {
        return currentStatus == "announcing"
    }

    fun isStarted(): Boolean {
        return currentStatus == "started"
    }

    fun isStopped(): Boolean {
        return currentStatus == "inactive"
    }

    fun isActive(): Boolean {
        return currentStatus != "inactive"
    }

    fun getCurrentStatus(): String {
        return currentStatus
    }
}
