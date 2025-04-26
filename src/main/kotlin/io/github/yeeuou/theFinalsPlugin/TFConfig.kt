package io.github.yeeuou.theFinalsPlugin

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object TFConfig {
    private val file =
        File(TFPlugin.instance.dataFolder, "config.yml")
    internal fun load() {
        if (!file.exists()) return
        YamlConfiguration.loadConfiguration(file).getConfigurationSection("TFConfig")?.run {
            mapOf(
                "start-coin" to ::startCoin,
                "player-respawn-tick" to ::playerRespawnTick,
                "team-respawn-tick" to ::teamRespawnTick
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
        val yaml = YamlConfiguration()
        yaml.createSection("TFConfig").run {
            set("start-coin", startCoin)
            set("player-respawn-tick", playerRespawnTick)
            set("team-respawn-tick", teamRespawnTick)
            set("clear-inventory-when-team-wipe", clearInventoryWhenTeamWipe)
        }
        yaml.save(file)
    }
    var startCoin = 2
    var playerRespawnTick = 30
    var teamRespawnTick = 15
    var clearInventoryWhenTeamWipe = true
    internal const val REVIVE_MAX_RANGE = 2.0
    internal const val GRAB_MAX_RANGE = 5.0
}
