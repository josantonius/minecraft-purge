package dev.josantonius.minecraft.purge

import java.time.Duration
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.command.CommandSender
import org.bukkit.scheduler.BukkitTask

class PurgeManager(private val plugin: Main) {
    val status: PurgeStatus
    val player: PurgePlayer
    val dependency: PurgeDependency
    private var grantImmunity: Boolean = false
    private var purgeTask: BukkitTask? = null
    private var showRulesTask: BukkitTask? = null
    private var purgeSoundTask: BukkitTask? = null
    private val announcementBossBar: PurgeBossBar
    private val startedBossBar: PurgeBossBar
    private val purgeSound = PurgeSound(plugin)
    private var purgeInfo: ArrayList<String> = ArrayList()
    private var duration: Duration = Duration.ZERO

    init {
        status = PurgeStatus()
        player = PurgePlayer(plugin)
        dependency = PurgeDependency(plugin)
        announcementBossBar =
                PurgeBossBar(
                        plugin.message.getString("boss_bar.start"),
                        BarColor.RED,
                        BarStyle.SOLID
                )
        startedBossBar =
                PurgeBossBar(plugin.message.getString("boss_bar.end"), BarColor.RED, BarStyle.SOLID)
    }

    fun start(purgeDuration: Duration, mode: String, sender: CommandSender) {
        if (status.isActive()) {
            plugin.sendMessage(sender, "error.purge.active")
            return
        }
        grantImmunity = mode == "unsupervised"
        duration = purgeDuration
        status.announcing()

        announcementBossBar.show()

        var runnable = Runnable { updates(announcementBossBar, startedBossBar) }
        purgeTask = Bukkit.getScheduler().runTaskTimer(plugin, runnable, 0L, 20L)

        runnable = Runnable { showPurgeInfoToPlayers() }
        showRulesTask = Bukkit.getScheduler().runTaskLater(plugin, runnable, 480L)

        showAnnouncePurgeTitleToAll()
        purgeSound.stopPluginMusic()
        purgeSound.playAnnouncePurgeSound()
        player.addOnlinePlayers()

        plugin.message.broadcast("purge.announce")
    }

    fun end(sender: CommandSender? = null) {
        if (sender != null && !status.isActive()) {
            plugin.sendMessage(sender, "error.purge.not_active")
            return
        }
        plugin.message.broadcast("purge.end")
        reset(true)
        showEndPurgeTitleToAll()
        purgeSound.stopMinecraftMusic()
        purgeSound.playEndPurgeSound()
    }

    fun cancel(sender: CommandSender) {
        if (!status.isActive()) {
            plugin.sendMessage(sender, "error.purge.not_active")
            return
        }
        showCancelPurgeTitleToAll()
        plugin.message.broadcast("purge.cancel")
        reset()
    }

    fun reset(isEnded: Boolean = false) {
        purgeSound.stopPluginMusic()
        startedBossBar.hidden()
        announcementBossBar.hidden()
        cancelTasks()
        player.immunity.removePrivilegesFromImmunePlayers(isEnded)
        dependency.restoreConfig()
        player.resetPlayerList()
        status.ended()
        purgeInfo.clear()
        if (!plugin.isDisabling) plugin.load()
    }

    fun getNoticeBossBar(): PurgeBossBar {
        return announcementBossBar
    }

    fun getOngoingBossBar(): PurgeBossBar {
        return startedBossBar
    }

    fun showPurgeInfoToPlayers() {
        setPurgeInfo()
        showPurgeInfo()
    }

    fun showPurgeInfoToPlayer(player: CommandSender) {
        if (purgeInfo.isEmpty()) return
        var runnable = Runnable { showPurgeInfo(player) }
        showRulesTask = Bukkit.getScheduler().runTaskLater(plugin, runnable, 40L)
    }

    fun showHelp(player: CommandSender) {
        if (player.hasPermission("purge.use")) {
            plugin.sendMessage(player, "help.header")
        }
        if (player.hasPermission("purge.admin")) {
            plugin.sendMessage(player, "help.start", "/purge start <time> <mode>")
            plugin.sendMessage(player, "help.end", "/purge end")
            plugin.sendMessage(player, "help.cancel", "/purge cancel")
            plugin.sendMessage(player, "help.immune", "/purge immune <action> <player>")
        }
        if (player.hasPermission("purge.use")) {
            plugin.sendMessage(player, "help.exit", "/purge exit")
        }
        if (player.hasPermission("pvp.admin")) {
            plugin.sendMessage(player, "help.reload", "/purge reload")
        }
    }

    fun getStartedBossBarElapsedTime(): Duration {
        return startedBossBar.getElapsedTime()
    }

    fun getPurgeDuration(): Duration {
        return duration
    }

    fun grantImmunity(): Boolean {
        return grantImmunity
    }

