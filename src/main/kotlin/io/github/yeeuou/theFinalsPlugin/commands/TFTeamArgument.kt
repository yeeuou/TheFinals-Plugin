package io.github.yeeuou.theFinalsPlugin.commands

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.github.yeeuou.theFinalsPlugin.TFTeam
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture

class TFTeamArgument : CustomArgumentType<TFTeam, String> {
    override fun parse(p0: StringReader) = runCatching {
        TFTeam.valueOf(p0.readUnquotedString().uppercase())
    }.getOrElse { throw SimpleCommandExceptionType {
        "Not found team '${p0.readUnquotedString()}'."
    }.create()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        TFTeam.Companion.nameByTeam.filter {
            it.key.startsWith(builder.remainingLowerCase)
        }.forEach { builder.suggest(it.key) { "${it.value.color}" } }
        return builder.buildFuture()
    }

    override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()
}
