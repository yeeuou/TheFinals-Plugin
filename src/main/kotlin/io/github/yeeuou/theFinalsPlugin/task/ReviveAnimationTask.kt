package io.github.yeeuou.theFinalsPlugin.task

import io.github.yeeuou.theFinalsPlugin.Figure
import io.github.yeeuou.theFinalsPlugin.Figure.Companion.figure
import io.github.yeeuou.theFinalsPlugin.TheFinalsPlugin
import io.github.yeeuou.theFinalsPlugin.TheFinalsPlugin.Companion.getLooseTargetEntity
import io.github.yeeuou.theFinalsPlugin.events.GameEvents
import io.papermc.paper.util.Tick
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.time.Duration
import java.util.function.Consumer
import kotlin.math.round

class ReviveAnimationTask(
    private val player: Player,
    private val figure: Figure
) : Consumer<BukkitTask> {
    private companion object {
        val key = NamespacedKey(TheFinalsPlugin.instance, "multiple_0")
        val multiplyZeroModifier = AttributeModifier(key, -1.0,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1)
    }
    private var progress = 0
    private val reviveMaxProgress = 100
    private val noFadeInOut =
        Title.Times.times(Duration.ZERO, Tick.of(3), Duration.ZERO)

    override fun accept(task: BukkitTask) {
        // 부활 끊기
        val targetedFigure = (player.getLooseTargetEntity(1.75) as? ArmorStand)?.figure()
        if (targetedFigure == null || targetedFigure != figure) {
            player.removeMetadata("tf_holdRevive", TheFinalsPlugin.instance)
            player.getAttribute(Attribute.MOVEMENT_SPEED)?.removeModifier(key)
            player.getAttribute(Attribute.JUMP_STRENGTH)?.removeModifier(key)
            task.cancel()
            player.resetTitle()
            return
        }
        if (!player.isSneaking) {
            if (progress > 0) progress = 0 // 초기화 효과 추가
            player.getAttribute(Attribute.MOVEMENT_SPEED)?.removeModifier(key)
            player.getAttribute(Attribute.JUMP_STRENGTH)?.removeModifier(key)
            player.showTitle(Title.title(
                Component.text(""),
                Component.text("[길게 웅크려서 부활]"),
                noFadeInOut
            ))
        } else {
            player.getAttribute(Attribute.MOVEMENT_SPEED)?.run{
                if (multiplyZeroModifier !in modifiers) addModifier(multiplyZeroModifier)
            }
            player.getAttribute(Attribute.JUMP_STRENGTH)?.run{
                if (multiplyZeroModifier !in modifiers) addModifier(multiplyZeroModifier)
            }
            val sb = StringBuilder("[")
            val step = round(progress / 100.0 * 15).toInt()
            for (i in 1..15)
                if (i <= step) sb.append('=')
                else sb.append(' ')
            player.showTitle(Title.title(
                Component.text(""), Component.text("$sb]"),
                noFadeInOut
            ))
//            figure.owner.player.showTitle(Title.title(
//                Component.text("부활중"), Component.text("$sb]"),
//                noFadeInOut
//            ))
            if (progress >= reviveMaxProgress) {
                figure.owner.reviveFromFigure()
                player.removeMetadata("tf_holdRevive", TheFinalsPlugin.instance)
                player.getAttribute(Attribute.MOVEMENT_SPEED)?.removeModifier(key)
                player.getAttribute(Attribute.JUMP_STRENGTH)?.removeModifier(key)
                player.resetTitle()
                task.cancel()
            }
            else progress++
        }
    }
}
