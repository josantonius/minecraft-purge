package dev.josantonius.minecraft.purge.command

import dev.josantonius.minecraft.purge.Main
import dev.josantonius.minecraft.purge.PurgeManager
import java.time.Duration
import java.util.regex.Pattern
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PurgeCommandExecutor(private val plugin: Main, private val purge: PurgeManager) :
        CommandExecutor {

    override fun onCommand(
            sender: CommandSender,
            command: Command,
            label: String,
            args: Array<String>
    ): Boolean {
        if (args.isEmpty()) {
            plugin.sendMessage(sender, "error.command.usage", "/purge help")
            return false
        }

        when (args[0].lowercase()) {
            "immune" -> {
                if (!hasAdminPermission(sender)) return false
                if (args.size < 3) {
                    plugin.sendMessage(
                            sender,
                            "error.command.usage",
                            "/purge immune <action> <player>"
                    )
                    return false
                }
                when {
                    args.size == 3 && args[1].lowercase() == "remove" -> {
                        handlePlayerAction(sender, args, "remove")
                    }
                    args.size == 3 && args[1].lowercase() == "add" -> {
                        handlePlayerAction(sender, args, "add")
                    }
                    else -> {
                        plugin.sendMessage(
                                sender,
                                "error.command.usage",
                                "/purge immune <action> <player>"
                        )
                        return false
                    }
                }
            }
            "exit" -> {
                if (!isPlayer(sender)) return false
                if (!hasUsePermission(sender)) return false
                purge.player.immunity.requestImmunity(sender as Player)
            }
            "start" -> {
                if (!hasAdminPermission(sender)) return false
                handleStart(sender, args)
            }
            "help" -> {
                if (!hasUsePermission(sender)) return false
                purge.showHelp(sender)
            }
            "end", "cancel", "reload" -> {
                if (!hasAdminPermission(sender)) return false
                when (args[0].lowercase()) {
                    "end" -> purge.end(sender)
                    "cancel" -> purge.cancel(sender)
                    "reload" -> plugin.reload(sender)
                }
            }
            else -> {
                plugin.sendMessage(sender, "error.command.usage", "/purge help")
                return false
            }
        }
        return true
    }

    private fun handlePlayerAction(
            sender: CommandSender,
            args: Array<String>,
            action: String
    ): Boolean {
        val targetPlayerName = args[2]
        val targetPlayer = plugin.server.getPlayer(targetPlayerName)
        if (targetPlayer == null) {
            plugin.sendMessage(sender, "error.player.offline", targetPlayerName)
            return false
        }

        when (action) {
            "add" -> purge.player.immunity.setAsImmune(targetPlayer, sender)
            "remove" -> purge.player.immunity.removeAsImmune(targetPlayer, sender)
        }

        return true
    }

    private fun handleStart(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size != 3) {
            plugin.sendMessage(sender, "error.command.usage", "/purge start <time> <mode>")
            return false
        }
        val timeString = args[1].lowercase()
        if (!Pattern.compile("^(\\d+[smhd])+\$").matcher(timeString).matches()) {
            plugin.sendMessage(sender, "error.time.invalid")
            return false
        }
        val mode = args[2].lowercase()
        if (mode != "supervised" && mode != "unsupervised") {
            plugin.sendMessage(
                    sender,
                    "error.mode.invalid",
                    "Valid modes: supervised, unsupervised"
            )
            return false
        }
        purge.start(parseDuration(timeString), mode, sender)
        return true
    }

    private fun hasAdminPermission(sender: CommandSender): Boolean {
        if (!sender.hasPermission("purge.admin")) {
            plugin.sendMessage(sender, "error.command.permission")
            return false
        }
        return true
    }

    private fun hasUsePermission(sender: CommandSender): Boolean {
        if (!sender.hasPermission("purge.use")) {
            plugin.sendMessage(sender, "error.command.permission")
            return false
        }
        return true
    }

    private fun isPlayer(sender: CommandSender): Boolean {
        if (sender !is Player) {
            plugin.sendMessage(sender, "error.command.for_players")
            return false
        }
        return true
    }

    private fun parseDuration(timeString: String): Duration {
        var remaining = timeString
        var duration = Duration.ZERO

        while (remaining.isNotEmpty()) {
            val value = remaining.takeWhile { it.isDigit() }.toInt()
            val unit = remaining.takeLastWhile { !it.isDigit() }
            val unitDuration =
                    when (unit) {
                        "s" -> Duration.ofSeconds(value.toLong())
                        "m" -> Duration.ofMinutes(value.toLong())
                        "h" -> Duration.ofHours(value.toLong())
                        "d" -> Duration.ofDays(value.toLong())
                        else -> throw IllegalArgumentException("error.time.invalid")
                    }
            duration = duration.plus(unitDuration)
            remaining = remaining.dropWhile { it.isDigit() }.dropWhile { !it.isDigit() }
        }
        return duration
    }
}
