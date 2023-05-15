package dev.josantonius.minecraft.purge

import java.util.concurrent.CompletableFuture
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import net.luckperms.api.node.types.PermissionNode
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class PurgeDependency(private val plugin: Main) {
    private var originalKeepInventoryValues = mutableMapOf<String, Boolean>()
    private var originalDropOnDeathValue: Boolean? = null
    private var serverUsesDependencies: Boolean = false
    private var serverUsesTabPlugin: Boolean = false
    private var minepacks: Plugin? = null
    private var luckPerms: Plugin? = null

    fun setConfig() {
        minepacks = Bukkit.getPluginManager().getPlugin("Minepacks")
        luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms")
        serverUsesTabPlugin = Bukkit.getPluginManager().getPlugin("TAB")?.isEnabled == true
        if (!serverUsesDependencies()) return
        setDropInventory()
        setDropOnDeathValue(true)
    }

    fun restoreConfig() {
        if (!serverUsesDependencies()) return
        restoreKeepInventoryConfig()
        restoreBackpackConfig()
        originalDropOnDeathValue = null
        serverUsesDependencies = false
        serverUsesTabPlugin = false
        minepacks = null
        luckPerms = null
    }

    fun addKeepOnDeathPermission(player: Player) {
        if (!serverUsesDependencies()) return
        val api: LuckPerms = LuckPermsProvider.get()
        val userFuture: CompletableFuture<User> = api.userManager.loadUser(player.uniqueId)
        userFuture.thenAccept { user ->
            val permissionNode: Node =
                    PermissionNode.builder("backpack.keepOnDeath").value(true).build()
            user.data().add(permissionNode)
            api.userManager.saveUser(user)
        }
    }

    fun removeKeepOnDeathPermission(player: Player) {
        if (!serverUsesDependencies()) return
        val api: LuckPerms = LuckPermsProvider.get()
        val userFuture: CompletableFuture<User> = api.userManager.loadUser(player.uniqueId)
        userFuture.thenAccept { user ->
            val permissionNode: Node = PermissionNode.builder("backpack.keepOnDeath").build()
            user.data().remove(permissionNode)
            api.userManager.saveUser(user)
        }
    }

    fun hasKeepOnDeathPermission(player: Player): Boolean {
        if (!serverUsesDependencies()) {
            return false
        }
        val luckPermsApi: LuckPerms = LuckPermsProvider.get()
        val user = luckPermsApi.userManager.getUser(player.uniqueId)
        return user != null &&
                user.cachedData.permissionData.checkPermission("backpack.keepOnDeath").asBoolean()
    }

    fun setTabPrefix(player: Player) {
        if (serverUsesTabPlugin) {
            val prefix = plugin.message.getString("immunity.player_prefix")
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    "tab player ${player.name} tabprefix <dark_aqua>${prefix}<white>"
            )
        }
    }

    fun removeTabPrefix(player: Player) {
        if (serverUsesTabPlugin) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player ${player.name} tabprefix")
        }
    }

    private fun serverUsesDependencies(): Boolean {
        if (minepacks?.isEnabled != true || luckPerms?.isEnabled != true) {
            return false
        }
        return true
    }

    private fun setDropInventory() {
        storeOriginalKeepInventoryValues()
        for (world in Bukkit.getWorlds()) {
            world.setGameRule(GameRule.KEEP_INVENTORY, false)
        }
    }

    private fun restoreKeepInventoryConfig() {
        for (world in Bukkit.getWorlds()) {
            val originalValue = originalKeepInventoryValues[world.name]
            if (originalValue != null) {
                world.setGameRule(GameRule.KEEP_INVENTORY, originalValue)
            }
        }
        originalKeepInventoryValues.clear()
    }

    private fun restoreBackpackConfig() {
        setDropOnDeathValue(originalDropOnDeathValue)
    }

    private fun storeOriginalKeepInventoryValues() {
        for (world in Bukkit.getWorlds()) {
            originalKeepInventoryValues[world.name] =
                    world.getGameRuleValue(GameRule.KEEP_INVENTORY) ?: true
        }
    }

    private fun setDropOnDeathValue(newValue: Boolean?) {
        if (originalDropOnDeathValue == null) {
            originalDropOnDeathValue = minepacks?.config?.getBoolean("DropOnDeath")
        }
        minepacks?.config?.set("DropOnDeath", newValue)
        minepacks?.saveConfig()
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "backpack reload")
    }
}
