package io.github.yeeuou.theFinals

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import java.util.function.Consumer

class DummyPlayer(
    val entity: LivingEntity
) {
    companion object {
        val entityByDummy = mutableMapOf<LivingEntity, DummyPlayer>()
//        fun newDummyPlayer(entity: LivingEntity) {
//            DummyPlayer(entity)
//        }
        val figures = mutableMapOf<ArmorStand, DummyPlayer>()
        fun ArmorStand.asDummyFigure() = figures[this]
    }
    init {
        entityByDummy[entity] = this
    }
    private var dummyFigure: ArmorStand? = null

    val figureUUID
        get() = dummyFigure?.uniqueId

    var isDead = false
        private set

    var reviving = false

    fun playDeadEffect() {
        isDead = true
        dummyFigure = entity.run {
            world.spawn(location, ArmorStand::class.java) {
                it.isSmall = true
                it.isInvulnerable = true
                it.setDisabledSlots(
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET
                )
                it.equipment.run {
                    val meta = ItemStack(Material.LEATHER_HELMET).itemMeta as LeatherArmorMeta
                    meta.setColor(Color.RED)
                    helmet = ItemStack(Material.LEATHER_HELMET).apply {
                        itemMeta = meta
                    }
                    chestplate = ItemStack(Material.LEATHER_CHESTPLATE).apply {
                        itemMeta = meta
                    }
                    leggings = ItemStack(Material.LEATHER_LEGGINGS).apply {
                        itemMeta = meta
                    }
                    boots = ItemStack(Material.LEATHER_BOOTS).apply {
                        itemMeta = meta
                    }
                }
            }
        }
        figures[dummyFigure!!] = this
        entity.world.spawnParticle(
            Particle.BLOCK,
            entity.location.add(0.0 ,1.0, 0.0),
            200,
            .2,
            .45,
            .2,
            Material.GOLD_BLOCK.createBlockData()
        )
        entity.world.playSound(
            entity.location, Sound.BLOCK_CHAIN_BREAK,
            .5f, 1f
        )
        Bukkit.getScheduler().runTaskLater(
            TheFinals.instance,
            DelayRespawnAction(),
            100
        )
        entity.remove()
    }

    fun respawn() {
        entityByDummy.remove(entity)
        dummyFigure?.remove()
        entity.world.spawn(dummyFigure?.location ?: entity.location,
            Pillager::class.java) {
            it.setAI(false)
            it.setGravity(false)
//            it.addScoreboardTag(TeamManager.TAG_DUMMY)
            it.noDamageTicks = 30
            it.equipment.setItemInMainHand(null)
            DummyPlayer(it)
        }
    }

    private inner class DelayRespawnAction : Consumer<BukkitTask> {
        override fun accept(task: BukkitTask) {
            if (!isDead) return
            if (reviving) {
                Bukkit.getScheduler().runTaskLater(
                    TheFinals.instance, DelayRespawnAction(), 1
                )
            } else respawn()
        }
    }
}

class DummyFigureRevive(
    val player: Player,
    val figure: DummyPlayer
) : Consumer<BukkitTask> {
    private var progress = 0
    private val maxProgress = 100
    override fun accept(task: BukkitTask) {
        val target = (player.getTargetEntity(3) as? ArmorStand)
        if (target == null || target.uniqueId != figure.figureUUID) {
            player.removeMetadata("tf_holdRevive", TheFinals.instance)
            player.resetTitle()
            figure.reviving = false
            task.cancel()
            return
        }
        if (!player.isSneaking) {
            if (progress > 0) progress = 0
            figure.reviving = false
            player.showTitle(
                Title.title(
                Component.text(""),
                Component.text("[길게 웅크려서 부활]"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
            ))
        } else {
            figure.reviving = true
            val sb = StringBuilder("[")
            for (i in 1..20)
                // 진행 바가 끝까지 도달하게 함
                if (i * 5 <= progress + 1) sb.append('=')
                else sb.append(' ')
            player.showTitle(Title.title(
                Component.text(""), Component.text("$sb]"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
            ))
            if (progress >= maxProgress) {
                figure.respawn()
                player.removeMetadata("tf_holdRevive", TheFinals.instance)
                player.resetTitle()
                task.cancel()
            }
            else progress++
        }
    }
}
