package io.github.yeeuou.theFinals.task

import io.github.yeeuou.theFinals.Figure
import io.github.yeeuou.theFinals.Figure.Companion.figure
import io.github.yeeuou.theFinals.TheFinals
import io.papermc.paper.util.Tick
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.function.Consumer

class ReviveAnimationTask(
    private val player: Player,
    private val figure: Figure
) : Consumer<BukkitTask> {
    private var progress = 0
    private val reviveMaxProgress = 100
    private val noFadeInOut =
        Title.Times.times(Duration.ZERO, Tick.of(3), Duration.ZERO)

    override fun accept(task: BukkitTask) {
        // 부활 끊기
        val targetedFigure = (player.getTargetEntity(3) as? ArmorStand)?.figure()
        if (targetedFigure == null || targetedFigure != figure) {
            player.removeMetadata("tf_holdRevive", TheFinals.instance)
            task.cancel()
            player.resetTitle()
            return
        }
        if (!player.isSneaking) {
            if (progress > 0) progress = 0 // 초기화 효과 추가
            player.showTitle(Title.title(
                Component.text(""),
                Component.text("[길게 웅크려서 부활]"),
                noFadeInOut
            ))
        } else {
            // TODO 부활중 움직일 수 없도록
            val sb = StringBuilder("[")
            for (i in 1..20)
                // 진행 바가 끝까지 도달하게 함
                if (i * 5 <= progress + 1) sb.append('=')
                else sb.append(' ')
            player.showTitle(Title.title(
                Component.text(""), Component.text("$sb]"),
                noFadeInOut
            ))
            if (progress >= reviveMaxProgress) {
                figure.owner.reviveFromFigure()
                player.removeMetadata("tf_holdRevive", TheFinals.instance)
                player.resetTitle()
                task.cancel()
            }
            else progress++
        }
    }
}
