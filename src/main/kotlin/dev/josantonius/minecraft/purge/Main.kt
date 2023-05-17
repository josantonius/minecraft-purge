package dev.josantonius.minecraft.purge

import dev.josantonius.minecraft.messaging.Message
import dev.josantonius.minecraft.messaging.Title
import dev.josantonius.minecraft.purge.command.PurgeCommandExecutor
import dev.josantonius.minecraft.purge.command.PurgeTabCompleter
import java.io.File
import kotlin.math.ceil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.SpectralArrow
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType

class Main : JavaPlugin(), Listener {
    lateinit var configuration: PurgeConfig
    lateinit var purge: PurgeManager
    lateinit var message: Message
    lateinit var title: Title
    val lastUsed = mutableMapOf<Player, MutableMap<Material, Long>>()

    override fun onEnable() {
        load()
    }

    override fun onDisable() {
        purge.reset()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!purge.status.isActive()) return
        val player = event.player
        purge.getNoticeBossBar().addPlayer(player)
        purge.getOngoingBossBar().addPlayer(player)
        if (!purge.player.exists(player)) {
            purge.player.add(player)
            purge.showPurgeInfoToPlayer(player)
        } else if (purge.player.immunity.isImmune(player)) {
            message.sendToPlayers("immunity.granted.advise", player.name)
            Bukkit.getScheduler()
                    .scheduleSyncDelayedTask(
                            this,
                            { purge.player.immunity.addPrivileges(player) },
                            1L
                    )
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!purge.status.isActive()) return
        val player = event.player
        purge.getNoticeBossBar().removePlayer(player)
        purge.getOngoingBossBar().removePlayer(player)
        purge.player.immunity.removePrivileges(player, false)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!purge.status.isStarted()) return
        val victim = event.entity
        val killer = victim.killer
        if (purge.player.immunity.isImmune(victim)) {
            event.keepInventory = true
            event.keepLevel = true
            event.drops.clear()
            event.setDroppedExp(0)
        }
        showDeathTitleToAll(victim, killer)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (!purge.status.isStarted()) return
        val player = event.player
        if (purge.player.immunity.isImmune(player)) {
            Bukkit.getScheduler()
                    .scheduleSyncDelayedTask(
                            this,
                            { purge.player.immunity.addPrivileges(player) },
                            1L
                    )
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        if (event.isCancelled) {
            return
        }
        if (!purge.status.isStarted() || purge.player.immunity.isImmune(event.player)) return
        val player = event.player
        val command = event.message.lowercase()

        if (!player.hasPermission("purge.admin")) {
            if (isCommandBlocked(command)) {
                event.isCancelled = true
                sendMessage(player, "error.command.not_allowed")
            }
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (!purge.status.isStarted()) return

        val damager = event.damager
        val damaged = event.entity
        val immunity = purge.player.immunity

        if (damager is Player && damaged is Player) {
            if (damager == damaged) return
            if (immunity.isImmune(damaged) && immunity.isImmune(damager)) return
            if (immunity.isImmune(damager) || immunity.isImmune(damaged)) {
                event.isCancelled = true
            }
        } else if (damaged is Player && damager is Projectile) {
            val shooter = damager.shooter
            if (shooter is Player && shooter != damaged) {
                if (immunity.isImmune(damaged) || immunity.isImmune(shooter)) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onPotionSplash(event: PotionSplashEvent) {
        if (!purge.status.isStarted()) return
        val potion = event.potion
        val thrower = potion.shooter
        val affectedEntities = event.affectedEntities
        val immunity = purge.player.immunity

        for (affectedEntity in affectedEntities) {
            if (thrower is Player && affectedEntity is Player && thrower != affectedEntity) {
                if (immunity.isImmune(thrower) || immunity.isImmune(affectedEntity)) {
                    event.setIntensity(affectedEntity, 0.0)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (!purge.status.isStarted()) return
        val player = event.player
        val item = event.item

        if (purge.player.immunity.isImmune(player) &&
                        item.type == Material.MILK_BUCKET &&
                        purge.player.hasGlowingEffect(player)
        ) {
            val glowingEffectDuration = player.getPotionEffect(PotionEffectType.GLOWING)?.duration

            if (glowingEffectDuration != null) {
                Bukkit.getScheduler()
                        .scheduleSyncDelayedTask(
                                this,
                                Runnable { purge.player.immunity.addPrivileges(player) },
                                1L
                        )
            }
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (!purge.status.isStarted()) return
        if (event.entity is SpectralArrow) {
            val hitEntity = event.hitEntity
            if (hitEntity is Player && !purge.player.immunity.isImmune(hitEntity)) {
                Bukkit.getScheduler()
                        .scheduleSyncDelayedTask(
                                this,
                                { purge.player.immunity.removePrivileges(hitEntity) },
                                1L
                        )
            }
        }
    }

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        if (!purge.status.isStarted()) return
        val player = event.player
        if (player.hasPermission("purge.admin") || purge.player.immunity.isImmune(player)) return
        val worldName = player.location.world.name
        val blockedWorlds = configuration.getBlockedWorlds()
        val mainWorld = configuration.getMainWorld()
        if (mainWorld != null && blockedWorlds != null && blockedWorlds.contains(worldName)) {
            val defaultWorld = Bukkit.getServer().getWorld(mainWorld) ?: return
            sendMessage(player, "error.world.access_denied", worldName)
            player.teleport(defaultWorld.spawnLocation)
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!purge.status.isStarted()) return
        val player = event.player
        if (purge.player.immunity.isImmune(player)) return
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val item = event.item
        val isFireworkRocket = item?.type == Material.FIREWORK_ROCKET
        val isEnderPearl = item?.type == Material.ENDER_PEARL
        if (item != null && (isFireworkRocket || isEnderPearl)) {
            val cooldown =
                    configuration.let {
                        if (isFireworkRocket) it.getFireworkRocketCooldown()
                        else it.getEnderPearlCooldown()
                    }
            val type =
                    if (isFireworkRocket) message.getString("material.firework_rocket")
                    else message.getString("material.ender_pearl")
            if (cooldown > 0) {
                val currentTime = System.currentTimeMillis()
                val lastUsedTime =
                        lastUsed.getOrPut(player, { mutableMapOf() }).getOrDefault(item.type, 0L)
                if (currentTime - lastUsedTime < cooldown * 1000) {
                    event.isCancelled = true
                    val remainingTime =
                            ceil((cooldown * 1000 - (currentTime - lastUsedTime)) / 1000.0)
                                    .toInt()
                                    .toString()
                    sendMessage(player, "error.item.with_cooldown", remainingTime, type)
                    return
                }

                lastUsed[player]!![item.type] = currentTime
            }
        }
    }

    fun load() {
        val messagesFile = File(dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false)
        }

        message = Message(messagesFile, this)
        setMessagePrefixes()
        title = Title(messagesFile)
        configuration = PurgeConfig(this)
        purge = PurgeManager(this)

        HandlerList.getHandlerLists().forEach { handlerList ->
            handlerList.unregister(this as Listener)
        }
        server.pluginManager.registerEvents(this, this)

        val purgeTabCompleter = PurgeTabCompleter()
        val purgeCommandExecutor = PurgeCommandExecutor(this, purge)

        getCommand("purge")?.setTabCompleter(purgeTabCompleter)
        getCommand("purge")?.setExecutor(purgeCommandExecutor)
    }

    fun reload(sender: CommandSender) {
        purge.reset()
        load()
        sendMessage(sender, "plugin.reloaded")
    }

    fun setMessagePrefixes() {
        message.setConsolePrefix(message.getString("plugin.prefix"))
        message.setChatPrefix(message.getString("plugin.prefix"))
    }

    fun clearMessagePrefixes() {
        message.setConsolePrefix("")
        message.setChatPrefix("")
    }

    fun sendNewLineMessage() {
        clearMessagePrefixes()
        message.sendToPlayers(" ")
        setMessagePrefixes()
    }

    fun sendMessage(sender: CommandSender, key: String, vararg params: String) {
        if (sender is Player) {
            message.sendToPlayer(sender, key, *params)
        } else {
            message.sendToConsole(key, *params)
        }
    }

    private fun showDeathTitleToAll(victim: Player, killer: Player?) {
        for (player in Bukkit.getOnlinePlayers()) {
            if (player == victim) continue
            if (killer == null) {
                title.showToPlayer(player, "title.player.death", "", victim.name)
            } else {
                title.showToPlayer(
                        player,
                        "title.player.death_by_player",
                        "",
                        killer.name,
                        victim.name,
                )
            }
        }
    }

    private fun isCommandBlocked(command: String): Boolean {
        val blockedCommands = configuration.getBlockedCommands()
        if (blockedCommands.isNullOrEmpty()) return false
        return blockedCommands.any { command.startsWith(it) }
    }
}
