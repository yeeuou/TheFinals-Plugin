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
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.time.Duration
import java.util.function.Consumer
import kotlin.random.Random

class TFPlayer private constructor (
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

        val playerByPlayers = mutableMapOf<Player, TFPlayer>()
        fun Player.tfPlayer() = playerByPlayers[this]
        fun registerOrUpdatePlayer(p: Player, team: TFTeam) {
            p.tfPlayer()?.run {
                changeTeam(team)
                return
            }
            TFPlayer(p, team)
        }

        val playerDataFolder = File(TFPlugin.instance.dataFolder, "players")

        private const val KEY_ROOT = "TFPlayer"
        private const val KEY_TEAM = "tfTeam"
        private const val KEY_COIN = "coin"
        private const val KEY_RESPAWN_TIME = "respawnTime"
        private const val KEY_DEAD = "dead"

        // 플레이어가 관전중인 플레이어 기록
        val spectatorPlayers = mutableMapOf<TFPlayer, TFPlayer>()
        fun TFPlayer.getSpectatePlayer() = spectatorPlayers[this]

        val playerGrabFigure = mutableMapOf<TFPlayer, Figure>()

        fun tryLoad(player: Player) {
            val file = File(playerDataFolder, "${player.uniqueId}.yml")
            if (!file.exists()) return

            val yaml = YamlConfiguration.loadConfiguration(file)
            val root = requireNotNull(yaml.getConfigurationSection(KEY_ROOT))
            { "$file: Unknown key $KEY_ROOT" }
            val teamName = requireNotNull(root.getString(KEY_TEAM))
            { "$file: Unknown key $KEY_TEAM" }.lowercase()
            val tfTeam = TFTeam.nameByTeam[teamName]
                ?: throw IllegalArgumentException("Not found team name: $teamName")
            val coin = root.getInt(KEY_COIN, TFConfig.startCoin)
            val respawnTime = root.getInt(KEY_RESPAWN_TIME)
            val dead =
                if (root.isBoolean(KEY_DEAD)) root.getBoolean(KEY_DEAD)
                else throw IllegalArgumentException("$file: Unknown key $KEY_DEAD")

            TFPlayer(player, tfTeam).apply {
                this.coin = coin
                this.respawnTime = if (respawnTime > 10) respawnTime else 10
                this.isDead = dead
                initializeOnLoaded()
            }
        }
    }

    var tfTeam = tfTeam
        private set

    private var coin: Int = TFConfig.startCoin

    private var respawnTime = 0

    val canRespawn
        get() = respawnTime <= 0 && coin > 0

    var isDead = false
        private set

    var waitTeamRespawn = false
        private set

    private val figure = Figure(this)

    val grabFigure: Figure?
        get() = playerGrabFigure[this]

    init { // 등록
        playerByPlayers[player] = this
        tfTeam.addPlayer(this)
    }

    // 로드시 죽은 상태등을 처리
    fun initializeOnLoaded() {
        if (isDead) {
            if (coin <= 0)
                Bukkit.getScheduler().runTaskTimer(
                    TFPlugin.instance,
                    WaitReviveFromTeam(),
                    0, 1
                )
            else
                Bukkit.getScheduler().runTaskTimer(
                    TFPlugin.instance,
                    DiedOnLoad(),
                    0, 20
                )
        }
    }

    fun unload() {
        if (grabFigure != null) putDownFigure()
        save()
        tfTeam.removePlayer(this)
        playerByPlayers.remove(player)
        if (isDead) {
            figure.remove()
            spectatorPlayers.remove(this)
        }
        isDead = false // remove remaining tasks
    }

    private fun save() {
        val file = File(playerDataFolder, "${player.uniqueId}.yml")
            .also { it.parentFile.mkdirs() }
        val yaml = YamlConfiguration()
        yaml.createSection(KEY_ROOT).let {
            it[KEY_TEAM] = tfTeam.name
            it[KEY_COIN] = coin
            it[KEY_RESPAWN_TIME] = respawnTime
            it[KEY_DEAD] = isDead
        }
        yaml.save(file)
    }

    fun unregister() {
        unload()
        File(playerDataFolder, "${player.uniqueId}.yml").delete()
    }

    fun addCoin(add: Int = 1) {
        coin += add
    }

    fun changeTeam(to: TFTeam) {
        tfTeam.removePlayer(this)
        tfTeam = to
        to.addPlayer(this)
    }

    fun playDeadEffect() {
        player.gameMode = GameMode.SPECTATOR
        player.world.spawnParticle(
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
            .5f, .8f
        )
        isDead = true
        if (tfTeam.isAllPlayerDead())
            tfTeam.playTeamWipeEffect()
        else {
            figure.spawn()
            Bukkit.getScheduler().runTaskTimer(
                TFPlugin.instance,
                SpectateOnTeam(),
                20 * 3, 1
            )
            if (coin > 0) {
                respawnTime = TFConfig.playerRespawnTick - 20
                Bukkit.getScheduler().runTaskTimer(
                    TFPlugin.instance,
                    WaitRespawnTime(),
                    0, 1
                )
            } else Bukkit.getScheduler().runTaskTimer(
                TFPlugin.instance,
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
                x += it.nextInt(-3, 3)
                z += it.nextInt(-3, 3)
            }
        }.toCenterLocation().toHighestLocation().add(0.0,1.0,0.0)
        playerRespawnProcess(respawnLoc)
    }

    fun reviveFromFigure() {
        if (!isDead) return // if late respawn
        val loc = requireNotNull(figure.figureLoc) { "No revive without figure!" }
            .apply { pitch = 0f }
        figure.remove()
        isDead = false
        playerRespawnProcess(loc)
    }

    fun lateRespawn() {
        isDead = false
        figure.remove()
        Bukkit.getScheduler().runTaskLater(
            TFPlugin.instance,
            { -> respawn(true) },
            10
        )
    }

    private fun playerRespawnProcess(loc: Location) {
        spectatorPlayers.remove(this)
        player.spectatorTarget = null
        player.gameMode = GameMode.SURVIVAL
        player.teleport(loc)
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 350)
        player.playSound(player, Sound.ITEM_TOTEM_USE, .25f, 1.6f)
        player.noDamageTicks = 20 * 2
        player.foodLevel = 20
        player.saturation = 5f
        player.arrowsInBody = 0
        player.beeStingersInBody = 0
    }

    internal fun grab(figure: Figure) {
        playerGrabFigure[this] = figure
    }

    fun throwFigure() {
        if (grabFigure == null) throw IllegalStateException("Grab figure is null.")
        grabFigure!!.throwIt(player.eyeLocation, player.eyeLocation.direction.clone())
        playerGrabFigure.remove(this)
    }

    fun putDownFigure() {
        if (grabFigure == null) throw IllegalStateException("Grab figure is null.")
        grabFigure!!.putDownIt(player.eyeLocation)
        playerGrabFigure.remove(this)
    }

    private inner class WaitRespawnTime : Consumer<BukkitTask> {
        private var tick = 0
        override fun accept(task: BukkitTask) {
            if (!isDead || waitTeamRespawn) {
                task.cancel()
                return
            }
            if (tick++ % 20 != 0) return
            if (respawnTime > 0)
                player.sendActionBar(
                    Component.text("리스폰 가능까지: ${respawnTime--} (C): $coin")
                )
            else {
                Bukkit.getScheduler().runTaskTimer(
                    TFPlugin.instance,
                    PressStartTask(),
                    0, 1
                )
                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, .3f, 1f)
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

    /** 매 초마다 업데이트 */
    private inner class WaitTeamRespawn : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (!isDead) {
                task.cancel()
                return
            }
            if (respawnTime > 0)
                player.sendActionBar(
                    Component.text("팀 리스폰까지: ${respawnTime--}"))
            else {
                waitTeamRespawn = false
                respawn(false)
                player.sendActionBar(Component.text())
                task.cancel()
            }
        }
    }
    internal fun startTeamRespawnTask() {
        respawnTime = TFConfig.teamRespawnTick - 5
        waitTeamRespawn = true
        spectatorPlayers.remove(this)
        player.spectatorTarget = null
        figure.remove()
        Bukkit.getScheduler().runTaskTimer(
            TFPlugin.instance,
            WaitTeamRespawn(),
            0, 20
        )
    }

    /** 초마다 업데이트 */
    private inner class DiedOnLoad : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (respawnTime > 0)
                player.sendActionBar(Component.text("부활까지: ${respawnTime--}"))
            else {
                respawn(false)
                task.cancel()
            }
        }
    }

    private inner class PressStartTask : Consumer<BukkitTask> {
        private var colorIndex = 0
            set(value) {
                field = value % rainbowColors.lastIndex
            }
        private var tick = 0

        override fun accept(task: BukkitTask) {
            if (!isDead || waitTeamRespawn) {
                if (!waitTeamRespawn) { // 팀 전멸 타이틀을 지우지 않도록 처리
                    player.resetTitle()
                    player.sendActionBar(Component.text())
                }
                task.cancel()
                return
            }
            if (tick++ % 3 != 0) return
            player.showTitle(Title.title(
                Component.text(""),
                Component.text("PRESS START")
                    .decorate(TextDecoration.BOLD).color(rainbowColors[colorIndex++]),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
            ))
            player.sendActionBar(
                Component.text()
                    .append(
                        Component.text("[웅크리기로 부활] ")
                            .decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY)
                    ).append(Component.text("크레딧 $coin"))
            )
        }
    }

    internal fun spectator(p: TFPlayer) {
        if (!isDead) return
        spectatorPlayers.remove(this)
        player.spectatorTarget = null
        spectatorPlayers[this] = p
        player.spectatorTarget = p.player
    }

    private inner class SpectateOnTeam : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (!isDead || waitTeamRespawn) {
                task.cancel()
                return
            }
            if (getSpectatePlayer() == null)
                spectatorPlayers[this@TFPlayer] = tfTeam.getFirstAlivePlayer()
            val spectatePlayer = getSpectatePlayer()!!
            if (player.spectatorTarget == null) {
                spectator(spectatePlayer)
                return
            }
            // re-attach player
            if (player.location != spectatePlayer.player.location)
                spectator(spectatePlayer)
        }
    }
}
