package io.github.yeeuou.theFinalsPlugin.commands

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player

object ColorTest {
    val cmd = Commands.literal("color_test").executes { ctx ->
        if (ctx.source.sender is Player)
            (ctx.source.sender as Player).run {
                val board = Bukkit.getScoreboardManager().mainScoreboard
                var posAddX = 1.0
                for (color in colorList) {
                    val team = board.getTeam("tf_test_$color")
                        ?: board.registerNewTeam("tf_test_$color").apply {
                            color(color)
                        }
                    world.spawn(
                        location.add(posAddX, 0.0, 0.0)
                            .setRotation(0f, 0f),
                        Pillager::class.java
                    ) { pillager ->
                        pillager.equipment.setItemInMainHand(null)
                        pillager.setAI(false)
                        pillager.removeWhenFarAway = false
                        pillager.isSilent = true
                        pillager.equipment.helmet = null
                        team.addEntity(pillager)
                        pillager.isGlowing = true
                        pillager.copy(pillager.location.add(0.0, 3.0, 0.0))
                            .let { copy ->
                                copy.isInvisible = true
                                team.addEntity(copy)
                            }
                    }
                    posAddX += 1.5
                }
            }
        Command.SINGLE_SUCCESS
    }.build()

    private val colorList = listOf(
        NamedTextColor.RED, NamedTextColor.DARK_RED,
        NamedTextColor.AQUA, NamedTextColor.DARK_AQUA,
        NamedTextColor.BLUE, NamedTextColor.DARK_BLUE,
        NamedTextColor.GREEN, NamedTextColor.DARK_GREEN,
        NamedTextColor.LIGHT_PURPLE, NamedTextColor.DARK_PURPLE,
        NamedTextColor.GOLD, NamedTextColor.YELLOW,
        NamedTextColor.WHITE,
        NamedTextColor.GRAY, NamedTextColor.DARK_GRAY,
        NamedTextColor.BLACK
    )
}
