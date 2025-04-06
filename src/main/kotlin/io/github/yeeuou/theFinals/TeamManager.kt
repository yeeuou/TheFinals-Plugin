package io.github.yeeuou.theFinals

import org.bukkit.entity.Player

object TeamManager {
    const val TAG_JOINED = "tf_joined"
    const val TAG_DUMMY = "tfDummy"

//    val activePlayerTeam = mutableMapOf<Player, TFTeam>()

    val playerByPlayers = mutableMapOf<Player, TFPlayer>()

//    fun Player.tfActivated(t: TFTeam) {
//        activePlayerTeam[this] = t
//    }
//    fun Player.tfInactive() {
//        activePlayerTeam.remove(this)
//    }
    fun Player.tfPlayer() = playerByPlayers[this]

    fun registerTFPlayer(p: TFPlayer) {
        playerByPlayers[p.player] = p
    }
    fun registerNewPlayer(player: Player, tfTeam: TFTeam) {
        player.tfPlayer()?.run {
            changeTeam(tfTeam)
            return
        }
        registerTFPlayer(TFPlayer(player, tfTeam))
    }
    fun TFPlayer.unregisterPlayer() {
        playerByPlayers.remove(player)
        tfTeam.removePlayer(this)
    }
}
