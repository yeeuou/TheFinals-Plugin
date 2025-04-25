package io.github.yeeuou.theFinalsPlugin.commands

import com.mojang.brigadier.Command
import io.github.yeeuou.theFinalsPlugin.TFTeam
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender

object ChangeTFTeamColor {
    val cmd = Commands.literal("color")
        .then(Commands.argument("team", TFTeamArgument())
            .then(Commands.argument("color", ArgumentTypes.namedColor())
                .executes { ctx ->
                    val team = ctx.getArgument("team", TFTeam::class.java)
                    val color =
                        ctx.getArgument("color", NamedTextColor::class.java)
                    val beforeColor = team.color
                    team.swapColor(color)
                    val message = Component.text("팀 ")
                        .append(Component.text("[${team.name.lowercase()}]")
                            .color(beforeColor))
                        .append(Component.text(" 의 색상을 "))
                        .append(Component.text("[$color]").color(color))
                        .append(Component.text(" 으로 바꿨습니다."))
                    ctx.source.sender.sendMessage(message)
                    if (ctx.source.sender !is ConsoleCommandSender)
                        Bukkit.getConsoleSender().sendMessage(Component.text('[',
                            NamedTextColor.GRAY, TextDecoration.ITALIC)
                            .append(message)
                            .append(Component.text(']')))
                    Command.SINGLE_SUCCESS
                }
            )
        ).build()
}
