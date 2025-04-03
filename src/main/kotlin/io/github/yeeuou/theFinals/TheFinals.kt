package io.github.yeeuou.theFinals

import io.github.yeeuou.theFinals.commands.TFJoinTeamCommand
import io.github.yeeuou.theFinals.events.SpawnFigureOnDeath
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.GameRule
import org.bukkit.plugin.java.JavaPlugin

class TheFinals : JavaPlugin() {

    override fun onEnable() {
        server.worlds.forEach {
            it.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
        }
        server.pluginManager.registerEvents(SpawnFigureOnDeath(this), this)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(TFJoinTeamCommand().build())
            it.registrar().register(TFJoinTeamCommand().testCmd)
        }
    }

    override fun onDisable() {
    }
}
