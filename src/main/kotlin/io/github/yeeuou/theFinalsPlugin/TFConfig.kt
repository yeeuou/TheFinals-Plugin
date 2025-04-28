package io.github.yeeuou.theFinalsPlugin

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object TFConfig {
    private val file =
        File(TFPlugin.instance.dataFolder, "config.yml")
    internal fun load() {
        if (!file.exists()) {
            save()
            return
        }
        YamlConfiguration.loadConfiguration(file).run {
            mapOf(
                "start-coin" to ::startCoin,
                "player-respawn-time" to ::playerRespawnTime,
                "team-respawn-time" to ::teamRespawnTime
            ).forEach {
                if (isInt(it.key)) {
                    val v = getInt(it.key)
                    if (v >= 0) it.value.set(v)
                }
            }
            "clear-inventory-when-team-wipe".let {
                if (isBoolean(it))
                    clearInventoryWhenTeamWipe = getBoolean(it)
            }
        }
    }
    internal fun save() {
        YamlConfiguration().run {
            set("start-coin", startCoin)
            set("player-respawn-time", playerRespawnTime)
            set("team-respawn-time", teamRespawnTime)
            set("clear-inventory-when-team-wipe", clearInventoryWhenTeamWipe)
            save(file)
        }
    }
    var startCoin = 2
    var playerRespawnTime = 30
    var teamRespawnTime = 15
    var clearInventoryWhenTeamWipe = true
    internal const val REVIVE_MAX_RANGE = 2.0
    internal const val GRAB_MAX_RANGE = 5.0
}
