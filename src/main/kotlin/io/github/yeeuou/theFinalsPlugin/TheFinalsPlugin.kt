package io.github.yeeuou.theFinalsPlugin

import com.mojang.brigadier.Command
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.tfPlayer
import io.github.yeeuou.theFinalsPlugin.commands.ColorTest
import io.github.yeeuou.theFinalsPlugin.commands.TFCommand
import io.github.yeeuou.theFinalsPlugin.events.GameEvents
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.GameRule
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.pow

class TheFinalsPlugin : JavaPlugin() {
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
            it.registrar().register(TFCommand.mainCmd)
//            it.registrar().run {
//                register(JoinTFTeamCommand.joinCmd,
//                    "팀에 자신(또는 다른 사람)을 등록해 보세요!")
//                register(JoinTFTeamCommand.ejectCmd,
//                    "팀이 마음에 들지 않는다면 떠나보세요!")
//                register(ChangeTFTeamColor.cmd,
//                    "팀의 색을 바꿉니다. 색을 바꾸면 바꿀 색의 팀과 교체됩니다.")
//            }
            // TESTING ONLY
            it.registrar().register(testCmd)
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

    private val testCmd = Commands.literal("newDummy")
        .executes { ctx ->
            if (ctx.source.sender is Player) {
                val loc = (ctx.source.sender as Player).location
                loc.world.spawn(loc, Pillager::class.java) {
                    it.setAI(false)
                    it.setGravity(false)
                    it.equipment.setItemInMainHand(null)
                    DummyPlayer(it)
                }
            }
            Command.SINGLE_SUCCESS
        }.build()

    override fun onDisable() {
//        // remove test team
//        NamedTextColor.NAMES.values().forEach {
//            server.scoreboardManager.mainScoreboard.run {
//                getTeam("tf_test_$it")?.unregister()
//            }
//        }
        Bukkit.getOnlinePlayers().forEach { it.tfPlayer()?.unload() }
    }

    companion object {
        private lateinit var pl: JavaPlugin
        val instance
            get() = pl

/*
        fun Player.getLooseTargetEntitiesByDist(maxDistance: Number): Map<LivingEntity, Double> {
            val distanceD = maxDistance.toDouble()
            val distSq = distanceD.pow(2)
            val entityInRadius =
                location.getNearbyLivingEntities(distanceD) {
                    val loc = it.location; loc.y = location.y
                    location.distanceSquared(loc) <= distSq
                }
            entityInRadius.remove(this)
            val entityByDistance = mutableMapOf<LivingEntity, Double>()
            entityInRadius.forEach { entity ->
                entity.boundingBox.expand(.3).rayTrace(
                    eyeLocation.toVector(),
                    eyeLocation.direction,
                    distanceD
                )?.let {
                    val entityDist = it.hitPosition.distanceSquared(eyeLocation.toVector())
                    val blocked = world.rayTraceBlocks(eyeLocation,
                        eyeLocation.direction, distanceD,
                        FluidCollisionMode.NEVER)
                    if (blocked != null && blocked.hitPosition
                            .distanceSquared(eyeLocation.toVector()) < entityDist)
                        return@forEach
                    entityByDistance[entity] = entityDist
                }
            }
            return entityByDistance.toMap()
        }
*/

//        fun Player.getLooseTargetEntity(maxDistance: Number): LivingEntity? {
//            return getLooseTargetEntitiesByDist(maxDistance).minByOrNull { it.value }?.key
//        }

        fun Player.getLooseTargetArmorStand(maxDistance: Number): ArmorStand? {
            val distanceD = maxDistance.toDouble()
            val distSq = distanceD.pow(2)
            val entityInRadius =
                location.getNearbyLivingEntities(distanceD) {
                    val loc = it.location; loc.y = location.y
                    location.distanceSquared(loc) <= distSq
                }
            val entityByDistance = mutableMapOf<LivingEntity, Double>()
            entityInRadius.filter { it is ArmorStand }.forEach { entity ->
                entity.boundingBox.expand(.2).rayTrace(
                    eyeLocation.toVector(),
                    eyeLocation.direction,
                    distanceD
                )?.let {
                    val entityDist = it.hitPosition.distanceSquared(eyeLocation.toVector())
                    val blocked = world.rayTraceBlocks(eyeLocation,
                        eyeLocation.direction, distanceD,
                        FluidCollisionMode.NEVER)
                    if (blocked != null && blocked.hitPosition
                            .distanceSquared(eyeLocation.toVector()) < entityDist)
                        return@forEach
                    entityByDistance[entity] = entityDist
                }
            }
            return entityByDistance.minByOrNull { it.value }?.key as? ArmorStand
        }
    }
}
