package io.github.yeeuou.theFinals.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import io.github.yeeuou.theFinals.TeamManager
import io.github.yeeuou.theFinals.TeamName
import io.github.yeeuou.theFinals.joinTeam
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Villager

class TFJoinTeamCommand : AbstractCommand() {
    private val cmd = Commands.literal("tf_jointeam")
    fun build() =
        cmd
            .requires { it.sender.isOp }
            .then(Commands.argument("team", StringArgumentType.word())
                .then(Commands.argument("target", ArgumentTypes.entities())
                    .executes { ctx ->
                        val tfTeam: TeamName = runCatching {
                            TeamName.valueOf(
                                StringArgumentType.getString(ctx, "team").uppercase())
                        }.getOrElse {
                            ctx.source.sender.sendMessage(
                                text("There is no team of the designated name.")
                                    .color(NamedTextColor.RED)
                            )
                            return@executes Command.SINGLE_SUCCESS
                        }
                        val targets = ctx.getArgument("target", EntitySelectorArgumentResolver::class.java)
                            .resolve(ctx.source)
                        var replaced = 0
                        targets.forEach {
                            if (it is LivingEntity) {
                                it.joinTeam(tfTeam)
                                replaced++
                            }
                        }
                        ctx.source.sender.sendMessage(
                            text("$replaced/${targets.size} 만큼의 개체를 변경했습니다."))
                        Command.SINGLE_SUCCESS
                    }
                )
            ).build()
    val testCmd = Commands.literal("tftest")
        .executes {
            if (it.source.sender is Player) {
                val loc = (it.source.sender as Player).location
                loc.world.spawn(loc, Villager::class.java) {
                    it.setAI(false)
                    it.setGravity(false)
                    it.joinTeam(TeamName.RED)
                    it.addScoreboardTag(TeamManager.TAG_DUMMY)
                }
            }
            Command.SINGLE_SUCCESS
        }.build()
}
