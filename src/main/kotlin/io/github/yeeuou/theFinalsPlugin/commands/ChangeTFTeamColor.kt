package io.github.yeeuou.theFinalsPlugin.commands

import com.mojang.brigadier.Command
import io.github.yeeuou.theFinalsPlugin.TFTeam
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.format.NamedTextColor

object ChangeTFTeamColor {
    val cmd = Commands.literal("tfteam-color")
        .then(Commands.argument("team", TFTeamArgument())
            .then(Commands.argument("color", ArgumentTypes.namedColor())
                .executes { ctx ->
                    val team = ctx.getArgument("team", TFTeam::class.java)
                    val color =
                        ctx.getArgument("color", NamedTextColor::class.java)
                    team.swapColor(color)
                    ctx.source.sender.sendMessage("팀 ${team.name.lowercase()} 의 색상을 $color 으로 바꿨습니다.")
                    Command.SINGLE_SUCCESS
                }
            )
        ).build()
}
