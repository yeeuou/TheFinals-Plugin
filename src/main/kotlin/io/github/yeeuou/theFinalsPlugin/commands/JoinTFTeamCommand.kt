package io.github.yeeuou.theFinalsPlugin.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.yeeuou.theFinalsPlugin.DummyPlayer
import io.github.yeeuou.theFinalsPlugin.TFPlayer
import io.github.yeeuou.theFinalsPlugin.TFTeam
import io.github.yeeuou.theFinalsPlugin.TFPlayer.Companion.tfPlayer
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.Component
import org.bukkit.entity.Pillager
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

object JoinTFTeamCommand {
    val joinCmd = Commands.literal("join-tfteam")
        .requires { it.sender.isOp }
        .then(Commands.argument("team", TFTeamArgument())
            .then(Commands.argument("target", ArgumentTypes.players())
                .executes { ctx ->
                    val tfTeam = ctx.getArgument("team", TFTeam::class.java)
                    val targets = ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                        .resolve(ctx.source)
                    targets.forEach {
                        TFPlayer.registerOrUpdatePlayer(it, tfTeam)
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
                target.forEach { p ->
                    if (p.tfPlayer()?.unregister() != null) replaced++
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

    private fun text(str: String) = Component.text(str)
}

class TFTeamArgument : CustomArgumentType<TFTeam, String> {
    override fun parse(p0: StringReader) = runCatching {
        TFTeam.valueOf(p0.readUnquotedString().uppercase())
    }.getOrElse { throw SimpleCommandExceptionType { it.message ?: "???" }.create() }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> { // TODO 색깔 툴팁 추가
        TFTeam.nameByTeam.filter {
            it.key.startsWith(builder.remainingLowerCase)
        }.forEach { builder.suggest(it.key) { it.value.color.examinableName() } }
//        TFTeam.teamNames.filter {
//            it.startsWith(builder.remainingLowerCase)
//        }.forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()
}
