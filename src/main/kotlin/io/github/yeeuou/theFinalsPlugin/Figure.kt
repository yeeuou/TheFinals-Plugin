package io.github.yeeuou.theFinalsPlugin

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import kotlin.math.max

class Figure(
    val owner: TFPlayer
) {
    companion object {
        val spawnedFigures = mutableMapOf<ArmorStand, Figure>()
        fun ArmorStand.figure() = spawnedFigures[this]
    }

    private var figureEntity: ArmorStand? = null

    private var overlapHandler: HandleFigureOverlapped? = null

    val figureLoc
        get() = figureEntity?.location

    fun spawn() {
        if (!owner.isDead) return
        val color = Color.fromRGB(owner.tfTeam.color.value())
        figureEntity = owner.player.run {
            world.spawn(location.setRotation(yaw, 0f), ArmorStand::class.java) {
                it.isSmall = true
                it.isInvulnerable = true
                it.setDisabledSlots(
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET
                )
                it.equipment.run {
                    val meta = ItemStack(Material.LEATHER_HELMET).itemMeta as LeatherArmorMeta
                    meta.setColor(color)
                    helmet = ItemStack(Material.PLAYER_HEAD).apply {
                        val skullMeta = (itemMeta as SkullMeta)
                        skullMeta.owningPlayer = owner.player
                        itemMeta = skullMeta
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
//                it.boundingBox.expand(.25, .0, .25)
            }
        }
        spawnedFigures[figureEntity!!] = this
        overlapHandler = HandleFigureOverlapped(figureEntity!!).apply {
            startTask()
        }
//        owner.tfTeam.addFigure(figureEntity!!)
    }

    fun remove() {
//        if (!owner.isDead) return
        overlapHandler?.cancelTask()
        spawnedFigures.remove(figureEntity)
        figureEntity?.remove()
        figureEntity = null
    }

    internal class HandleFigureOverlapped(private val armorStand: ArmorStand) {
        private lateinit var task: BukkitTask
        fun startTask() {
            task = Bukkit.getScheduler().runTaskTimer(
                TheFinalsPlugin.instance,
                Task(), 0, 1
            )
        }

        fun cancelTask() {
            task.cancel()
        }

        private inner class Task : Runnable {
            override fun run() {
                if (!armorStand.isValid) {
                    task.cancel()
                    return
                }
                val isInLiquid = armorStand.isInLava || armorStand.isInWater
                if (isInLiquid) {
                    armorStand.velocity = Vector(.0, .1, .0)
                }
                if (armorStand.world.minHeight - 3 >= armorStand.y)
                    armorStand.velocity = Vector(.0, 1.2, .0)
                val blocks = buildList {
                    armorStand.boundingBox.let { box ->
                        armorStand.location.let {
                            /*
                            1 2 -X
                            3 4
                            Z*/
                            add(it.set(box.minX, box.minY, box.minZ).block) // 1
                            add(it.set(box.maxX, box.minY, box.minZ).block) // 2
                            add(it.set(box.minX, box.minY, box.maxZ).block) // 3
                            add(it.set(box.maxX, box.minY, box.maxZ).block) // 4
                        }
                    }
                }
                val overlapped = blocks.filter {
                    armorStand.boundingBox.overlaps(it.boundingBox) }

                if (overlapped.isNotEmpty()) {
//                    armorStand.addPotionEffect(PotionEffectType.GLOWING
//                        .createEffect(5,0))
                    overlapped.map {
                        armorStand.boundingBox.center
                            .subtract(it.location.toCenterLocation().toVector())
                            .normalize()
                    }.fold(Vector(0, 0, 0)) { acc, vec ->
                        acc.add(vec)
                    }.let {
                        it.y = max(it.y, .4)
                        armorStand.velocity = it.normalize().multiply(.175)
                    }
                    // 위가 막혔을 때
                    armorStand.location.toBlockLocation().let {
                        it.y++
                        if (it.block.isCollidable)
                            armorStand.teleport(armorStand
                                .location.add(.0, .1, .0))
                    }
                }
            }
        }
    }
}
