package io.github.yeeuou.theFinals

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
    private val head = ItemStack(Material.PLAYER_HEAD).apply {
        val meta = (itemMeta as SkullMeta)
        meta.owningPlayer = owner.player
        itemMeta = meta
    }

    private val figureEntity: ArmorStand = owner.player.run {
        world.spawn(location, ArmorStand::class.java) {
            it.isSmall = true
            it.isInvulnerable = true
            it.setDisabledSlots(
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
            )
            it.equipment.run {
                val meta = ItemStack(Material.LEATHER_HELMET).itemMeta as LeatherArmorMeta
                helmet = head
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
            it.remove()
        }
    }

    fun spawn() {
        if (!owner.isDead) return
        spawnedFigures[figureEntity] = this
        val figureColor = Color.fromRGB(owner.tfTeam.color.value())
        owner.player.run {
            figureEntity.spawnAt(location.setRotation(0f, location.pitch))
            figureEntity.equipment.run {
//                (chestplate.itemMeta as LeatherArmorMeta).setColor(figureColor)
//                (leggings.itemMeta as LeatherArmorMeta).setColor(figureColor)
                (boots.itemMeta as LeatherArmorMeta).setColor(figureColor)
            }
        }
    }
    fun remove() {
        if (!owner.isDead) return
        spawnedFigures.remove(figureEntity)
        figureEntity.remove()
    }
}
