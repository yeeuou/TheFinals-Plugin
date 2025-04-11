package io.github.yeeuou.theFinalsPlugin.events

import io.github.yeeuou.theFinalsPlugin.DummyFigureRevive
import io.github.yeeuou.theFinalsPlugin.DummyPlayer
import io.github.yeeuou.theFinalsPlugin.DummyPlayer.Companion.asDummyFigure
import io.github.yeeuou.theFinalsPlugin.Figure.Companion.figure
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.getSpectatePlayer
import io.github.yeeuou.theFinalsPlugin.TeamManager.tfPlayer
import io.github.yeeuou.theFinalsPlugin.TheFinalsPlugin
import io.github.yeeuou.theFinalsPlugin.task.ReviveAnimationTask
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector

class GameEvents : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerDead(ev: PlayerDeathEvent) {
        ev.player.tfPlayer()?.run {
            ev.isCancelled = true
            if (isDead) return
            player.velocity = Vector()
            playDeadEffect()
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun figureDestroy(ev: EntityDeathEvent) {
        (ev.entity as? ArmorStand)?.figure()?.let {
            ev.isCancelled = true
            return
        }
        (ev.entity as? ArmorStand)?.asDummyFigure()?.let {
            ev.isCancelled = true
        }
    }

    @EventHandler
    fun playerLookAtFigure(ev: PlayerMoveEvent) {
        ev.player.getMetadata("tf_holdRevive")
            .forEach { if (it.owningPlugin is TheFinalsPlugin) return }
        ev.player.getTargetEntity(3)?.run {
            (this as? ArmorStand)?.figure()?.let {
                ev.player.setMetadata("tf_holdRevive",
                    FixedMetadataValue(TheFinalsPlugin.instance, null))
                server.scheduler.runTaskTimer(
                    TheFinalsPlugin.instance,
                    ReviveAnimationTask(ev.player, it),
                    0L, 1L
                )
            }
            (this as? ArmorStand)?.let {
                it.asDummyFigure()?.run {
                    ev.player.setMetadata("tf_holdRevive",
                        FixedMetadataValue(TheFinalsPlugin.instance, null))
                    Bukkit.getScheduler().runTaskTimer(
                        TheFinalsPlugin.instance,
                        DummyFigureRevive(ev.player, this),
                        0, 1
                    )
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun dummyPlayerDead(ev: EntityDeathEvent) {
        if (ev.entity is Player) return
        DummyPlayer.entityByDummy[ev.entity]?.run {
            ev.isCancelled = true
            playDeadEffect()
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun spectatorTeleport(ev: PlayerTeleportEvent) {
        if (ev.cause == PlayerTeleportEvent.TeleportCause.SPECTATE)
            ev.isCancelled = true
    }

    @EventHandler
    fun spectatorClickMouseBtn(ev: PlayerInteractEvent) {
        if (ev.player.gameMode != GameMode.SPECTATOR) return
        ev.player.tfPlayer()?.run {
            if (!isDead) return
            runCatching {
                player.spectatorTarget = runCatching {
                    if (ev.action.isRightClick) {
                        tfTeam.getPrevAlivePlayer(getSpectatePlayer()!!).player
                    } else tfTeam.getNextAlivePlayer(getSpectatePlayer()!!).player
                }.getOrElse { tfTeam.getFirstAlivePlayer().player }
            }.onFailure { it.printStackTrace() }
        }
    }
}
