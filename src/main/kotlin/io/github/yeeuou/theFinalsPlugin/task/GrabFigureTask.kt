package io.github.yeeuou.theFinalsPlugin.task

import io.github.yeeuou.theFinalsPlugin.Figure
import io.github.yeeuou.theFinalsPlugin.TFPlayer
import org.bukkit.entity.ArmorStand
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.function.Consumer

class GrabFigureTask(
    private val tfPlayer: TFPlayer,
    figure: Figure,
    private val stand: ArmorStand
): Consumer<BukkitTask> {
    init {
        tfPlayer.grab(figure)
    }
    override fun accept(task: BukkitTask) {
        if (tfPlayer.grabFigure == null /*|| player.grabFigure != figure*/) {
            task.cancel()
            return
        }
        val eyeDir = tfPlayer.player.eyeLocation.direction.normalize()
        val mvRight = eyeDir.clone().crossProduct(Vector(0, 1, 0)).normalize()
        stand.teleport(tfPlayer.player.eyeLocation.add(.0, -.5, .0).add(
            eyeDir.multiply(.9).add(mvRight.multiply(.5))
        ).apply { yaw -= 180 })
    }
}
