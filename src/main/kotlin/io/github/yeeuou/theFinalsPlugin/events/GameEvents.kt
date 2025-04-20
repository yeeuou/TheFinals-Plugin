package io.github.yeeuou.theFinalsPlugin.events

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
import io.github.yeeuou.theFinalsPlugin.DummyFigureRevive
import io.github.yeeuou.theFinalsPlugin.DummyPlayer
import io.github.yeeuou.theFinalsPlugin.DummyPlayer.Companion.asDummyFigure
import io.github.yeeuou.theFinalsPlugin.Figure.Companion.figure
import io.github.yeeuou.theFinalsPlugin.TFPlayer
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.getSpectatePlayer
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.tfPlayer
import io.github.yeeuou.theFinalsPlugin.TheFinalsPlugin
import io.github.yeeuou.theFinalsPlugin.TheFinalsPlugin.Companion.getLooseTargetEntity
import io.github.yeeuou.theFinalsPlugin.task.ReviveAnimationTask
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
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
    fun figureDestroy(ev: EntityDamageEvent) {
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
        ev.player.tfPlayer()?.run {
            val targetEntity = player.getLooseTargetEntity(1.75) ?: return
            if (targetEntity is ArmorStand) targetEntity.figure()?.let {
                // is self & check team
                if (it.owner == this || it.owner.tfTeam != this.tfTeam) return
                player.setMetadata("tf_holdRevive",
                    FixedMetadataValue(TheFinalsPlugin.instance, null))
                Bukkit.getServer().scheduler.runTaskTimer(
                    TheFinalsPlugin.instance,
                    ReviveAnimationTask(player, it),
                    0L, 1L
                )
            }
        }
        ev.player.getLooseTargetEntity(1.75)?.run {
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
        if (ev.cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            val tfP1 = ev.player.tfPlayer() ?: return
            val player = ev.player.spectatorTarget
            if (player !is Player) return
            val tfP2 = player.tfPlayer() ?: return
            if (tfP1.tfTeam != tfP2.tfTeam) ev.isCancelled = true
        }
    }

    @EventHandler
    fun spectatorClickMouseBtn(ev: PlayerInteractEvent) {
        if (ev.player.gameMode != GameMode.SPECTATOR) return
        ev.player.tfPlayer()?.run {
            if (!isDead || tfTeam.isAllPlayerDead()) return
            runCatching {
                player.spectatorTarget = runCatching {
                    if (ev.action.isRightClick) {
                        tfTeam.getPrevAlivePlayer(getSpectatePlayer()!!).player
                    } else tfTeam.getNextAlivePlayer(getSpectatePlayer()!!).player
                }.getOrElse { tfTeam.getFirstAlivePlayer().player }
            }.onFailure { it.printStackTrace() }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun enterSpector(ev: PlayerStartSpectatingEntityEvent) {
        ev.player.tfPlayer()?.run {
            if (ev.newSpectatorTarget !is Player) {
                ev.isCancelled = true
                return
            }
            val p = ev.newSpectatorTarget as Player
            if (p.tfPlayer()?.tfTeam != tfTeam)
                ev.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun exitSpector(ev: PlayerStopSpectatingEntityEvent) {
        ev.player.tfPlayer()?.run {
            if (canRespawn && isDead) lateRespawn()
            if (this in TFPlayer.spectatorPlayers)
                ev.isCancelled = true
        }
    }

    @EventHandler
    fun playerGetAdvancement(ev: PlayerAdvancementDoneEvent) {
        // check root advancement
        if (ev.advancement.parent == null
            // check recipe advancement
            || ev.advancement.key.key.startsWith("recipes"))
            return
        ev.player.tfPlayer()?.addCoin()
    }

    @EventHandler
    fun playerLeave(ev: PlayerQuitEvent) {
        ev.player.tfPlayer()?.unload()
    }

    @EventHandler
    fun playerJoin(ev: PlayerJoinEvent) {
        TFPlayer.tryLoad(ev.player)
    }
}
