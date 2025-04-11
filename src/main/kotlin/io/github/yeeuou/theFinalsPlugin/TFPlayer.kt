package io.github.yeeuou.theFinalsPlugin

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.function.Consumer
import kotlin.random.Random

class TFPlayer(
    val player: Player,
    tfTeam: TFTeam
) {
    companion object {
        private val rainbowColors = arrayOf(
            NamedTextColor.RED,
            NamedTextColor.GOLD,
            NamedTextColor.YELLOW,
            NamedTextColor.GREEN,
            NamedTextColor.AQUA,
            NamedTextColor.BLUE,
            NamedTextColor.DARK_PURPLE
        )
        val spectatorPlayers = mutableMapOf<TFPlayer, TFPlayer>()
        fun TFPlayer.getSpectatePlayer() = spectatorPlayers[this]
    }

    var tfTeam = tfTeam
        private set

    private var coin: Int = TFConfig.startCoin

    private var respawnTime = 0

    var isDead = false
        private set

    private var waitTeamRespawn = false

    private val figure = Figure(this)

    init {
        tfTeam.addPlayer(this)
    }

    // 플레이어 스탯 저장(접속시 불러오기, 플레이어가 나갈 때/플러그인 언로드시 저장)

    fun changeTeam(to: TFTeam) {
        tfTeam.removePlayer(this)
        tfTeam = to
        to.addPlayer(this)
    }

    fun playDeadEffect() {
        player.gameMode = GameMode.SPECTATOR
        player.spawnParticle(
            Particle.BLOCK,
            player.location.add(0.0 ,1.0, 0.0),
            250,
            .2,
            .45,
            .2,
            Material.GOLD_BLOCK.createBlockData()
        )
        player.world.playSound(
            player.location, Sound.BLOCK_CHAIN_BREAK,
            .5f, 1f
        )
        isDead = true
        if (tfTeam.isAllPlayerDead()) {
            respawnTime = TFConfig.teamRespawnTick - 10
            tfTeam.playTeamWipeEffect()
        } else {
            figure.spawn()
            Bukkit.getScheduler().runTaskTimer(
                TheFinalsPlugin.instance,
                SpectateOnTeam(),
                20 * 3, 1
            )
            if (coin > 0) {
                respawnTime = TFConfig.playerRespawnTick
                Bukkit.getScheduler().runTaskTimer(
                    TheFinalsPlugin.instance,
                    WaitRespawnTime(),
                    0, 1
                )
            } else Bukkit.getScheduler().runTaskTimer(
                TheFinalsPlugin.instance,
                WaitReviveFromTeam(),
                0, 1
            )
        }
    }

    fun respawn(useCoin: Boolean) {
        if (useCoin) coin--
        figure.remove()
        isDead = false
        // 위치가 없으면 오버월드에서 리스폰
        val respawnLoc = player.respawnLocation ?:
        Bukkit.getServer().worlds.first().spawnLocation.apply {
            Random(System.nanoTime()).let {
                x = x + it.nextInt(7) - 3
                z = z + it.nextInt(7) - 3
            }
        }.toCenterLocation().toHighestLocation().add(0.0,1.0,0.0)
        playerRespawnProcess(respawnLoc)
    }

    fun reviveFromFigure() {
        val loc = requireNotNull(figure.figureLoc) { "No revive without figure!" }
        figure.remove()
        isDead = false
        playerRespawnProcess(loc)
    }

    private fun playerRespawnProcess(loc: Location) {
        player.gameMode = GameMode.SURVIVAL
        player.teleport(loc)
        player.noDamageTicks = 20 * 2
        player.foodLevel = 20
        player.saturation = 5f
        player.arrowsInBody = 0
        spectatorPlayers.remove(this)
    }

    private inner class WaitRespawnTime : Consumer<BukkitTask> {
        private var tick = 0
        override fun accept(task: BukkitTask) {
            if (!isDead || waitTeamRespawn) {
                task.cancel()
                return
            }
            if (tick++ % 20 != 0) return
            if (respawnTime > 0) {
                respawnTime--
                player.sendActionBar(
                    Component.text("리스폰 가능까지: $respawnTime (C): $coin"))
            } else {
                Bukkit.getScheduler().runTaskTimer(
                    TheFinalsPlugin.instance,
                    PressStartTask(),
                    0, 1
                )
                task.cancel()
            }
        }
    }

    private inner class WaitReviveFromTeam : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (!isDead || waitTeamRespawn) {
                player.resetTitle()
                task.cancel()
                return
            }
            player.sendActionBar(Component.text("부활이나 팀 전멸을 기다리세요"))
        }
    }

    /**
     * 매 초마다 업데이트
     */
    private inner class WaitTeamRespawn : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (respawnTime > 0) {
                player.sendActionBar(
                    Component.text("팀 리스폰까지: $respawnTime"))
                respawnTime--
            } else {
                waitTeamRespawn = false
                respawn(false)
                player.sendActionBar(Component.text())
                task.cancel()
            }
        }
    }
    fun startTeamRespawnTask() {
        figure.remove()
        waitTeamRespawn = true
        Bukkit.getScheduler().runTaskTimer(
            TheFinalsPlugin.instance,
            WaitTeamRespawn(),
            0, 20
        )
    }

    private inner class PressStartTask : Consumer<BukkitTask> {
        private var colorIndex = 0
            set(value) {
                field = value % rainbowColors.lastIndex
            }
        private var tick = 0

        override fun accept(task: BukkitTask) {
            if (!isDead || waitTeamRespawn) {
                player.resetTitle()
                task.cancel()
                return
            }
            if (tick++ % 10 != 0) return
            player.showTitle(Title.title(
                Component.text(""),
                Component.text("SNEAK START")
                    .decorate(TextDecoration.BOLD).color(rainbowColors[colorIndex++]),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
            ))
            player.sendActionBar(
                Component.text()
                    .append(
                        Component.text("(웅크리기로 부활) ")
                            .decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY)
                    ).append(Component.text("크레딧 $coin"))
            )
        }
    }

    private inner class SpectateOnTeam : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (!isDead || waitTeamRespawn) {
                task.cancel()
                return
            }
            if (getSpectatePlayer() == null)
                spectatorPlayers[this@TFPlayer] = tfTeam.getFirstAlivePlayer()
            if (player.spectatorTarget == null)
                player.spectatorTarget = getSpectatePlayer()?.player
        }
    }
}
