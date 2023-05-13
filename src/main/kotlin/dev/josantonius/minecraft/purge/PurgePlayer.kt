package dev.josantonius.minecraft.purge

import java.util.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class PurgePlayer(private val plugin: Main) {
    var immunity: PurgeImmunity
    private var players = HashMap<UUID, PurgePlayerEntity>()

    init {
        immunity = PurgeImmunity(plugin)
    }

    fun add(player: Player) {
        if (exists(player)) return
        players[player.uniqueId] = PurgePlayerEntity(player)
    }

    fun get(player: Player): PurgePlayerEntity {
        var purgePlayer = players[player.uniqueId]
        if (purgePlayer == null) {
            purgePlayer = PurgePlayerEntity(player)
            players[player.uniqueId] = purgePlayer
        }
        return purgePlayer
    }

    fun exists(player: Player): Boolean {
        return players.containsKey(player.uniqueId)
    }

    fun getAll(): Collection<PurgePlayerEntity> {
        return players.values
    }

    fun addOnlinePlayers() {
        for (player in Bukkit.getOnlinePlayers()) {
            add(player)
        }
    }

    fun hasGlowingEffect(player: Player): Boolean {
        return player.hasPotionEffect(PotionEffectType.GLOWING)
    }

    fun addGlowingEffect(player: Player) {
        player.addPotionEffect(
                PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, true, false, false, null)
        )
    }

    fun removeGlowingEffect(player: Player) {
        player.removePotionEffect(PotionEffectType.GLOWING)
    }

    fun resetPlayerList() {
        players.clear()
        immunity = PurgeImmunity(plugin)
    }
}
