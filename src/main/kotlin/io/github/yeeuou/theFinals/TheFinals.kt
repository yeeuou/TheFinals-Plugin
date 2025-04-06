package io.github.yeeuou.theFinals

import com.mojang.brigadier.Command
import io.github.yeeuou.theFinals.commands.JoinTFTeamCommand
import io.github.yeeuou.theFinals.events.GameEvents
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin

class TheFinals : JavaPlugin() {
    companion object {
        private lateinit var pl: JavaPlugin
        val instance
            get() = pl
    }

    init {
        pl = this
    }

    override fun onEnable() {
        server.worlds.forEach {
            it.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
            it.setGameRule(GameRule.KEEP_INVENTORY, true)
        }
        server.pluginManager.registerEvents(GameEvents(), this)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(JoinTFTeamCommand.joinCmd)
            it.registrar().register(JoinTFTeamCommand.testCmd)
            it.registrar().register(Commands.literal("test").executes{
                if (it.source.sender is Player)
                    (it.source.sender as Player).run {
                        val a = hasMetadata("test")
                        Bukkit.broadcast(Component.text(a))
                        if (!a)
                            setMetadata("test", FixedMetadataValue(
                                this@TheFinals, null))
                    }
                Command.SINGLE_SUCCESS
            }.build())
        }
    }

    override fun onDisable() {
    }
}