    private fun sendPlayersToMainWorld() {
        val blockedWorlds = plugin.configuration.getBlockedWorlds()
        val mainWorld = plugin.configuration.getMainWorld()
        if (blockedWorlds == null || mainWorld == null) return
        for (onlinePlayer in Bukkit.getServer().onlinePlayers) {
            if (onlinePlayer.hasPermission("purge.admin") || player.immunity.isImmune(onlinePlayer))
                    return
            if (blockedWorlds.contains(onlinePlayer.world.name)) {
                val defaultWorld = Bukkit.getServer().getWorld(mainWorld) ?: return
                plugin.sendMessage(
                        onlinePlayer,
                        "error.world.access_denied",
                        onlinePlayer.world.name
                )
                onlinePlayer.teleport(defaultWorld.spawnLocation)
            }
        }
    }

    private fun setPurgeInfo() {
        if (!status.isAnnouncing()) {
            return
        }

        var ruleNumber = 1
        val ruleKey = "rules.rule_"
        var rules = ArrayList<String>()
        while (true) {
            val key = ruleKey + ruleNumber
            val value = plugin.message.getString(key)
            if (value == key) {
                break
            }
            rules.add(key)
            ruleNumber++
        }

        var privilegeNumber = 1
        val privilegeKey = "privileges.privilege_"
        var privileges = ArrayList<String>()
        while (true) {
            val key = privilegeKey + privilegeNumber
            val value = plugin.message.getString(key)
            if (value == key) {
                break
            }
            privileges.add(key)
            privilegeNumber++
        }

        if (!rules.isEmpty()) {
            purgeInfo.add("purge.with_rules")
            for (rule in rules) {
                purgeInfo.add(rule)
            }
        } else {
            purgeInfo.add("purge.without_rules")
        }

        if (!privileges.isEmpty()) {
            purgeInfo.add("purge.with_privileges")
            for (privilege in privileges) {
                purgeInfo.add(privilege)
            }
        } else {
            purgeInfo.add("purge.whitoout_privileges")
        }
    }

    private fun showPurgeInfo(player: CommandSender? = null) {
        if (player != null) {
            plugin.sendMessage(player, if (status.isStarted()) "purge.start" else "purge.announce")
        }
        plugin.sendNewLineMessage()
        if (player != null) {
            purgeInfo.forEach { plugin.sendMessage(player, it) }
            if (!player.hasPermission("purge.admin")) {
                plugin.sendMessage(player, "purge.button.request_immunity")
            } else {
                plugin.sendMessage(player, "purge.button.use_immunity")
            }
        } else {
            Bukkit.getOnlinePlayers().forEach { onlinePlayer ->
                purgeInfo.forEach { plugin.sendMessage(onlinePlayer, it) }
                if (!onlinePlayer.hasPermission("purge.admin")) {
                    plugin.sendMessage(onlinePlayer, "purge.button.request_immunity")
                } else {
                    plugin.sendMessage(onlinePlayer, "purge.button.use_immunity")
                }
            }
        }
        plugin.sendNewLineMessage()
    }

    private fun showAnnouncePurgeTitleToAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.title.showToPlayer(player, "title.purge.announce", "")
        }
    }

    private fun showStartPurgeTitleToAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.title.showToPlayer(player, "title.purge.start", "")
        }
    }

    private fun showEndPurgeTitleToAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.title.showToPlayer(player, "title.purge.end", "")
        }
    }

    private fun showCancelPurgeTitleToAll() {
        for (player in Bukkit.getOnlinePlayers()) {
            plugin.title.showToPlayer(player, "title.purge.cancel", "")
        }
    }

    private fun updates(announcementBossBar: PurgeBossBar, startedBossBar: PurgeBossBar) {
        if (status.isAnnouncing()) {
            purgeSound.stopMinecraftMusic()
            announcementBossBar.update(Duration.ofSeconds(120))
            if (announcementBossBar.getElapsedTime() >= Duration.ofSeconds(120)) {
                plugin.message.broadcast("purge.start")
                dependency.setConfig()
                player.immunity.managePrivileges()
                sendPlayersToMainWorld()
                purgeSound.stopPluginMusic()
                val soundPeriod = Duration.ofMinutes(3).plusSeconds(4).toTicks()
                val soundRunnable = Runnable { purgeSound.playOngoingPurgeSound() }
                purgeSoundTask =
                        Bukkit.getScheduler().runTaskTimer(plugin, soundRunnable, 0L, soundPeriod)
                announcementBossBar.hidden()
                showStartPurgeTitleToAll()
                startedBossBar.show()
                status.started()
            }
        } else if (status.isStarted()) {
            purgeSound.stopMinecraftMusic()
            startedBossBar.update(duration)
            if (startedBossBar.getElapsedTime() >= duration) {
                end()
            }
        }
    }

    private fun cancelTasks() {
        purgeTask?.cancel()
        purgeTask = null
        purgeSoundTask?.cancel()
        purgeSoundTask = null
        showRulesTask?.cancel()
        showRulesTask = null
    }

    private fun Duration.toTicks(): Long {
        return this.toMillis() / 50
    }
}
