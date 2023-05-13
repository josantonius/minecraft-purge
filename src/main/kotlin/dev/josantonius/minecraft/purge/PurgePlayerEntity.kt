package dev.josantonius.minecraft.purge

import java.util.UUID
import org.bukkit.entity.Player

class PurgePlayerEntity(player: Player) {
    private val name: String
    private val uniqueId: UUID
    private var isImmune = false
    private var isApplicant = false

    init {
        name = player.name
        uniqueId = player.uniqueId
    }

    fun getUniqueId(): UUID {
        return uniqueId
    }

    fun getName(): String {
        return name
    }

    fun isImmune(): Boolean {
        return isImmune
    }

    fun setAsImmune() {
        isImmune = true
    }

    fun removeAsImmune() {
        isImmune = false
    }

    fun isApplicant(): Boolean {
        return isApplicant
    }

    fun setAsApplicant() {
        isApplicant = true
    }
}
