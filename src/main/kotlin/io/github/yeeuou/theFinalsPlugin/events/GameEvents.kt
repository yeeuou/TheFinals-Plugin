package io.github.yeeuou.theFinalsPlugin.events

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent
import io.github.yeeuou.theFinalsPlugin.Figure.Companion.figure
import io.github.yeeuou.theFinalsPlugin.TFConfig
import io.github.yeeuou.theFinalsPlugin.TFPlayer
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.tfPlayer
import io.github.yeeuou.theFinalsPlugin.TFPlugin
import io.github.yeeuou.theFinalsPlugin.TFPlugin.Companion.getLooseTargetArmorStand
import io.github.yeeuou.theFinalsPlugin.task.ReviveAnimationTask
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
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
    fun onFigureDamage(ev: EntityDamageEvent) {
        (ev.entity as? ArmorStand)?.figure()?.let {
            ev.isCancelled = true
            return
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onFigureDead(ev: EntityDeathEvent) {
        (ev.entity as? ArmorStand)?.figure()
            ?.let { ev.isCancelled = true }
    }

    @EventHandler
    fun playerLookAtFigure(ev: PlayerMoveEvent) {
        ev.player.getMetadata("tf_holdRevive")
            .forEach { if (it.owningPlugin is TFPlugin) return }
        ev.player.tfPlayer()?.run {
            if (this in TFPlayer.playerGrabFigure) return
            val targetEntity =
                player.getLooseTargetArmorStand(TFConfig.REVIVE_MAX_RANGE) ?: return
            targetEntity.figure()?.let {
                // is self & check team
                if (it.owner == this || it.owner.tfTeam != this.tfTeam) return
                player.setMetadata("tf_holdRevive",
                    FixedMetadataValue(TFPlugin.instance, null))
                Bukkit.getServer().scheduler.runTaskTimer(
                    TFPlugin.instance,
                    ReviveAnimationTask(player, it),
                    0L, 1L
                )
            }
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
    fun onClickMouseBtn(ev: PlayerInteractEvent) {
        if (ev.player.gameMode == GameMode.SPECTATOR) return
        ev.player.tfPlayer()?.run {
            if (grabFigure == null && ev.action.isLeftClick) {
                player.getLooseTargetArmorStand(TFConfig.GRAB_MAX_RANGE)
                    ?.figure()?.let {
                        it.startGrabTask(this)
                        ev.isCancelled = true
                    }
                return
            }
            if (grabFigure == null) return
            if (ev.action.isLeftClick) throwFigure()
            else putDownFigure()
            ev.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerAttackFigure(ev: PrePlayerAttackEntityEvent) {
        ev.player.tfPlayer()?.run {
            if (grabFigure != null) return
            (ev.attacked as? ArmorStand)?.figure()
                ?.let {
                    it.startGrabTask(this)
                    ev.isCancelled = true
                }
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
        ev.player.tfPlayer()?.let {
            if (it in TFPlayer.spectatorPlayers)
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
        runCatching{ TFPlayer.tryLoad(ev.player) }
            .onFailure {
                TFPlugin.instance.logger.warning("Failed load player: ${it.message}")
            }
    }
}
