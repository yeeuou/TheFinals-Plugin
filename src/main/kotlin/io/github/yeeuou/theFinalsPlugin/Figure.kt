package io.github.yeeuou.theFinalsPlugin

import io.github.yeeuou.theFinalsPlugin.task.GrabFigureTask
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
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
        private val spawnedFigures = mutableMapOf<ArmorStand, Figure>()
        fun ArmorStand.figure() = spawnedFigures[this]
    }

    private var figureEntity: ArmorStand? = null

    private var overlapHandler: HandleFigureOverlapped? = null

    val figureLoc
        get() = figureEntity?.location

    internal fun spawn() {
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
        overlapHandler = HandleFigureOverlapped(this, figureEntity!!)
            .apply { startTask() }
//        owner.tfTeam.addFigure(figureEntity!!)
    }

    internal fun remove() {
//        if (!owner.isDead) return
        overlapHandler?.cancelTask()
        // 잡기 해제
        TFPlayer.playerGrabFigure.entries.find { it.value == this }
            ?.let { TFPlayer.playerGrabFigure.remove(it.key) }
        spawnedFigures.remove(figureEntity)
        figureEntity?.remove()
        figureEntity = null
    }

    fun startGrabTask(p: TFPlayer) {
        checkNotNull(figureEntity) { "No grab entity" }
        p.player.sendActionBar(Component.text()
            .append(Component.text('['),
                Component.keybind("key.mouse.left"),
                Component.text(']')
            ).append(Component.text(" 던지기 | "))
            .append(Component.text('['),
                Component.keybind("key.mouse.right"),
                Component.text(']')
            ).append(Component.text(" 놓기"))
        )
        Bukkit.getScheduler().runTaskTimer(
            TFPlugin.instance,
            GrabFigureTask(p, this, figureEntity!!),
            0, 1
        )
    }

    internal fun throwIt(eyeLoc: Location, vec: Vector) {
        if (figureEntity == null) return
        putDownIt(eyeLoc)
        figureEntity!!.velocity = vec.normalize()
    }

    internal fun putDownIt(eyeLoc: Location) {
        if (figureEntity == null) return
        eyeLoc.clone().let {
            figureEntity!!.teleport(it
                .add(it.direction.normalize().multiply(.75))
                .add(.0, -.6, .0)
                .apply { yaw -= 180 })
        }
    }

    private class HandleFigureOverlapped(
        private val figure: Figure,
        private val armorStand: ArmorStand
    ) {
        private lateinit var task: BukkitTask
        fun startTask() {
            task = Bukkit.getScheduler().runTaskTimer(
                TFPlugin.instance,
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
                if (figure in TFPlayer.playerGrabFigure.values) return
                val isInLiquid = armorStand.isInLava || armorStand.isInWater
                if (isInLiquid) {
                    armorStand.velocity = Vector(.0, .1, .0)
                }
                if (armorStand.world.minHeight - 3 >= armorStand.y)
                    armorStand.velocity = Vector(0, 1, 0)

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
                            add(it.block) // this position
                        }
                    }
                }
                val overlapped = blocks.filter {
                   it.isSolid && armorStand.boundingBox.overlaps(it.boundingBox) }

                if (overlapped.isNotEmpty()) {
//                    armorStand.addPotionEffect(PotionEffectType.GLOWING
//                        .createEffect(5,0))
                    // 위가 막혔을 때
                    armorStand.location.toBlockLocation().let {
                        it.y++
                        if (it.block.isCollidable)
                            armorStand.teleport(armorStand
                                .location.add(.0, .1, .0))
                    }
                    overlapped.map {
                        armorStand.boundingBox.center
                            .subtract(it.location.toCenterLocation().toVector())
                            .normalize()
                    }.fold(Vector()) { acc, vec ->
                        acc.add(vec)
                    }.let {
                        it.y = max(it.y, .4)
                        armorStand.velocity = it.normalize().multiply(.2)
//                        armorStand.world.spawnParticle(
//                            Particle.DUST,
//                            armorStand.location.add(armorStand.velocity),
//                            1, Particle.DustOptions(Color.RED, 1f)
//                        )
                    }
                }
            }
        }
    }
}
