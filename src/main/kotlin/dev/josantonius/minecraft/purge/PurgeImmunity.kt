package dev.josantonius.minecraft.purge

import kotlin.random.Random
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PurgeImmunity(private val plugin: Main) {
    fun isImmune(player: Player): Boolean {
        return plugin.purge.player.get(player).isImmune()
    }

    fun setAsImmune(player: Player, sender: CommandSender) {
        if (!plugin.purge.status.isActive()) {
            plugin.sendMessage(sender, "error.purge.not_active")
            return
        }
        var playerInfo = plugin.purge.player.get(player)
        if (plugin.purge.status.isAnnouncing()) {
            if (playerInfo.isImmune()) {
                plugin.sendMessage(sender, "error.player.already_immune", player.name)
                return
            }
            playerInfo.setAsImmune()
            plugin.sendMessage(sender, "immunity.granted.confirmation", player.name)
        } else {
            if (!playerInfo.isImmune()) {
                addPrivileges(player)
                plugin.message.sendToPlayers("immunity.granted.advise", player.name)
            } else {
                plugin.sendMessage(sender, "error.player.already_immune", player.name)
            }
        }
    }

    fun removeAsImmune(player: Player, sender: CommandSender) {
        if (!plugin.purge.status.isActive()) {
            plugin.sendMessage(sender, "error.purge.not_active")
            return
        }
        var playerInfo = plugin.purge.player.get(player)
        if (plugin.purge.status.isAnnouncing()) {
            if (!playerInfo.isImmune()) {
                plugin.sendMessage(sender, "error.player.not_immune", player.name)
                return
            }
            playerInfo.removeAsImmune()
            plugin.sendMessage(sender, "immunity.denied.confirmation", player.name)
        } else {
            if (playerInfo.isImmune()) {
                removePrivileges(player)
                plugin.message.sendToPlayers("immunity.denied.advise", player.name)
            } else {
                plugin.sendMessage(sender, "error.player.not_immune", player.name)
            }
        }
    }

    fun requestImmunity(player: Player) {
        val playerInfo = plugin.purge.player.get(player)

        if (!plugin.purge.status.isActive()) {
            plugin.sendMessage(player, "error.purge.not_active")
            return
        }

        if (playerInfo.isImmune()) {
            plugin.sendMessage(player, "error.immunity.already_granted")
            return
        }

        if (playerInfo.isApplicant() && !hasAdminPermission(player)) {
            plugin.sendMessage(player, "error.player.already_applicant", player.name)
            return
        }

        val grantImmunity = plugin.purge.grantImmunity()

        if (hasAdminPermission(player) && plugin.purge.status.isAnnouncing()) {
            playerInfo.setAsImmune()
            plugin.message.sendToPlayers("immunity.request.by_staff", player.name)
        } else if (hasAdminPermission(player) && plugin.purge.status.isStarted()) {
            addPrivileges(player)
            plugin.message.sendToPlayers("immunity.request.by_staff", player.name)
            plugin.message.sendToPlayers("immunity.granted.advise", player.name)
        } else if (!playerInfo.isApplicant() && plugin.purge.status.isAnnouncing() && grantImmunity
        ) {
            playerInfo.setAsImmune()
            plugin.message.sendToPlayers("immunity.request.by_player", player.name)
        } else if (!playerInfo.isApplicant() && plugin.purge.status.isAnnouncing() && !grantImmunity
        ) {
            Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
                if (hasAdminPermission(onlinePlayer)) {
                    plugin.sendMessage(onlinePlayer, "immunity.request.question", player.name)
                } else {
                    plugin.sendMessage(onlinePlayer, "immunity.request.by_player", player.name)
                }
            }
        } else if (!playerInfo.isApplicant() && plugin.purge.status.isStarted() && grantImmunity) {
            val elapsedTime = plugin.purge.getStartedBossBarElapsedTime()
            val remainingTime = plugin.purge.getPurgeDuration().minus(elapsedTime)
            if (remainingTime != null && remainingTime.toMinutes() <= 10) {
                plugin.message.sendToPlayers("immunity.request.by_player", player.name)
                plugin.message.sendToPlayers("immunity.denied.announce", player.name)
            } else if (remainingTime != null && remainingTime.toMinutes() >= 9) {
                plugin.message.sendToPlayers("immunity.request.by_player", player.name)
                scheduleTask(180.0..480.0) {
                    if (plugin.purge.status.isStarted()) {
                        addPrivileges(player)
                        plugin.message.sendToPlayers("immunity.granted.advise", player.name)
                    }
                }
            }
        } else {
            plugin.message.sendToPlayers("immunity.request.by_player", player.name)
        }
        playerInfo.setAsApplicant()
    }

    fun addPrivileges(player: Player) {
        var playerInfo = plugin.purge.player.get(player)
        playerInfo.setAsImmune()
        plugin.purge.player.addGlowingEffect(player)
        plugin.purge.dependency.addKeepOnDeathPermission(player)
    }

    fun removePrivileges(player: Player) {
        var playerInfo = plugin.purge.player.get(player)
        playerInfo.removeAsImmune()
        plugin.purge.player.removeGlowingEffect(player)
        plugin.purge.dependency.removeKeepOnDeathPermission(player)
    }

    fun removePrivilegesFromImmunePlayers(purgeEnded: Boolean) {
        plugin.purge.player.getAll().filter { it.isImmune() }.forEach { purgePlayer ->
            if (purgeEnded) {
                plugin.message.sendToPlayers("immunity.denied.advise", purgePlayer.getName())
            }
            removePrivileges(Bukkit.getOfflinePlayer(purgePlayer.getUniqueId()) as Player)
        }
    }

    fun managePrivileges() {
        plugin.purge.player.getAll().forEach { purgePlayer ->
            if (purgePlayer.isImmune()) {
                addPrivileges(Bukkit.getOfflinePlayer(purgePlayer.getUniqueId()) as Player)
                plugin.message.broadcast("immunity.granted.announce", purgePlayer.getName())
            } else {
                removePrivileges(Bukkit.getOfflinePlayer(purgePlayer.getUniqueId()) as Player)
                if (purgePlayer.isApplicant()) {
                    plugin.message.broadcast("immunity.denied.announce", purgePlayer.getName())
                }
            }
        }
    }

    private fun ClosedFloatingPointRange<Double>.random(): Double {
        return Random.nextDouble(start, endInclusive)
    }

    private fun scheduleTask(range: ClosedFloatingPointRange<Double>, task: () -> Unit) {
        val randomDelay = (range.random() * 20).toLong()
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, task, randomDelay)
    }

    private fun hasAdminPermission(player: Player): Boolean {
        return player.hasPermission("purge.admin")
    }
}
