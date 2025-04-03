package io.github.yeeuou.theFinals

import io.github.yeeuou.theFinals.TeamManager.activePlayerTeam
import io.github.yeeuou.theFinals.TeamManager.tfActivated
import io.github.yeeuou.theFinals.TeamManager.tfInactive
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

object TeamManager {
    const val TAG_JOINED = "tf_joined"
    const val TAG_DUMMY = "tfDummy"

    val activePlayerTeam = mutableMapOf<LivingEntity, TeamName>()
    fun LivingEntity.tfActivated(t: TeamName) {
        activePlayerTeam[this] = t
    }
    fun LivingEntity.tfInactive() {
        activePlayerTeam.remove(this)
    }
}

//object PlayerCoinTracker

fun entityJoinToTeam(target: LivingEntity, dest: TeamName) {
    target.run {
        dest.join(this)
        removeWhenFarAway = false
        addScoreboardTag(TeamManager.TAG_JOINED)
        tfActivated(dest)
    }
}

fun LivingEntity.removeTfTeam() {
    getTfTeam()?.removePlayer(this) ?: return
    tfInactive()
}

fun LivingEntity.joinTeam(dest: TeamName) = entityJoinToTeam(this, dest)

fun LivingEntity.getTfTeam() = activePlayerTeam[this]

enum class TeamName(val color: NamedTextColor) {
    BLUE(NamedTextColor.BLUE), RED(NamedTextColor.RED),
    PURPLE(NamedTextColor.DARK_PURPLE), PINK(NamedTextColor.LIGHT_PURPLE);

    private val players = mutableListOf<LivingEntity>()

    fun join(entity: LivingEntity) {
        players.add(entity)
        team.addEntity(entity)
    }

    fun isDummy(entity: LivingEntity) =
        entity !is Player && entity.scoreboardTags.contains(TeamManager.TAG_DUMMY)

    fun removePlayer(entity: LivingEntity) {
        players.remove(entity)
        team.removeEntity(entity)
    }

    val team: Team
        get() {
            val board = Bukkit.getScoreboardManager().mainScoreboard
            return board.getTeam("TF_$color") ?: board.registerNewTeam("TF_$color")
                .apply {
                    color(this@TeamName.color)
                    setCanSeeFriendlyInvisibles(true)
                    setAllowFriendlyFire(false)
                }
        }
}
