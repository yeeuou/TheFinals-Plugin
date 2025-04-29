package io.github.yeeuou.theFinalsPlugin

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Team
import java.io.File
import java.time.Duration
import java.util.function.Consumer

enum class TFTeam(color: NamedTextColor) {
    LIVE_WIRES(NamedTextColor.BLUE), RETROS(NamedTextColor.RED),
    MIGHTY(NamedTextColor.DARK_PURPLE), BOUNDLESS(NamedTextColor.LIGHT_PURPLE),
    VOGUES(NamedTextColor.GOLD), BIG_SPLASH(NamedTextColor.GREEN),
    SOCIALITES(NamedTextColor.AQUA), SHOCK_AND_AWE(NamedTextColor.YELLOW),
    STEAMROLLERS(NamedTextColor.BLACK), OVERDOGS(NamedTextColor.GRAY),
    ULTRA_RARES(NamedTextColor.DARK_GREEN), JET_SETTERS(NamedTextColor.DARK_BLUE),
    KINGFISH(NamedTextColor.DARK_RED), TOUGH_SHELLS(NamedTextColor.DARK_GRAY),
    POWERHOUSES(NamedTextColor.DARK_AQUA), HIGH_NOTES(NamedTextColor.WHITE);

    companion object {
        val nameByTeam = buildMap {
            TFTeam.entries.forEach {
                set(it.name.lowercase(), it)
            }
        }
        private val colorDefFile =
            File(TFPlugin.instance.dataFolder, "teamColor.yml")
        fun loadColor() {
            if (!colorDefFile.exists()) {
                saveColor()
                return
            }
            val config = YamlConfiguration.loadConfiguration(colorDefFile)
            val unsolvedTeams = mutableListOf<TFTeam>()
            val colorList = entries.map { it.color }.toMutableList()
            for ((k, v) in nameByTeam) {
                val s = config.getString(k)
                if (s == null) {
                    unsolvedTeams.add(v); continue
                }
                // disallow color overlap
                val c = colorList.firstOrNull { it.toString() == s }
                if (c == null) {
                    unsolvedTeams.add(v); continue
                }
                v.color = c
                colorList.remove(c)
            }
            // handling when config file has an error
            if (unsolvedTeams.isNotEmpty()) {
                (unsolvedTeams zip colorList).forEach {
                    it.first.color = it.second
                }
                saveColor()
            }
        }
        internal fun saveColor() {
            colorDefFile.apply { parentFile.mkdirs() }
            val yaml = YamlConfiguration()
            nameByTeam.forEach { yaml.set(it.key, "${it.value.color}") }
            yaml.save(colorDefFile)
        }
    }

    private val players = mutableListOf<TFPlayer>()

    internal var color = color
        private set(c) {
            team.color(c)
            field = c
        }

    var teamWiped = false
        private set

    private var teamRespawnTime = 0

    fun isAllPlayerDead(): Boolean {
        players.forEach {
            if (!it.isDead) return false
        }
        return true
    }

    internal fun addPlayer(p: TFPlayer) {
        players.add(p)
        team.addPlayer(p.player)
    }
    internal fun removePlayer(p: TFPlayer) {
        players.remove(p)
        team.removePlayer(p.player)
        if (TFPlugin.loaded && !teamWiped
            && players.isNotEmpty() && isAllPlayerDead())
            playTeamWipeEffect()
    }

    // 다음 플레이어 관전
    fun getNextAlivePlayer(current: TFPlayer): TFPlayer {
        check(!teamWiped) { "This team already wiped." }
        val alivePlayers = players.filter { !it.isDead }
//        if (alivePlayers.isEmpty()) throw IllegalStateException("This team is wiped.")
        // 한명만 살았을 때
        if (alivePlayers.size == 1) return current
        val currentIndex = alivePlayers.indexOf(current)
        return alivePlayers[(currentIndex + 1) % alivePlayers.lastIndex]
    }

    // 이전 플레이어 관전
    fun getPrevAlivePlayer(current: TFPlayer): TFPlayer {
        check(!teamWiped) { "This team already wiped." }
        val alivePlayers = players.filter { !it.isDead }
//        if (alivePlayers.isEmpty()) throw IllegalStateException("This team is wiped.")
        // 한명만 살았을 때
        if (alivePlayers.size == 1) return current
        val currentIndex = alivePlayers.indexOf(current)
        return alivePlayers[(currentIndex - 1 + alivePlayers.lastIndex) % alivePlayers.lastIndex]
    }

    fun getFirstAlivePlayer() =
        checkNotNull(players.firstOrNull { !it.isDead }) { "This team already wiped." }

    internal fun playTeamWipeEffect() {
        if (players.isEmpty()) return
        teamWiped = true
        players.forEach {
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
                if (TFConfig.clearInventoryWhenTeamWipe) {
                    inventory.clear()
                    exp = 0f
                    level = 0
                }
            }
            teamRespawnHandler.addPlayer(it)
            it.onTeamWiped()
        }
        teamRespawnHandler.startTask()
    }

    /** 성공시 true, 실패시 false */
    internal fun tryAddTeamRespawn(p: TFPlayer): Boolean {
        if (!teamWiped || teamRespawnTime < 6) return false
        teamRespawnHandler.addPlayer(p)
        return true
    }

    fun swapColor(newColor: NamedTextColor) {
        if (newColor == color) return
        entries.first { it.color == newColor }.color = color
        color = newColor
    }

//    fun addFigure(e: ArmorStand) { // 생성된 피규어의 색이 바뀌게?
//        team.addEntity(e)
//    }

    private val team: Team
        get() = Bukkit.getScoreboardManager().mainScoreboard.run {
            getTeam("TF_$name") ?:
            registerNewTeam("TF_$name").apply {
                setCanSeeFriendlyInvisibles(true)
                setAllowFriendlyFire(false)
                setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OWN_TEAM)
                setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS)
            }
        }

    private val teamRespawnHandler = TeamRespawnHandler()

    private inner class TeamRespawnHandler : Consumer<BukkitTask> {
        private val deadPlayers = mutableSetOf<TFPlayer>()
        private var isRunning = false

        fun addPlayer(p: TFPlayer) {
            if (p.valid && p.isDead)
                deadPlayers.add(p)
        }
        fun startTask() {
            check(!isRunning) { "This team already wiped." }
            teamRespawnTime = TFConfig.teamRespawnTime
            Bukkit.getScheduler().runTaskTimer(
                TFPlugin.instance,
                this,
                0, 20
            )
        }
        override fun accept(task: BukkitTask) {
            deadPlayers.removeIf { !it.valid }
            if (deadPlayers.isEmpty()) {
                teamRespawnTime = 0
                teamWiped = false; isRunning = false
                task.cancel()
            }
            if (teamRespawnTime > 0) {
                deadPlayers.forEach {
                    it.player.sendActionBar(
                        Component.text("팀 리스폰까지: $teamRespawnTime")
                    )
                }; teamRespawnTime--
            } else {
                deadPlayers.forEach {
                    it.respawn(false)
                    it.player.sendActionBar(Component.text())
                }
                deadPlayers.clear()
                teamWiped = false; isRunning = false
                task.cancel()
            }
        }
    }
}
