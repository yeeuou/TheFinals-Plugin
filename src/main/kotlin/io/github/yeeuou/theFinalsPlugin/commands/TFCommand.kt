package io.github.yeeuou.theFinalsPlugin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.yeeuou.theFinalsPlugin.TFPlayer
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.tfPlayer
import io.github.yeeuou.theFinalsPlugin.TFTeam
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object TFCommand {
    val mainCmd = Commands.literal("tf-team")
        .requires { it.sender.isOp }
        .then(joinCmd())
        .then(leaveCmd())
        .then(ChangeTFTeamColor.cmd)
        .build()

    private fun joinCmd(): LiteralArgumentBuilder<CommandSourceStack> {
        val joinCmd = Commands.literal("join")
            .then(Commands.argument("team", TFTeamArgument())
                .executes(::joinCmdExeByTeam)
                .then(Commands.argument("target", ArgumentTypes.players())
                    .executes(::joinCmdExeByTeamAndPlayers))
            )
        return joinCmd
    }

    private fun joinCmdExeByTeam(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = ctx.source.sender
        if (sender !is Player) {
            sender.sendMessage(Component.text("플레이어만 팀에 참여할 수 있습니다.")
                .color(NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }
        val team = ctx.getArgument("team", TFTeam::class.java)
        TFPlayer.registerOrUpdatePlayer(sender, team)
        sender.sendMessage("팀 [${team.name.lowercase()}]에 추가되었습니다.")
        return Command.SINGLE_SUCCESS
    }

    private fun joinCmdExeByTeamAndPlayers(ctx: CommandContext<CommandSourceStack>): Int {
        val team = ctx.getArgument("team", TFTeam::class.java)
        val targets = ctx.getArgument("target",
            PlayerSelectorArgumentResolver::class.java).resolve(ctx.source)
        targets.forEach {
            TFPlayer.registerOrUpdatePlayer(it, team)
        }
        ctx.source.sender.sendMessage(Component.text(
            "${targets.size}명의 플레이어를 팀 [${team.name.lowercase()}]에 추가했습니다."))
        return Command.SINGLE_SUCCESS
    }

    private fun leaveCmd() =
        Commands.literal("leave")
            .executes(::leaveCmdExeBySelf)
            .then(Commands.argument("target", ArgumentTypes.players())
                .executes(::leaveCmdExeByPlayers))

    private fun leaveCmdExeBySelf(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = ctx.source.sender
        if (sender !is Player) {
            sender.sendMessage(Component.text("플레이어만 이 명령어에 영향을 받을 수 있습니다.")
                .color(NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }
        val player = sender.tfPlayer()
        if (player == null) {
            sender.sendMessage(Component.text("팀에 가입되어 있지 않습니다.")
                .color(NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }
        player.unregister()
        sender.sendMessage("팀 [${player.tfTeam.name.lowercase()}]에서 제거되었습니다.")
        return Command.SINGLE_SUCCESS
    }

    private fun leaveCmdExeByPlayers(ctx: CommandContext<CommandSourceStack>): Int {
        val targets = ctx.getArgument("target",
            PlayerSelectorArgumentResolver::class.java).resolve(ctx.source)
        var replaced = 0
        targets.forEach {
            it.tfPlayer()?.run {
                unregister()
                replaced++
            }
        }
        ctx.source.sender.sendMessage(
            Component.text("${replaced}명의 플레이어를 팀에서 제거했습니다."))
        return Command.SINGLE_SUCCESS
    }
}
