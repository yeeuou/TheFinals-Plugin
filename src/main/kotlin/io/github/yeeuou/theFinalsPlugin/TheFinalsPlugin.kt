package io.github.yeeuou.theFinalsPlugin

import com.mojang.brigadier.Command
import io.github.yeeuou.theFinalsPlugin.commands.ColorTest
import io.github.yeeuou.theFinalsPlugin.commands.JoinTFTeamCommand
import io.github.yeeuou.theFinalsPlugin.events.GameEvents
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin

class TheFinalsPlugin : JavaPlugin() {
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
            it.registrar().register(JoinTFTeamCommand.ejectCmd)
            // TESTING ONLY
            it.registrar().register(JoinTFTeamCommand.testCmd)
            it.registrar().register(ColorTest.cmd)
            // ========
//            it.registrar().register(Commands.literal("test").executes{ ctx ->
//                if (ctx.source.sender is Player)
//                        (ctx.source.sender as Player).run {
//                        }
//                Command.SINGLE_SUCCESS
//            }.build())
        }
    }

    override fun onDisable() {
        // remove test team
        NamedTextColor.NAMES.values().forEach {
            server.scoreboardManager.mainScoreboard.run {
                getTeam("tf_test_$it")?.unregister()
            }
        }
    }
}
