package dev.josantonius.minecraft.purge.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PurgeTabCompleter : TabCompleter {

    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            label: String,
            args: Array<String>
    ): List<String>? {
        if (sender !is Player) {
            return null
        }

        if (cmd.name.equals("purge", ignoreCase = true)) {
            when (args.size) {
                1 -> {
                    val subcommands = mutableListOf<String>()
                    if (sender.hasPermission("purge.admin")) {
                        subcommands.addAll(
                                listOf("cancel", "end", "exit", "help", "immune", "reload", "start")
                        )
                    }

                    if (sender.hasPermission("purge.use")) {
                        subcommands.addAll(listOf("exit", "help"))
                    }

                    return subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
                }
                2 -> {
                    if (args[0].equals("start", ignoreCase = true)) {
                        val suggestedValues = listOf("5", "8")
                        val timeUnits = listOf("s", "m", "h", "d")
                        return suggestedValues
                                .flatMap { value ->
                                    timeUnits.map { unit -> "${value}${args[1]}$unit" }
                                }
                                .filter { it.startsWith(args[1], ignoreCase = true) }
                    } else if (args[0].equals("immune", ignoreCase = true)) {
                        return listOf("add", "remove").filter {
                            it.startsWith(args[1], ignoreCase = true)
                        }
                    } else if (args[0].equals("exit", ignoreCase = true)) {
                        return emptyList()
                    }
                }
                3 -> {
                    if (args[0].equals("start", ignoreCase = true)) {
                        return listOf("supervised", "unsupervised").filter {
                            it.startsWith(args[2], ignoreCase = true)
                        }
                    } else if (args[0].equals("immune", ignoreCase = true)) {
                        return sender.server.onlinePlayers.map { it.name }.filter {
                            it.startsWith(args[2], ignoreCase = true)
                        }
                    }
                }
            }
        }
        return emptyList()
    }
}
