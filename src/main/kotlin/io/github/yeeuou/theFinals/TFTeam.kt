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

    fun isAllPlayerDead(): Boolean { // 오프라인인 플레이어가 리스트에 없도록 처리
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

    // 다음 플레이어 관전
    fun getNextAlivePlayer(current: TFPlayer): TFPlayer {
        val alivePlayers = players.filter { !it.isDead }
        if (alivePlayers.isEmpty()) throw IllegalStateException("This team is wiped.")
        // 한명만 살았을 때
        if (alivePlayers.size == 1) return current
        val currentIndex = alivePlayers.indexOf(current)
//        // 현재 관전중인 플레이어가 리스트의 끝에 있을 때
//        if (alivePlayers.lastIndex == currentIndex) return alivePlayers[0]
        return alivePlayers[(currentIndex + 1) % alivePlayers.lastIndex]
    }

    // 이전 플레이어 관전
    fun getPrevAlivePlayer(current: TFPlayer): TFPlayer {
        val alivePlayers = players.filter { !it.isDead }
        if (alivePlayers.isEmpty()) throw IllegalStateException("This team is wiped.")
        // 한명만 살았을 때
        if (alivePlayers.size == 1) return current
        val currentIndex = alivePlayers.indexOf(current)
        return alivePlayers[(currentIndex - 1 + alivePlayers.lastIndex) % alivePlayers.lastIndex]
    }

    fun getFirstAlivePlayer() = players.firstOrNull { !it.isDead }
        ?: throw IllegalStateException("This team is wiped.")

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
                            Ticks.duration(13)
                        )
                    )
                )
                inventory.clear()
                exp = 0f
                level = 0
            }
            it.startTeamRespawnTask()
        }
    }

    val team: Team
        get() = Bukkit.getScoreboardManager().mainScoreboard.run {
            getTeam("TF_$color") ?:
            registerNewTeam("TF_$color").apply {
                color(this@TFTeam.color)
                setCanSeeFriendlyInvisibles(true)
                setAllowFriendlyFire(false)
            }
        }
}