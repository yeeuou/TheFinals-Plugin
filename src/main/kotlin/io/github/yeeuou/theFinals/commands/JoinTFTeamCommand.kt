package io.github.yeeuou.theFinals.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.yeeuou.theFinals.DummyPlayer
import io.github.yeeuou.theFinals.TeamManager
import io.github.yeeuou.theFinals.TFTeam
import io.github.yeeuou.theFinals.TeamManager.tfPlayer
import io.github.yeeuou.theFinals.TeamManager.unregisterPlayer
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.entity.Vindicator
import java.util.concurrent.CompletableFuture

object JoinTFTeamCommand : AbstractCommand() {
    val joinCmd = Commands.literal("join-tfteam")
        .requires { it.sender.isOp }
        .then(Commands.argument("team", TFTeamArgument())
            .then(Commands.argument("target", ArgumentTypes.players())
                .executes { ctx ->
                    val tfTeam = ctx.getArgument("team", TFTeam::class.java)
                    val targets = ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                        .resolve(ctx.source)
                    targets.forEach {
                        TeamManager.registerNewPlayer(it, tfTeam)
                    }
                    ctx.source.sender.sendMessage(
                        text("${targets.size}명의 플레이어를 추가했습니다"))
                    Command.SINGLE_SUCCESS
                }
            )
        ).build()

    val ejectCmd = Commands.literal("leave-tfteam")
        .then(Commands.argument("target", ArgumentTypes.players())
            .executes {
                val target = it.getArgument("target",
                    PlayerSelectorArgumentResolver::class.java).resolve(it.source)
                var replaced = 0
                target.forEach {
                    if (it.tfPlayer()?.unregisterPlayer() != null) replaced++
                }
                it.source.sender.sendMessage("${replaced}명의 플레이어를 변경했습니다")
                Command.SINGLE_SUCCESS
            }).build()

    val testCmd = Commands.literal("tftest")
        .executes {
            if (it.source.sender is Player) {
                val loc = (it.source.sender as Player).location
                loc.world.spawn(loc, Pillager::class.java) {
                    it.setAI(false)
                    it.setGravity(false)
                    it.equipment.setItemInMainHand(null)
                    DummyPlayer(it)
                }
            }
            Command.SINGLE_SUCCESS
        }.build()
}

class TFTeamArgument : CustomArgumentType<TFTeam, String> {
    override fun parse(p0: StringReader) = runCatching {
        TFTeam.valueOf(p0.readUnquotedString().uppercase())
    }.getOrElse { throw SimpleCommandExceptionType { it.message ?: "???" }.create() }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        TFTeam.entries.map { it.name }.filter {
            it.lowercase().startsWith(builder.remainingLowerCase)
        }.forEach { builder.suggest(it.lowercase()) }
        return builder.buildFuture()
    }

    override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()
}
