package io.github.yeeuou.theFinals.events

import io.github.yeeuou.theFinals.Figure.Companion.figure
import io.github.yeeuou.theFinals.TeamManager.tfPlayer
import io.github.yeeuou.theFinals.TheFinals
import io.github.yeeuou.theFinals.task.ReviveAnimation
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
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
        if (ev.entity.type == EntityType.ARMOR_STAND
            && ev.entity.scoreboardTags.contains("tf_figure"))
            ev.isCancelled = true
    }

    @EventHandler
    fun playerLookAtFigure(ev: PlayerMoveEvent) {
        ev.player.getMetadata("tf_holdRevive")
            .forEach { if (it.owningPlugin is TheFinals) return }
        ev.player.getTargetEntity(3)?.run {
            val figure = (this as? ArmorStand)?.figure() ?: return
            ev.player.setMetadata("tf_holdRevive",
                FixedMetadataValue(TheFinals.instance, null))
            server.scheduler.runTaskTimer(
                TheFinals.instance,
                ReviveAnimation(ev.player, figure),
                0L, 1L
            )
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun spectatorTeleport(ev: PlayerTeleportEvent) {
        if (ev.cause == PlayerTeleportEvent.TeleportCause.SPECTATE)
            ev.isCancelled = true
    }
}
