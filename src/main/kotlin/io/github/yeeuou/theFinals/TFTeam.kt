package io.github.yeeuou.theFinals

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Team
import java.time.Duration

enum class TFTeam(val color: NamedTextColor) {
    BLUE(NamedTextColor.BLUE), RED(NamedTextColor.RED),
    PURPLE(NamedTextColor.DARK_PURPLE), PINK(NamedTextColor.LIGHT_PURPLE);

    private val players = mutableListOf<TFPlayer>()

    fun isAllPlayerDead(): Boolean {
        players.filter { it.player.isOnline }.forEach {
            if (!it.isDead) return false
        }
        return true
    }

    fun addPlayer(p: TFPlayer) {
        players.add(p)
        team.addPlayer(p.player)
    }
    fun removePlayer(p: TFPlayer) {
        players.remove(p)
        team.removePlayer(p.player)
    }

    fun getFirstAlivePlayer() = players.firstOrNull { !it.isDead }

    fun playTeamWipeEffect() {
        players.filter { it.player.isOnline }.forEach {
            it.player.run {
                showTitle(
                    Title.title(
                        Component.text("팀 전멸!")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD, TextDecoration.ITALIC),
                        Component.text("모든 아이템을 잃었습니다!"),
                        Title.Times.times(
                            Ticks.duration(10),
                            Duration.ofSeconds(2),
                            Ticks.duration(15)
                        )
                    )
                )
                inventory.clear()
            }
            it.startTeamRespawnTask()
            it.figure.remove()
        }
    }

    val team: Team
        get() {
            val board = Bukkit.getScoreboardManager().mainScoreboard
            return board.getTeam("TF_$color") ?: board.registerNewTeam("TF_$color")
                .apply {
                    color(this@TFTeam.color)
                    setCanSeeFriendlyInvisibles(true)
                    setAllowFriendlyFire(false)
                }
        }
}