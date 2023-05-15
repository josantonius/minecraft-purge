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
        var purgePlayer = plugin.purge.player.get(player)
        if (plugin.purge.status.isAnnouncing()) {
            plugin.sendMessage(sender, "immunity.granted.confirmation", player.name)
            if (purgePlayer.isImmune()) return
            purgePlayer.setAsImmune()
        } else {
            if (!purgePlayer.isImmune()) {
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
        var purgePlayer = plugin.purge.player.get(player)
        if (plugin.purge.status.isAnnouncing()) {
            plugin.sendMessage(sender, "immunity.denied.confirmation", player.name)
            if (!purgePlayer.isImmune()) return
            purgePlayer.removeAsImmune()
        } else {
            if (purgePlayer.isImmune()) {
                removePrivileges(player)
                plugin.message.sendToPlayers("immunity.denied.advise", player.name)
            } else {
                plugin.sendMessage(sender, "error.player.not_immune", player.name)
            }
        }
    }

    fun requestImmunity(player: Player) {
        val purgePlayer = plugin.purge.player.get(player)
        val isApplicant = purgePlayer.isApplicant()

        if (!plugin.purge.status.isActive()) {
            plugin.sendMessage(player, "error.purge.not_active")
            return
        }

        if (purgePlayer.isImmune()) {
            plugin.sendMessage(player, "error.immunity.already_granted")
            return
        }

        if (isApplicant && !hasAdminPermission(player)) {
            plugin.sendMessage(player, "error.player.already_applicant", player.name)
            return
        }

        val grantImmunity = plugin.purge.grantImmunity()
        val purgeIsStarted = plugin.purge.status.isStarted()
        val purgeIsAnnouncing = plugin.purge.status.isAnnouncing()

        if (hasAdminPermission(player) && purgeIsAnnouncing) {
            purgePlayer.setAsImmune()
            plugin.message.sendToPlayers("immunity.request.by_staff", player.name)
        } else if (hasAdminPermission(player) && purgeIsStarted) {
            addPrivileges(player)
            plugin.message.sendToPlayers("immunity.request.by_staff", player.name)
            plugin.message.sendToPlayers("immunity.granted.advise", player.name)
        } else if (!isApplicant && purgeIsAnnouncing && grantImmunity) {
            purgePlayer.setAsImmune()
            plugin.message.sendToPlayers("immunity.request.by_player", player.name)
            plugin.sendMessage(player, "immunity.granted.info")
        } else if (!isApplicant && purgeIsAnnouncing && !grantImmunity) {
            Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
                if (hasAdminPermission(onlinePlayer)) {
                    plugin.sendMessage(onlinePlayer, "immunity.request.question", player.name)
                } else {
                    plugin.sendMessage(onlinePlayer, "immunity.request.by_player", player.name)
                }
            }
            plugin.sendMessage(player, "immunity.pending.ungranted_info")
        } else if (!isApplicant && purgeIsStarted && grantImmunity) {
            val elapsedTime = plugin.purge.getStartedBossBarElapsedTime()
            val remainingTime = plugin.purge.getPurgeDuration().minus(elapsedTime)
            if (remainingTime != null && remainingTime.toMinutes() <= 10) {
                plugin.message.sendToPlayers("immunity.request.by_player", player.name)
                plugin.message.sendToPlayers("immunity.denied.announce", player.name)
                plugin.sendMessage(player, "error.purge.already_ending")
            } else if (remainingTime != null && remainingTime.toMinutes() >= 9) {
                plugin.message.sendToPlayers("immunity.request.by_player", player.name)
                scheduleTask(180.0..480.0) {
                    if (purgeIsStarted) {
                        addPrivileges(player)
                        plugin.message.sendToPlayers("immunity.granted.advise", player.name)
                    }
                }
                plugin.sendMessage(player, "immunity.pending.granted_info")
            }
        } else if (!isApplicant && purgeIsStarted && !grantImmunity) {
            Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
                if (hasAdminPermission(onlinePlayer)) {
                    plugin.sendMessage(onlinePlayer, "immunity.request.question", player.name)
                } else {
                    plugin.sendMessage(onlinePlayer, "immunity.request.by_player", player.name)
                }
            }
            plugin.sendMessage(player, "immunity.pending.ungranted_info")
        } else {
            plugin.message.sendToPlayers("immunity.request.by_player", player.name)
        }
        purgePlayer.setAsApplicant()
    }

    fun addPrivileges(player: Player) {
        var purgePlayer = plugin.purge.player.get(player)
        purgePlayer.setAsImmune()
        plugin.purge.player.addGlowingEffect(player)
        plugin.purge.dependency.addKeepOnDeathPermission(player)
        plugin.purge.dependency.setTabPrefix(player)
    }

    fun removePrivileges(player: Player) {
        var purgePlayer = plugin.purge.player.get(player)
        purgePlayer.removeAsImmune()
        plugin.purge.player.removeGlowingEffect(player)
        plugin.purge.dependency.removeKeepOnDeathPermission(player)
        plugin.purge.dependency.removeTabPrefix(player)
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
