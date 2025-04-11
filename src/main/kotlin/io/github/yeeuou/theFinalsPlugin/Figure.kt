package io.github.yeeuou.theFinalsPlugin

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta

class Figure(
    val owner: TFPlayer
) {
    companion object {
        val spawnedFigures = mutableMapOf<ArmorStand, Figure>()
        fun ArmorStand.figure() = spawnedFigures[this]
    }

    private var figureEntity: ArmorStand? = null

    val figureLoc
        get() = figureEntity?.location

    fun spawn() {
        if (!owner.isDead) return
        val color = Color.fromRGB(owner.tfTeam.color.value())
        figureEntity = owner.player.run {
            world.spawn(location, ArmorStand::class.java) {
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
            }
        }
        spawnedFigures[figureEntity!!] = this
    }

    fun remove() {
        if (!owner.isDead) return
        spawnedFigures.remove(figureEntity)
        figureEntity?.remove()
    }
}
