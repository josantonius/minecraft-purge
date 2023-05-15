package dev.josantonius.minecraft.purge

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class PurgeConfig(plugin: JavaPlugin) {
    private val config: FileConfiguration
    private val announcePurgeSound: String?
    private val ongoingPurgeSound: String?
    private val endPurgeSound: String?
    private val blockedCommands: List<String>?
    private val blockedWorlds: List<String>?
    private val mainWorld: String?
    private var enderPearlCooldown: Int = 0
    private var fireworkRocketCooldown: Int = 0

    init {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config

        announcePurgeSound = config.getString("announcePurgeSound")
        ongoingPurgeSound = config.getString("ongoingPurgeSound")
        endPurgeSound = config.getString("endPurgeSound")
        blockedCommands = config.getStringList("blockedCommands")
        blockedWorlds = config.getStringList("blockedWorlds")
        mainWorld = config.getString("mainWorld")
        enderPearlCooldown = config.getInt("enderPearlCooldown")
        fireworkRocketCooldown = config.getInt("fireworkRocketCooldown")
    }

    fun getAnnouncePurgeSound(): String? {
        return announcePurgeSound
    }

    fun getOngoingPurgeSound(): String? {
        return ongoingPurgeSound
    }

    fun getEndPurgeSound(): String? {
        return endPurgeSound
    }

    fun getBlockedCommands(): List<String>? {
        return blockedCommands
    }

    fun getBlockedWorlds(): List<String>? {
        return blockedWorlds
    }

    fun getMainWorld(): String? {
        return mainWorld
    }

    fun getEnderPearlCooldown(): Int {
        return enderPearlCooldown
    }

    fun getFireworkRocketCooldown(): Int {
        return fireworkRocketCooldown
    }
}
