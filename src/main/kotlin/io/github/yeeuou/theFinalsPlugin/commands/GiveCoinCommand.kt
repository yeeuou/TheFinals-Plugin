package io.github.yeeuou.theFinalsPlugin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.tfPlayer
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object GiveCoinCommand {
    val cmd = Commands.literal("give-coin")
        .requires { it.sender.isOp }
        .then(Commands.argument("amount",
            IntegerArgumentType.integer(1))
            .executes { ctx ->
                val sender = ctx.source.sender
                if (sender !is Player) {
                    sender.sendMessage(Component.text(
                        "플레이어만 이 명령어에 영향을 받을 수 있습니다.",
                        NamedTextColor.RED))
                    return@executes Command.SINGLE_SUCCESS
                }
                val tfPlayer = sender.tfPlayer()
                if (tfPlayer == null) {
                    sender.sendMessage(Component.text("팀에 가입되어 있지 않습니다.",
                        NamedTextColor.RED))
                    return@executes Command.SINGLE_SUCCESS
                }
                val amt = ctx.getArgument("amount", Int::class.java)
                tfPlayer.addCoin(amt)
                Command.SINGLE_SUCCESS
            }.then(Commands.argument("target", ArgumentTypes.players())
                .executes { ctx ->
                    val amt = ctx.getArgument("amount", Int::class.java)
                    var replaced = 0
                    ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                        .resolve(ctx.source).forEach {
                            it.tfPlayer()?.run {
                                addCoin(amt)
                                replaced++
                            }
                        }
                    ctx.source.sender.sendMessage("${replaced}명의 플레이어에게 ${amt}개의 코인을 지급했습니다.")
                    Command.SINGLE_SUCCESS
                }
            )
        ).build()
}
