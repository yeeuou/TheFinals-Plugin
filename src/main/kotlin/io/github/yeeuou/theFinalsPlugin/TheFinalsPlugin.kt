package io.github.yeeuou.theFinalsPlugin

import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.tfPlayer
import io.github.yeeuou.theFinalsPlugin.commands.ChangeTFTeamColor
import io.github.yeeuou.theFinalsPlugin.commands.ColorTest
import io.github.yeeuou.theFinalsPlugin.commands.JoinTFTeamCommand
import io.github.yeeuou.theFinalsPlugin.events.GameEvents
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameRule
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
        Bukkit.getOnlinePlayers().forEach(TFPlayer::tryLoad)
        TFTeam.loadColor()
//        val t = server.scoreboardManager.mainScoreboard.run {
//            getTeam("TEST") ?: registerNewTeam("TEST")
//        }.apply { setCanSeeFriendlyInvisibles(true) }
        server.pluginManager.registerEvents(GameEvents(), this)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(JoinTFTeamCommand.joinCmd,
                "팀에 자신(또는 다른 사람)을 등록해 보세요!")
            it.registrar().register(JoinTFTeamCommand.ejectCmd,
                "팀이 마음에 들지 않는다면 떠나보세요!")
            it.registrar().register(ChangeTFTeamColor.cmd,
                "팀의 색을 바꿉니다. 색을 바꾸면 바꿀 색을 가진 팀의 색상과 교체됩니다.")
            // TESTING ONLY
            it.registrar().register(JoinTFTeamCommand.testCmd)
            it.registrar().register(ColorTest.cmd)
            // ========
//            it.registrar().register(Commands.literal("test").executes{ ctx ->
//                if (ctx.source.sender is Player)
//                        (ctx.source.sender as Player).run {
//                            t.entries.forEach(t::removeEntry)
//                            t.addEntity(this)
//                            world.spawn(location, Villager::class.java) {
//                                it.setAI(false)
////                                this.sendPotionEffectChange(it, PotionEffect(
////                                    PotionEffectType.GLOWING, 30, 0,
////                                    false, false, false
////                                ))
//                                it.isInvisible = true
//                                it.location.setRotation(0f, 0f)
//                                t.addEntity(it)
//                            }
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
        Bukkit.getOnlinePlayers().forEach { it.tfPlayer()?.unload() }
    }
}
