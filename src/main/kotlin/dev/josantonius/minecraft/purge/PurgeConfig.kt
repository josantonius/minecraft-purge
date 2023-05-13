package dev.josantonius.minecraft.purge

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class PurgeConfig(plugin: JavaPlugin) {
    private val config: FileConfiguration
    private val announcePurgeSound: String?
    private val ongoingPurgeSound: String?
    private val endPurgeSound: String?
    private val blockedCommands: List<String>?

    init {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config

        announcePurgeSound = config.getString("announcePurgeSound")
        ongoingPurgeSound = config.getString("ongoingPurgeSound")
        endPurgeSound = config.getString("endPurgeSound")
        blockedCommands = config.getStringList("blockedCommands")
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
}
