package io.github.yeeuou.theFinals

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
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
    }

    var tfTeam = tfTeam
        private set
    private var coin: Int = TFSettings.startCoin

    private var respawnTime = 0

    var isDead = false
        private set

    private var changeWaitAnimation = false

    val figure = Figure(this)

//    private var waitTask: BukkitTask? = null

    init {
        tfTeam.addPlayer(this)
    }

    fun changeTeam(to: TFTeam) {
        tfTeam.removePlayer(this)
        tfTeam = to
        to.addPlayer(this)
    }

    fun playDeadEffect() {
        player.gameMode = GameMode.SPECTATOR
        player.spawnParticle(
            Particle.BLOCK_CRUMBLE,
            player.location.add(0.0 ,1.0, 0.0),
            300,
            .2,
            .45,
            .2,
            Material.GOLD_BLOCK.createBlockData()
        )
        isDead = true
        if (tfTeam.isAllPlayerDead()) {
            respawnTime = TFSettings.teamRespawnTick - 10
            tfTeam.playTeamWipeEffect()
        } else {
            figure.spawn()
            if (coin > 0) {
                respawnTime = TFSettings.playerRespawnTick
                Bukkit.getScheduler().runTaskTimer(
                    TheFinals.instance,
                    WaitingRespawn(),
                    0L, 20L
                )
            } else Bukkit.getScheduler().runTaskTimer(
                TheFinals.instance,
                WaitReviveFromTeam(),
                0L, 20L
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
        player.gameMode = GameMode.SURVIVAL
        player.teleport(respawnLoc)
        player.noDamageTicks = 20 * 2
        player.foodLevel = 20
        player.saturation = 5f
//        player.addPotionEffect(PotionEffectType.RESISTANCE.createEffect(5, 5))
    }

    private inner class WaitingRespawn : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (changeWaitAnimation) {
                changeWaitAnimation = false
                task.cancel()
                return
            }
            if (respawnTime > 0) {
                respawnTime--
                player.sendActionBar(
                    Component.text("리스폰 가능까지: $respawnTime (C): $coin"))
            } else {
                Bukkit.getScheduler().runTaskTimer(
                    TheFinals.instance,
                    PressToStartAnimation(),
                    0L, 10L
                )
                task.cancel()
            }
        }
    }

    private inner class WaitTeamRespawn : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (respawnTime > 0) {
                player.sendActionBar(
                    Component.text("팀 리스폰까지: $respawnTime"))
                respawnTime--
            } else {
                respawn(false)
                player.sendActionBar(Component.text())
                task.cancel()
            }
        }
    }
    fun startTeamRespawnTask() {
        changeWaitAnimation = true
        Bukkit.getScheduler().runTaskTimer(
            TheFinals.instance,
            WaitTeamRespawn(),
            0L, 20L
        )
    }

    private inner class WaitReviveFromTeam : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (!isDead || changeWaitAnimation) {
                changeWaitAnimation = false
                player.resetTitle()
                task.cancel()
                return
            }
            player.sendActionBar(Component.text("부활이나 팀 전멸을 기다리세요"))
        }
    }

    private inner class PressToStartAnimation : Consumer<BukkitTask> {
        private var colorIndex = 0
            set(value) {
                field = value % rainbowColors.lastIndex
            }
        override fun accept(task: BukkitTask) {
            if (!isDead || changeWaitAnimation) {
                changeWaitAnimation = false
                player.resetTitle()
                task.cancel()
                return
            }
            player.showTitle(Title.title(
                Component.text(""),
                Component.text("SNEAK START")
                    .decorate(TextDecoration.BOLD).color(rainbowColors[colorIndex++]),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
            ))
            player.sendActionBar(Component.text("크레딧 $coin"))
        }
    }
}
